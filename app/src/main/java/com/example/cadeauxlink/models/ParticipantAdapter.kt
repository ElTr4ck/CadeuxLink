package com.example.cadeauxlink.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cadeauxlink.R

class ParticipantAdapter(private val participants: List<Participant>) :
    RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder>() {

    class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvParticipantName)
        val email: TextView = itemView.findViewById(R.id.tvParticipantEmail)
        val status: TextView = itemView.findViewById(R.id.chipStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_participant, parent, false)
        return ParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val participant = participants[position]
        holder.name.text = participant.name
        holder.email.text = participant.email
        holder.status.text = participant.status
    }

    override fun getItemCount(): Int {
        return participants.size
    }
}