package com.lupus.mobilepayment

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.lupus.mobilepayment.ui.components.QrCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.text.replace

//class WidgetReceiver : GlanceAppWidgetReceiver() {
//    override val glanceAppWidget: GlanceAppWidget
//        get() = Widget
//}

class WidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        if (context == null) return

        appWidgetIds?.forEach { appWidgetId ->
            val views = RemoteViews(
                context.packageName,
                R.layout.widget_layout
            ).apply {
                runBlocking {
                    val code = getPixCode(context.dataStore).first()
                    setTextViewText(
                        R.id.widget_amount,
                        toCurrency(getPixAmount(code))
                    )
                    setTextViewText(
                        R.id.user_text,
                        getPixUser(code)
                    )
                    val bitmap = QrCode(code, 256).bitmap()
                    if (bitmap != null) {
                        setImageViewBitmap(R.id.qr_image, bitmap)
                    }
                }

                setOnClickPendingIntent(
                    R.id.button1,
                    callWidgetAction(context, ACTION_KEY_CLICK + "1", appWidgetId)
                )

                setOnClickPendingIntent(
                    R.id.button2,
                    callWidgetAction(context, ACTION_KEY_CLICK + "2", appWidgetId)
                )

                setOnClickPendingIntent(
                    R.id.button3,
                    callWidgetAction(context, ACTION_KEY_CLICK + "3", appWidgetId)
                )

                setOnClickPendingIntent(
                    R.id.button4,
                    callWidgetAction(context, ACTION_KEY_CLICK + "4", appWidgetId)
                )

                setOnClickPendingIntent(
                    R.id.button5,
                    callWidgetAction(context, ACTION_KEY_CLICK + "5", appWidgetId)
                )

                setOnClickPendingIntent(
                    R.id.button6,
                    callWidgetAction(context, ACTION_KEY_CLICK + "6", appWidgetId)
                )

                setOnClickPendingIntent(
                    R.id.button7,
                    callWidgetAction(context, ACTION_KEY_CLICK + "7", appWidgetId)
                )

                setOnClickPendingIntent(
                    R.id.button8,
                    callWidgetAction(context, ACTION_KEY_CLICK + "8", appWidgetId)
                )

                setOnClickPendingIntent(
                    R.id.button9,
                    callWidgetAction(context, ACTION_KEY_CLICK + "9", appWidgetId)
                )

                setOnClickPendingIntent(
                    R.id.button0,
                    callWidgetAction(context, ACTION_KEY_CLICK + "0", appWidgetId)
                )

                setOnClickPendingIntent(
                    R.id.button_backspace,
                    callWidgetAction(context, ACTION_KEY_CLICK + "backspace", appWidgetId)
                )

                setOnClickPendingIntent(
                    R.id.button_open_app,
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

            appWidgetManager?.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent != null && context != null) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val key = try {
                (intent.action ?: "-").substringAfter("-")
            } catch (_: Exception) {
                ""
            }

            val action = intent.action

            if (appWidgetId != -1 && action != null) {
                val views = RemoteViews(
                    context.packageName,
                    R.layout.widget_layout
                ).apply {
                    when {
                        action.contains("KEY_CLICK") -> {
                            runBlocking {
                                val pixCode = getPixCode(context.dataStore).first()
                                val amount = getPixAmount(pixCode)
                                listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9").forEach {
                                    if (key == it && amount.toString().length < 8) {
                                        val newAmount =
                                            (toCurrency(amount) + it).replace("\\D".toRegex(), "")
                                                .toDouble() / 100
                                        savePixCode(
                                            context.dataStore,
                                            changePixAmount(pixCode, newAmount)
                                        )
                                    }
                                }
                                if (key == "backspace") {
                                    val newAmount =
                                        toCurrency(amount).replace("\\D".toRegex(), "")
                                            .dropLast(1)
                                            .toDouble() / 100
                                    savePixCode(
                                        context.dataStore,
                                        changePixAmount(pixCode, newAmount)
                                    )
                                }
                                val newCode = getPixCode(context.dataStore).first()
                                setTextViewText(
                                    R.id.widget_amount,
                                    toCurrency(getPixAmount(newCode))
                                )
                                setTextViewText(
                                    R.id.user_text,
                                    getPixUser(newCode)
                                )
                                val bitmap = QrCode(newCode, 256).bitmap()
                                if (bitmap != null) {
                                    setImageViewBitmap(R.id.qr_image, bitmap)
                                }
                            }
                        }
                    }
                }

                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

const val ACTION_KEY_CLICK = "com.lupus.mobilepayment.KEY_CLICK-"

fun callWidgetAction(context: Context?, action: String, appWidgetId: Int): PendingIntent {
    val intent = Intent(context, WidgetReceiver::class.java).apply {
        this.action = action
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

    return PendingIntent.getBroadcast(
        context,
        appWidgetId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

fun updateAllWidgets(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)

    val widgetProvider = ComponentName(context, WidgetReceiver::class.java)

    val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider)

    val intent = Intent(context, WidgetReceiver::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
    }
    context.sendBroadcast(intent)
}