package com.mcis.memoir.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat
import com.mcis.memoir.data.prefs.UserPreferencesRepository
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LocaleController {
    private var localeSource: LocaleSource = AppCompatLocaleSource

    suspend fun setLocale(tag: String, prefs: UserPreferencesRepository) {
        prefs.setLanguage(tag)
        withContext(Dispatchers.Main.immediate) {
            localeSource.setAppLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    fun reconcileAtStartup(prefs: UserPreferencesRepository, scope: CoroutineScope) {
        scope.launch {
            val appCompat = localeSource.appLocales()
            val storedTag = prefs.persistedLanguageTag()
            when {
                !appCompat.isEmpty() -> {
                    val tag = appCompat.toLanguageTags()
                    if (tag != storedTag) {
                        prefs.setLanguage(tag)
                    }
                }
                storedTag != null -> {
                    withContext(Dispatchers.Main.immediate) {
                        localeSource.setAppLocales(LocaleListCompat.forLanguageTags(storedTag))
                    }
                }
                else -> Unit
            }
        }
    }

    @Composable
    fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

    internal fun replaceLocaleSourceForTest(source: LocaleSource) {
        localeSource = source
    }

    internal fun resetLocaleSourceForTest() {
        localeSource = AppCompatLocaleSource
    }
}

internal interface LocaleSource {
    fun appLocales(): LocaleListCompat
    fun setAppLocales(tags: LocaleListCompat)
}

private object AppCompatLocaleSource : LocaleSource {
    override fun appLocales(): LocaleListCompat =
        AppCompatDelegate.getApplicationLocales()

    override fun setAppLocales(tags: LocaleListCompat) {
        AppCompatDelegate.setApplicationLocales(tags)
    }
}
