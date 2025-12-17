package com.example.app_aeroclima.network

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Modelos de respuesta rápida
data class SimpleResponse(val status: String, val message: String?)
data class LoginResponse(val status: String, val message: String?)
data class FavoriteCheckResponse(val is_favorite: Boolean)
data class FavoritesListResponse(val status: String, val data: List<FavoriteItem>?)
data class FavoriteItem(val icao_code: String)

interface MySqlApiService {

    @FormUrlEncoded
    @POST("register.php")
    fun registerUser(
        @Field("email") email: String,
        @Field("hashed_password") passHash: String
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("login.php")
    fun loginUser(
        @Field("email") email: String,
        @Field("password") passPlain: String
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("add_favorite.php")
    fun addFavorite(
        @Field("email") email: String,
        @Field("icao") icao: String
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("remove_favorite.php")
    fun removeFavorite(
        @Field("email") email: String,
        @Field("icao") icao: String
    ): Call<SimpleResponse>

    @FormUrlEncoded
    @POST("check_favorite.php")
    fun checkFavorite(
        @Field("email") email: String,
        @Field("icao") icao: String
    ): Call<FavoriteCheckResponse>

    @GET("get_favorites.php")
    fun getFavorites(
        @Query("email") email: String
    ): Call<FavoritesListResponse>

    @FormUrlEncoded
    @POST("google_login.php")
    fun loginWithGoogle(
        @Field("email") email: String
    ): Call<SimpleResponse>
}