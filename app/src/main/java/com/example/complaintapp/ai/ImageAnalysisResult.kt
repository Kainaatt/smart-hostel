package com.example.complaintapp.ai

data class ImageAnalysisResult(
    val category: String,
    val urgency: String,
    val problemDescription: String,
    val suggestedRepairSteps: String,
    val detectedLocation: String? = null
)

