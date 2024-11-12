package com.df.document_scanner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.df.document_scanner.constants.DefaultSetting
import com.df.document_scanner.constants.DocumentScannerExtra
import com.df.document_scanner.extensions.move
import com.df.document_scanner.extensions.saveToFile
import com.df.document_scanner.loading.LoadingViewModel
import com.df.document_scanner.models.DataUri
import com.df.document_scanner.models.Document
import com.df.document_scanner.models.Quad
import com.df.document_scanner.utils.FileUtil
import com.df.document_scanner.utils.ImageUtil
import com.df.document_scanner.utils.PhotoPickerUtil
import kotlinx.coroutines.*
import org.opencv.core.Point

/**
 * This class contains the main document scanner code. It opens the camera, lets the user
 * take a photo of a document (homework paper, business card, etc.), detects document corners,
 * allows user to make adjustments to the detected corners, depending on options, and saves
 * the cropped document. It allows the user to do this for 1 or more documents.
 *
 * @constructor creates document scanner activity
 */
class DocumentScannerMultiActivity : AppCompatActivity() {
    /**
     * @property maxNumDocuments maximum number of documents a user can scan at a time
     */
    private var maxNumDocuments = DefaultSetting.MAX_NUM_DOCUMENTS

    /**
     * @property croppedImageQuality the 0 - 100 quality of the cropped image
     */
    private var croppedImageQuality = DefaultSetting.CROPPED_IMAGE_QUALITY

    /**
     * @property cropperOffsetWhenCornersNotFound if we can't find document corners, we set
     * corners to image size with a slight margin
     */
    private val cropperOffsetWhenCornersNotFound = 100.0

    /**
     * @property document This is the current document. Initially it's null. Once we capture
     * the photo, and find the corners we update document.
     */
    private var document: Document? = null

    /**
     * @property documents a list of documents (original photo file path, original photo
     * dimensions and 4 corner points)
     */
    private val documents = mutableListOf<Document>()

    /**
     * @property cameraUtil gets called with photo file path once user takes photo, or
     * exits camera
     */

    private lateinit var progressBar: ProgressBar

    private var currentItems: Int = 0

    private val photoPickerUtil = PhotoPickerUtil(this, onPhotoSuccess = { photoUris ->
//        if (currentItems + photoUris.count() > maxNumDocuments) {
//            Toast.makeText(this, "Chỉ chọn tối đa $maxNumDocuments hình", Toast.LENGTH_SHORT).show()
//            onClickCancel()
//            return@PhotoPickerUtil
//        }

            // Use coroutines for parallel processing
            runBlocking {
                photoUris.map { photoPath ->
                    async(Dispatchers.IO) {
                        processPhoto(photoPath)
                    }
                }.awaitAll() // Wait for all coroutines to complete
            }

            cropDocumentAndFinishIntent()
        },
            onCancelPhoto = {
                if (documents.isEmpty()) {
                    onClickCancel()
                }
            }
        )

    // Function to process a single photo
    private suspend fun processPhoto(photoUri: DataUri) {
        // Get bitmap from photo file path
        val photo: Bitmap = ImageUtil().getImageFromFilePath(photoUri.path!!)

        // Get document corners by detecting them, or falling back to photo corners with slight margin if we can't find the corners
        val corners = try {
            val (topLeft, topRight, bottomLeft, bottomRight) = getDocumentCorners(photo)
            Quad(topLeft, topRight, bottomRight, bottomLeft)
        } catch (exception: Exception) {
            withContext(Dispatchers.Main) {
                finishIntentWithError("unable to get document corners: ${exception.message}")
            }
            return
        }

        // Update document details
        document = Document(photoUri.uri, photoUri.path, photo.width, photo.height, corners)
        // User isn't allowed to move corners, so accept automatically detected corners
        document?.let { document ->
            documents.add(document)
        }
    }


    /**
     * @property imageView container with original photo and cropper
     */

    private val viewModel: LoadingViewModel by viewModels()

    /**
     * called when activity is created
     *
     * @param savedInstanceState persisted data that maintains state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread {
            try {
                System.loadLibrary("opencv_java4")
            } catch (exception: Exception) {
                runOnUiThread {
                    finishIntentWithError("Error starting OpenCV: ${exception.message}")
                }
            }
        }.start()

        setContentView(R.layout.activity_scanner_multi)
        progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Observe the loading state
        viewModel.isLoading.observe(this, Observer { isLoading ->
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
        })

        try {
            // validate maxNumDocuments option, and update default if user sets it
            var userSpecifiedMaxImages: Int? = null
            intent.extras?.get(DocumentScannerExtra.EXTRA_MAX_NUM_DOCUMENTS)?.let {
                if (it.toString().toIntOrNull() == null) {
                    throw Exception(
                        "${DocumentScannerExtra.EXTRA_MAX_NUM_DOCUMENTS} must be a positive number"
                    )
                }
                userSpecifiedMaxImages = it as Int
                maxNumDocuments = userSpecifiedMaxImages as Int
            }

            // validate letUserAdjustCrop option, and update default if user sets it
            viewModel.showLoading()

            // validate croppedImageQuality option, and update value if user sets it
            intent.extras?.get(DocumentScannerExtra.EXTRA_CROPPED_IMAGE_QUALITY)?.let {
                if (it !is Int || it < 0 || it > 100) {
                    throw Exception(
                        "${DocumentScannerExtra.EXTRA_CROPPED_IMAGE_QUALITY} must be a number " +
                                "between 0 and 100"
                    )
                }
                croppedImageQuality = it
            }

            intent.extras?.getInt(DocumentScannerExtra.EXTRA_CURRENT_NUM_DOCUMENTS)?.let {
                currentItems = it
            }

        } catch (exception: Exception) {
            finishIntentWithError(
                "invalid extra: ${exception.message}"
            )
            return
        }


        // open camera, so user can snap document photo
        try {
            openGallery()
        } catch (exception: Exception) {
            finishIntentWithError(
                "error opening camera: ${exception.message}"
            )
        }


    }

    /**
     * Pass in a photo of a document, and get back 4 corner points (top left, top right, bottom
     * right, bottom left). This tries to detect document corners, but falls back to photo corners
     * with slight margin in case we can't detect document corners.
     *
     * @param photo the original photo with a rectangular document
     * @return a List of 4 OpenCV points (document corners)
     */
    private fun getDocumentCorners(photo: Bitmap): List<Point> {
        val cornerPoints: List<Point>? = DocumentDetector().findDocumentCorners(photo)

        // if cornerPoints is null then default the corners to the photo bounds with a margin
        return cornerPoints ?: listOf(
            Point(0.0, 0.0).move(
                cropperOffsetWhenCornersNotFound,
                cropperOffsetWhenCornersNotFound
            ),
            Point(photo.width.toDouble(), 0.0).move(
                -cropperOffsetWhenCornersNotFound,
                cropperOffsetWhenCornersNotFound
            ),
            Point(0.0, photo.height.toDouble()).move(
                cropperOffsetWhenCornersNotFound,
                -cropperOffsetWhenCornersNotFound
            ),
            Point(photo.width.toDouble(), photo.height.toDouble()).move(
                -cropperOffsetWhenCornersNotFound,
                -cropperOffsetWhenCornersNotFound
            )
        )
    }

    /**
     * Set document to null since we're capturing a new document, and open the camera. If the
     * user captures a photo successfully document gets updated.
     */
    private fun openGallery() {
        document = null
        photoPickerUtil.openPhotoPicker(maxNumDocuments - currentItems)
    }

    /**
     * This gets called when a user doesn't want to complete the document scan after starting.
     * For example a user can quit out of the camera before snapping a photo of the document.
     */
    private fun onClickCancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun cropDocumentAndFinishIntent() {
        val croppedImageResults = arrayListOf<String>()

        // Use coroutines to process each document concurrently
        runBlocking {
            documents.mapIndexed { pageNumber, document ->
                async(Dispatchers.IO) {
                    processAndSaveCroppedImage(document, pageNumber)
                }
            }.awaitAll().forEach { result ->
                result?.let { croppedImageResults.add(it) }
            }
        }

        // Return array of cropped document photo file paths
        val intent = Intent()
        val originalImages = documents.map { it.uri }.toCollection(ArrayList())
        intent.putExtra("croppedImageResults", croppedImageResults)
        intent.putExtra("originalImageResults", originalImages)
        setResult(
            Activity.RESULT_OK,
            intent
        )
        finish()
    }

    // Function to process and save the cropped image
    private suspend fun processAndSaveCroppedImage(document: Document, pageNumber: Int): String? {
        // Crop document photo by using corners
        val croppedImage: Bitmap = try {
            ImageUtil().crop(document.originalPhotoFilePath, document.corners)
        } catch (exception: Exception) {
            withContext(Dispatchers.Main) {
                finishIntentWithError("unable to crop image: ${exception.message}")
            }
            return null
        }

        // Delete original document photo
//        File(document.originalPhotoFilePath).delete()

        // Save cropped document photo
        return try {
            val croppedImageFile = FileUtil().createImageFile(this, pageNumber)
            croppedImage.saveToFile(croppedImageFile, croppedImageQuality)
            Uri.fromFile(croppedImageFile).toString()
        } catch (exception: Exception) {
            withContext(Dispatchers.Main) {
                finishIntentWithError("unable to save cropped image: ${exception.message}")
            }
            null
        } finally {
            // Recycle the bitmap to free up memory
            croppedImage.recycle()
        }
    }


    /**
     * This ends the document scanner activity, and returns an error message that can be
     * used to debug error
     *
     * @param errorMessage an error message
     */
    private fun finishIntentWithError(errorMessage: String) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra("error", errorMessage)
        )
        finish()
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("WrongConstant")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val selectedUri: Uri = data?.data ?: return
            val takeFlags =
                data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            contentResolver.takePersistableUriPermission(selectedUri, takeFlags)
        }
    }
}