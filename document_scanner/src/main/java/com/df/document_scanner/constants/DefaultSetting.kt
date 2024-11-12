package com.df.document_scanner.constants

/**
 * This class contains default document scanner options
 */
class DefaultSetting {
    companion object {
        const val CROPPED_IMAGE_QUALITY = 100
        const val LET_USER_ADJUST_CROP = false
        const val MAX_NUM_DOCUMENTS = 24
        const val RESPONSE_TYPE = ResponseType.IMAGE_FILE_PATH
    }
}