package com.example.complaintapp.data

import com.google.firebase.firestore.DocumentSnapshot

data class Complaint(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRoom: String = "",
    val category: String = "",
    val urgency: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val status: String = "pending", // pending, in_progress, resolved, cancelled
    val adminNotes: String = "",
    val imageBase64: String = "", // Compressed Base64 image data
    val aiImageAnalysis: String = "", // AI-generated description from image
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): Complaint {
            return Complaint(
                id = document.id,
                userId = document.getString("userId") ?: "",
                userName = document.getString("userName") ?: "",
                userRoom = document.getString("userRoom") ?: "",
                category = document.getString("category") ?: "",
                urgency = document.getString("urgency") ?: "low",
                title = document.getString("title") ?: "",
                description = document.getString("description") ?: "",
                location = document.getString("location") ?: "",
                status = document.getString("status") ?: "pending",
                adminNotes = document.getString("adminNotes") ?: "",
                imageBase64 = document.getString("imageBase64") ?: "",
                aiImageAnalysis = document.getString("aiImageAnalysis") ?: "",
                createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = document.getLong("updatedAt") ?: System.currentTimeMillis()
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "userName" to userName,
            "userRoom" to userRoom,
            "category" to category,
            "urgency" to urgency,
            "title" to title,
            "description" to description,
            "location" to location,
            "status" to status,
            "adminNotes" to adminNotes,
            "imageBase64" to imageBase64,
            "aiImageAnalysis" to aiImageAnalysis,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}

