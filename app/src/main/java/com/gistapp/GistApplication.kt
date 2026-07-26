package com.gistapp

import android.app.Application

/**
 * Aplikasi GitHub Gist Client.
 * Berfungsi penuh di Android 8.0 (API 26) ke atas.
 */
class GistApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GistApplication
            private set
    }
}
