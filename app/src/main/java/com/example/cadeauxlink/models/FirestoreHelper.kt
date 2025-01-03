package com.example.cadeauxlink.models

import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class FirestoreHelper(private val db: FirebaseFirestore) {

    /**
     * Obtiene la fecha límite ("deadline") de un intercambio.
     * @param exchangeId Código del intercambio (invitationCode).
     * @param onResult Callback con la fecha límite como String o null si ocurre un error.
     */
    fun getDeadline(exchangeId: String, onResult: (String?) -> Unit) {
        db.collection("exchanges")
            .whereEqualTo("invitationCode", exchangeId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onResult(null) // No se encontró el intercambio
                    return@addOnSuccessListener
                }
                val document = documents.first()
                val deadline = document.getString("deadline")
                onResult(deadline)
            }
            .addOnFailureListener {
                onResult(null) // Error al consultar Firestore
            }
    }

    /**
     * Verifica si la fecha límite de un intercambio ha pasado.
     * @param exchangeId Código del intercambio (invitationCode).
     * @param onResult Callback con un valor booleano (true si la fecha límite ha pasado, false en caso contrario).
     */
    fun isDeadlinePassed(exchangeId: String, onResult: (Boolean) -> Unit) {
        getDeadline(exchangeId) { deadlineString ->
            if (deadlineString == null) {
                onResult(false) // No se puede determinar, considerar como no pasada
                return@getDeadline
            }
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val deadlineDate = sdf.parse(deadlineString)
                val currentDate = Calendar.getInstance().time
                onResult(deadlineDate != null && currentDate.after(deadlineDate))
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false) // Error al procesar la fecha
            }
        }
    }

    /**
     * Obtiene los participantes de un intercambio.
     * @param exchangeId Código del intercambio (invitationCode).
     * @param onResult Callback con la lista de participantes o null si ocurre un error.
     */
    fun getParticipants(exchangeId: String, onResult: (List<Participant>?) -> Unit) {
        db.collection("exchanges")
            .whereEqualTo("invitationCode", exchangeId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onResult(null) // No se encontró el intercambio
                    return@addOnSuccessListener
                }
                val document = documents.first()
                document.reference.collection("participants")
                    .get()
                    .addOnSuccessListener { participants ->
                        val participantList = participants.mapNotNull { it.toObject(Participant::class.java) }
                        onResult(participantList)
                    }
                    .addOnFailureListener {
                        onResult(null) // Error al consultar participantes
                    }
            }
            .addOnFailureListener {
                onResult(null) // Error al consultar Firestore
            }
    }
}
