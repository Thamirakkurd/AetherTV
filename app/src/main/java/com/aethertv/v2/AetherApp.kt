package com.aethertv.v2

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.aethertv.v2.data.XtreamClient
import java.io.File

class AetherApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .okHttpClient { XtreamClient.imageHttpClient }
            .memoryCache {
                MemoryCache.Builder(applicationContext)
                    .maxSizePercent(0.30)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(applicationContext.cacheDir, "poster_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .allowHardware(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
    }
}
