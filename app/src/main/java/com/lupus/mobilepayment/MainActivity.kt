package com.lupus.mobilepayment

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.lupus.mobilepayment.ui.screens.Home
import com.lupus.mobilepayment.ui.theme.MobilePaymentTheme
import kotlinx.coroutines.runBlocking
import androidx.glance.appwidget.updateAll

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        runBlocking {
//            Widget.updateAll(this@MainActivity)
//        }


        enableEdgeToEdge()
        setContent {
            MobilePaymentTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        Home()
                    }
                }
            }
        }
    }
}


