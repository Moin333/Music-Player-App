package com.example.musicplayerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.musicplayerapp.ui.theme.MusicPlayerAppTheme
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

class MainActivity : ComponentActivity() {
    private lateinit var realm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = RealmConfiguration.Builder(schema = setOf(Song::class)).build()
        realm = Realm.open(config)
        enableEdgeToEdge()
        setContent {
            MusicPlayerAppTheme {
                MusicApp(MusicViewModel(realm))
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }
}
