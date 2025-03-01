package com.lupus.mobilepayment.ui.screens

import com.lupus.mobilepayment.ui.components.QrCodeReader
import com.lupus.mobilepayment.ui.components.QrCodeView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lupus.mobilepayment.changePixAmount
import com.lupus.mobilepayment.dataStore
import com.lupus.mobilepayment.getPixAmount
import com.lupus.mobilepayment.getPixCode
import com.lupus.mobilepayment.getPixUser
import com.lupus.mobilepayment.savePixCode
import com.lupus.mobilepayment.ui.components.CurrencyTextField
import com.lupus.mobilepayment.ui.theme.MobilePaymentTheme
import com.lupus.mobilepayment.updateAllWidgets
import com.lupus.mobilepayment.validateCrc
import kotlinx.coroutines.runBlocking

@Composable
fun Home() {
    val context = LocalContext.current
    val dataStore = context.dataStore

    val pixCode by getPixCode(dataStore).collectAsState("-1")

    updateAllWidgets(context)

    var transactionAmount by remember { mutableDoubleStateOf(getPixAmount(pixCode)) }

    var state by remember { mutableStateOf("ready") }
    var message by remember { mutableStateOf("Please scan a QR Code") }
    val mColor = MaterialTheme.colorScheme.onBackground
    var messageColor by remember { mutableStateOf(mColor) }

    val validCode = validateCrc(pixCode)

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Button(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 128.dp),
            onClick = {
                state = "scan"
            }) {
            Text("Scan")
        }
        when {
            state == "ready" && validCode && pixCode.isNotEmpty() -> {
                transactionAmount = getPixAmount(pixCode)
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QrCodeView(text = pixCode)
                    Text(
                        getPixUser(pixCode),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(24.dp)
                    )
                    CurrencyTextField(value = transactionAmount, onValueChange = { value ->
                        runBlocking {
                            savePixCode(dataStore, changePixAmount(pixCode, value))
                        }
                    })
                }
            }

            state == "scan" -> {
                QrCodeReader { code ->
                    runBlocking {
                        if (code != null) {
                            savePixCode(dataStore, code)
                        } else {
                            savePixCode(dataStore, "0")
                        }
                    }
                    state = "ready"
                }
            }

            else -> {
                if (!validCode && pixCode.isNotEmpty() && pixCode != "-1") {
                    message = "Invalid pix code"
                    messageColor = Color.Red
                } else if (pixCode.isEmpty()) {
                    message = "Please scan a QR Code"
                    messageColor = MaterialTheme.colorScheme.onBackground
                } else {
                    message = "Loading..."
                    messageColor = MaterialTheme.colorScheme.onBackground
                }

                Box(Modifier.fillMaxSize()) {
                    Text(
                        text = message,
                        color = messageColor,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun HomePreview() {
    MobilePaymentTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            Home()
        }
    }
}
