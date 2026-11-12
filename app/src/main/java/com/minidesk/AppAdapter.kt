package com.minidesk

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class AppAdapter(private val context: Context, private var apps: List<AppItem>) : BaseAdapter() {

    fun updateData(newApps: List<AppItem>) {
        apps = newApps
        notifyDataSetChanged()
    }

    override fun getCount(): Int = apps.size

    override fun getItem(position: Int): AppItem = apps[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: TextView = (convertView as? TextView) ?: LayoutInflater.from(context)
            .inflate(R.layout.item_app, parent, false) as TextView
        
        val app = getItem(position)
        view.text = app.label
        return view
    }
}
