package com.zdmgold.autoclickernega

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.zdmgold.autoclickernega.ui.NegaRoot

/** The only activity. It is an AppCompat activity so the per-app language works on every supported Android version. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as NegaApp).graph
        setContent { NegaRoot(graph) }
    }
}
