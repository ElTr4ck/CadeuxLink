package com.example.cadeauxlink

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cadeauxlink.databinding.ActivityParticipantsBinding
import com.example.cadeauxlink.models.FirestoreHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.example.cadeauxlink.models.ParticipantEditableAdapter
import com.example.cadeauxlink.models.Participant

class ParticipantsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParticipantsBinding
    private lateinit var db: FirebaseFirestore
    private val participantList = mutableListOf<Participant>()
    private lateinit var participantAdapter: ParticipantEditableAdapter
    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParticipantsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        firestoreHelper = FirestoreHelper(db)


        // Configurar el RecyclerView
        participantAdapter = ParticipantEditableAdapter(
            participantList,
            onEditClick = { participant ->
                editParticipant(participant)
            },
            onDeleteClick = { participant ->
                // Lógica para eliminar el participante
                deleteParticipant(participant)
            }
        )
        binding.rvParticipants.layoutManager = LinearLayoutManager(this)
        binding.rvParticipants.adapter = participantAdapter

        // Obtén el código de intercambio desde el Intent
        val exchangeId = intent.getStringExtra("exchangeId")
        if (exchangeId != null) {
            loadParticipants(exchangeId)
        } else {
            Toast.makeText(this, "No se pudo cargar el intercambio.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Configurar el botón para agregar participantes
        binding.btnAddParticipant.setOnClickListener {
            if (addParticipant()) return@setOnClickListener
        }


    }

    private fun addParticipant(): Boolean {
        val exchangeId = intent.getStringExtra("exchangeId")

        if (exchangeId == null) {
            Toast.makeText(this, "No se pudo cargar el intercambio.", Toast.LENGTH_SHORT).show()
            finish()
            return true
        }
        firestoreHelper.getDeadline(exchangeId) { deadline ->
            if (deadline == null) {
                Toast.makeText(this, "Error al obtener la fecha límite.", Toast.LENGTH_SHORT).show()
                return@getDeadline
            } else if (deadline.isEmpty()) {
                Toast.makeText(this, "La fecha límite no está configurada.", Toast.LENGTH_SHORT)
                    .show()
                return@getDeadline
            }
            else {
                // Verificar si la fecha limite ha pasado
                firestoreHelper.isDeadlinePassed(exchangeId) {
                    if (it) {
                        Toast.makeText(this, "El intercambio ya no acepta nuevos participantes.", Toast.LENGTH_SHORT).show()
                        return@isDeadlinePassed
                    }
                    else {
                        intent = Intent(this, AddParticipantActivity::class.java)
                        intent.putExtra("exchangeId", exchangeId)
                        intent.putExtra("exchangeDate", deadline)
                        startActivity(intent)
                    }
                }
            }
        }
        return false
    }

    private fun loadParticipants(exchangeId: String) {
        db.collection("exchanges")
            .whereEqualTo("invitationCode", exchangeId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No se encontró el intercambio.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    document.reference.collection("participants")
                        .get()
                        .addOnSuccessListener { participants ->
                            participantList.clear()
                            for (participantDoc in participants) {
                                val participant = participantDoc.toObject(Participant::class.java)
                                participantList.add(participant)
                            }
                            participantAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al cargar los participantes.", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al buscar el intercambio.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    private fun deleteParticipant(participant: Participant) {
        val exchangeId = intent.getStringExtra("exchangeId") ?: return
        db.collection("exchanges")
            .whereEqualTo("invitationCode", exchangeId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Intercambio no encontrado.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Verificar si el participante es el organizador
                if(participant.status == "Organizador"){
                    Toast.makeText(this, "No se puede eliminar al organizador.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    document.reference.collection("participants")
                        .whereEqualTo("email", participant.email)
                        .get()
                        .addOnSuccessListener { participantDocs ->
                            for (participantDoc in participantDocs) {
                                participantDoc.reference.delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Participante eliminado.", Toast.LENGTH_SHORT).show()
                                        participantList.remove(participant)
                                        participantAdapter.notifyDataSetChanged()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error al eliminar participante.", Toast.LENGTH_SHORT).show()
                                        e.printStackTrace()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al buscar participante.", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al buscar el intercambio.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    private fun editParticipant(participant: Participant) {
        val exchangeId = intent.getStringExtra("exchangeId") ?: return
        // Mandar a la vista de EditParticipantActivity
        intent = Intent(this, EditParticipantActivity::class.java)
        intent.putExtra("exchangeId", exchangeId)
        intent.putExtra("participantEmail", participant.email)
        intent.putExtra("participantName", participant.name)
        intent.putExtra("participantStatus", participant.status)
        intent.putExtra("participantPhone", participant.phone)
        startActivity(intent)
    }
}

