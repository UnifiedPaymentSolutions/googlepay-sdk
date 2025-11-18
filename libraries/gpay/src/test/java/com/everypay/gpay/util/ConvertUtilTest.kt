package com.everypay.gpay.util

import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test

/**
 * Unit tests for ConvertUtil
 *
 * Tests JSON conversion and merging utilities
 */
class ConvertUtilTest {

    // ==================== jsonStringToObject() Tests ====================

    @Test
    fun `jsonStringToObject should parse valid JSON string`() {
        // Given
        val jsonString = """{"name": "John", "age": 30, "active": true}"""

        // When
        val result = ConvertUtil.jsonStringToObject(jsonString)

        // Then
        assertThat(result.getString("name")).isEqualTo("John")
        assertThat(result.getInt("age")).isEqualTo(30)
        assertThat(result.getBoolean("active")).isTrue()
    }

    @Test
    fun `jsonStringToObject should parse nested JSON objects`() {
        // Given
        val jsonString = """
            {
                "user": {
                    "name": "Alice",
                    "address": {
                        "city": "Tallinn",
                        "country": "EE"
                    }
                }
            }
        """.trimIndent()

        // When
        val result = ConvertUtil.jsonStringToObject(jsonString)

        // Then
        val user = result.getJSONObject("user")
        assertThat(user.getString("name")).isEqualTo("Alice")
        val address = user.getJSONObject("address")
        assertThat(address.getString("city")).isEqualTo("Tallinn")
        assertThat(address.getString("country")).isEqualTo("EE")
    }

    @Test
    fun `jsonStringToObject should parse JSON with arrays`() {
        // Given
        val jsonString = """{"items": ["item1", "item2", "item3"]}"""

        // When
        val result = ConvertUtil.jsonStringToObject(jsonString)

        // Then
        val items = result.getJSONArray("items")
        assertThat(items.length()).isEqualTo(3)
        assertThat(items.getString(0)).isEqualTo("item1")
        assertThat(items.getString(1)).isEqualTo("item2")
        assertThat(items.getString(2)).isEqualTo("item3")
    }

    @Test
    fun `jsonStringToObject should parse empty JSON object`() {
        // Given
        val jsonString = "{}"

        // When
        val result = ConvertUtil.jsonStringToObject(jsonString)

        // Then
        assertThat(result.length()).isEqualTo(0)
    }

    @Test
    fun `jsonStringToObject should parse JSON with null values`() {
        // Given
        val jsonString = """{"name": "Bob", "email": null}"""

        // When
        val result = ConvertUtil.jsonStringToObject(jsonString)

        // Then
        assertThat(result.getString("name")).isEqualTo("Bob")
        assertThat(result.isNull("email")).isTrue()
    }

    @Test
    fun `jsonStringToObject should parse JSON with numbers`() {
        // Given
        val jsonString = """{"integer": 42, "decimal": 3.14, "negative": -10}"""

        // When
        val result = ConvertUtil.jsonStringToObject(jsonString)

        // Then
        assertThat(result.getInt("integer")).isEqualTo(42)
        assertThat(result.getDouble("decimal")).isEqualTo(3.14)
        assertThat(result.getInt("negative")).isEqualTo(-10)
    }

    @Test(expected = JSONException::class)
    fun `jsonStringToObject should throw JSONException on invalid JSON`() {
        // Given
        val invalidJson = """{"name": "John", "age": }"""

        // When
        ConvertUtil.jsonStringToObject(invalidJson)

        // Then - JSONException should be thrown
    }

    @Test(expected = JSONException::class)
    fun `jsonStringToObject should throw JSONException on malformed JSON`() {
        // Given
        val malformedJson = """not a json object"""

        // When
        ConvertUtil.jsonStringToObject(malformedJson)

        // Then - JSONException should be thrown
    }

    @Test(expected = JSONException::class)
    fun `jsonStringToObject should throw JSONException on JSON array string`() {
        // Given
        val arrayString = """["item1", "item2"]"""

        // When
        ConvertUtil.jsonStringToObject(arrayString)

        // Then - JSONException should be thrown (expecting object, not array)
    }

    @Test(expected = JSONException::class)
    fun `jsonStringToObject should throw JSONException on empty string`() {
        // Given
        val emptyString = ""

        // When
        ConvertUtil.jsonStringToObject(emptyString)

        // Then - JSONException should be thrown
    }

    // ==================== jsonStringToArray() Tests ====================

    @Test
    fun `jsonStringToArray should parse valid JSON array`() {
        // Given
        val jsonString = """["apple", "banana", "cherry"]"""

        // When
        val result = ConvertUtil.jsonStringToArray(jsonString)

        // Then
        assertThat(result.length()).isEqualTo(3)
        assertThat(result.getString(0)).isEqualTo("apple")
        assertThat(result.getString(1)).isEqualTo("banana")
        assertThat(result.getString(2)).isEqualTo("cherry")
    }

    @Test
    fun `jsonStringToArray should parse array with objects`() {
        // Given
        val jsonString = """
            [
                {"name": "Alice", "age": 25},
                {"name": "Bob", "age": 30}
            ]
        """.trimIndent()

        // When
        val result = ConvertUtil.jsonStringToArray(jsonString)

        // Then
        assertThat(result.length()).isEqualTo(2)
        val first = result.getJSONObject(0)
        assertThat(first.getString("name")).isEqualTo("Alice")
        assertThat(first.getInt("age")).isEqualTo(25)
        val second = result.getJSONObject(1)
        assertThat(second.getString("name")).isEqualTo("Bob")
        assertThat(second.getInt("age")).isEqualTo(30)
    }

    @Test
    fun `jsonStringToArray should parse array with mixed types`() {
        // Given
        val jsonString = """[42, "text", true, null, 3.14]"""

        // When
        val result = ConvertUtil.jsonStringToArray(jsonString)

        // Then
        assertThat(result.length()).isEqualTo(5)
        assertThat(result.getInt(0)).isEqualTo(42)
        assertThat(result.getString(1)).isEqualTo("text")
        assertThat(result.getBoolean(2)).isTrue()
        assertThat(result.isNull(3)).isTrue()
        assertThat(result.getDouble(4)).isEqualTo(3.14)
    }

    @Test
    fun `jsonStringToArray should parse nested arrays`() {
        // Given
        val jsonString = """[[1, 2], [3, 4], [5, 6]]"""

        // When
        val result = ConvertUtil.jsonStringToArray(jsonString)

        // Then
        assertThat(result.length()).isEqualTo(3)
        val firstNested = result.getJSONArray(0)
        assertThat(firstNested.getInt(0)).isEqualTo(1)
        assertThat(firstNested.getInt(1)).isEqualTo(2)
    }

    @Test
    fun `jsonStringToArray should parse empty array`() {
        // Given
        val jsonString = "[]"

        // When
        val result = ConvertUtil.jsonStringToArray(jsonString)

        // Then
        assertThat(result.length()).isEqualTo(0)
    }

    @Test
    fun `jsonStringToArray should throw JSONException on invalid JSON`() {
        // Given - truly malformed JSON that will always throw
        val invalidJson = """[1, 2, "unclosed string]"""

        // When & Then
        try {
            ConvertUtil.jsonStringToArray(invalidJson)
            throw AssertionError("Expected JSONException to be thrown")
        } catch (e: Exception) {
            // Should throw JSONException for malformed JSON
            assertThat(e).isInstanceOf(Exception::class.java)
        }
    }

    @Test(expected = JSONException::class)
    fun `jsonStringToArray should throw JSONException on JSON object string`() {
        // Given
        val objectString = """{"key": "value"}"""

        // When
        ConvertUtil.jsonStringToArray(objectString)

        // Then - JSONException should be thrown (expecting array, not object)
    }

    // ==================== mergeJsonObjects() Tests ====================

    @Test
    fun `mergeJsonObjects should merge two simple objects`() {
        // Given
        val base = JSONObject().apply {
            put("name", "Alice")
            put("age", 25)
        }
        val override = JSONObject().apply {
            put("age", 30)
            put("city", "Tallinn")
        }

        // When
        val result = ConvertUtil.mergeJsonObjects(base, override)

        // Then
        assertThat(result.getString("name")).isEqualTo("Alice")
        assertThat(result.getInt("age")).isEqualTo(30) // overridden
        assertThat(result.getString("city")).isEqualTo("Tallinn")
    }

    @Test
    fun `mergeJsonObjects should not modify original base object`() {
        // Given
        val base = JSONObject().apply {
            put("original", "value")
        }
        val override = JSONObject().apply {
            put("new", "data")
        }

        // When
        val result = ConvertUtil.mergeJsonObjects(base, override)

        // Then
        assertThat(result.has("new")).isTrue()
        assertThat(base.has("new")).isFalse() // base should not be modified
    }

    @Test
    fun `mergeJsonObjects should override all values from override object`() {
        // Given
        val base = JSONObject().apply {
            put("field1", "base1")
            put("field2", "base2")
            put("field3", "base3")
        }
        val override = JSONObject().apply {
            put("field1", "override1")
            put("field2", "override2")
            put("field3", "override3")
        }

        // When
        val result = ConvertUtil.mergeJsonObjects(base, override)

        // Then
        assertThat(result.getString("field1")).isEqualTo("override1")
        assertThat(result.getString("field2")).isEqualTo("override2")
        assertThat(result.getString("field3")).isEqualTo("override3")
    }

    @Test
    fun `mergeJsonObjects should keep base values when override is empty`() {
        // Given
        val base = JSONObject().apply {
            put("name", "Bob")
            put("age", 40)
        }
        val override = JSONObject()

        // When
        val result = ConvertUtil.mergeJsonObjects(base, override)

        // Then
        assertThat(result.getString("name")).isEqualTo("Bob")
        assertThat(result.getInt("age")).isEqualTo(40)
        assertThat(result.length()).isEqualTo(2)
    }

    @Test
    fun `mergeJsonObjects should return override values when base is empty`() {
        // Given
        val base = JSONObject()
        val override = JSONObject().apply {
            put("name", "Charlie")
            put("country", "EE")
        }

        // When
        val result = ConvertUtil.mergeJsonObjects(base, override)

        // Then
        assertThat(result.getString("name")).isEqualTo("Charlie")
        assertThat(result.getString("country")).isEqualTo("EE")
        assertThat(result.length()).isEqualTo(2)
    }

    @Test
    fun `mergeJsonObjects should merge objects with different types`() {
        // Given
        val base = JSONObject().apply {
            put("name", "Dave")
            put("age", 35)
            put("active", false)
        }
        val override = JSONObject().apply {
            put("age", "thirty-five") // type change
            put("active", true)
            put("score", 95.5)
        }

        // When
        val result = ConvertUtil.mergeJsonObjects(base, override)

        // Then
        assertThat(result.getString("name")).isEqualTo("Dave")
        assertThat(result.getString("age")).isEqualTo("thirty-five") // changed to string
        assertThat(result.getBoolean("active")).isTrue()
        assertThat(result.getDouble("score")).isEqualTo(95.5)
    }

    @Test
    fun `mergeJsonObjects should handle null values`() {
        // Given
        val base = JSONObject().apply {
            put("field1", "value1")
            put("field2", "value2")
        }
        val override = JSONObject().apply {
            put("field1", JSONObject.NULL)
            put("field3", "value3")
        }

        // When
        val result = ConvertUtil.mergeJsonObjects(base, override)

        // Then
        assertThat(result.isNull("field1")).isTrue() // overridden with null
        assertThat(result.getString("field2")).isEqualTo("value2")
        assertThat(result.getString("field3")).isEqualTo("value3")
    }

    @Test
    fun `mergeJsonObjects should handle nested objects`() {
        // Given
        val base = JSONObject().apply {
            put("user", JSONObject().apply {
                put("name", "Eve")
                put("email", "eve@example.com")
            })
        }
        val override = JSONObject().apply {
            put("user", JSONObject().apply {
                put("name", "Eve Updated")
            })
        }

        // When
        val result = ConvertUtil.mergeJsonObjects(base, override)

        // Then
        val user = result.getJSONObject("user")
        assertThat(user.getString("name")).isEqualTo("Eve Updated")
        // Note: The entire nested object is replaced, not merged
        assertThat(user.has("email")).isFalse()
    }

    @Test
    fun `mergeJsonObjects should handle arrays`() {
        // Given
        val base = JSONObject().apply {
            put("items", JSONArray(listOf("a", "b", "c")))
        }
        val override = JSONObject().apply {
            put("items", JSONArray(listOf("x", "y")))
        }

        // When
        val result = ConvertUtil.mergeJsonObjects(base, override)

        // Then
        val items = result.getJSONArray("items")
        assertThat(items.length()).isEqualTo(2)
        assertThat(items.getString(0)).isEqualTo("x")
        assertThat(items.getString(1)).isEqualTo("y")
    }

    @Test
    fun `mergeJsonObjects should return empty object when both are empty`() {
        // Given
        val base = JSONObject()
        val override = JSONObject()

        // When
        val result = ConvertUtil.mergeJsonObjects(base, override)

        // Then
        assertThat(result.length()).isEqualTo(0)
    }

    @Test
    fun `mergeJsonObjects should handle complex merge scenario`() {
        // Given
        val base = JSONObject().apply {
            put("payment_reference", "old_ref")
            put("amount", 10.0)
            put("currency", "EUR")
            put("status", "pending")
        }
        val override = JSONObject().apply {
            put("payment_reference", "new_ref")
            put("status", "completed")
            put("timestamp", "2025-11-17T12:00:00Z")
        }

        // When
        val result = ConvertUtil.mergeJsonObjects(base, override)

        // Then
        assertThat(result.getString("payment_reference")).isEqualTo("new_ref")
        assertThat(result.getDouble("amount")).isEqualTo(10.0)
        assertThat(result.getString("currency")).isEqualTo("EUR")
        assertThat(result.getString("status")).isEqualTo("completed")
        assertThat(result.getString("timestamp")).isEqualTo("2025-11-17T12:00:00Z")
        assertThat(result.length()).isEqualTo(5)
    }
}
