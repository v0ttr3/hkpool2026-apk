package com.hkpool.app.ui
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.room.Room
import com.hkpool.app.data.AppDatabase

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "hkpool.db").fallbackToDestructiveMigration().build()
        setContent { MaterialTheme { AppNav(db, this) } }
    }
}
