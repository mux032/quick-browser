package com.quick.browser.data.local.database

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream

/**
 * Type converters for Room database
 *
 * Handles conversion of complex types to and from database-storable types.
 * This class provides converters for Bitmap images and String lists.
 */
class Converters {
    private val gson = Gson()

    /**
     * Convert a Bitmap to a ByteArray for storage in the database
     *
     * @param bitmap The Bitmap to convert
     * @return The ByteArray representation of the Bitmap, or null if the input was null
     */
    @TypeConverter
    fun fromBitmap(bitmap: Bitmap?): ByteArray? {
        if (bitmap == null) return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Convert a ByteArray from the database back to a Bitmap
     *
     * @param bytes The ByteArray to convert
     * @return The Bitmap representation of the ByteArray, or null if the input was null
     */
    @TypeConverter
    fun toBitmap(bytes: ByteArray?): Bitmap? {
        return bytes?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }

    /**
     * Converts a List<String> to a JSON string for storage in the database
     *
     * @param value The List<String> to convert
     * @return The JSON string representation of the list
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return if (value == null || value.isEmpty()) {
            ""
        } else {
            gson.toJson(value)
        }
    }

    /**
     * Converts a JSON string from the database back to a List<String>
     *
     * @param value The JSON string to convert
     * @return The List<String> representation of the JSON string
     */
    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) {
            return emptyList()
        }

        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }
}