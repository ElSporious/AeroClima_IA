package com.example.app_aeroclima.db

import com.google.firebase.auth.FirebaseAuth
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Lo que enviamos al servidor (Email + ICAO)
data class FavoriteRequest(
    val email: String,
    val icao: String
)

data class BackendResponse(
    val status: String?,
    val message: String?,
    val is_favorite: Boolean?
)

interface BackendApiService {
    @POST("add_favorite.php")
    suspend fun addFavorite(@Body request: FavoriteRequest): Response<BackendResponse>

    @POST("remove_favorite.php")
    suspend fun removeFavorite(@Body request: FavoriteRequest): Response<BackendResponse>

    @GET("check_favorite.php")
    suspend fun checkFavorite(
        @Query("email") email: String,
        @Query("icao") icao: String
    ): Response<BackendResponse>
}

class MySqlManager {

    private val auth = FirebaseAuth.getInstance()
    private val service: BackendApiService

    init {

        val BASE_URL = "http://aeroclima.xo.je/aeroclima_api/"

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        service = retrofit.create(BackendApiService::class.java)
    }

    // Obtiene el correo del usuario logueado actualmente (Google o Local)
    private fun getCurrentEmail(): String? {
        return auth.currentUser?.email
    }

    // Función para AGREGAR favorito
    fun addFavorite(icao: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val email = getCurrentEmail()
        if (email == null) {
            onFailure() // No hay usuario logueado
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.addFavorite(FavoriteRequest(email, icao))
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        onSuccess()
                    } else {
                        onFailure()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onFailure() }
            }
        }
    }

    // Función para ELIMINAR favorito
    fun removeFavorite(icao: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val email = getCurrentEmail()
        if (email == null) {
            onFailure()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.removeFavorite(FavoriteRequest(email, icao))
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        onSuccess()
                    } else {
                        onFailure()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onFailure() }
            }
        }
    }

    // Función para VERIFICAR si ya es favorito (para pintar la estrella)
    fun checkIsFavorite(icao: String, onResult: (Boolean) -> Unit) {
        val email = getCurrentEmail()
        if (email == null) {
            onResult(false)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.checkFavorite(email, icao)
                val isFav = response.body()?.is_favorite ?: false
                withContext(Dispatchers.Main) {
                    onResult(isFav)
                }
            } catch (e: Exception) {
                // Si falla la conexión, asumimos que no es favorito por seguridad
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }
}