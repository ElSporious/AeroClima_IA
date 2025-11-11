package com.example.app_aeroclima.network

import com.squareup.moshi.Json

// Esta clase representa la respuesta completa de la API
data class MetarResponse(
    @field:Json(name = "data") val data: List<MetarData>
)