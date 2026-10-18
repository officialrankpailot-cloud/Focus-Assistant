package com.example

import android.app.Application
import com.example.data.local.DisciplineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DisciplineApp : Application() {
    val database: DisciplineDatabase by lazy {
        DisciplineDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Ensure initial seeds exist
        CoroutineScope(Dispatchers.IO).launch {
            val stats = database.disciplineStatsDao().getStatsSync()
            if (stats == null) {
                DisciplineDatabase.populateInitialData(database)
            }
        }
    }

    companion object {
        lateinit var instance: DisciplineApp
            private set
    }
}
