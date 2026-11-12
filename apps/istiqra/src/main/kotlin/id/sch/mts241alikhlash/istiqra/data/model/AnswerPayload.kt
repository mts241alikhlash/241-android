package id.sch.mts241alikhlash.istiqra.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

sealed interface AnswerPayload {
    data class Choice(
        val key: String,
    ) : AnswerPayload

    data class Multiple(
        val keys: List<String>,
    ) : AnswerPayload

    data class Bool(
        val value: Boolean,
    ) : AnswerPayload

    data class Text(
        val text: String,
    ) : AnswerPayload

    fun toJson(): JsonObject =
        when (this) {
            is Choice -> buildJsonObject { put("key", key) }
            is Multiple -> buildJsonObject { putJsonArray("keys") { keys.sorted().forEach { add(JsonPrimitive(it)) } } }
            is Bool -> buildJsonObject { put("value", value) }
            is Text -> buildJsonObject { put("text", text) }
        }

    fun encode(): String = toJson().toString()

    companion object {
        private val parser = Json

        fun decode(encoded: String): AnswerPayload? {
            val parsed = runCatching { parser.parseToJsonElement(encoded) }.getOrNull()
            return (parsed as? JsonObject)?.let(::fromJson)
        }

        fun fromJson(json: Map<String, JsonElement>): AnswerPayload? {
            if (json.size != 1) return null
            val (name, value) = json.entries.first()
            return when (name) {
                "key" -> stringOf(value)?.let(::Choice)
                "keys" ->
                    (value as? JsonArray)?.let { array ->
                        val keys = array.map { stringOf(it) ?: return null }
                        Multiple(keys.sorted())
                    }
                "value" -> (value as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull?.let(::Bool)
                "text" -> stringOf(value)?.let(::Text)
                else -> null
            }
        }

        private fun stringOf(element: JsonElement): String? = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
