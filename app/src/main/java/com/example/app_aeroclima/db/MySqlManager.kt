package com.example.app_aeroclima.db

import com.google.firebase.auth.FirebaseAuth
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// --- MODELOS ---
data class BackendResponse(
    val status: String?,
    val message: String?,
    val is_favorite: Boolean?
)

// --- INTERFAZ API ---
interface BackendApiService {
    @FormUrlEncoded
    @POST("add_favorite.php")
    suspend fun addFavorite(
        @Field("email") email: String,
        @Field("icao") icao: String
    ): Response<BackendResponse>

    @FormUrlEncoded
    @POST("remove_favorite.php")
    suspend fun removeFavorite(
        @Field("email") email: String,
        @Field("icao") icao: String
    ): Response<BackendResponse>

    @GET("check_favorite.php")
    suspend fun checkFavorite(
        @Query("email") email: String,
        @Query("icao") icao: String
    ): Response<BackendResponse>
}

// --- MANAGER ---
class MySqlManager {

    private val auth = FirebaseAuth.getInstance()
    private val service: BackendApiService

    init {
        // URL de Ngrok (Recuerda cambiarla si reinicias Ngrok)
        val BASE_URL = "https://noncorrespondingly-didymous-basil.ngrok-free.dev/aeroclima_api/"

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        // Logger (Muestra las peticiones en Logcat si es necesario debuggear)
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                // Header especial para saltar la advertencia de Ngrok gratuito
                val request = chain.request().newBuilder()
                    .header("ngrok-skip-browser-warning", "true")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()

        service = retrofit.create(BackendApiService::class.java)
    }

    private fun getCurrentEmail(): String? {
        return auth.currentUser?.email
    }

    // Función AGREGAR
    fun addFavorite(icao: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val email = getCurrentEmail()
        if (email == null) {
            onFailure()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.addFavorite(email, icao)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        onSuccess()
                    } else {
                        onFailure()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // Imprime el error solo en consola para el desarrollador
                withContext(Dispatchers.Main) { onFailure() }
            }
        }
    }

    // Función ELIMINAR
    fun removeFavorite(icao: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val email = getCurrentEmail()
        if (email == null) {
            onFailure()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.removeFavorite(email, icao)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        onSuccess()
                    } else {
                        onFailure()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onFailure() }
            }
        }
    }

    // Función VERIFICAR
    fun checkIsFavorite(icao: String, onResult: (Boolean) -> Unit) {
        val email = getCurrentEmail()
        if (email == null) {
            onResult(false)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.checkFavorite(email, icao)
                // Si la respuesta es correcta y is_favorite es true, devolvemos true
                val isFav = response.body()?.is_favorite ?: false
                withContext(Dispatchers.Main) { onResult(isFav) }
            } catch (e: Exception) {
                // Si falla la conexión, asumimos que no es favorito (para no bloquear la UI)
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }
}