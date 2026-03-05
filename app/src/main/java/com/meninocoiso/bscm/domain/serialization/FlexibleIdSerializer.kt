package com.meninocoiso.bscm.domain.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

object FlexibleIdSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        // Try to decode as JsonElement to handle both string and number
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            element.jsonPrimitive.content
        } else {
            // Fallback to original method
            try {
                decoder.decodeString()
            } catch (e: Exception) {
                // In case of failure, try decoding as Long and convert to String
                decoder.decodeLong().toString()
            }
        }
    }
}