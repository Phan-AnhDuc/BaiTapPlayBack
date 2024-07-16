package com.example.cameraplayback.utils

import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.cameraplayback.BuildConfig
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESedeKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("UNNECESSARY_SAFE_CALL")
class CryptoAES {


    /**
     * Encrypt Data from String
     *
     * @param plainText
     * @return
     */

    private external fun getKeyTest(): String

    private var keyT = getKeyTest()

    companion object {
        init {
            System.loadLibrary("test-lib")
        }

        private val cryptoAES = CryptoAES()

        fun getHashKey(): String {
            return cryptoAES.keyT
        }

        fun encrypt(plainText: String): String {
            try {
                val cipher = Cipher.getInstance(BuildConfig.CIPHER_INSTANCE)
                val key = CryptoAES.decrypt3DES(BuildConfig.KEY_SECRET)
                val keyspec = SecretKeySpec(key.toByteArray(), "AES")
                val iv = CryptoAES.decrypt3DES(BuildConfig.IV)
                val ivspec = IvParameterSpec(iv.toByteArray())
                cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec)
                return if (Build.VERSION.SDK_INT >= 26) {
                    java.util.Base64.getEncoder()
                        .encodeToString(cipher.doFinal(plainText.toByteArray()))
                } else {
                    Base64.encodeToString(cipher.doFinal(plainText.toByteArray()), Base64.NO_WRAP)
                }
            } catch (exception: Exception) {
                Objects.requireNonNull(exception.message)?.let { Log.d("AES encrypt fail:", it) }
            }
            return ""
        }

        /**
         * Decrypt From textDecrypt
         *
         * @param textDecrypt
         * @return
         */
        fun decrypt(textDecrypt: String?): String? {
            try {
                val iv = CryptoAES.decrypt3DES(BuildConfig.IV)
                val ivParameterSpec = IvParameterSpec(iv.toByteArray())
                val key = CryptoAES.decrypt3DES(BuildConfig.KEY_SECRET)
                val keyspec = SecretKeySpec(key.toByteArray(), "AES")
                val cipher = Cipher.getInstance(BuildConfig.CIPHER_INSTANCE)
                cipher.init(Cipher.DECRYPT_MODE, keyspec, ivParameterSpec)
                return if (Build.VERSION.SDK_INT >= 26) {
                    String(cipher.doFinal(java.util.Base64.getDecoder().decode(textDecrypt)))
                } else {
                    String(cipher.doFinal(Base64.decode(textDecrypt, Base64.DEFAULT)))
                }
            } catch (exception: Exception) {
                Objects.requireNonNull(exception.message)?.let { Log.d("AES decrypt fail:", it) }
            }
            return ""
        }

        fun encrypt3DES(plainText: String): String {
            try {
                // Convert the secret key to DESedeKeySpec
                val spec = DESedeKeySpec(getHashKey().toByteArray())
                val keyfactory = SecretKeyFactory.getInstance("desede")
                val deskey = keyfactory.generateSecret(spec)

                // Initialize the cipher for encryption with the specified transformation
                val cipher = Cipher.getInstance("desede/ECB/PKCS7PADDING")
                cipher.init(Cipher.ENCRYPT_MODE, deskey)

                // Perform encryption
                val encryptedText: ByteArray
                if (Build.VERSION.SDK_INT >= 26) {
                    encryptedText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                    return java.util.Base64.getEncoder().encodeToString(encryptedText)
                } else {
                    encryptedText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                    return Base64.encodeToString(encryptedText, Base64.DEFAULT)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }

        fun decrypt3DES(encryptText: String): String {
            try {
                val spec = DESedeKeySpec(getHashKey().toByteArray());
                val keyfactory = SecretKeyFactory.getInstance("desede");
                val deskey = keyfactory.generateSecret(spec);
                val cipher = Cipher.getInstance("desede/ECB/PKCS7PADDING");
                cipher.init(Cipher.DECRYPT_MODE, deskey);
                var plainText = ""
//                val decryptData = cipher.doFinal(Base64.decode(encryptText, Base64.DEFAULT));
                if (Build.VERSION.SDK_INT >= 26) {
                    plainText =
                        String(cipher.doFinal(java.util.Base64.getDecoder().decode(encryptText)))

                } else {
                    plainText = String(cipher.doFinal(Base64.decode(encryptText, Base64.DEFAULT)))
                }

                return plainText
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }


    }

}