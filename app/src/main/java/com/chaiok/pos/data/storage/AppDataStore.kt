package com.chaiok.pos.data.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "chaiok_store")

class AppDataStore(private val context: Context) {
    private object Keys {
        val integrationMode = booleanPreferencesKey("integration_mode")
        val tableMode = booleanPreferencesKey("table_mode")
        val tileBackground = stringPreferencesKey("tile_background")
        val waiterStatus = stringPreferencesKey("waiter_status")
        val hasLinkedCard = booleanPreferencesKey("has_linked_card")
        val cardSha = stringPreferencesKey("card_sha")
    }

    val integrationModeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.integrationMode] ?: false }
    val tableModeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.tableMode] ?: false }
    val tileBackgroundFlow: Flow<String> = context.dataStore.data.map { it[Keys.tileBackground] ?: "default" }
    val waiterStatusFlow: Flow<String> = context.dataStore.data.map { it[Keys.waiterStatus] ?: "На смене" }
    val hasLinkedCardFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.hasLinkedCard] ?: false }
    val cardShaFlow: Flow<String?> = context.dataStore.data.map { it[Keys.cardSha] }

    suspend fun setIntegrationMode(value: Boolean) = context.dataStore.edit { it[Keys.integrationMode] = value }
    suspend fun setTableMode(value: Boolean) = context.dataStore.edit { it[Keys.tableMode] = value }
    suspend fun setTileBackground(value: String) = context.dataStore.edit { it[Keys.tileBackground] = value }
    suspend fun setWaiterStatus(value: String) = context.dataStore.edit { it[Keys.waiterStatus] = value }
    suspend fun setHasLinkedCard(value: Boolean) = context.dataStore.edit { it[Keys.hasLinkedCard] = value }
    suspend fun setCardSha(value: String) = context.dataStore.edit { it[Keys.cardSha] = value }
}
