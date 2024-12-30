package com.example.cadeauxlink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cadeauxlink.databinding.ActivityExchangeListBinding
import com.example.cadeauxlink.models.Exchange
import com.example.cadeauxlink.models.ExchangeAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ExchangeListActivity : AppCompatActivity(){

    private lateinit var binding: ActivityExchangeListBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ExchangeAdapter
    private lateinit var exchangeList: MutableList<Exchange>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExchangeListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        exchangeList = mutableListOf()
        adapter = ExchangeAdapter(binding.root.context, exchangeList)
        binding.rvIntercambios.layoutManager = LinearLayoutManager(this)
        binding.rvIntercambios.adapter = adapter

        binding.fabCreateExchange.setOnClickListener {
            val intent = Intent(this, CreateExchangeActivity::class.java)
            startActivity(intent)
        }

        loadExchanges()
    }

    private fun loadExchanges() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        db.collection("exchanges")
            .whereEqualTo("organizer", currentUser?.email)
            .get()
            .addOnSuccessListener { documents ->
                exchangeList.clear()
                for (document in documents) {
                    val exchange = document.toObject(Exchange::class.java)
                    exchangeList.add(exchange)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }

    }

}