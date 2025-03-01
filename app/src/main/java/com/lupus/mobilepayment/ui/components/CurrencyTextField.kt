package com.lupus.mobilepayment.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.lupus.mobilepayment.toCurrency
import com.lupus.mobilepayment.ui.theme.Typography

@Composable
fun CurrencyTextField(
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = TextFieldValue(
            toCurrency(value),
            selection = TextRange(toCurrency(value).length)
        ),
        onValueChange = { input ->
            val cleanInput = input.text.replace("\\D".toRegex(), "")
            onValueChange(cleanInput.toDouble()/100)
        },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        textStyle = Typography.titleLarge,
        colors = TextFieldDefaults.colors(
            cursorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        singleLine = true
    )
}