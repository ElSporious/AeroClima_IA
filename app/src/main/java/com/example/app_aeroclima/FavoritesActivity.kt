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
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        recycler = findViewById(R.id.recycler_favorites)
        emptyView = findViewById(R.id.tv_empty)

        recycler.layoutManager = LinearLayoutManager(this)

        adapter = FavoritesAdapter(emptyList()) { selectedIcao ->
            val resultIntent = Intent()
            resultIntent.putExtra("SELECTED_ICAO", selectedIcao)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        recycler.adapter = adapter

        loadFavorites()
    }

    private fun loadFavorites() {
        mysqlManager.getFavorites(
            onSuccess = { list ->
                if (list.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    recycler.visibility = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    recycler.visibility = View.VISIBLE
                    adapter.updateList(list)
                }
            },
            onFailure = {
                Toast.makeText(this, "Error al cargar favoritos", Toast.LENGTH_SHORT).show()
            }
        )
    }
}