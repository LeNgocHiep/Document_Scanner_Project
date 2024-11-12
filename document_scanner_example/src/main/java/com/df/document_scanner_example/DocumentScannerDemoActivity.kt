package com.df.document_scanner_example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.df.document_scanner.DocumentScanner
class DocumentScannerDemoActivity : AppCompatActivity() {

    private lateinit var documentScanner: DocumentScanner
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_scanner_demo)

        // Initialize DocumentScanner
        documentScanner = DocumentScanner()

        // Set up UI elements
        resultTextView = findViewById(R.id.resultTextView)
        val startScannerButton: Button = findViewById(R.id.startScannerButton)
        val openGalleryButton: Button = findViewById(R.id.openGalleryButton)

        // Set button click listeners
        startScannerButton.setOnClickListener {
            startDocumentScanner()
        }

        openGalleryButton.setOnClickListener {
            openGalleryScanner()
        }
    }

    private fun startDocumentScanner() {
        // Start document scanner
        documentScanner.startDocumentScanner(this, 5) { result ->
            result.fold(
                onSuccess = { resultMap ->
                    displayResult("Document Scan Result:\n$resultMap")
                },
                onFailure = { error ->
                    displayResult("Error: ${error.message}")
                }
            )
        }
    }

    private fun openGalleryScanner() {
        // Open gallery for image selection with max 5 items
        documentScanner.openGalleryScanner(this, maxItems = 5)
    }

    private fun displayResult(resultText: String) {
        resultTextView.text = resultText
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle the result from DocumentScanner
        val result = documentScanner.handleActivityResult(requestCode, resultCode, data)
        result?.let {
            displayResult("Gallery/Scanner Result:\n$it")
        }
    }
}