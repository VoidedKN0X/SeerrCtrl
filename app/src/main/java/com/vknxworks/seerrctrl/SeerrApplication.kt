package com.vknxworks.seerrctrl

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade

class SeerrApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        App.init(this)
    }

    // Images go through the app's HTTP client, so Seerr's image cache and avatars get the session cookie
    // while TMDB, Plex and Gravatar images get normal certificate checks and no cookie.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { App.api.imageCallFactory })) }
            .crossfade(true)
            .build()
}
