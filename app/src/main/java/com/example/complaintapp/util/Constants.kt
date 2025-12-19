package com.example.complaintapp.util

object Constants {
    const val GEMINI_API_KEY = "AIzaSyB4YvajlCpfFiQyGJly7F_VXzl2OREq4cg"
    
    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_COMPLAINTS = "complaints"
    
    // Complaint Status
    const val STATUS_PENDING = "pending"
    const val STATUS_IN_PROGRESS = "in_progress"
    const val STATUS_RESOLVED = "resolved"
    const val STATUS_CANCELLED = "cancelled"
    
    // Complaint Categories
    const val CATEGORY_ELECTRICITY = "electricity"
    const val CATEGORY_WATER = "water"
    const val CATEGORY_MAINTENANCE = "maintenance"
    const val CATEGORY_CLEANLINESS = "cleanliness"
    const val CATEGORY_STAFF = "staff"
    
    // Urgency Levels
    const val URGENCY_HIGH = "high"
    const val URGENCY_MEDIUM = "medium"
    const val URGENCY_LOW = "low"
}

