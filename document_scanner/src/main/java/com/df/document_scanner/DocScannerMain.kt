package com.df.document_scanner

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.df.document_scanner.adapter.GalleryAdapter
import com.df.document_scanner.fragment.ViewPagerAdapter
import com.df.document_scanner.utils.FileUtil

/**
 * A demo showing how to use the document scanner
 *
 * @constructor creates demo activity
 */
class DocScannerMain : AppCompatActivity() {
    /**
     * @property croppedImageView the cropped image view
     */
    // Initialize UI elements
    private lateinit var doneButton: Button
    private lateinit var cropRotateButton: View
    private lateinit var deleteButton: View
    private lateinit var addButton: View
    private lateinit var viewPager: ViewPager2
    private var listImageCropped: ArrayList<String> = ArrayList()
    private var listImageOriginal: ArrayList<Uri> = ArrayList()
    private var selectedPosition: Int = 0
    private lateinit var galleryRecyclerView: RecyclerView
    private var galleryAdapter: GalleryAdapter? = null
    private var viewPagerAdapter: ViewPagerAdapter? = null
    private lateinit var pageNumberText: TextView
    private lateinit var progressDialog: ProgressDialog
    private var maxNumDocuments: Int = 10

    /**
     * @property documentScanner the document scanner
     */


    private var documentScanner: DocumentScanner? = null

    /**
     * Called when activity is created
     *
     * @param savedInstanceState persisted data that maintains state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.doc_scanner_main_activity)
        // Initialize UI elements
        initializeUI()

        documentScanner = DocumentScanner(
            this,
            { croppedImageResults, originalImageResults, letUserAdjustCrop ->
                if (letUserAdjustCrop) {
                    listImageCropped.removeAt(selectedPosition)
                    listImageCropped.add(selectedPosition, croppedImageResults.first())
                    viewPagerAdapter?.update(selectedPosition, croppedImageResults.first())
                    galleryAdapter?.notifyDataSetChanged()

                } else {
                    handleDocumentScanSuccess(croppedImageResults, originalImageResults)
                }
            },
            { errorMessage ->
                Log.v("documentscannerlogs", errorMessage)
            },
            {
                Log.v("documentscannerlogs", "User canceled document scan")
                setResult(Activity.RESULT_CANCELED)
                finish()
            }, maxNumDocuments = maxNumDocuments, currentNumDocuments = 0
        )
        // Start document scan
        documentScanner!!.start()
    }

    private fun initializeUI() {
        // Initialize ViewPager and RecyclerView
        viewPager = findViewById(R.id.viewPager)
        galleryRecyclerView = findViewById(R.id.galleryRecyclerView)
        galleryRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Initialize buttons and set click listeners
        doneButton = findViewById(R.id.doneButton)
        cropRotateButton = findViewById(R.id.cropRotateButton)
        deleteButton = findViewById(R.id.deleteButton)
        addButton = findViewById(R.id.addButton)
        pageNumberText = findViewById(R.id.pageNumberText)
        pageNumberText.text = "1/${listImageCropped.size}"
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Đang tạo file PDF, vui lòng chờ...")
        progressDialog.setCancelable(false)

        intent.extras?.getInt("maxItems").let {
            if (it != null) {
                maxNumDocuments = it
            }
        }


        doneButton.setOnClickListener {
            // Show loading dialog
            progressDialog.show()

            // Perform PDF creation in a background thread to avoid blocking the UI thread
            Thread {
                val pdfPath = FileUtil().createPdfFromImages(
                    applicationContext,
                    listImageCropped,
                    "doc_scanner_pdf_output_${System.currentTimeMillis()}.pdf"
                )

                runOnUiThread {
                    // Dismiss loading dialog
                    progressDialog.dismiss()

                    // Handle the result
                    val intent = Intent()
                    intent.putExtra("path", pdfPath)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }.start()
        }


        cropRotateButton.setOnClickListener {
            documentScanner?.letUserAdjustCrop(true)
            documentScanner?.start(listImageOriginal[selectedPosition])//listImageOriginal[selectedPosition]
        }

        deleteButton.setOnClickListener {
            handleDeleteImage()
            if (listImageCropped.isEmpty()) {
                pageNumberText.visibility = View.GONE
                setResult(Activity.RESULT_CANCELED)
                finish()
            } else {
                pageNumberText.text = "${selectedPosition + 1}/${listImageCropped.size}"
            }
        }

        addButton.setOnClickListener {
            documentScanner?.setCurrentNumDocuments(listImageCropped.size)
            documentScanner?.letUserAdjustCrop(false)
            documentScanner?.start()
        }
    }

    /**
     * Handles the success result from document scanning
     */
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun handleDocumentScanSuccess(
        croppedImageResults: List<String>,
        originalImageResults: List<Uri>
    ) {
        listImageCropped.addAll(croppedImageResults)
        listImageOriginal.addAll(originalImageResults)

        // Set or update RecyclerView Adapter
        if (galleryAdapter == null) {
            galleryAdapter =
                GalleryAdapter(
                    listImageCropped,
                ) { path, position ->
                    onGalleryClick(path, position)
                    pageNumberText.text = "${position + 1}/${listImageCropped.size}"
                }
            galleryRecyclerView.adapter = galleryAdapter
        } else {
            galleryAdapter!!.notifyDataSetChanged()
        }
        // Set or update ViewPager Adapter
        if (viewPagerAdapter == null) {
            viewPagerAdapter = ViewPagerAdapter(this, listImageCropped)
            viewPagerAdapter?.onCreated()
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position == selectedPosition) {
                        return
                    }
                    selectedPosition = position
                    galleryAdapter?.updateSelectedPosition(
                        position
                    )
                    pageNumberText.text = "${position + 1}/${listImageCropped.size}"
                }

            })
            viewPager.adapter = viewPagerAdapter
        } else {
            viewPagerAdapter?.notifyDataSetChanged()
            viewPagerAdapter?.onCreated()
        }
        pageNumberText.text = "${selectedPosition + 1}/${listImageCropped.size}"
    }

    /**
     * Handles the deletion of selected image
     */
    private fun handleDeleteImage() {
        if (listImageCropped.size == 0) {
            return
        }
        listImageCropped.removeAt(selectedPosition)
        listImageOriginal.removeAt(selectedPosition)

        viewPagerAdapter?.remove(selectedPosition)

        galleryAdapter?.notifyItemRemoved(selectedPosition)
        if (selectedPosition == listImageCropped.size) {
            selectedPosition = listImageCropped.size - 1
        }
        galleryAdapter?.notifyItemChanged(selectedPosition)
    }

    /**
     * Handles click events on images in the gallery
     */
    private fun onGalleryClick(selectedImage: String, position: Int) {
        selectedPosition = position
        viewPager.setCurrentItem(position, true)
//        Toast.makeText(this, "Image clicked: $selectedImage", Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("WrongConstant")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED) {
            if (listImageCropped.size == 0) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        } else {
            documentScanner?.onActivityResult(requestCode, resultCode, data)
        }
    }

}