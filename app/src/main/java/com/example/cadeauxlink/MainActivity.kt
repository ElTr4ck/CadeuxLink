package com.example.cadeauxlink

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<MaterialCardView>(R.id.cvCreateExchange).setOnClickListener {
            createExchange()
        }

        //Si se toca el boton de "Mis intercambios" ir a la vista de ExchangeListActivity
        findViewById<MaterialCardView>(R.id.cvMyExchanges).setOnClickListener {
            val intent = Intent(this, ExchangeListActivity::class.java)
            startActivity(intent)
        }

        //Si se toca el boton de "Unirse a un intercambio" ir a la vista de JoinExchangeActivity
        findViewById<MaterialCardView>(R.id.cvJoinExchange).setOnClickListener {
            val intent = Intent(this, JoinExchangeActivity::class.java)
            startActivity(intent)
        }

        //Crear el boton del perfil
        findViewById<FloatingActionButton>(R.id.fabProfile).setOnClickListener {
            showProfileMenu()
        }
    }

    // Si se presiona el boton de crear intercambio se navega a la vista de CreateExchangeActivity
    private fun createExchange() {
        val intent = Intent(this, CreateExchangeActivity::class.java)
        startActivity(intent)
    }

    private fun showProfileMenu() {
        val popupMenu = PopupMenu(this, findViewById<FloatingActionButton>(R.id.fabProfile))
        popupMenu.menuInflater.inflate(R.menu.profile_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Cierra la actividad actual
    }

    // Si se presiona el boton de ver intercambios se navega a la vista de MyExchangesActivity
    //fun viewExchanges(view: View) {
    //    val intent = Intent(this, MyExchangesActivity::class.java)
    //    startActivity(intent)
    //}


}