package com.carlmanning.carlsbrain.domain.model

data class Bucket(
    val id: Long = 0,
    val name: String,
    val isVault: Boolean = false,
    val isUserCreated: Boolean = false,
    val colorHex: String = "#6750A4",
    val sortOrder: Int = 0
)
