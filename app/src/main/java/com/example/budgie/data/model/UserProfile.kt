package com.example.budgie.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val name: String,
    val birthday: String, // Format: YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis()
)
