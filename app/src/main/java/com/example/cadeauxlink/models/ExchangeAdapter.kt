package com.example.cadeauxlink.models

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cadeauxlink.ExchangeDetailsActivity
import com.example.cadeauxlink.R

class ExchangeAdapter(
    private val context: Context,
    private val exchangeList: List<Exchange>
) : RecyclerView.Adapter<ExchangeAdapter.ExchangeViewHolder>() {

    class ExchangeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val exchangeName: TextView = itemView.findViewById(R.id.tvExchangeName)
        val exchangeDate: TextView = itemView.findViewById(R.id.tvExchangeDate)
        val exchangeParticipants: TextView = itemView.findViewById(R.id.tvParticipants)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_intercambio, parent, false)
        return ExchangeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExchangeViewHolder, position: Int) {
        val exchange = exchangeList[position]
        holder.exchangeName.text = exchange.exchangeName
        holder.exchangeDate.text = exchange.exchangeDate
        holder.exchangeParticipants.text = exchange.participants.size.toString()

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ExchangeDetailsActivity::class.java)
            intent.putExtra("exchangeId", exchange.invitationCode)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return exchangeList.size
    }
}
