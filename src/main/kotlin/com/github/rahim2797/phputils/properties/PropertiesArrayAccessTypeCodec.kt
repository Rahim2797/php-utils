package com.github.rahim2797.phputils.properties

import java.nio.charset.StandardCharsets
import java.util.*

object PropertiesArrayAccessTypeCodec {
    data class Payload(
        val receiverRawType: String,
        val key: String
    )

    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(receiverRawType: String, key: String): String {
        val left = b64(receiverRawType)
        val right = b64(key)
        return "$left.$right"
    }

    fun decode(payload: String): Payload? {
        val sep = payload.indexOf('.')
        if (sep <= 0 || sep >= payload.length - 1) return null

        val left = payload.substring(0, sep)
        val right = payload.substring(sep + 1)

        return try {
            Payload(
                receiverRawType = unb64(left),
                key = unb64(right)
            )
        } catch (_: IllegalArgumentException) {
            null
            }
        }

    private fun b64(value: String): String {
        return encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        }

    private fun unb64(value: String): String {
        return String(decoder.decode(value), StandardCharsets.UTF_8)
    }
}