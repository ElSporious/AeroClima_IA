package com.example.app_aeroclima.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface WeatherApiService {
    @GET("metar/{icao}/decoded")
    fun getMetar(
        @Path("icao") icaoCode: String,
        @Header("X-API-Key") apiKey: String
    ): Call<MetarResponse>

    @GET("taf/{icao}/decoded")
    fun getTaf(
        @Path("icao") icaoCode: String,
        @Header("X-API-Key") apiKey: String
    ): Call<TafResponse>


}