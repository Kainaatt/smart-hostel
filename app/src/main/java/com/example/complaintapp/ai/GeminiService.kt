package com.example.complaintapp.ai

import com.example.complaintapp.util.Constants
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ComplaintAnalysis(
    val category: String,
    val urgency: String,
    val confidence: Float = 0.8f
)

class GeminiService {
    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = Constants.GEMINI_API_KEY
        )
    }

    suspend fun analyzeComplaint(description: String): Result<ComplaintAnalysis> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = createAnalysisPrompt(description)
                val response = model.generateContent(prompt).text
                
                if (response != null) {
                    val analysis = parseResponse(response)
                    Result.success(analysis)
                } else {
                    // Fallback if response is null
                    Result.success(
                        ComplaintAnalysis(
                            category = Constants.CATEGORY_MAINTENANCE,
                            urgency = Constants.URGENCY_LOW
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun createAnalysisPrompt(description: String): String {
        return """
            Analyze the following hostel complaint and determine:
            1. Category: Choose ONE from [electricity, water, maintenance, cleanliness, staff]
            2. Urgency: Choose ONE from [high, low]
            
            Rules:
            - Category "electricity": power outages, electrical issues, fan/light problems
            - Category "water": water supply, pressure, leaks, sanitation issues
            - Category "maintenance": broken furniture, doors, windows, repairs needed
            - Category "cleanliness": dirty areas, cleaning requests, hygiene issues
            - Category "staff": behavior issues, complaints about hostel staff
            - Urgency "high": critical issues like no water, power failure, safety concerns
            - Urgency "low": minor maintenance, non-urgent cleaning, routine issues
            
            Complaint text: "$description"
            
            Respond ONLY with a JSON object in this exact format:
            {
                "category": "electricity",
                "urgency": "high"
            }
        """.trimIndent()
    }

    private fun parseResponse(response: String): ComplaintAnalysis {
        return try {
            // Extract JSON from response (handle markdown code blocks if present)
            var jsonString = response.trim()
            if (jsonString.startsWith("```")) {
                jsonString = jsonString.substringAfter("```")
                jsonString = jsonString.substringBeforeLast("```")
            }
            if (jsonString.startsWith("json")) {
                jsonString = jsonString.substringAfter("json")
            }
            jsonString = jsonString.trim()
            
            val json = JSONObject(jsonString)
            val category = json.getString("category").lowercase()
            val urgency = json.getString("urgency").lowercase()
            
            // Validate category
            val validCategory = when (category) {
                "electricity", "water", "maintenance", "cleanliness", "staff" -> category
                else -> Constants.CATEGORY_MAINTENANCE // default
            }
            
            // Validate urgency
            val validUrgency = when (urgency) {
                "high", "medium", "low" -> urgency
                else -> Constants.URGENCY_LOW // default
            }
            
            ComplaintAnalysis(
                category = validCategory,
                urgency = validUrgency
            )
        } catch (e: Exception) {
            // Fallback to default values if parsing fails
            ComplaintAnalysis(
                category = Constants.CATEGORY_MAINTENANCE,
                urgency = Constants.URGENCY_LOW
            )
        }
    }
}

