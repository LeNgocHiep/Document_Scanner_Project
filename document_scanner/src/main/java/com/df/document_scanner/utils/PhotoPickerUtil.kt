package com.df.document_scanner.utils

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.df.document_scanner.models.DataUri

class PhotoPickerUtil(
    private val activity: ComponentActivity,
    private val onPhotoSuccess: (photoFilePaths: ArrayList<DataUri>) -> Unit,
    private val onCancelPhoto: () -> Unit
) {

    /**
     * @property photoFilePath the photo file path
     */
    private lateinit var photoFilePath: String

    /**
     * @property startForResult used to launch camera
     */
//    private val startForResult = activity.registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { result: ActivityResult ->
//        when (result.resultCode) {
//            Activity.RESULT_OK -> {
//                // send back photo file path on capture success
//                onPhotoCaptureSuccess(photoFilePath)
//            }
//
//            Activity.RESULT_CANCELED -> {
//                // delete the photo since the user didn't finish taking the photo
//                File(photoFilePath).delete()
//                onCancelPhoto()
//            }
//        }
//    }

    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    fun openPhotoPicker(maxItems: Int) {
        if (maxItems > 1) {
            pickMedia = activity.registerForActivityResult(
                ActivityResultContracts.PickMultipleVisualMedia(maxItems)
            ) { uris ->
                if (uris.isNotEmpty()) {
                    onPhotoSuccess(uris.map { uri ->
                        DataUri(FileUtil().getRealPathFromURI(uri, activity).toString(), uri)
                    }.toCollection(ArrayList()))
                } else {
                    onCancelPhoto()
                }
            }
        } else {
            pickMedia = activity.registerForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    val arrayList = ArrayList<DataUri>()
                    arrayList.add(
                        DataUri(
                            FileUtil().getRealPathFromURI(uri, activity).toString(),
                            uri
                        )
                    )
                    onPhotoSuccess(
                        arrayList
                    )
                } else {
                    onCancelPhoto()
                }
            }
        }
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

//    @Throws(IOException::class)
//    fun openCamera(pageNumber: Int) {
//        // create intent to launch camera
//        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//
//        // create new file for photo
//        val photoFile: File = FileUtil().createImageFile(activity, pageNumber)
//
//        // store the photo file path, and send it back once the photo is saved
//        photoFilePath = photoFile.absolutePath
//
//        // photo gets saved to this file path
//        val photoURI: Uri = FileProvider.getUriForFile(
//            activity,
//            "${activity.packageName}.DocumentScannerFileProvider",
//            photoFile
//        )
//        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//
//        // open camera
//        startForResult.launch(takePictureIntent)
//    }
}