package com.example.app_aeroclima

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoritesAdapter(
    private var favorites: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcao: TextView = view.findViewById(R.id.tv_fav_icao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val icao = favorites[position]
        holder.tvIcao.text = icao
        holder.itemView.setOnClickListener { onClick(icao) }
    }

    override fun getItemCount() = favorites.size

    fun updateList(newList: List<String>) {
        favorites = newList
        notifyDataSetChanged()
    }
}