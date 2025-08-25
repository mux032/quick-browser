package com.quick.browser.utils

import android.content.Context
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
 *
 * This module configures Glide with custom cache sizes and default options
 * optimized for the browser application. It sets up disk and memory caching
 * strategies to balance performance with resource usage.
 */
@GlideModule
class QuickBrowserGlideModule : AppGlideModule() {
    companion object {
        private const val TAG = "QuickBrowserGlideModule"
        private const val DISK_CACHE_SIZE_BYTES = 100 * 1024 * 1024L // 100 MB
        private const val MEMORY_CACHE_SIZE_BYTES = 20 * 1024 * 1024L // 20 MB
    }

    /**
     * Apply custom options to Glide configuration
     *
     * This method configures Glide with custom disk and memory cache sizes,
     * and sets default request options for optimal image loading performance.
     *
     * @param context The application context
     * @param builder The GlideBuilder instance to configure
     */
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
        
        Logger.d(TAG, "Glide module configured with disk cache: ${DISK_CACHE_SIZE_BYTES} bytes, memory cache: ${MEMORY_CACHE_SIZE_BYTES} bytes")
    }

    /**
     * Register custom components with Glide
     *
     * This method allows registering custom components such as ModelLoaders,
     * ResourceDecoders, etc. with Glide's registry.
     *
     * @param context The application context
     * @param glide The Glide instance
     * @param registry The registry to register components with
     */
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Register custom components if needed
        super.registerComponents(context, glide, registry)
    }

    /**
     * Disable manifest parsing to avoid startup time overhead
     *
     * @return False to disable manifest parsing
     */
    override fun isManifestParsingEnabled(): Boolean = false
}