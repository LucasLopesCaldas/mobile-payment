package com.lupus.mobilepayment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.BitmapImageProvider
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lupus.mobilepayment.ui.components.QrCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

public object Widget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            context.WidgetContent(context)
        }
    }
}

@Composable
fun Context.WidgetContent(context: Context) {
    val pixCode by getPixCode(dataStore).collectAsState("")

    val amountDouble = getPixAmount(pixCode)

    var amountStr = amountDouble.toString().replace(".", "").replace(",", "")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pixCode.isEmpty()) {
            return@Column Button(
                text = "Scan a QrCode",
                onClick = actionStartActivity<MainActivity>()
            )
        }

        QrCodeView(pixCode)
        Text(
            getPixUser(pixCode),
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
        )
        Text(
            text = toCurrency(amountDouble),
            modifier = GlanceModifier.padding(12.dp),
            style = TextStyle(fontSize = 24.sp)
        )
        Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
            Keyboard(onKeyClick = { value ->
                when (value) {
                    "<-" -> {
                        amountStr = amountStr.dropLast(1)
                    }

                    else -> {
                        amountStr += if (amountStr.length < 8) {
                            value
                        } else {
                            ""
                        }
                    }
                }
                runBlocking {
                    try {
                        savePixCode(dataStore, changePixAmount(pixCode, (amountStr.toDouble() / 100)))
                        Log.i("teste", "$value -- $pixCode")
                    } catch (_: Exception) {
                        savePixCode(dataStore, changePixAmount(pixCode, 0.0))
                    }
                }
            }, context = context)
            Spacer(GlanceModifier.height(24.dp))
        }
    }
}

@Composable
fun Keyboard(onKeyClick: (keyValue: String) -> Unit, context: Context) {

    Column() {
        listOf(1, 2, 3, 4, 5, 6, 7, 8, 9).chunked(3).forEachIndexed { index, chunk ->
            Row(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier.fillMaxWidth()
            ) {

                chunk.forEachIndexed { index, key ->
                    Key(key.toString(), onKeyClick = action { onKeyClick(key.toString()) })
                    if (index < 2) {
                        Spacer(GlanceModifier.width(16.dp))
                    }
                }
            }
            if (index < 3) {
                Spacer(GlanceModifier.height(8.dp))
            }
        }
        Row(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            Key(
                "0",
                content = {
                    Image(
                        provider = ImageProvider(R.drawable.baseline_open_in_new_24),
                        contentDescription = "Open app",
                        modifier = GlanceModifier.size(32.dp),
                    )
                },
                onKeyClick = actionStartActivity<MainActivity>(),
            )
            Spacer(GlanceModifier.width(16.dp))
            Key("0", onKeyClick = action { onKeyClick("0") })
            Spacer(GlanceModifier.width(16.dp))
            Key(
                "<-",
                content = {
                    Image(
                        provider = ImageProvider(R.drawable.baseline_backspace_24),
                        contentDescription = "Backspace",
                        modifier = GlanceModifier.size(32.dp).padding(end = 2.dp),
                    )
                },
                onKeyClick = action { onKeyClick("<-") },
            )
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun Key(
    key: String,
    content: (@Composable () -> Unit)? = null,
    onKeyClick: Action = action { },
    visible: Boolean = true
) {
    val keyColor = if (visible) {
        MaterialTheme.colorScheme.onBackground
    } else {
        Color.Transparent
    }
    val textColor = ColorProvider(
        if (visible) {
            MaterialTheme.colorScheme.background
        } else {
            Color.Transparent
        }
    )

    Box(
        modifier = GlanceModifier
            .cornerRadius(100.dp)
            .size(72.dp)
            .clickable(onKeyClick)
            .background(keyColor),
        contentAlignment = Alignment.Center,
    ) {
        if (content == null) {
            Text(
                key.toString(),
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                ),
            )
        } else {
            content()
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun QrCodeView(text: String, size: Int = 200, modifier: GlanceModifier = GlanceModifier) {
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(text) {
        qrCodeBitmap = QrCode(text, size).bitmap()
    }

    qrCodeBitmap?.let { bitmap ->
        Image(
            provider = BitmapImageProvider(bitmap),
            contentDescription = "QR Code",
            modifier = modifier.size(size.dp),
        )
    } ?: run {
        Box(modifier = modifier.size(size.dp)) {}
    }
}