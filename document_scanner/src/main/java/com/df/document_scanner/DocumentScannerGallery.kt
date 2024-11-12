package com.df.document_scanner

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import com.df.document_scanner.constants.DefaultSetting
import com.df.document_scanner.constants.DocumentScannerExtra
import com.df.document_scanner.constants.ResponseType
import com.df.document_scanner.extensions.toBase64
import com.df.document_scanner.utils.ImageUtil
import java.io.File

/**
 * This class is used to start a document scan. It accepts parameters used to customize
 * the document scan, and callback parameters.
 *
 * @param activity the current activity
 * @param successHandler event handler that gets called on document scan success
 * @param errorHandler event handler that gets called on document scan error
 * @param cancelHandler event handler that gets called when a user cancels the document scan
 * @param responseType the cropped image gets returned in this format
 * @param letUserAdjustCrop whether or not the user can change the auto detected document corners
 * @param maxNumDocuments the maximum number of documents a user can scan at once
 * @param croppedImageQuality the 0 - 100 quality of the cropped image
 * @constructor creates document scanner
 */
class DocumentScannerGallery(
    private val activity: ComponentActivity,
    private val successHandler: ((documentScanResults: ArrayList<String>, originalImages: ArrayList<Uri>, letUserAdjustCrop: Boolean) -> Unit)? = null,
    private val errorHandler: ((errorMessage: String) -> Unit)? = null,
    private val cancelHandler: (() -> Unit)? = null,
    private var responseType: String? = null,
    private var currentNumDocuments: Int? = null,
    private var maxNumDocuments: Int? = null,
    private var croppedImageQuality: Int? = null
) {
    init {
        responseType = responseType ?: DefaultSetting.RESPONSE_TYPE
        croppedImageQuality = croppedImageQuality ?: DefaultSetting.CROPPED_IMAGE_QUALITY
    }

    private var letUserAdjustCrop = false

    fun letUserAdjustCrop(letUserAdjustCrop: Boolean) {
        this.letUserAdjustCrop = letUserAdjustCrop
    }

    fun setCurrentNumDocuments(currentNumDocuments: Int) {
        this.currentNumDocuments = currentNumDocuments
    }

    /**
     * create intent to launch document scanner and set custom options
     */
    private fun createDocumentScanIntent(uri: Uri? = null): Intent {
        // Determine the target activity based on user adjustment preference
        val targetActivity = if (letUserAdjustCrop) {
            DocumentScannerSingleActivity::class.java
        } else {
            DocumentScannerMultiActivity::class.java
        }
        // Create the intent and add the common extras
        val documentScanIntent = Intent(activity, targetActivity).apply {
            putExtra(DocumentScannerExtra.EXTRA_CROPPED_IMAGE_QUALITY, croppedImageQuality)
            putExtra(DocumentScannerExtra.EXTRA_MAX_NUM_DOCUMENTS, maxNumDocuments)
            if (letUserAdjustCrop) {
                putExtra(DocumentScannerExtra.EXTRA_EDIT_PATH, uri)
            }
            putExtra(DocumentScannerExtra.EXTRA_CURRENT_NUM_DOCUMENTS, currentNumDocuments)
        }
//        documentScanIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        documentScanIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return documentScanIntent
    }

    /**
     * handle response from document scanner
     *
     * @param result the document scanner activity result
     */
    private fun handleDocumentScanIntentResult(result: ActivityResult) {
        try {
            // make sure responseType is valid
            if (!arrayOf(
                    ResponseType.BASE64,
                    ResponseType.IMAGE_FILE_PATH
                ).contains(responseType)
            ) {
                throw Exception(
                    "responseType must be either ${ResponseType.BASE64} " +
                            "or ${ResponseType.IMAGE_FILE_PATH}"
                )
            }

            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    // check for errors
                    val error = result.data?.extras?.get("error") as String?
                    if (error != null) {
                        throw Exception("error - $error")
                    }

                    // get an array with scanned document file paths
                    val croppedImageResults: ArrayList<String> =
                        result.data?.getStringArrayListExtra(
                            "croppedImageResults"
                        ) ?: throw Exception("No cropped images returned")

                    val originalImageResults: ArrayList<Uri> =
                        result.data?.getParcelableArrayListExtra("originalImageResults")
                            ?: throw Exception("No original images returned")

                    // if responseType is imageFilePath return an array of file paths
                    var successResponse: ArrayList<String> = croppedImageResults

                    // if responseType is base64 return an array of base64 images
                    if (responseType == ResponseType.BASE64) {
                        val base64CroppedImages =
                            croppedImageResults.map { croppedImagePath ->
                                // read cropped image from file path, and convert to base 64
                                val base64Image = ImageUtil().readBitmapFromFileUriString(
                                    croppedImagePath,
                                    activity.contentResolver
                                ).toBase64(croppedImageQuality!!)

                                // delete cropped image from android device to avoid
                                // accumulating photos
                                File(croppedImagePath).delete()

                                base64Image
                            }

                        successResponse = base64CroppedImages as ArrayList<String>
                    }

                    // trigger the success event handler with an array of cropped images
                    successHandler?.let {
                        it(
                            successResponse,
                            originalImageResults,
                            letUserAdjustCrop
                        )
                    }
                }

                Activity.RESULT_CANCELED -> {
                    // user closed camera
                    cancelHandler?.let { it() }
                }
            }
        } catch (exception: Exception) {
            // trigger the error event handler
            errorHandler?.let { it(exception.localizedMessage ?: "An error happened") }
        }
    }

    /**
     * add document scanner result handler and launch the document scanner
     */
    fun start(uri: Uri? = null) {
        val intent = createDocumentScanIntent(uri)
        if (letUserAdjustCrop) {
            activity.startActivityForResult(intent, 101)
        } else {
            activity.startActivityForResult(intent, 100)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handleDocumentScanIntentResult(ActivityResult(resultCode, data))
    }
}

