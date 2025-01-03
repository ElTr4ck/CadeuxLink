package com.example.cadeauxlink

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var rvUpcomingExchanges: RecyclerView
    private lateinit var rvNotifications: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvUpcomingExchanges = findViewById(R.id.rvUpcomingExchanges)
        rvNotifications = findViewById(R.id.rvNotifications)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        setupUpcomingExchanges()
        setupNotifications()
        setupBottomNavigation()
    }

    private fun setupUpcomingExchanges() {
        rvUpcomingExchanges.layoutManager = LinearLayoutManager(this)
        // TODO: Implement ExchangeAdapter and populate with data
        // rvUpcomingExchanges.adapter = ExchangeAdapter(getUpcomingExchanges())
    }

    private fun setupNotifications() {
        rvNotifications.layoutManager = LinearLayoutManager(this)
        // TODO: Implement NotificationAdapter and populate with data
        // rvNotifications.adapter = NotificationAdapter(getRecentNotifications())
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.page_home -> {
                    // Already on home, do nothing
                    true
                }
                R.id.page_exchanges -> {
                    // Ir a la lista de intercambios
                    val intent = Intent(this, ExchangeListActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.page_create -> {
                    // Ir a la vista de crear intercambio
                    val intent = Intent(this, CreateExchangeActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.page_profile -> {
                    // TODO: Navigate to Profile screen
                    true
                }
                else -> false
            }
        }
    }
}

