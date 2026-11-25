package com.skyplus.billdesk.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * A PIN is never stored. It is salted, stretched and stored as a verifiable hash.
 *
 * Format: `algorithm$iterations$saltHex$hashHex`
 *
 * The algorithm name is stored with the hash because PBKDF2WithHmacSHA256 does not
 * exist on older Android (API 23-25, which many POS terminals still run), and a
 * PIN set on one device must still verify on another. Verification always uses the
 * algorithm recorded in the record, never the current default.
 */
object PinHasher {

    private const val PREFERRED = "PBKDF2WithHmacSHA256"
    private const val FALLBACK = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    private val random = SecureRandom()

    /** A fresh 16-byte salt. Never reused across profiles. */
    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { random.nextBytes(it) }

    fun preferredAlgorithm(): String = try {
        SecretKeyFactory.getInstance(PREFERRED)
        PREFERRED
    } catch (e: Exception) {
        FALLBACK
    }

    /** Builds the storable record for a PIN. */
    fun hash(pin: String, salt: ByteArray = newSalt(), iterations: Int = ITERATIONS): String {
        val algorithm = preferredAlgorithm()
        val hash = derive(pin, salt, iterations, algorithm)
        return algorithm + "$" + iterations + "$" + encode(salt) + "$" + encode(hash)
    }

    /** Constant-time check of a PIN against a stored record. */
    fun verify(pin: String, record: String): Boolean {
        val parts = record.split("$")
        if (parts.size != 4) return false
        val algorithm = parts[0]
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = decode(parts[2]) ?: return false
        val expected = decode(parts[3]) ?: return false
        val actual = derive(pin, salt, iterations, algorithm)
        return MessageDigest.isEqual(actual, expected)
    }

    /** True when the record is well formed, without checking a PIN. */
    fun isWellFormed(record: String): Boolean {
        val parts = record.split("$")
        if (parts.size != 4) return false
        if (parts[1].toIntOrNull() == null) return false
        return decode(parts[2]) != null && decode(parts[3]) != null
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int, algorithm: String): ByteArray {
        require(iterations >= 10_000) { "Iterations too low: $iterations" }
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        try {
            return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Hex, not Base64, on purpose: android.util.Base64 does not exist on the JVM,
     * so using it would make this class untestable in a plain unit test. Hex is
     * bigger by a third and completely portable.
     */
    private fun encode(bytes: ByteArray): String {
        val builder = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            builder.append(HEX[(byte.toInt() shr 4) and 0x0F])
            builder.append(HEX[byte.toInt() and 0x0F])
        }
        return builder.toString()
    }

    private fun decode(text: String): ByteArray? {
        if (text.isEmpty() || text.length % 2 != 0) return null
        val out = ByteArray(text.length / 2)
        for (i in out.indices) {
            val high = hexValue(text[i * 2]) ?: return null
            val low = hexValue(text[i * 2 + 1]) ?: return null
            out[i] = ((high shl 4) or low).toByte()
        }
        return out
    }

    private fun hexValue(char: Char): Int? = when (char) {
        in '0'..'9' -> char - '0'
        in 'a'..'f' -> char - 'a' + 10
        in 'A'..'F' -> char - 'A' + 10
        else -> null
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
