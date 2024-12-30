package com.example.cadeauxlink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddParticipantActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnAddParticipant: MaterialButton

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_participant)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        btnAddParticipant = findViewById(R.id.btnAddParticipant)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        btnAddParticipant.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (validateInputs(name, email, phone)) {
                addParticipant(name, email, phone)
            }
        }
    }

    private fun validateInputs(name: String, email: String, phone: String): Boolean {
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.invalid_email, Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.PHONE.matcher(phone).matches()) {
            Toast.makeText(this, R.string.invalid_phone, Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun addParticipant(name: String, email: String, phone: String) {
        val participant = hashMapOf(
            "name" to name,
            "email" to email,
            "status" to "Pendiente",
            "phone" to phone,
        )

        // Obtén el código de invitación del intent
        val invitationCode = intent.getStringExtra("exchangeId")
        if (invitationCode == null) {
            Log.e("ExchangeDetails", "No se recibió un código de invitación.")
            finish() // Cierra la actividad si no hay código
        }

        db.collection("exchanges")
            .whereEqualTo("invitationCode", invitationCode)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Codigo de invitación invalido", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Validar cada documento (aunque debería ser único por código)
                val document = documents.first() // Considerar solo el primer documento encontrado

                // Verificar si el correo electrónico ya está en la lista de participantes
                val participantsCollection = document.reference.collection("participants")
                participantsCollection.whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            Toast.makeText(
                                this,
                                "El correo electrónico ya está en la lista de participantes",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else {
                            // Ingresar el participante a la subcolección de participantes
                            document.reference.collection("participants")
                                .add(participant)
                                .addOnSuccessListener {
                                    sendInvitation(email)
                                    Toast.makeText(this, "Participante agregado exitosamente", Toast.LENGTH_SHORT).show()
                                    clearInputs()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("AddParticipantActivity", "Error al verificar participantes", e)
                    }
            }
    }

    private fun sendInvitation(email: String) {
        // Aquí iría la lógica para enviar el correo electrónico de invitación
        val emailIntent = Intent(Intent.ACTION_SEND)
        val auth = FirebaseAuth.getInstance()

        // Obtén el código de invitación del intent
        val invitationCode = intent.getStringExtra("exchangeId")
        if (invitationCode == null) {
            Log.e("ExchangeDetails", "No se recibió un código de invitación.")
            finish() // Cierra la actividad si no hay código
        }

        //Obtener la fecha limite del intercambio
        val exchangeDate = intent.getStringExtra("exchangeDate")
        if (exchangeDate == null) {
            Log.e("ExchangeDetails", "No se recibió una fecha de intercambio.")
            finish() // Cierra la actividad si no hay fecha
        }

        emailIntent.data = Uri.parse("mailto:")
        emailIntent.type = "text/plain"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "HAS SIDO INVITADO A UN INTERCAMBIO - CADEUXLINK")

        emailIntent.putExtra(Intent.EXTRA_TEXT, """Hola!
Has sido invitado a un intercambio.
Te invita el usuario: ${auth.currentUser?.email}
El codigo de invitacion de este intercambio es: $invitationCode
Favor de confirmar tu asistencia antes de la fecha: $exchangeDate
Saludos cordiales,
${auth.currentUser?.email}
Si no tienes la app CadeuxLink puedes descargarla en el siguiente enlace: PROXIMAMENTE""".trimMargin()) //TODO: PONER ENLACE DE DESCARGA DE LA APP

        try {
            startActivity(Intent.createChooser(emailIntent, "Enviar correo electrónico"))
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_sending_email, Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearInputs() {
        etName.text?.clear()
        etEmail.text?.clear()
        etPhone.text?.clear()
    }
}

