package com.example.app_aeroclima

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.app_aeroclima.network.MetarData
import com.example.app_aeroclima.network.MetarResponse
import com.example.app_aeroclima.network.TafResponse
import com.example.app_aeroclima.network.WeatherApiService
import com.google.ai.client.generativeai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.Exception

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val BASE_URL = "https://api.checkwx.com/"
    private var currentMetarData: MetarData? = null
    private var currentTafText: String? = null

    // Componentes de la UI
    private lateinit var icaoInput: EditText
    private lateinit var searchButton: Button
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var searchLayout: LinearLayout

    // Vistas del Bottom Sheet
    private lateinit var bsIcaoCode: TextView
    private lateinit var bsStationName: TextView
    private lateinit var bsWind: TextView
    private lateinit var bsVisibility: TextView
    private lateinit var bsTemperature: TextView
    private lateinit var bsRawMetar: TextView
    private lateinit var bsRawTaf: TextView

    // Vistas para la Lógica de Gemini
    private lateinit var btnTraducirTaf: Button
    private lateinit var tafLoadingSpinner: ProgressBar
    private lateinit var bsTraduccionIia: TextView

    // Modelo de Gemini
    private lateinit var generativeModel: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Conexión de vistas
        searchLayout = findViewById(R.id.search_layout)
        icaoInput = findViewById(R.id.icao_input)
        searchButton = findViewById(R.id.search_button)
        val bottomSheetLayout = findViewById<LinearLayout>(R.id.bottom_sheet_layout)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
        bsIcaoCode = findViewById(R.id.bs_icao_code)
        bsStationName = findViewById(R.id.bs_station_name)
        bsWind = findViewById(R.id.bs_wind)
        bsVisibility = findViewById(R.id.bs_visibility)
        bsTemperature = findViewById(R.id.bs_temperature)
        bsRawMetar = findViewById(R.id.bs_raw_metar)
        bsRawTaf = findViewById(R.id.bs_raw_taf)

        // Conexión de las vistas de Gemini
        btnTraducirTaf = findViewById(R.id.btn_traducir_taf)
        tafLoadingSpinner = findViewById(R.id.taf_loading_spinner)
        bsTraduccionIia = findViewById(R.id.bs_traduccion_ia)

        // Inicializando el Modelo de Gemini
        // Obtiene la API Key de forma segura desde 'build.gradle.kts'
        val apiKey = BuildConfig.GEMINI_API_KEY
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-pro",
            apiKey = apiKey
        )

        applyWindowInsets(bottomSheetLayout)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        searchButton.setOnClickListener {
            val icaoCode = icaoInput.text.toString().trim().uppercase()
            if (icaoCode.length == 4) {
                fetchWeatherData(icaoCode)
            } else {
                Toast.makeText(this, "Por favor, ingresa un código ICAO válido de 4 letras.", Toast.LENGTH_SHORT).show()
            }
        }

        // Asignación del botón al metodo de traducir con IA
        btnTraducirTaf.setOnClickListener {
            traducirTafConIA()
        }

        fetchWeatherData("SCEL")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()
        updateMapWithMetar()
        mMap.setOnMarkerClickListener {
            populateBottomSheet(currentMetarData)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            true
        }
        mMap.setOnMapClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (::mMap.isInitialized) {
                mMap.isMyLocationEnabled = true
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyWindowInsets(bottomSheetLayout: LinearLayout) {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            searchLayout.setPadding(searchLayout.paddingLeft, systemBarInsets.top, searchLayout.paddingRight, searchLayout.paddingBottom)
            bottomSheetLayout.setPadding(bottomSheetLayout.paddingLeft, bottomSheetLayout.paddingTop, bottomSheetLayout.paddingRight, systemBarInsets.bottom)
            insets
        }
    }

    private fun populateBottomSheet(metar: MetarData?) {
        if (metar == null) {
            bsIcaoCode.text = "..."
            bsStationName.text = "Buscando información..."
            bsWind.text = ""; bsVisibility.text = ""; bsTemperature.text = ""; bsRawMetar.text = ""; bsRawTaf.text = ""
            btnTraducirTaf.visibility = View.GONE
            tafLoadingSpinner.visibility = View.GONE
            bsTraduccionIia.visibility = View.GONE
            return
        }

        bsIcaoCode.text = metar.icao ?: "N/A"
        bsStationName.text = metar.station?.name ?: "Nombre no disponible"
        bsWind.text = "Viento: ${metar.wind?.speed_kts ?: "N/A"} nudos"
        bsVisibility.text = "Visibilidad: ${metar.visibility?.meters ?: "N/A"} m"
        bsTemperature.text = "Temperatura: ${metar.temperature?.celsius ?: "N/A"}°C"
        bsRawMetar.text = metar.raw_text ?: "Reporte no disponible"

        val tafText = currentTafText ?: "TAF no disponible para esta estación."
        bsRawTaf.text = tafText

        // Lógica para mostrar/ocultar el botón de IA
        if (currentTafText != null && !tafText.contains("Cargando") && !tafText.contains("no disponible")) {
            // Si hay un TAF real, muestra el botón
            btnTraducirTaf.visibility = View.VISIBLE
        } else {
            // Si no hay TAF, oculta el botón
            btnTraducirTaf.visibility = View.GONE
        }

        // Oculta la carga y la traducción anterior cada vez que se actualizan los datos
        tafLoadingSpinner.visibility = View.GONE
        bsTraduccionIia.visibility = View.GONE
        bsTraduccionIia.text = ""
    }

    private fun fetchWeatherData(icaoCode: String) {
        currentMetarData = null
        currentTafText = "Cargando TAF..."
        populateBottomSheet(null)

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofit = Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(MoshiConverterFactory.create(moshi)).build()
        val weatherService = retrofit.create(WeatherApiService::class.java)

        weatherService.getMetar(icaoCode, BuildConfig.CHECKWX_API_KEY).enqueue(object : Callback<MetarResponse> {
            override fun onResponse(call: Call<MetarResponse>, response: Response<MetarResponse>) {
                if (response.isSuccessful) {
                    currentMetarData = response.body()?.data?.firstOrNull()
                    if (currentMetarData != null) {
                        updateMapWithMetar()
                        populateBottomSheet(currentMetarData)
                        fetchTafData(weatherService, icaoCode)
                    } else {
                        Toast.makeText(this@MainActivity, "No se encontraron datos METAR para $icaoCode.", Toast.LENGTH_SHORT).show()
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error al obtener METAR.", Toast.LENGTH_SHORT).show()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }

            override fun onFailure(call: Call<MetarResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Fallo en la conexión de red.", Toast.LENGTH_SHORT).show()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        })
    }

    private fun fetchTafData(weatherService: WeatherApiService, icaoCode: String) {
        weatherService.getTaf(icaoCode, BuildConfig.CHECKWX_API_KEY).enqueue(object : Callback<TafResponse> {
            override fun onResponse(call: Call<TafResponse>, response: Response<TafResponse>) {
                if (response.isSuccessful) {
                    currentTafText = response.body()?.data?.firstOrNull()?.raw_text
                } else {
                    currentTafText = "TAF no disponible para esta estación."
                }
                populateBottomSheet(currentMetarData)
            }

            override fun onFailure(call: Call<TafResponse>, t: Throwable) {
                currentTafText = "Error de red al obtener TAF."
                populateBottomSheet(currentMetarData)
            }
        })
    }

    private fun updateMapWithMetar() {
        val metar = currentMetarData ?: return
        if (!::mMap.isInitialized) return

        val lon = metar.station?.geometry?.coordinates?.getOrNull(0)
        val lat = metar.station?.geometry?.coordinates?.getOrNull(1)

        if (lat != null && lon != null) {
            val airportLocation = LatLng(lat, lon)
            mMap.clear()
            mMap.addMarker(
                MarkerOptions()
                    .position(airportLocation)
                    .title(metar.icao ?: "Aeropuerto")
                    .snippet("Toca para ver detalles")
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(airportLocation, 12f))
        }
    }


    // Funciones de Gemini
    // Se llama cuando se presiona el botón "Traducir TAF con IA".
    private fun traducirTafConIA() {
        val tafCrudo = bsRawTaf.text.toString()

        // Validación simple
        if (tafCrudo.isEmpty() || tafCrudo.contains("no disponible")) {
            Toast.makeText(this, "No hay TAF para traducir.", Toast.LENGTH_SHORT).show()
            return
        }

        // Muestra la UI de carga
        btnTraducirTaf.visibility = View.GONE
        tafLoadingSpinner.visibility = View.VISIBLE
        bsTraduccionIia.visibility = View.GONE // Oculta la traducción anterior

        // Define el prompt para la IA
        val prompt = """
            Eres un experto meteorólogo aeronáutico. 
            Traduce el siguiente reporte TAF a un lenguaje simple y claro, en español.
            Enfócate en las condiciones más importantes (viento, visibilidad, nubes y tiempo significativo).
            
            TAF: "$tafCrudo"
        """.trimIndent()

        // Lanza la corutina para llamar a Gemini
        lifecycleScope.launch {
            try {
                // Envía el prompt a Gemini
                val response = generativeModel.generateContent(prompt)
                // Muestra la respuesta
                val traduccion = response.text ?: "No se pudo generar respuesta."
                mostrarResultadoTraduccion(traduccion)
            } catch (e: Exception) {
                // Maneja el error
                mostrarResultadoTraduccion("Error: ${e.message}", esError = true)
            }
        }
    }


    // Función auxiliar para actualizar la UI con la respuesta de Gemini.

    private fun mostrarResultadoTraduccion(texto: String, esError: Boolean = false) {
        // Oculta la rueda de carga
        tafLoadingSpinner.visibility = View.GONE
        // Muestra el resultado de la traducción
        bsTraduccionIia.text = texto
        bsTraduccionIia.visibility = View.VISIBLE

        // Si fue un error, vuelve a mostrar el botón para reintentar.
        // Si fue exitoso, el botón se queda oculto (para no traducir de nuevo).
        if (esError) {
            btnTraducirTaf.visibility = View.VISIBLE
        }
    }
}