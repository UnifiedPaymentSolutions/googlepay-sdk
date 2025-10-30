package com.everypay.gpay.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Utility methods for JSON conversion.
 * Pure Android utilities with no React Native dependencies.
 */
object ConvertUtil {

    /**
     * Converts a JSON string to a JSONObject
     *
     * @param json JSON string
     * @return JSONObject instance
     * @throws JSONException if parsing fails
     */
    @Throws(JSONException::class)
    fun jsonStringToObject(json: String): JSONObject {
        return JSONObject(json)
    }

    /**
     * Converts a JSON string to a JSONArray
     *
     * @param json JSON string
     * @return JSONArray instance
     * @throws JSONException if parsing fails
     */
    @Throws(JSONException::class)
    fun jsonStringToArray(json: String): JSONArray {
        return JSONArray(json)
    }

    /**
     * Merges two JSONObjects. Values from the second object override values in the first.
     *
     * @param base Base JSONObject
     * @param override JSONObject with override values
     * @return Merged JSONObject
     * @throws JSONException if merging fails
     */
    @Throws(JSONException::class)
    fun mergeJsonObjects(base: JSONObject, override: JSONObject): JSONObject {
        val result = JSONObject(base.toString())
        val keys = override.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result.put(key, override.get(key))
        }
        return result
    }
}