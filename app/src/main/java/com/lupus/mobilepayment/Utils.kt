package com.lupus.mobilepayment

import java.text.NumberFormat
import java.util.Locale

fun toCurrency(value: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return formatter.format(value)
}
