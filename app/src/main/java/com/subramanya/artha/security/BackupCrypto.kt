package com.subramanya.artha.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-based AES-GCM encryption for backup payloads.
 *
 * Format of an encrypted backup (Base64 outer wrap so it's drop-anywhere-friendly):
 *
 *     ARTHA1\n<base64(16-byte salt)>\n<base64(12-byte iv)>\n<base64(ciphertext+tag)>
 *
 * The magic prefix doubles as a version marker — bump it if the KDF or cipher
 * ever changes so old backups can still be detected and migrated.
 *
 * Threat model: protects the JSON snapshot if the file leaves the device. NOT a
 * replacement for full-disk encryption — Android device storage already encrypts
 * at rest. This kicks in when the user shares the file via email, Drive, etc.
 */
object BackupCrypto {

    private const val MAGIC = "ARTHA1"
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val KEY_BITS = 256
    private const val PBKDF2_ITERATIONS = 120_000

    fun encrypt(plaintext: String, password: CharArray): String {
        require(password.isNotEmpty()) { "password must not be empty" }
        val rng = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(rng::nextBytes)
        val iv = ByteArray(IV_BYTES).also(rng::nextBytes)
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return listOf(
            MAGIC,
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP),
            Base64.encodeToString(ct, Base64.NO_WRAP),
        ).joinToString("\n")
    }

    fun isEncrypted(raw: String): Boolean = raw.lineSequence().firstOrNull()?.trim() == MAGIC

    fun decrypt(raw: String, password: CharArray): Result<String> = runCatching {
        val lines = raw.lineSequence().toList()
        require(lines.size >= 4 && lines[0].trim() == MAGIC) { "not an Artha encrypted backup" }
        val salt = Base64.decode(lines[1], Base64.NO_WRAP)
        val iv = Base64.decode(lines[2], Base64.NO_WRAP)
        val ct = Base64.decode(lines[3], Base64.NO_WRAP)
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val pt = cipher.doFinal(ct)
        String(pt, Charsets.UTF_8)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }
}
