package com.example.app_aeroclima

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.app_aeroclima.network.MetarData
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // ViewModel
    private val viewModel: WeatherViewModel by viewModels()

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var mMap: GoogleMap

    // UI Variables
    private lateinit var icaoInput: EditText
    private lateinit var searchButton: Button
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var searchLayout: LinearLayout
    private lateinit var bsIcaoCode: TextView
    private lateinit var bsStationName: TextView
    private lateinit var bsWind: TextView
    private lateinit var bsVisibility: TextView
    private lateinit var bsTemperature: TextView
    private lateinit var bsRawMetar: TextView
    private lateinit var bsRawTaf: TextView
    private lateinit var btnTraducirTaf: Button
    private lateinit var tafLoadingSpinner: ProgressBar
    private lateinit var bsTraduccionIia: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        setContentView(R.layout.activity_main)

        initViews()
        setupObservers()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Carga inicial de prueba
        viewModel.fetchWeatherData("SCEL")
    }

    private fun initViews() {
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
        btnTraducirTaf = findViewById(R.id.btn_traducir_taf)
        tafLoadingSpinner = findViewById(R.id.taf_loading_spinner)
        bsTraduccionIia = findViewById(R.id.bs_traduccion_ia)

        applyWindowInsets(bottomSheetLayout)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        searchButton.setOnClickListener {
            val code = icaoInput.text.toString().trim().uppercase()
            if (code.length == 4) {
                hideKeyboard()
                viewModel.fetchWeatherData(code) // Llamada al ViewModel
            } else {
                Toast.makeText(this, "Código ICAO inválido", Toast.LENGTH_SHORT).show()
            }
        }

        btnTraducirTaf.setOnClickListener {
            val taf = bsRawTaf.text.toString()
            if (taf.isNotEmpty()) viewModel.traducirTafConIA(taf)
        }
    }

    private fun setupObservers() {
        // Observador de METAR
        viewModel.metarData.observe(this) { metar ->
            if (metar != null) {
                populateBottomSheet(metar)
                updateMap(metar)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        // Observador del TAF
        viewModel.tafText.observe(this) { taf ->
            bsRawTaf.text = taf ?: "No disponible"
            val showBtn = !taf.isNullOrEmpty() && !taf.contains("Cargando") && !taf.contains("no disponible")
            btnTraducirTaf.visibility = if (showBtn) View.VISIBLE else View.GONE
            bsTraduccionIia.visibility = View.GONE // Reset traducción anterior
        }

        // Observador de traducción IA
        viewModel.translation.observe(this) { translation ->
            bsTraduccionIia.text = translation
            bsTraduccionIia.visibility = View.VISIBLE
            btnTraducirTaf.visibility = View.GONE
        }

        // Observador de la Carga
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading && bsTraduccionIia.visibility == View.GONE && bsRawTaf.text.isNotEmpty()) {
                tafLoadingSpinner.visibility = View.VISIBLE
            } else {
                tafLoadingSpinner.visibility = View.GONE
            }
        }

        // Observador de Errores
        viewModel.errorMessage.observe(this) { msg ->
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateBottomSheet(metar: MetarData) {
        bsIcaoCode.text = metar.icao ?: ""
        bsStationName.text = metar.station?.name ?: ""
        bsWind.text = "Viento: ${metar.wind?.speed_kts ?: "-"} kts"
        bsVisibility.text = "Visibilidad: ${metar.visibility?.meters ?: "-"} m"
        bsTemperature.text = "Temp: ${metar.temperature?.celsius ?: "-"}°C"
        bsRawMetar.text = metar.raw_text ?: ""
    }

    private fun updateMap(metar: MetarData) {
        if (!::mMap.isInitialized) return
        val lat = metar.station?.geometry?.coordinates?.getOrNull(1)
        val lon = metar.station?.geometry?.coordinates?.getOrNull(0)
        if (lat != null && lon != null) {
            val pos = LatLng(lat, lon)
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(pos).title(metar.icao))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 12f))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()
        viewModel.metarData.value?.let { updateMap(it) }

        mMap.setOnMarkerClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED; true
        }
        mMap.setOnMapClickListener { bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (::mMap.isInitialized) mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) enableMyLocation()
    }

    private fun applyWindowInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            searchLayout.setPadding(searchLayout.paddingLeft, bars.top, searchLayout.paddingRight, searchLayout.paddingBottom)
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bars.bottom)
            insets
        }
    }

    private fun hideKeyboard() {
        currentFocus?.let {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}