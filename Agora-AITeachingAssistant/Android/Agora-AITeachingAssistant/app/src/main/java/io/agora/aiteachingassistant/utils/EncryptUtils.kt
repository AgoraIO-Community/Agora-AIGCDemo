package io.agora.aiteachingassistant.utils

import io.agora.aiteachingassistant.BuildConfig
import io.agora.aiteachingassistant.constants.Constants
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object EncryptUtils {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"

    fun encryptByAes(plainText: String): String {
        try {
            val secretKey = SecretKeySpec(BuildConfig.KEY.toByteArray(), ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val cipherText = cipher.doFinal(plainText.toByteArray())
            return Base64Utils.encode(cipherText)
        } catch (e: Exception) {
            LogUtils.e(Constants.TAG, e.message ?: "")
        }
        return ""
    }

    fun decryptByAes(encryptedText: String?): String {
        try {
            val secretKey = SecretKeySpec(BuildConfig.KEY.toByteArray(), ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val cipherText: ByteArray? = Base64Utils.decode(encryptedText)
            val plainText = cipher.doFinal(cipherText)
            return String(plainText)
        } catch (e: Exception) {
            LogUtils.e(Constants.TAG, e.message ?: "")
        }
        return ""
    }

}
