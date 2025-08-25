package com.quick.browser.data.local.entity

import android.graphics.Bitmap
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Entity representing a web page in history and offline storage
 *
 * This class defines the structure of the web pages table in the database.
 * It includes information about web pages visited by the user, including
 * content for offline viewing and metadata for display purposes.
 *
 * @property url The URL of the web page (primary key)
 * @property title The title of the web page
 * @property timestamp The timestamp when the page was visited
 * @property content The content of the web page for offline viewing
 * @property isAvailableOffline Whether the page is available for offline viewing
 * @property visitCount The number of times the page has been visited
 * @property favicon The favicon of the web page
 * @property faviconUrl The URL of the favicon
 * @property previewImageUrl The URL of a preview image for the page
 */
@Entity(tableName = "web_pages")
data class WebPage(
    @PrimaryKey
    val url: String,
    var title: String,
    var timestamp: Long,
    var content: String = "",
    var isAvailableOffline: Boolean = false,
    var visitCount: Int = 1,
    var favicon: Bitmap? = null,
    var faviconUrl: String? = null,
    var previewImageUrl: String? = null
) : Parcelable {

    // Transient fields not stored in the database
    @Ignore
    var summary: List<String> = emptyList()
    
    @Ignore
    var parentBubbleId: String? = null

    /**
     * Constructor to create a WebPage object from a Parcel.
     *
     * @param parcel The Parcel to read data from
     */
    private constructor(parcel: Parcel) : this(
        url = parcel.readString() ?: "",
        title = parcel.readString() ?: "",
        content = parcel.readString() ?: "",
        favicon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Bitmap::class.java.classLoader, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Bitmap::class.java.classLoader)
        },
        faviconUrl = parcel.readString(),
        previewImageUrl = parcel.readString(),
        timestamp = parcel.readLong(),
        visitCount = parcel.readInt()
    ) {
        summary = parcel.createStringArrayList() ?: emptyList()
        parentBubbleId = parcel.readString()
    }

    /**
     * Copies the transient fields to a new WebPage instance
     *
     * @param webPage The WebPage instance to copy fields to
     * @return The WebPage instance with copied transient fields
     */
    fun copyTransientFields(webPage: WebPage): WebPage {
        webPage.summary = this.summary
        webPage.parentBubbleId = this.parentBubbleId
        return webPage
    }

    /**
     * Writes the WebPage object to a Parcel for serialization.
     *
     * @param parcel The Parcel to write data to
     * @param flags Additional flags about how the object should be written
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeString(title)
        parcel.writeLong(timestamp)
        parcel.writeString(content)
        parcel.writeInt(if (isAvailableOffline) 1 else 0)
        parcel.writeInt(visitCount)
        parcel.writeParcelable(favicon, flags)
        parcel.writeString(faviconUrl)
        parcel.writeString(previewImageUrl)
        parcel.writeStringList(summary)
        parcel.writeString(parentBubbleId)
    }

    /**
     * Describes the contents of the Parcelable object.
     *
     * @return Always returns 0
     */
    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WebPage> {
        /**
         * Creates a WebPage object from a Parcel.
         *
         * @param parcel The Parcel to read data from
         * @return A new WebPage instance
         */
        override fun createFromParcel(parcel: Parcel): WebPage = WebPage(parcel)

        /**
         * Creates a new array of WebPage objects.
         *
         * @param size The size of the array
         * @return A new array of WebPage objects
         */
        override fun newArray(size: Int): Array<WebPage?> = arrayOfNulls(size)
    }
}