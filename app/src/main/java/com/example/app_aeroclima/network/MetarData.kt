package com.example.app_aeroclima.network

import com.squareup.moshi.Json
data class MetarData(
    @field:Json(name = "icao") val icao: String?,
    @field:Json(name = "raw_text") val raw_text: String?,
    @field:Json(name = "station") val station: Station?,
    val wind: Wind?,
    val visibility: Visibility?,
    val temperature: Temperature?,
)

data class Station(
    val name: String?,
    val location: String?,
    val geometry: Geometry?
)

data class Geometry(
    val coordinates: List<Double>?
)


data class Wind(
    val speed_kts: Int?,
    val degrees: Int?,
    val gust_kts: Int?
)

data class Visibility(
    @field:Json(name = "meters") val meters: String?
)

data class Temperature(
    @field:Json(name = "celsius") val celsius: Int?
)