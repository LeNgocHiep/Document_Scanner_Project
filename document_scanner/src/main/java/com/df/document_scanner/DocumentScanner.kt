package com.df.document_scanner

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class DocumentScanner {
    private val instances = mutableMapOf<String, GmsDocumentScanner>()
    private val START_DOCUMENT_ACTIVITY = 0x362738
    private val REQUEST_IMAGE_PICK = 1809
    fun startDocumentScanner(
        activity: Activity,
        maxItems: Int
    ) {
        val options = mapOf(
            "isGalleryImport" to false,
            "pageLimit" to maxItems,
            "format" to "pdf",
            "mode" to "base"
        )
        val id = System.currentTimeMillis().toString()
        var scanner = instances[id]

        // Create scanner instance if it doesn't exist
        if (scanner == null) {
            val scannerOptions = parseOptions(options)
            scanner = GmsDocumentScanning.getClient(scannerOptions)
            instances[id] = scanner
        }
        // Start document scanner
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener(OnSuccessListener { intentSender ->
                try {
                    activity.startIntentSenderForResult(
                        intentSender,
                        START_DOCUMENT_ACTIVITY,
                        null,
                        0,
                        0,
                        0
                    )
                } catch (_: IntentSender.SendIntentException) {
                }
            }).addOnFailureListener(OnFailureListener {})
    }

    private fun parseOptions(options: Map<String, Any>): GmsDocumentScannerOptions {
        val isGalleryImportAllowed = options["isGalleryImport"] as Boolean
        val pageLimit = options["pageLimit"] as Int
        val format = when (options["format"] as String) {
            "pdf" -> GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            "jpeg" -> GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
            else -> throw IllegalArgumentException("Not a format: ${options["format"]}")
        }
        val mode = when (options["mode"] as String) {
            "base" -> GmsDocumentScannerOptions.SCANNER_MODE_BASE
            "filter" -> GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER
            "full" -> GmsDocumentScannerOptions.SCANNER_MODE_FULL
            else -> throw IllegalArgumentException("Not a mode: ${options["mode"]}")
        }
        return GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(isGalleryImportAllowed)
            .setPageLimit(pageLimit)
            .setResultFormats(format)
            .setScannerMode(mode)
            .build()
    }

    fun openGalleryScanner(activity: Activity, maxItems: Int) {
        val intent = Intent(activity, DocumentScannerGalleryActivity::class.java).apply {
            putExtra("maxItems", maxItems)
        }
        activity.startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Map<String, Any?>? {
        return when (requestCode) {
            START_DOCUMENT_ACTIVITY -> {
                if (resultCode == Activity.RESULT_OK) {
                    val result = GmsDocumentScanningResult.fromActivityResultIntent(data)
                    result?.let { handleScanningResult(it) }
                } else {
                    null
                }
            }

            REQUEST_IMAGE_PICK -> {
                if (resultCode == Activity.RESULT_OK) {
                    val resultData = data?.getStringExtra("path")
                    mapOf("pdf" to resultData)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    private fun handleScanningResult(result: GmsDocumentScanningResult): Map<String, Any?> {
        val resultMap = mutableMapOf<String, Any?>()

        val pdf = result.pdf
        resultMap["pdf"] = pdf?.uri?.path
        return resultMap
    }

    fun closeScanner(id: String) {
        instances.remove(id)
    }
}