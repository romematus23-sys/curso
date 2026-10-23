package com.cursokotlin.android.notakmp.model

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)