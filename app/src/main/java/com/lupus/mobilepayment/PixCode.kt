package com.lupus.mobilepayment

import android.util.Log


fun getPixAmount(pixCode: String): Double {
    val amountReg = Regex("(54\\d\\d)(\\d+\\.\\d\\d)")
    val amount = amountReg.find(pixCode)?.groups[2]?.value?.toDouble()
    return amount ?: 0.0
}

fun getPixUser(pixCode: String): String {
    val amountReg = Regex("(59\\d\\d)([A-Za-z\\s]+)")
    val amount = amountReg.find(pixCode)?.groups[2]?.value
    return amount ?: "Fail"
}

fun changePixAmount(pixCode: String, newAmount: Double): String {
    val newAmountStr = "%.2f".format(newAmount).replace(",", ".")

    val amountReg = Regex("(54\\d\\d)(\\d+\\.\\d\\d)")
    val countryReg = Regex("58\\d\\d[A-Z]{2}")
    val newSize = newAmountStr.length

    if (newAmount == 0.00) {
        return newCrc(amountReg.replace(pixCode, ""))
    }

    var id: List<String>? = null

    var changedPixCode = amountReg.replace(pixCode) { matchResult ->
        id = matchResult.groupValues
        "54%02d%s".format(newSize, newAmountStr)
    }

    if (id?.get(2) == "%s".format(newAmountStr)) {
        return pixCode
    }

    val country = countryReg.find(changedPixCode)

    if (changedPixCode == pixCode && country != null) {
        changedPixCode = pixCode.substring(0, country.range.start)
        changedPixCode += "54%02d%s".format(newSize, newAmountStr)
        changedPixCode += pixCode.substring(country.range.start)
    }

    return newCrc(changedPixCode)
}

fun calculateCrc(pixCode: String): String {
    val crc16Polynomial = 0x1021
    var crc = 0xFFFF

    for (char in pixCode.toByteArray()) {
        crc = crc xor (char.toInt() shl 8)
        for (i in 0 until 8) {
            crc = if ((crc and 0x8000) != 0) {
                (crc shl 1) xor crc16Polynomial
            } else {
                crc shl 1
            }
        }
        crc = crc and 0xFFFF
    }

    return String.format("%04X", crc)
}

fun newCrc(pixCode: String): String {
    val noCrcCode = if (pixCode.contains("6304")) {
        pixCode.substring(0, pixCode.indexOf("6304") + 4)
    } else {
        pixCode
    }
    val newCrc = calculateCrc(noCrcCode)
    return "$noCrcCode$newCrc"
}

fun validateCrc(pixCode: String): Boolean {
    val crcReg = Regex("6304([A-Fa-f0-9]{4})$")
    val matchResult = crcReg.find(pixCode)

    if (matchResult != null) {
        val currentCrc = matchResult.groups[1]?.value
        val noCrcCode = pixCode.substring(0, pixCode.length - 4) // Exclui o CRC atual
        val calculatedCrc = calculateCrc(noCrcCode)

        return currentCrc.equals(calculatedCrc, ignoreCase = true)
    }
    return false
}

//00020126360014br.gov.bcb.pix0114+5598984294874520400005303986540510.005802BR5918Lucas Lopes Caldas6009Sao Paulo62290525REC678BCBC0DB9B48460464606304B92A