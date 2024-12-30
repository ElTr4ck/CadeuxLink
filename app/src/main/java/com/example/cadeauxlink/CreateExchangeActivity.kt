package com.example.cadeauxlink

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cadeauxlink.databinding.ActivityCreateExchangeBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.UUID

class CreateExchangeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateExchangeBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateExchangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        binding.btnCreateExchange.setOnClickListener {
            createExchange()
        }

        configureDatePicker(binding.tilDeadline.editText!!)
        configureDatePicker(binding.tilExchangeDate.editText!!)
    }

    private fun configureDatePicker(editText: EditText) {
        editText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                editText.setText(date)
            }, year, month, day)

            datePicker.show()
        }
    }

    private fun createExchange() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val exchangeName = binding.tilExchangeName.editText?.text.toString().trim()
        val maxAmount = binding.tilMaxAmount.editText?.text.toString().trim()
        val deadline = binding.tilDeadline.editText?.text.toString().trim()
        val exchangeDate = binding.tilExchangeDate.editText?.text.toString().trim()
        val location = binding.tilLocation.editText?.text.toString().trim()
        val additionalComments = binding.tilAdditionalComments.editText?.text.toString().trim()

        val selectedThemes = mutableListOf<String>()
        for (i in 0 until binding.cgThemes.childCount) {
            val chip = binding.cgThemes.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedThemes.add(chip.text.toString())
            }
        }

        if (exchangeName.isEmpty() || maxAmount.isEmpty() || deadline.isEmpty() || exchangeDate.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val invitationCode = UUID.randomUUID().toString().substring(0, 8).uppercase()

        val exchangeData = hashMapOf(
            "exchangeName" to exchangeName,
            "maxAmount" to maxAmount,
            "deadline" to deadline,
            "exchangeDate" to exchangeDate,
            "location" to location,
            "additionalComments" to additionalComments,
            "themes" to selectedThemes,
            "invitationCode" to invitationCode,
            "organizer" to currentUser.email
        )

        db.collection("exchanges").add(exchangeData)
            .addOnSuccessListener {
                // Agregar al usuario actual a la lista de participantes
                val participantData = hashMapOf(
                    "name" to (currentUser.displayName ?: "Usuario Anónimo"),
                    "email" to (currentUser.email ?: "Desconocido"),
                    "status" to "Organizador"
                )
                it.collection("participants").document(currentUser.uid).set(participantData)
                    .addOnSuccessListener {
                        Log.d("CreateExchange", "Usuario agregado a la lista de participantes")
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            "CreateExchange",
                            "Error al agregar usuario a la lista de participantes",
                            e
                        )

                        Toast.makeText(
                            this,
                            "Intercambio creado con éxito. Código: $invitationCode",
                            Toast.LENGTH_LONG
                        ).show()
                        finish() // Cierra la actividad
                    }
                    .addOnFailureListener { e ->
                        Log.e("CreateExchange", "Error al guardar el intercambio", e)
                        Toast.makeText(this, "Error al crear el intercambio", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
    }
}
