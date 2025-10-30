package com.everypay.gpay.util

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object ConvertUtil {
    /**
     * Utility method to convert ReadableArray to List<String>
     * Handles null values gracefully by skipping them
     */
    fun readableArrayToStringList(readableArray: ReadableArray?): List<String> {
        if (readableArray == null) return emptyList()

        val result = mutableListOf<String>()
        for (i in 0 until readableArray.size()) {
            try {
                readableArray.getString(i)?.let { value ->
                    result.add(value)
                }
            } catch (e: Exception) {
                Log.w("EverypayGpayRnBridgeModule", "Skipping invalid array element at index $i", e)
            }
        }
        return result
    }

    fun readableArrayToJsonString(readableArray: ReadableArray): String {
        val jsonArray = org.json.JSONArray()
        for (i in 0 until readableArray.size()) {
            when (readableArray.getType(i)) {
                com.facebook.react.bridge.ReadableType.String -> {
                    jsonArray.put(readableArray.getString(i))
                }
                com.facebook.react.bridge.ReadableType.Number -> {
                    jsonArray.put(readableArray.getDouble(i))
                }
                com.facebook.react.bridge.ReadableType.Boolean -> {
                    jsonArray.put(readableArray.getBoolean(i))
                }
                // Add other types as needed
                else -> {
                    jsonArray.put(readableArray.getString(i))
                }
            }
        }
        return jsonArray.toString()
    }

    @Throws(JSONException::class)
    fun mapToJson(readableMap: ReadableMap): JSONObject {
        val jsonObject = JSONObject()
        val iterator = readableMap.keySetIterator()
        while (iterator.hasNextKey()) {
            val key = iterator.nextKey()
            when (readableMap.getType(key)) {
                ReadableType.Null -> jsonObject.put(key, JSONObject.NULL)
                ReadableType.Boolean -> jsonObject.put(key, readableMap.getBoolean(key))
                ReadableType.Number -> jsonObject.put(key, readableMap.getDouble(key))
                ReadableType.String -> jsonObject.put(key, readableMap.getString(key))
                ReadableType.Map -> jsonObject.put(key, mapToJson(readableMap.getMap(key)!!))
                ReadableType.Array -> jsonObject.put(key, arrayToJson(readableMap.getArray(key)!!))
            }
        }
        return jsonObject
    }

    @Throws(JSONException::class)
    fun arrayToJson(readableArray: ReadableArray): JSONArray {
        val array = JSONArray()
        for (i in 0 until readableArray.size()) {
            when (readableArray.getType(i)) {
                ReadableType.Null -> {}
                ReadableType.Boolean -> array.put(readableArray.getBoolean(i))
                ReadableType.Number -> array.put(readableArray.getDouble(i))
                ReadableType.String -> array.put(readableArray.getString(i))
                ReadableType.Map -> array.put(readableArray.getMap(i)?.let { mapToJson(it) })
                ReadableType.Array -> array.put(readableArray.getArray(i)?.let { arrayToJson(it) })
            }
        }
        return array
    }

    @Throws(JSONException::class)
    fun jsonStringToMap(json: String): WritableMap {
        return jsonToMap(JSONObject(json))
    }

    @Throws(JSONException::class)
    private fun jsonToMap(jsonObject: JSONObject): WritableMap {
        val map = WritableNativeMap()
        val iterator = jsonObject.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val value = jsonObject[key]
            when (value) {
                is JSONObject -> map.putMap(key, jsonToMap(value))
                is JSONArray -> map.putArray(key, jsonToArray(value))
                is Boolean -> map.putBoolean(key, value)
                is Int -> map.putInt(key, value)
                is Double -> map.putDouble(key, value)
                is String -> map.putString(key, value)
                else -> map.putString(key, value.toString())
            }
        }
        return map
    }

    @Throws(JSONException::class)
    fun jsonToArray(jsonArray: JSONArray): WritableArray {
        val array = WritableNativeArray()
        for (i in 0 until jsonArray.length()) {
            val value = jsonArray[i]
            when (value) {
                is JSONObject -> array.pushMap(jsonToMap(value))
                is JSONArray -> array.pushArray(jsonToArray(value))
                is Boolean -> array.pushBoolean(value)
                is Int -> array.pushInt(value)
                is Double -> array.pushDouble(value)
                is String -> array.pushString(value)
                else -> array.pushString(value.toString())
            }
        }
        return array
    }
}