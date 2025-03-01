package com.lupus.mobilepayment

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")
val TRANSACTION_AMOUNT_KEY = stringPreferencesKey("transactionAmount")
val PIX_CODE_KEY = stringPreferencesKey("pixCode")

suspend fun saveTransactionAmount(dataStore: DataStore<Preferences>, amount: String) {
    dataStore.edit { preferences ->
        preferences[TRANSACTION_AMOUNT_KEY] = amount
    }
}

suspend fun savePixCode(dataStore: DataStore<Preferences>, pixCode: String) {
    dataStore.edit { preferences ->
        preferences[PIX_CODE_KEY] = pixCode
    }
}

fun getTransactionAmount(dataStore: DataStore<Preferences>,): Flow<String> {
    return dataStore.data.map { preferences ->
        preferences[TRANSACTION_AMOUNT_KEY] ?: "0.0"
    }
}

fun getPixCode(dataStore: DataStore<Preferences>,): Flow<String> {
    return dataStore.data.map { preferences ->
        preferences[PIX_CODE_KEY] ?: ""
    }
}
