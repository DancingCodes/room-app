package love.moonc.room.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import love.moonc.room.core.network.NetworkConfig
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.storage.TokenStore
import love.moonc.room.data.websocket.RoomSocketFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val tokenStore: TokenStore = TokenStore(context.applicationContext)

    private val json = Json {
        ignoreUnknownKeys = true
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = runBlocking { tokenStore.token.firstOrNull() }
            val request = if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }
        .apply {
            if (NetworkConfig.ENABLE_HTTP_LOGGING) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    },
                )
            }
        }
        .build()

    val roomApi: RoomApi = Retrofit.Builder()
        .baseUrl(NetworkConfig.BACKEND_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RoomApi::class.java)

    val roomSocketFactory: RoomSocketFactory = RoomSocketFactory(okHttpClient)
}
