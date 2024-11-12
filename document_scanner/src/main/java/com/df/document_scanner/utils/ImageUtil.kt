package com.df.document_scanner.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.df.document_scanner.extensions.distance
import com.df.document_scanner.extensions.toOpenCVPoint
import com.df.document_scanner.models.Quad
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.InputStream
import kotlin.math.min

/**
 * This class contains helper functions for processing images
 *
 * @constructor creates image util
 */
class ImageUtil {
    /**
     * get image matrix from file path
     *
     * @param filePath image is saved here
     * @return image matrix
     */
    private fun getImageMatrixFromFilePath(filePath: String): Mat {
        // read image as matrix using OpenCV
        val image: Mat = Imgcodecs.imread(filePath)

        // if OpenCV fails to read the image then it's empty
        if (!image.empty()) {
            // convert image to RGB color space since OpenCV reads it using BGR color space
            Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGB)
            return image
        }

        if (!File(filePath).exists()) {
            throw Exception("File doesn't exist - $filePath")
        }

        if (!File(filePath).canRead()) {
            throw Exception("You don't have permission to read $filePath")
        }

        // try reading image without OpenCV
        var imageBitmap = BitmapFactory.decodeFile(filePath)
        val rotation = when (ExifInterface(filePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        imageBitmap = Bitmap.createBitmap(
            imageBitmap,
            0,
            0,
            imageBitmap.width,
            imageBitmap.height,
            Matrix().apply { postRotate(rotation.toFloat()) },
            true
        )
        Utils.bitmapToMat(imageBitmap, image)

        return image
    }

    /**
     * get bitmap image from file path
     *
     * @param filePath image is saved here
     * @return image bitmap
     */
    fun getImageFromFilePath(filePath: String): Bitmap {
        // read image as matrix using OpenCV
        val image: Mat = this.getImageMatrixFromFilePath(filePath)

        // convert image matrix to bitmap
        val bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(image, bitmap)
        return bitmap
    }


    fun uriToBitmap(uri: Uri, activity: AppCompatActivity): Bitmap? {
        return try {
            val inputStream: InputStream? = activity.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Read EXIF data to correct the orientation
            val inputStreamForExif = activity.contentResolver.openInputStream(uri)
            val exif = ExifInterface(inputStreamForExif!!)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            rotateBitmap(bitmap, orientation)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * take a photo with a document, crop everything out but document, and force it to display
     * as a rectangle
     *
     * @param photoFilePath original image is saved here
     * @param corners the 4 document corners
     * @return bitmap with cropped and warped document
     */
//    fun crop(photoFilePath: String, corners: Quad): Bitmap {
//        // read image with OpenCV
//        val image = this.getImageMatrixFromFilePath(photoFilePath)
//
//        // convert top left, top right, bottom right, and bottom left document corners from
//        // Android points to OpenCV points
//        val tLC = corners.topLeftCorner.toOpenCVPoint()
//        val tRC = corners.topRightCorner.toOpenCVPoint()
//        val bRC = corners.bottomRightCorner.toOpenCVPoint()
//        val bLC = corners.bottomLeftCorner.toOpenCVPoint()
//
//        // Calculate the document edge distances. The user might take a skewed photo of the
//        // document, so the top left corner to top right corner distance might not be the same
//        // as the bottom left to bottom right corner. We could take an average of the 2, but
//        // this takes the smaller of the 2. It does the same for height.
//        val width = min(tLC.distance(tRC), bLC.distance(bRC))
//        val height = min(tLC.distance(bLC), tRC.distance(bRC))
//
//        // create empty image matrix with cropped and warped document width and height
//        val croppedImage = MatOfPoint2f(
//            Point(0.0, 0.0),
//            Point(width, 0.0),
//            Point(width, height),
//            Point(0.0, height),
//        )
//
//        // This crops the document out of the rest of the photo. Since the user might take a
//        // skewed photo instead of a straight on photo, the document might be rotated and
//        // skewed. This corrects that problem. output is an image matrix that contains the
//        // corrected image after this fix.
//        val output = Mat()
//        Imgproc.warpPerspective(
//            image,
//            output,
//            Imgproc.getPerspectiveTransform(
//                MatOfPoint2f(tLC, tRC, bRC, bLC),
//                croppedImage
//            ),
//            Size(width, height)
//        )
//
//        // convert output image matrix to bitmap
//        val croppedBitmap = Bitmap.createBitmap(
//            output.cols(),
//            output.rows(),
//            Bitmap.Config.ARGB_8888
//        )
//        Utils.matToBitmap(output, croppedBitmap)
//
//        return croppedBitmap
//    }

    fun crop(photoFilePath: String, corners: Quad): Bitmap {
        // Read image with OpenCV
        val image = this.getImageMatrixFromFilePath(photoFilePath)

        // Convert top left, top right, bottom right, and bottom left document corners from
        // Android points to OpenCV points
        val tLC = corners.topLeftCorner.toOpenCVPoint()
        val tRC = corners.topRightCorner.toOpenCVPoint()
        val bRC = corners.bottomRightCorner.toOpenCVPoint()
        val bLC = corners.bottomLeftCorner.toOpenCVPoint()

        // Calculate the document edge distances.
        val originalWidth = min(tLC.distance(tRC), bLC.distance(bRC))
        val originalHeight = min(tLC.distance(bLC), tRC.distance(bRC))

        // Set width to a fixed size of 400 pixels
        val fixedWidth = 500.0
        // Calculate the height to maintain the aspect ratio
        val aspectRatio = originalHeight / originalWidth
        val fixedHeight = fixedWidth * aspectRatio

        // Create an empty image matrix with cropped and warped document width and height
        val croppedImage = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(fixedWidth, 0.0),
            Point(fixedWidth, fixedHeight),
            Point(0.0, fixedHeight)
        )

        // This crops the document out of the rest of the photo. It corrects rotation and skewing.
        val output = Mat()
        Imgproc.warpPerspective(
            image,
            output,
            Imgproc.getPerspectiveTransform(
                MatOfPoint2f(tLC, tRC, bRC, bLC),
                croppedImage
            ),
            Size(fixedWidth, fixedHeight)
        )

        // Convert output image matrix to bitmap
        val croppedBitmap = Bitmap.createBitmap(
            output.cols(),
            output.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(output, croppedBitmap)

        return croppedBitmap
    }


    /**
     * get bitmap image from file uri
     *
     * @param fileUriString image is saved here and starts with file:///
     * @return bitmap image
     */
    fun readBitmapFromFileUriString(
        fileUriString: String,
        contentResolver: ContentResolver
    ): Bitmap {
        return BitmapFactory.decodeStream(
            contentResolver.openInputStream(Uri.parse(fileUriString))
        )
    }
}