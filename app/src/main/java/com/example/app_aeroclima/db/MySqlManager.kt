package com.example.app_aeroclima.db

import android.util.Log
import com.example.app_aeroclima.network.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MySqlManager {

    private val BASE_URL = "https://noncorrespondingly-didymous-basil.ngrok-free.dev/aeroclima_api/"

    private val api: MySqlApiService

    init {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(MySqlApiService::class.java)
    }

    // REGISTRO
    fun registerUser(email: String, passHash: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        api.registerUser(email, passHash).enqueue(object : Callback<SimpleResponse> {
            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") onSuccess()
                else onFailure(response.body()?.message ?: "Error desconocido")
            }
            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                onFailure(t.message ?: "Error de conexión")
            }
        })
    }

    // LOGIN
    fun loginUser(email: String, passPlain: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        api.loginUser(email, passPlain).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") onSuccess()
                else onFailure(response.body()?.message ?: "Credenciales inválidas")
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                onFailure("Error de conexión: ${t.message}")
            }
        })
    }

    // AGREGAR FAVORITO
    fun addFavorite(icao: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val email = UserSession.currentUserEmail ?: return onFailure()
        api.addFavorite(email, icao).enqueue(object : Callback<SimpleResponse> {
            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") onSuccess()
                else onFailure()
            }
            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { onFailure() }
        })
    }

    // ELIMINAR FAVORITO
    fun removeFavorite(icao: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val email = UserSession.currentUserEmail ?: return onFailure()
        api.removeFavorite(email, icao).enqueue(object : Callback<SimpleResponse> {
            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                if (response.isSuccessful) onSuccess() else onFailure()
            }
            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { onFailure() }
        })
    }

    // VERIFICAR SI ES FAVORITO
    fun checkIsFavorite(icao: String, onResult: (Boolean) -> Unit) {
        val email = UserSession.currentUserEmail
        if (email == null) {
            onResult(false)
            return
        }
        api.checkFavorite(email, icao).enqueue(object : Callback<FavoriteCheckResponse> {
            override fun onResponse(call: Call<FavoriteCheckResponse>, response: Response<FavoriteCheckResponse>) {
                onResult(response.body()?.is_favorite == true)
            }
            override fun onFailure(call: Call<FavoriteCheckResponse>, t: Throwable) {
                onResult(false)
            }
        })
    }

    // OBTENER LISTA DE FAVORITOS
    fun getFavorites(onSuccess: (List<String>) -> Unit, onFailure: () -> Unit) {
        val email = UserSession.currentUserEmail ?: return onFailure()
        api.getFavorites(email).enqueue(object : Callback<FavoritesListResponse> {
            override fun onResponse(call: Call<FavoritesListResponse>, response: Response<FavoritesListResponse>) {
                if (response.isSuccessful) {
                    val list = response.body()?.data?.map { it.icao_code } ?: emptyList()
                    onSuccess(list)
                } else onFailure()
            }
            override fun onFailure(call: Call<FavoritesListResponse>, t: Throwable) { onFailure() }
        })
    }

    fun loginWithGoogle(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        api.loginWithGoogle(email).enqueue(object : Callback<SimpleResponse> {
            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    onSuccess()
                } else {
                    onFailure(response.body()?.message ?: "Error en login con Google")
                }
            }
            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                onFailure("Error de conexión: ${t.message}")
            }
        })
    }
}

// Objeto simple para guardar el Email en memoria mientras la app está abierta
object UserSession {
    var currentUserEmail: String? = null
}