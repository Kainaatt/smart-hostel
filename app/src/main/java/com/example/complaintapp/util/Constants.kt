package com.example.complaintapp.util

object Constants {
    // Gemini API Key - Replace with your actual API key
    // Get it from: https://makersuite.google.com/app/apikey
    const val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"
    
    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_COMPLAINTS = "complaints"
    
    // Complaint Status
    const val STATUS_PENDING = "pending"
    const val STATUS_IN_PROGRESS = "in_progress"
    const val STATUS_RESOLVED = "resolved"
    
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

