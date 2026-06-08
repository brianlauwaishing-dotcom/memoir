package com.mcis.memoir

import android.app.Application
import com.mcis.memoir.data.content.AssetManagerContentLoader
import com.mcis.memoir.data.content.ContentJson
import com.mcis.memoir.data.content.ContentRepository
import com.mcis.memoir.data.prefs.DataStoreUserPreferencesRepository
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import com.mcis.memoir.data.prefs.userDataStore
import com.mcis.memoir.i18n.LocaleController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MemoirApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        prefs = DataStoreUserPreferencesRepository(applicationContext.userDataStore)
        LocaleController.reconcileAtStartup(prefs, appScope)

        content = ContentRepository(AssetManagerContentLoader(assets, ContentJson), appScope)
    }

    companion object {
        lateinit var prefs: UserPreferencesRepository
            private set
        lateinit var content: ContentRepository
            private set
    }
}
