package com.example.cadeauxlink

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cadeauxlink.databinding.ActivityExchangeDetailsBinding
import com.example.cadeauxlink.models.Participant
import com.example.cadeauxlink.models.ParticipantAdapter
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExchangeDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExchangeDetailsBinding
    private lateinit var db: FirebaseFirestore
    private val participantList = mutableListOf<Participant>()
    private lateinit var participantAdapter: ParticipantAdapter
    private lateinit var alertBuilder: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExchangeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // RecyclerView para los participantes
        participantAdapter = ParticipantAdapter(participantList)
        binding.rvParticipants.adapter = participantAdapter
        binding.rvParticipants.layoutManager = LinearLayoutManager(this)

        // Obtén el código de invitación del intent
        val invitationCode = intent.getStringExtra("exchangeId")
        if (invitationCode != null) {
            loadExchangeDetails(invitationCode)
            loadParticipants(invitationCode)
        } else {
            Log.e("ExchangeDetails", "No se recibió un código de invitación.")
            finish() // Cierra la actividad si no hay código
        }

        //TODO: Agregar funcionalidad para editar intercambio

        // Lógica para crear el sorteo
        binding.btnCreateRaffle.setOnClickListener {
            // Mostrar un mensaje que si se continua no se podran unir más participantes
            alertBuilder = AlertDialog.Builder(this)
            alertBuilder.setTitle("Aviso")
                .setMessage("Si continua no se podran unir más participantes")
                .setCancelable(true)
                .setPositiveButton("Aceptar") { dialog, _ ->
                    // Cerrar el sorteo y mostrar resultados
                    closeRaffle()
                }
                .setNegativeButton("Cancelar"){ dialog, _ ->
                    dialog.cancel()
                }
            val alert = alertBuilder.create()
            alert.show()
        }

        // Logica para agregar participantes
        binding.btnAddParticipant.setOnClickListener {
            if (invitationCode != null) {
                addParticipant(invitationCode)
            } else {
                Log.e("ExchangeDetails", "No se recibió un código de invitación.")
                finish() // Cierra la actividad si no hay código
            }
        }
    }

    private fun loadExchangeDetails(invitationCode: String) {
        db.collection("exchanges")
            .whereEqualTo("invitationCode", invitationCode)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    binding.tvExchangeName.text = document.getString("exchangeName")
                    binding.tvExchangeDate.text = "Fecha: ${document.getString("exchangeDate")}"
                    binding.tvExchangeLocation.text = "Lugar: ${document.getString("location")}"
                    binding.tvMaxAmount.text = "Monto máximo: ${document.getString("maxAmount")}"
                    binding.tvDeadline.text = "Fecha límite de registro: ${document.getString("deadline")}"
                    binding.tvInvitationCode.text = "Código de invitación: ${document.getString("invitationCode")}"
                    binding.tvAdditionalComments.text = "Comentarios adicionales: ${document.getString("additionalComments")}"

                    // Agregar temas al ChipGroup
                    val themes = document.get("themes") as? List<*>
                    themes?.forEach { theme ->
                        val chip = Chip(this)
                        chip.text = theme.toString()
                        chip.isCheckable = false
                        binding.cgThemes.addView(chip)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ExchangeDetails", "Error al cargar los detalles del intercambio", e)
            }
    }

    private fun loadParticipants(invitationCode: String) {
        db.collection("exchanges")
            .whereEqualTo("invitationCode", invitationCode)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val exchangeId = document.id
                    db.collection("exchanges").document(exchangeId).collection("participants")
                        .get()
                        .addOnSuccessListener { participantDocs ->
                            participantList.clear()
                            for (participantDoc in participantDocs) {
                                val participant = participantDoc.toObject(Participant::class.java)
                                participantList.add(participant)
                            }
                            participantAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ExchangeDetails", "Error al cargar los participantes", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ExchangeDetails", "Error al buscar el intercambio para participantes", e)
            }
    }

    private fun closeRaffle() {
        // Comprobar si hay al menos 3 participantes en el intercambio actual
        if (participantList.size < 3) {
            Toast.makeText(this, "El sorteo no se puede llevar a cabo, se necesitan al menos 3 participantes", Toast.LENGTH_LONG).show()
        } else {
            val shuffledParticipants = participantList.shuffled()
            val assignments = mutableMapOf<String, String>() // Almacena asignaciones email -> email

            for (i in shuffledParticipants.indices) {
                val giver = shuffledParticipants[i]
                val receiver = shuffledParticipants[(i + 1) % shuffledParticipants.size] // Asignar circularmente
                assignments[giver.email] = receiver.email
            }

            val exchangeId = intent.getStringExtra("exchangeId") ?: return

            // Guardar las asignaciones en la base de datos
            val batch = db.batch()
            assignments.forEach { (giverEmail, receiverEmail) ->
                val assignmentData = mapOf(
                    "giver" to giverEmail,
                    "receiver" to receiverEmail
                )
                val assignmentRef = db.collection("exchanges")
                    .document(exchangeId)
                    .collection("assignments")
                    .document(giverEmail)
                batch.set(assignmentRef, assignmentData)
            }

            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(this, "El sorteo se ha realizado con éxito", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("ExchangeDetails", "Error al guardar las asignaciones", e)
                    Toast.makeText(this, "Error al realizar el sorteo", Toast.LENGTH_SHORT).show()
                }
        }
    }

    //Agregar participantes
    private fun addParticipant(invitationCode: String) {
        // Verificar si aun no pasa la fecha limite de registro consultandola de la BD
        db.collection("exchanges")
            .whereEqualTo("invitationCode", invitationCode)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.any { document ->
                        val deadlineString = document.getString("deadline")
                        if (deadlineString != null) {
                            try {
                                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                val deadlineDate = sdf.parse(deadlineString)
                                val currentDate = Calendar.getInstance().time
                                deadlineDate != null && deadlineDate.before(currentDate) // Comparar fechas
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
            }

    }
}
