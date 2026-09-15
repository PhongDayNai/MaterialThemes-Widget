package com.iatb.materialthemes.data.content.util

import com.iatb.materialthemes.data.content.model.ContentType
import java.security.MessageDigest
import java.util.Locale

object ContentNormalizer {

    fun normalizeText(text: String): String {
        return text.trim()
            .replace("\\s+".toRegex(), " ")
            .replace("[\u201C\u201D]".toRegex(), "\"")
            .replace("[\u2018\u2019]".toRegex(), "'")
    }

    fun calculateHash(type: ContentType, text: String, author: String?): String {
        val normText = normalizeText(text).lowercase(Locale.ROOT)
        val normAuthor = if (type == ContentType.THOUGHT) {
            ""
        } else {
            author?.let { normalizeText(it).lowercase(Locale.ROOT) } ?: ""
        }
        val payload = "${type.name}|$normText|$normAuthor"
        return MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
