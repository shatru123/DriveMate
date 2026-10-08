package com.shatrughna.drivemate.auth.security

import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Production cryptographic helper for hashing passwords with per-user salt.
 * Uses PBKDF2WithHmacSHA256 with 10,000 iterations.
 */
object PasswordHasher {

    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun hashPassword(password: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)

        val hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val saltHex = bytesToHex(salt)
        val hashHex = bytesToHex(hash)

        return "$saltHex:$hashHex"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false

        val salt = hexToBytes(parts[0])
        val expectedHash = parts[1]

        val computedHash = bytesToHex(pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH))
        return computedHash == expectedHash
    }

    private fun pbkdf2(chars: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec: KeySpec = PBEKeySpec(chars, salt, iterations, keyLength)
            factory.generateSecret(spec).encoded
        } catch (e: NoSuchAlgorithmException) {
            // Fallback for older JVMs
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec: KeySpec = PBEKeySpec(chars, salt, iterations, keyLength)
            factory.generateSecret(spec).encoded
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b.toInt() and 0xff))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
