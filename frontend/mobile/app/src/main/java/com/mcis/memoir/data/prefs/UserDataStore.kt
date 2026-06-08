package com.mcis.memoir.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_prefs",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "user_prefs"))
    }
)
