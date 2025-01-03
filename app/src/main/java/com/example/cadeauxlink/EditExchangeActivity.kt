package com.example.cadeauxlink

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cadeauxlink.databinding.ActivityEditExchangeBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class EditExchangeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditExchangeBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditExchangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // Llenar los campos con los datos del intercambio
        val exchangeId = intent.getStringExtra("exchangeId")
        fillExchangeData(exchangeId)

        binding.btnEditExchange.setOnClickListener {
            val exchangeId = intent.getStringExtra("exchangeId")
            if (exchangeId != null) {
                updateExchange(exchangeId)
            } else {
                Toast.makeText(this, "ID de intercambio no válido", Toast.LENGTH_SHORT).show()
            }
        }

        configureDatePicker(binding.tilDeadline.editText!!)
        configureDatePicker(binding.tilExchangeDate.editText!!)
    }

    private fun fillExchangeData(exchangeId: String?) {
        if (exchangeId != null) {
            db.collection("exchanges")
                .whereEqualTo("invitationCode", exchangeId)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(this, "Intercambio no encontrado", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    for (document in documents) {
                        val exchangeName = document.getString("exchangeName")
                        val maxAmount = document.getString("maxAmount")
                        val deadline = document.getString("deadline")
                        val exchangeDate = document.getString("exchangeDate")
                        val location = document.getString("location")
                        val additionalComments = document.getString("additionalComments")
                        val themes = document.get("themes") as? List<*>

                        binding.tilExchangeName.editText?.setText(exchangeName)
                        binding.tilMaxAmount.editText?.setText(maxAmount)
                        binding.tilDeadline.editText?.setText(deadline)
                        binding.tilExchangeDate.editText?.setText(exchangeDate)
                        binding.tilLocation.editText?.setText(location)
                        binding.tilAdditionalComments.editText?.setText(additionalComments)

                        themes?.forEach { theme ->
                            val chip = Chip(this)
                            chip.text = theme.toString()
                            chip.isCheckable = false
                        }
                    }
                }
        }
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

    private fun updateExchange(exchangeId: String) {
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

        // Validar que la fecha del intercambio sea mayor a la fecha actual y mayor a la fecha límite de registro
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        try {
            val deadlineDate = sdf.parse(deadline)
            val exchangeDateParsed = sdf.parse(exchangeDate)
            val currentDate = Calendar.getInstance().time

            if (deadlineDate == null || exchangeDateParsed == null) {
                Toast.makeText(this, "Formato de fecha inválido", Toast.LENGTH_SHORT).show()
                return
            }

            if (currentDate.after(exchangeDateParsed)) {
                Toast.makeText(this, "La fecha del intercambio debe ser posterior a la fecha actual", Toast.LENGTH_SHORT).show()
                return
            }

            if (deadlineDate.after(exchangeDateParsed)) {
                Toast.makeText(this, "La fecha del intercambio debe ser posterior a la fecha límite de registro", Toast.LENGTH_SHORT).show()
                return
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar las fechas", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            return
        }

        val exchangeData = hashMapOf(
            "exchangeName" to exchangeName,
            "maxAmount" to maxAmount,
            "deadline" to deadline,
            "exchangeDate" to exchangeDate,
            "location" to location,
            "additionalComments" to additionalComments,
            "themes" to selectedThemes
        )

        db.collection("exchanges")
            .whereEqualTo("invitationCode", exchangeId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Intercambio no encontrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    document.reference.update(exchangeData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Intercambio actualizado exitosamente", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("UpdateExchange", "Error al actualizar el intercambio", e)
                            Toast.makeText(this, "Error al actualizar el intercambio", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("UpdateExchange", "Error al buscar el intercambio", e)
                Toast.makeText(this, "Error al buscar el intercambio", Toast.LENGTH_SHORT).show()
            }
    }

}
