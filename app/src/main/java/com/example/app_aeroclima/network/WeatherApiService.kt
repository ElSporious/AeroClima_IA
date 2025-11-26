package com.example.app_aeroclima.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface WeatherApiService {
    @GET("metar/{icao}/decoded")
    suspend fun getMetar(
        @Path("icao") icaoCode: String,
        @Header("X-API-Key") apiKey: String
    ): Response<MetarResponse>

    @GET("taf/{icao}/decoded")
    suspend fun getTaf(
        @Path("icao") icaoCode: String,
        @Header("X-API-Key") apiKey: String
    ): Response<TafResponse>
}