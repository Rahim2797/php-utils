package com.github.rahim2797.phputils.properties

object PropertiesArrayAccessTypeCodec {
    data class Payload(
        val receiverRawType: String,
        val key: String
    )

    fun encode(receiverRawType: String, key: String): String {
        return escape(receiverRawType) + "::" + escape(key)
    }

    fun decode(payload: String): Payload? {
        val sep = findSeparator(payload)
        if (sep < 0) return null

        val left = payload.substring(0, sep)
        val right = payload.substring(sep + 2)

        val receiverRawType = unescape(left)
        val key = unescape(right)

        if (receiverRawType.isEmpty() || key.isEmpty()) return null
        return Payload(receiverRawType, key)
    }

    private fun findSeparator(s: String): Int {
        var escaped = false
        var i = 0

        while (i < s.length - 1) {
            val ch = s[i]

            if (escaped) {
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == ':' && s[i + 1] == ':') {
                return i
            }

            i++
        }

        return -1
    }

    private fun escape(s: String): String {
        val out = StringBuilder(s.length)
        for (ch in s) {
            when (ch) {
                '\\' -> out.append("\\\\")
                ':' -> out.append("\\:")
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    private fun unescape(s: String): String {
        val out = StringBuilder(s.length)
        var escaped = false

        for (ch in s) {
            if (escaped) {
                out.append(ch)
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else {
                out.append(ch)
            }
        }

        if (escaped) {
            out.append('\\')
        }

        return out.toString()
    }
}