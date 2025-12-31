package com.hiren.offlineaar

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppClass : Application() {

    /*@Inject
    lateinit var grpcSdk: GrpcSdk*/

    override fun onCreate() {
        super.onCreate()

        //grpcSdk.start(this)

        //GrpcSdkProvider.init()
        //GrpcSdkProvider.getInstance().start(this)

    }
}