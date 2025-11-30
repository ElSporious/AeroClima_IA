package com.example.app_aeroclima

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_aeroclima.db.MySqlManager

class FavoritesActivity : AppCompatActivity() {

    private val mysqlManager = MySqlManager()
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: FavoritesAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        recycler = findViewById(R.id.recycler_favorites)
        tvEmpty = findViewById(R.id.tv_empty)

        // Configurar la lista vacía al principio
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = FavoritesAdapter(emptyList()) { selectedIcao ->
            // CUANDO TOCAN UN AEROPUERTO:
            // Devolvemos el resultado a la MainActivity y cerramos esta pantalla
            val intent = Intent()
            intent.putExtra("SELECTED_ICAO", selectedIcao)
            setResult(RESULT_OK, intent)
            finish()
        }
        recycler.adapter = adapter

        loadFavorites()
    }

    private fun loadFavorites() {
        // Llamamos al PHP a través de Ngrok
        mysqlManager.getFavorites { list ->
            if (list.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                recycler.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                recycler.visibility = View.VISIBLE
                adapter.updateList(list)
            }
        }
    }
}