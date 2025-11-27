package com.example.app_aeroclima


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_aeroclima.network.MetarData
import com.example.app_aeroclima.network.WeatherApiService
import com.example.app_aeroclima.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class WeatherViewModel : ViewModel() {

    private val _metarData = MutableLiveData<MetarData?>()
    val metarData: LiveData<MetarData?> = _metarData
    private val _tafText = MutableLiveData<String?>()
    val tafText: LiveData<String?> = _tafText
    private val _translation = MutableLiveData<String>()
    val translation: LiveData<String> = _translation
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Configuración de Retrofit
    private val weatherService: WeatherApiService by lazy {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        Retrofit.Builder()
            .baseUrl("https://api.checkwx.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WeatherApiService::class.java)
    }

    // Configuración de Gemini
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-pro",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun fetchWeatherData(icaoCode: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val metarResponse = weatherService.getMetar(icaoCode, BuildConfig.CHECKWX_API_KEY)

                if (metarResponse.isSuccessful) {
                    val metar = metarResponse.body()?.data?.firstOrNull()
                    _metarData.value = metar
                    if (metar != null) fetchTaf(icaoCode)
                } else {
                    _errorMessage.value = "Error API: ${metarResponse.code()}"
                    _metarData.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchTaf(icaoCode: String) {
        _tafText.value = "Cargando TAF..."
        try {
            val tafResponse = weatherService.getTaf(icaoCode, BuildConfig.CHECKWX_API_KEY)
            if (tafResponse.isSuccessful) {
                _tafText.value = tafResponse.body()?.data?.firstOrNull()?.raw_text ?: "TAF no disponible"
            } else {
                _tafText.value = "TAF no disponible."
            }
        } catch (e: Exception) {
            _tafText.value = "Error al cargar TAF."
        }
    }

    fun traducirTafConIA(tafCrudo: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = "Traduce este TAF aeronáutico a español simple, resaltando viento y visibilidad: $tafCrudo"
                val response = generativeModel.generateContent(prompt)
                _translation.value = response.text ?: "Error al traducir."
            } catch (e: Exception) {
                _translation.value = "Error IA: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}