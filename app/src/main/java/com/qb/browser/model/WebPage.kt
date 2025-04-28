package com.qb.browser.model

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Entity representing a web page in history and offline storage
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
    var favicon: Bitmap? = null
) : Parcelable {

    // Transient fields not stored in the database
    @Ignore
    var summary: List<String> = emptyList()
    
    @Ignore
    var parentBubbleId: String? = null

    /**
     * No-arg constructor required by Room
     */
    constructor() : this(
        url = "",
        title = "",
        timestamp = 0,
        content = "",
        isAvailableOffline = false,
        visitCount = 0,
        favicon = null
    )

    /**
     * Constructor to create a WebPage object from a Parcel.
     */
    private constructor(parcel: Parcel) : this(
        url = parcel.readString() ?: "",
        title = parcel.readString() ?: "",
        timestamp = parcel.readLong(),
        content = parcel.readString() ?: "",
        isAvailableOffline = parcel.readInt() == 1,
        visitCount = parcel.readInt(),
        favicon = parcel.readParcelable(Bitmap::class.java.classLoader)
    ) {
        summary = parcel.createStringArrayList() ?: emptyList()
        parentBubbleId = parcel.readString()
    }

    /**
     * Copies the transient fields to a new WebPage instance
     */
    fun copyTransientFields(webPage: WebPage): WebPage {
        webPage.summary = this.summary
        webPage.parentBubbleId = this.parentBubbleId
        return webPage
    }

    /**
     * Writes the WebPage object to a Parcel for serialization.
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeString(title)
        parcel.writeLong(timestamp)
        parcel.writeString(content)
        parcel.writeInt(if (isAvailableOffline) 1 else 0)
        parcel.writeInt(visitCount)
        parcel.writeParcelable(favicon, flags)
        parcel.writeStringList(summary)
        parcel.writeString(parentBubbleId)
    }

    /**
     * Describes the contents of the Parcelable object.
     */
    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WebPage> {
        /**
         * Creates a WebPage object from a Parcel.
         */
        override fun createFromParcel(parcel: Parcel): WebPage = WebPage(parcel)

        /**
         * Creates a new array of WebPage objects.
         */
        override fun newArray(size: Int): Array<WebPage?> = arrayOfNulls(size)
    }
}
