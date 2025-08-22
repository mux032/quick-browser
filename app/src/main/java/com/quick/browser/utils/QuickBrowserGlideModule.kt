package com.quick.browser.utils

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule

/**
 * Custom Glide module for the Quick Browser app
 */
@GlideModule
class QuickBrowserGlideModule : AppGlideModule() {
    companion object {
        private const val TAG = "QuickBrowserGlideModule"
        private const val DISK_CACHE_SIZE_BYTES = 100 * 1024 * 1024L // 100 MB
        private const val MEMORY_CACHE_SIZE_BYTES = 20 * 1024 * 1024L // 20 MB
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Set disk cache size
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, DISK_CACHE_SIZE_BYTES))
        
        // Set memory cache size
        builder.setMemoryCache(LruResourceCache(MEMORY_CACHE_SIZE_BYTES))
        
        // Set default disk cache strategy
        builder.setDefaultRequestOptions(
            com.bumptech.glide.request.RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        )
        
        Log.d(TAG, "Glide module configured with disk cache: ${DISK_CACHE_SIZE_BYTES} bytes, memory cache: ${MEMORY_CACHE_SIZE_BYTES} bytes")
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Register custom components if needed
        super.registerComponents(context, glide, registry)
    }

    // Disable manifest parsing to avoid startup time overhead
    override fun isManifestParsingEnabled(): Boolean = false
}