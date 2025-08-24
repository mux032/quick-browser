package com.quick.browser.data.local.database

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream

/**
 * Type converters for Room database
 * Handles conversion of complex types to and from database-storable types
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromBitmap(bitmap: Bitmap?): ByteArray? {
        if (bitmap == null) return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    @TypeConverter
    fun toBitmap(bytes: ByteArray?): Bitmap? {
        return bytes?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }

    /**
     * Converts a List<String> to a JSON string for storage in the database
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