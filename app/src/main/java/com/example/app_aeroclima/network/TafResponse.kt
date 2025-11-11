package com.example.app_aeroclima.network

import com.squareup.moshi.Json

// Clase para la respuesta completa de la API de TAF
data class TafResponse(
    @field:Json(name = "data") val data: List<TafData>
)

// Clase para contener los datos de un TAF individual
data class TafData(
    @field:Json(name = "raw_text") val raw_text: String?
)