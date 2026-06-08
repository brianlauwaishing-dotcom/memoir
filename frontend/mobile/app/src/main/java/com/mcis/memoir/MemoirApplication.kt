package com.mcis.memoir

import android.app.Application
import com.mcis.memoir.data.content.AssetManagerContentLoader
import com.mcis.memoir.data.content.ContentJson
import com.mcis.memoir.data.content.ContentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MemoirApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        content = ContentRepository(AssetManagerContentLoader(assets, ContentJson), scope)
    }

    companion object {
        lateinit var content: ContentRepository
            private set
    }
}
