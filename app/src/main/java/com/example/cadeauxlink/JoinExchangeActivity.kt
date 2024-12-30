package com.example.cadeauxlink

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cadeauxlink.databinding.ActivityJoinExchangeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class JoinExchangeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJoinExchangeBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinExchangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // Configurar el botón para unirse a un intercambio
        binding.btnJoinExchange.setOnClickListener {
            joinExchange()
        }

        // Configurar el botón para escanear código QR (sin lógica de escaneo aún)
        binding.btnScanQR.setOnClickListener {
            Toast.makeText(this, "Funcionalidad de escaneo QR próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun joinExchange() {
        val invitationCode = binding.etInvitationCode.text.toString().trim()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (invitationCode.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa un código de invitación", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar si el código de invitación es válido
        db.collection("exchanges")
            .whereEqualTo("invitationCode", invitationCode)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Código de invitación no válido", Toast.LENGTH_SHORT).show()
                }
                // Verificar si el usuario ya está en el intercambio
                else if (documents.any { document ->
                    document.reference.collection("participants")
                        .document(currentUser.uid)
                        .get()
                        .isSuccessful
                        }) {
                    Toast.makeText(this, "Ya estás en este intercambio", Toast.LENGTH_SHORT).show()
                }
                // Verificar si la fecha límite de registro ha pasado
                else if (documents.any { document ->
                        val deadlineString = document.getString("deadline")
                        if (deadlineString != null) {
                            try {
                                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                val deadlineDate = sdf.parse(deadlineString)
                                val currentDate = Calendar.getInstance().time
                                !(deadlineDate != null && deadlineDate.before(currentDate))// Comparar fechas
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false // Si hay un error al procesar la fecha, ignorar
                            }
                        } else {
                            false // Si el campo está ausente, no considerar este documento
                        }
                    }) {
                    Toast.makeText(this, "El intercambio ya no acepta nuevos participantes", Toast.LENGTH_SHORT).show()
                }

                else {
                    for (document in documents) {
                        val exchangeId = document.id

                        // Agregar al usuario a la subcolección de participantes
                        val participantData = mapOf(
                            "name" to (currentUser.displayName ?: "Usuario Anónimo"),
                            "email" to (currentUser.email ?: "Desconocido"),
                            "status" to "Confirmado"
                        )

                        db.collection("exchanges")
                            .document(exchangeId)
                            .collection("participants")
                            .document(currentUser.uid)
                            .set(participantData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Te has unido al intercambio exitosamente", Toast.LENGTH_SHORT).show()
                                finish() // Cierra la actividad
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al unirte al intercambio", Toast.LENGTH_SHORT).show()
                                e.printStackTrace()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al buscar el intercambio", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }
}
