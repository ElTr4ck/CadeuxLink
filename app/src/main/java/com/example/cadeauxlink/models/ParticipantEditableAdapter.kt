package com.example.cadeauxlink.models
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cadeauxlink.models.Participant
import com.example.cadeauxlink.R

// Adaptador para el RecyclerView
class ParticipantEditableAdapter(
    private val participants: List<Participant>,
    private val onEditClick: (Participant) -> Unit,
    private val onDeleteClick: (Participant) -> Unit
) : RecyclerView.Adapter<ParticipantEditableAdapter.ParticipantEditableViewHolder>() {

    class ParticipantEditableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvParticipantName)
        val email: TextView = itemView.findViewById(R.id.tvParticipantEmail)
        val editButton: ImageButton = itemView.findViewById(R.id.btnEdit)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantEditableViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_participant_editable, parent, false)
        return ParticipantEditableViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantEditableViewHolder, position: Int) {
        val participant = participants[position]
        holder.name.text = participant.name
        holder.email.text = participant.email

        // Configurar los botones de edición y eliminación
        holder.editButton.setOnClickListener {
            onEditClick(participant)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(participant)
        }
    }

    override fun getItemCount(): Int {
        return participants.size
    }

}