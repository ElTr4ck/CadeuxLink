package com.example.cadeauxlink.models

data class Exchange(
    val exchangeName: String = "",
    val maxAmount: String = "",
    val deadline: String = "",
    val exchangeDate: String = "",
    val location: String = "",
    val additionalComments: String = "",
    val invitationCode: String = "",
    val participants: List<String> = emptyList()
)

