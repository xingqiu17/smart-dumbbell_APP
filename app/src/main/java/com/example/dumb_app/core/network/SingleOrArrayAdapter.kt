package com.example.dumb_app.core.network

import com.google.gson.*
import java.lang.reflect.Type

/** 把服务端“可能返回单个对象”的场景，兼容成 List<T> */
class SingleOrArrayAdapter<T>(
    private val elementType: Type,
    private val gson: Gson
) : JsonDeserializer<List<T>> {

    override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): List<T> {
        return when {
            json.isJsonArray  -> json.asJsonArray.map { gson.fromJson<T>(it, elementType) }
            json.isJsonObject -> listOf(gson.fromJson<T>(json, elementType))
            json.isJsonNull   -> emptyList()
            else              -> throw JsonParseException("Unexpected JSON for list")
        }
    }
}
