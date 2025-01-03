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

class EditParticipantActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnEditParticipant: MaterialButton

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_participant)

        etName = findViewById(R.id.edName)
        etEmail = findViewById(R.id.edEmail)
        etPhone = findViewById(R.id.edPhone)
        btnEditParticipant = findViewById(R.id.btnEditParticipant)

        // Obtén los datos del participante actual del intent
        val participantName = intent.getStringExtra("participantName")
        val participantEmail = intent.getStringExtra("participantEmail")
        val participantPhone = intent.getStringExtra("participantPhone")

        // Configura los campos de entrada con los datos del participante actual
        etName.setText(participantName)
        etEmail.setText(participantEmail)
        etPhone.setText(participantPhone)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        btnEditParticipant.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (validateInputs(name, email, phone)) {
                deleteActualParticipant(name, email, phone)
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

    private fun deleteActualParticipant(name: String, email: String, phone: String) {
        // Obtén el código de invitación del intent
        val invitationCode = intent.getStringExtra("exchangeId")
        if (invitationCode == null) {
            Log.e("ExchangeDetails", "No se recibió un código de invitación.")
            finish() // Cierra la actividad si no hay código
        }

        // Obtén el email del participante actual del intent
        val participantEmail = intent.getStringExtra("participantEmail")
        if (participantEmail == null) {
            Log.e("ExchangeDetails", "No se recibió un email de participante.")
            finish() // Cierra la actividad si no hay email
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

                // Eliminar el participante actual de la subcolección de participantes
                val participantsCollection = document.reference.collection("participants")
                participantsCollection.whereEqualTo("email", participantEmail)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val participantDocument = querySnapshot.documents[0]
                            participantDocument.reference.delete()
                                .addOnSuccessListener {
                                    addParticipant(name, email, phone)
                                    Toast.makeText(this, "Participante editado exitosamente", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("EditParticipantActivity", "Error al eliminar participante actual", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("EditParticipantActivity", "Error al eliminar participante actual", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("EditParticipantActivity", "Error al obtener el documento de intercambio", e)
            }
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

        emailIntent.data = Uri.parse("mailto:")
        emailIntent.type = "text/plain"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "HAS SIDO INVITADO A UN INTERCAMBIO - CADEUXLINK")

        emailIntent.putExtra(Intent.EXTRA_TEXT, """Hola!
Has sido invitado a un intercambio.
Te invita el usuario: ${auth.currentUser?.email}
El codigo de invitacion de este intercambio es: $invitationCode
Favor de confirmar tu asistencia lo más pronto posible.
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

