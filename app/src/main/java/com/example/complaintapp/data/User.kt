package com.example.complaintapp.data

import com.google.firebase.firestore.DocumentSnapshot

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val studentId: String = "",
    val room: String = "",
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): User {
            return User(
                id = document.id,
                name = document.getString("name") ?: "",
                email = document.getString("email") ?: "",
                studentId = document.getString("studentId") ?: "",
                room = document.getString("room") ?: "",
                isAdmin = document.getBoolean("isAdmin") ?: false,
                createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "email" to email,
            "studentId" to studentId,
            "room" to room,
            "isAdmin" to isAdmin,
            "createdAt" to createdAt
        )
    }
}

