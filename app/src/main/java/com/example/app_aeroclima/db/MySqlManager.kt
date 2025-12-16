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

//  MODELOS
data class BackendResponse(
    val status: String?,
    val message: String?,
    val is_favorite: Boolean?
)

data class FavoritesResponse(
    val status: String?,
    val data: List<FavoriteItem>?
)

data class FavoriteItem(
    val icao_code: String
)

// INTERFAZ API
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

    @GET("get_favorites.php")
    suspend fun getFavorites(
        @Query("email") email: String
    ): Response<FavoritesResponse>

    @FormUrlEncoded
    @POST("register.php")
    suspend fun registerUser(
        @Field("email") email: String,
        @Field("hashed_password") hashedPassword: String
    ): Response<BackendResponse>

    @FormUrlEncoded
    @POST("login.php")
    suspend fun loginUser(
        @Field("email") email: String,
        @Field("password") plainPassword: String
    ): Response<BackendResponse>
}

// MANAGER
class MySqlManager {

    private val auth = FirebaseAuth.getInstance()
    private val service: BackendApiService

    // Guardar el email del usuario local
    private var localUserEmail: String? = null

    init {
        val BASE_URL = "https://noncorrespondingly-didymous-basil.ngrok-free.dev/aeroclima_api/"

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        // Logger para ver la respuesta en Logcat
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                // Header especial para Ngrok
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
        val firebaseEmail = auth.currentUser?.email
        return firebaseEmail ?: localUserEmail // Retorna Firebase email o el email guardado localmente
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
                e.printStackTrace()
                withContext(Dispatchers.Main) { onFailure() }
            }
        }
    }
    fun removeFavorite(icao: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val email = getCurrentEmail()
        if (email == null) { onFailure(); return }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.removeFavorite(email, icao)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == "success") { onSuccess() } else { onFailure() }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { onFailure() } }
        }
    }

    fun checkIsFavorite(icao: String, onResult: (Boolean) -> Unit) {
        val email = getCurrentEmail()
        if (email == null) { onResult(false); return }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.checkFavorite(email, icao)
                val isFav = response.body()?.is_favorite ?: false
                withContext(Dispatchers.Main) { onResult(isFav) }
            } catch (e: Exception) { withContext(Dispatchers.Main) { onResult(false) } }
        }
    }

    fun getFavorites(onResult: (List<String>) -> Unit) {
        val email = getCurrentEmail()
        if (email == null) { onResult(emptyList()); return }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.getFavorites(email)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val list = response.body()?.data?.map { it.icao_code } ?: emptyList()
                    withContext(Dispatchers.Main) { onResult(list) }
                } else { withContext(Dispatchers.Main) { onResult(emptyList()) } }
            } catch (e: Exception) { e.printStackTrace(); withContext(Dispatchers.Main) { onResult(emptyList()) } }
        }
    }

    fun registerUser(email: String, hashedPassword: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.registerUser(email, hashedPassword)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == "success") { onSuccess() } else { onFailure() }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { onFailure() } }
        }
    }

    // Guardamos el email al iniciar sesión local
    fun loginUser(email: String, plainPassword: String, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.loginUser(email, plainPassword)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        // ¡GUARDAMOS EL EMAIL DEL USUARIO LOCAL!
                        localUserEmail = email
                        onSuccess(email)
                    } else {
                        onFailure()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onFailure() }
            }
        }
    }
}