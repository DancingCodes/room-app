package love.moonc.room.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.roomDataStore by preferencesDataStore(name = "room_settings")

class TokenStore(
    private val context: Context,
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    val token: Flow<String?> = context.roomDataStore.data.map { preferences ->
        preferences[tokenKey]
    }

    suspend fun saveToken(token: String) {
        context.roomDataStore.edit { preferences ->
            preferences[tokenKey] = token
        }
    }

    suspend fun clearToken() {
        context.roomDataStore.edit { preferences ->
            preferences.remove(tokenKey)
        }
    }
}
