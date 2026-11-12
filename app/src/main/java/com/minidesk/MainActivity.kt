package com.minidesk

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var tvClock: TextView
    private lateinit var etSearch: EditText
    private lateinit var listApps: ListView
    private lateinit var appAdapter: AppAdapter
    private lateinit var prefs: SharedPreferences

    private val favoriteViews = mutableListOf<TextView>()
    private val favoritePackages = arrayOfNulls<String>(6)

    private val timeTickReceiver = object : BroadcastReceiver() {
        onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_TIME_TICK) {
                updateClock()
            }
        }
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_TIME_TICK) {
                updateClock()
            }
        }
    }

    private lateinit var packageReceiver: PackageChangeReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("minidesk_prefs", Context.MODE_PRIVATE)

        tvClock = findViewById(R.id.tv_clock)
        etSearch = findViewById(R.id.et_search)
        listApps = findViewById(R.id.list_apps)

        favoriteViews.addAll(listOf(
            findViewById(R.id.fav_0),
            findViewById(R.id.fav_1),
            findViewById(R.id.fav_2),
            findViewById(R.id.fav_3),
            findViewById(R.id.fav_4),
            findViewById(R.id.fav_5)
        ))

        loadFavorites()
        updateClock()

        appAdapter = AppAdapter(this, emptyList())
        listApps.adapter = appAdapter

        loadAppList()

        setupListeners()
        registerReceivers()
    }

    private fun updateClock() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvClock.text = timeFormat.format(Date())
    }

    private fun loadAppList() {
        AppRepository.loadApps(packageManager) { apps ->
            runOnUiThread {
                appAdapter.updateData(AppRepository.search(etSearch.text.toString()))
                refreshFavoritesLabels(apps)
            }
        }
    }

    private fun setupListeners() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val results = AppRepository.search(s.toString())
                appAdapter.updateData(results)
                // Lanzamiento automático si queda exactamente 1 resultado y se pulsa buscar
                if (results.size == 1 && s.toString().endsWith("\n")) {
                    launchApp(results[0])
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        listApps.setOnItemClickListener { _, _, position, _ ->
            val app = appAdapter.getItem(position)
            launchApp(app)
        }

        listApps.setOnItemLongClickListener { _, _, position, _ ->
            val app = appAdapter.getItem(position)
            showAppSettings(app.packageName)
            true
        }

        for (i in favoriteViews.indices) {
            favoriteViews[i].setOnClickListener {
                val pkg = favoritePackages[i]
                if (pkg != null) {
                    launchPackage(pkg)
                } else {
                    // Seleccionar de la lista al hacer clic en vacío
                    etSearch.requestFocus()
                }
            }

            favoriteViews[i].setOnLongClickListener {
                // Asignar primera app del buscador actual o limpiar
                val currentResults = AppRepository.search(etSearch.text.toString())
                if (currentResults.isNotEmpty()) {
                    setFavorite(i, currentResults[0].packageName)
                } else {
                    setFavorite(i, null)
                }
                true
            }
        }
    }

    private fun launchApp(app: AppItem) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            etSearch.setText("")
            startActivity(intent)
        }
    }

    private fun launchPackage(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            // Si la app fue desinstalada, limpiar favorito
            for (i in favoritePackages.indices) {
                if (favoritePackages[i] == packageName) {
                    setFavorite(i, null)
                }
            }
        }
    }

    private fun showAppSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun loadFavorites() {
        for (i in 0 until 6) {
            favoritePackages[i] = prefs.getString("fav_$i", null)
        }
    }

    private fun setFavorite(index: Int, packageName: String?) {
        favoritePackages[index] = packageName
        prefs.edit().putString("fav_$index", packageName).apply()
        loadAppList()
    }

    private fun refreshFavoritesLabels(allApps: List<AppItem>) {
        val appMap = allApps.associateBy { it.packageName }
        for (i in favoriteViews.indices) {
            val pkg = favoritePackages[i]
            if (pkg != null && appMap.containsKey(pkg)) {
                favoriteViews[i].text = appMap[pkg]?.label
            } else {
                favoriteViews[i].text = "+ favorito ${i + 1}"
            }
        }
    }

    private fun registerReceivers() {
        packageReceiver = PackageChangeReceiver {
            loadAppList()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
        registerReceiver(timeTickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageReceiver)
        unregisterReceiver(timeTickReceiver)
    }

    override fun onBackPressed() {
        // Un launcher no debe salir de la app al pulsar atrás
        etSearch.setText("")
        etSearch.clearFocus()
    }
}
