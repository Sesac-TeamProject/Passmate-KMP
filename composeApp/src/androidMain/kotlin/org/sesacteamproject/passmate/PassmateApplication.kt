package org.sesacteamproject.passmate

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.sesacteamproject.passmate.core.di.initKoin
import org.sesacteamproject.passmate.di.viewModelModule

class PassmateApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@PassmateApplication)
            modules(viewModelModule)
        }
    }
}
