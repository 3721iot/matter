package com.dsh.tether

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class TetherApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        // Plant a tree
        Timber.plant(
            object :  Timber.DebugTree(){
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    super.log(priority, "Tether-$tag", message, t)
                }
            }
        )
    }
}