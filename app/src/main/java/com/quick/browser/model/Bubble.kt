/**
 * Bubble.kt
 *
 * Summary:
 * This data class represents a "Bubble" entity used in the Quick Browser application. Each bubble contains
 * information about a URL, its title, favicon, and the last accessed timestamp. The class implements the
 * `Parcelable` interface to allow efficient serialization and deserialization of Bubble objects for Android IPC.
 *
 * Features:
 * - Parcelable implementation for efficient object transfer between components.
 * - Default values for optional fields like `title` and `favicon`.
 * - Optimized handling of `Parcel` operations with null safety.
 *
 * Improvements:
 * - Added detailed documentation for better code readability.
 * - Ensured null safety for `Bitmap` and `String` fields during parceling.
 * - Optimized `Parcelable.Creator` implementation for clarity.
 */

 package com.quick.browser.model

 import android.graphics.Bitmap
 import android.os.Build
 import android.os.Parcel
 import android.os.Parcelable
 
 /**
  * Represents a bubble entity with details about a URL, its title, favicon, and last accessed timestamp.
  *
  * @property id Unique identifier for the bubble.
  * @property url URL associated with the bubble.
  * @property title Title of the webpage (optional, defaults to an empty string).
  * @property favicon Favicon of the webpage (optional, defaults to null).
  * @property lastAccessed Timestamp of the last access (defaults to the current system time).
  */
 data class Bubble(
     val id: String,
     val url: String,
     val title: String = "",
     val favicon: Bitmap? = null,
     val lastAccessed: Long = System.currentTimeMillis()
 ) : Parcelable {
 
     /**
      * Constructor to create a Bubble object from a Parcel.
      *
      * @param parcel The Parcel containing serialized Bubble data.
      */
     private constructor(parcel: Parcel) : this(
         id = parcel.readString() ?: "",
         url = parcel.readString() ?: "",
         title = parcel.readString() ?: "",
         favicon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             parcel.readParcelable(Bitmap::class.java.classLoader, Bitmap::class.java)
         } else {
             @Suppress("DEPRECATION") // Suppress the deprecation warning for older Android versions
             parcel.readParcelable(Bitmap::class.java.classLoader)
         },
         lastAccessed = parcel.readLong()
     )

     /**
      * Writes the Bubble object to a Parcel for serialization.
      *
      * @param parcel The Parcel to write data into.
      * @param flags Additional flags for parceling.
      */
     override fun writeToParcel(parcel: Parcel, flags: Int) {
         parcel.writeString(id)
         parcel.writeString(url)
         parcel.writeString(title)
         parcel.writeParcelable(favicon, flags)
         parcel.writeLong(lastAccessed)
     }
 
     /**
      * Describes the contents of the Parcelable object.
      *
      * @return An integer representing the contents (default is 0).
      */
     override fun describeContents(): Int = 0
 
     companion object CREATOR : Parcelable.Creator<Bubble> {
         /**
          * Creates a Bubble object from a Parcel.
          *
          * @param parcel The Parcel containing serialized Bubble data.
          * @return A new Bubble object.
          */
         override fun createFromParcel(parcel: Parcel): Bubble = Bubble(parcel)
 
         /**
          * Creates a new array of Bubble objects.
          *
          * @param size The size of the array.
          * @return An array of Bubble objects, initialized to null.
          */
         override fun newArray(size: Int): Array<Bubble?> = arrayOfNulls(size)
     }
 }