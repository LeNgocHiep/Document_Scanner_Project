package com.df.document_scanner.utils

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.activity.ComponentActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * This class contains a helper function creating temporary files
 *
 * @constructor creates file util
 */
class FileUtil {
    /**
     * create a temporary file
     *
     * @param activity the current activity
     * @param pageNumber the current document page number
     */
    @Throws(IOException::class)
    fun createImageFile(activity: ComponentActivity, pageNumber: Int): File {
        // use current time to make file name more unique
        val dateTime: String = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.US
        ).format(Date())

        // create file in pictures directory
        val storageDir: File? = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "DOCUMENT_SCAN_${pageNumber}_${dateTime}",
            ".jpg",
            storageDir
        )
    }

    fun getRealPathFromURI(uri: Uri, activity: ComponentActivity): String? {
        var filePath: String? = null
        activity.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val cursor = activity.contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex("_data")
                if (columnIndex != -1) {
                    filePath = it.getString(columnIndex)
                } else {
                    // For newer Android versions where the _data column is not available
                    filePath = getFileFromUri(uri, activity)?.absolutePath
                }
            }
        }
        return filePath
    }

    private fun getFileFromUri(uri: Uri, activity: ComponentActivity): File? {
        return try {
            val inputStream = activity.contentResolver.openInputStream(uri)
            val file = File.createTempFile("temp", null, activity.cacheDir)
            val outputStream = FileOutputStream(file)

            inputStream?.copyTo(outputStream)

            inputStream?.close()
            outputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createPdfFromImages(context: Context, imagePaths: List<String>, outputFileName: String): String {
        // Create a PdfDocument instance
        val pdfDocument = PdfDocument()

        // Loop through each image path
        for ((index, imagePath) in imagePaths.withIndex()) {
            // Load the image from the file path
//            val bitmap = BitmapFactory.decodeFile(imagePath)
            //context.contentResolver.takePersistableUriPermission(Uri.parse(imagePath), Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val inputStream: InputStream? = context.contentResolver.openInputStream(Uri.parse(imagePath))//?.openInputStream(selectedImageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Xử lý bitmap theo nhu cầu của bạn
            inputStream?.close()

            if (bitmap != null) {
                // Create a new page description
                val pageInfo =
                    PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()

                // Start a page
                val page = pdfDocument.startPage(pageInfo)

                // Draw the bitmap on the page
                val canvas = page.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                // Finish the page
                pdfDocument.finishPage(page)

                // Recycle the bitmap to free memory
                bitmap.recycle()
            } else {
                println("Failed to load image: $imagePath")
            }
        }

        // Define the path to save the PDF file in the app's local data directory
        val file = File(context.filesDir, outputFileName)

        // Write the document content to the file
        try {
            val fileOutputStream = FileOutputStream(file)
            pdfDocument.writeTo(fileOutputStream)
            fileOutputStream.close()

            println("PDF created successfully at: ${file.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error creating PDF: ${e.message}")
        } finally {
            // Close the document
            pdfDocument.close()
            println("PDF closed successfully")
        }
        return file.absolutePath
    }
}