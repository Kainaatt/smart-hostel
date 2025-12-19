package com.example.complaintapp.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.complaintapp.util.Constants
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
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
    
    companion object {
        private const val TAG = "GeminiService"
        // Using gemini-2.0-flash - stable model without thinking overhead
        private const val MODEL_NAME = "gemini-2.0-flash"
    }
    
    private val model: GenerativeModel by lazy {
        Log.d(TAG, "Initializing GenerativeModel with $MODEL_NAME")
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = Constants.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 1024  // Increased to avoid MAX_TOKENS error
            }
        )
    }

    suspend fun analyzeComplaint(description: String): Result<ComplaintAnalysis> {
        Log.d(TAG, "========== AI ANALYSIS START ==========")
        Log.d(TAG, "Input description: $description")
        
        return withContext(Dispatchers.IO) {
            try {
                val prompt = createAnalysisPrompt(description)
                Log.d(TAG, "Generated prompt (length: ${prompt.length})")
                
                Log.d(TAG, "Calling Gemini API...")
                val response = model.generateContent(prompt).text
                Log.d(TAG, "Raw API response: $response")
                
                if (response != null) {
                    val analysis = parseResponse(response)
                    Log.d(TAG, "✅ Parsed analysis - Category: ${analysis.category}, Urgency: ${analysis.urgency}")
                    Log.d(TAG, "========== AI ANALYSIS SUCCESS ==========")
                    Result.success(analysis)
                } else {
                    Log.e(TAG, "❌ API returned null response - using fallback LOW urgency")
                    Log.d(TAG, "========== AI ANALYSIS FALLBACK ==========")
                    Result.success(
                        ComplaintAnalysis(
                            category = Constants.CATEGORY_MAINTENANCE,
                            urgency = Constants.URGENCY_LOW
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ AI Analysis FAILED with exception: ${e.message}", e)
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                Log.d(TAG, "========== AI ANALYSIS ERROR ==========")
                Result.failure(e)
            }
        }
    }

    private fun createAnalysisPrompt(description: String): String {
        // Keep prompt short and simple to minimize token usage
        return """Classify this hostel complaint into category and urgency.

Categories: electricity, water, maintenance, cleanliness, staff
Urgency: high (safety/danger/emergency/hazard/short circuit/naked wires/fire) or low (routine)

Complaint: "$description"

Reply with JSON only: {"category":"...","urgency":"..."}"""
    }

    private fun parseResponse(response: String): ComplaintAnalysis {
        Log.d(TAG, "Parsing response: $response")
        
        return try {
            // Extract JSON from response (handle markdown code blocks if present)
            var jsonString = response.trim()
            Log.d(TAG, "Trimmed response: $jsonString")
            
            if (jsonString.startsWith("```")) {
                jsonString = jsonString.substringAfter("```")
                jsonString = jsonString.substringBeforeLast("```")
                Log.d(TAG, "After removing code blocks: $jsonString")
            }
            if (jsonString.startsWith("json")) {
                jsonString = jsonString.substringAfter("json")
                Log.d(TAG, "After removing 'json' prefix: $jsonString")
            }
            jsonString = jsonString.trim()
            Log.d(TAG, "Final JSON string to parse: $jsonString")
            
            val json = JSONObject(jsonString)
            val category = json.getString("category").lowercase()
            val urgency = json.getString("urgency").lowercase()
            
            Log.d(TAG, "Extracted from JSON - category: $category, urgency: $urgency")
            
            // Validate category
            val validCategory = when (category) {
                "electricity", "water", "maintenance", "cleanliness", "staff" -> category
                else -> {
                    Log.w(TAG, "Invalid category '$category', defaulting to maintenance")
                    Constants.CATEGORY_MAINTENANCE
                }
            }
            
            // Validate urgency - only accept high or low
            val validUrgency = when (urgency) {
                "high" -> Constants.URGENCY_HIGH
                "low" -> Constants.URGENCY_LOW
                "medium" -> {
                    Log.w(TAG, "AI returned 'medium' but we only use high/low - converting to low")
                    Constants.URGENCY_LOW
                }
                else -> {
                    Log.w(TAG, "Invalid urgency '$urgency', defaulting to low")
                    Constants.URGENCY_LOW
                }
            }
            
            Log.d(TAG, "Validated - category: $validCategory, urgency: $validUrgency")
            Log.d(TAG, "========================================")
            Log.d(TAG, "🎯 FINAL AI RESULT: Category=$validCategory, Urgency=$validUrgency")
            Log.d(TAG, "========================================")
            
            ComplaintAnalysis(
                category = validCategory,
                urgency = validUrgency
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ JSON parsing FAILED: ${e.message}", e)
            Log.e(TAG, "Using fallback values: maintenance, low")
            // Fallback to default values if parsing fails
            ComplaintAnalysis(
                category = Constants.CATEGORY_MAINTENANCE,
                urgency = Constants.URGENCY_LOW
            )
        }
    }

    suspend fun analyzeComplaintImage(bitmap: Bitmap): Result<ImageAnalysisResult> {
        Log.d(TAG, "========== IMAGE ANALYSIS START ==========")
        
        return withContext(Dispatchers.IO) {
            try {
                val prompt = createImageAnalysisPrompt()
                Log.d(TAG, "Generated image analysis prompt")
                
                // Create content with image and text
                val imageContent = content(role = "user") {
                    image(bitmap)
                    text(prompt)
                }
                
                Log.d(TAG, "Calling Gemini Vision API...")
                val response = model.generateContent(imageContent).text
                Log.d(TAG, "Raw API response: $response")
                
                if (response != null) {
                    val analysis = parseImageAnalysisResponse(response)
                    Log.d(TAG, "✅ Parsed image analysis - Category: ${analysis.category}, Urgency: ${analysis.urgency}")
                    Log.d(TAG, "========== IMAGE ANALYSIS SUCCESS ==========")
                    Result.success(analysis)
                } else {
                    Log.e(TAG, "❌ API returned null response")
                    Log.d(TAG, "========== IMAGE ANALYSIS FAILURE ==========")
                    Result.failure(Exception("API returned null response"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Image Analysis FAILED with exception: ${e.message}", e)
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                Log.d(TAG, "========== IMAGE ANALYSIS ERROR ==========")
                Result.failure(e)
            }
        }
    }

    private fun createImageAnalysisPrompt(): String {
        return """Analyze this hostel complaint image and provide:
1. Category: electricity/water/maintenance/cleanliness/staff
2. Urgency: high (safety hazard/emergency/danger) or low (routine)
3. Problem description: What issue is visible in the image
4. Suggested repair steps: Brief repair recommendations
5. Location details: Any visible room/area identifiers (optional)

Reply with JSON only: {"category":"...","urgency":"...","problemDescription":"...","suggestedRepairSteps":"...","detectedLocation":"..."}"""
    }

    private fun parseImageAnalysisResponse(response: String): ImageAnalysisResult {
        Log.d(TAG, "Parsing image analysis response: $response")
        
        return try {
            // Extract JSON from response
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
            val problemDescription = json.optString("problemDescription", "")
            val suggestedRepairSteps = json.optString("suggestedRepairSteps", "")
            val detectedLocation = json.optString("detectedLocation", null)
            
            // Validate category
            val validCategory = when (category) {
                "electricity", "water", "maintenance", "cleanliness", "staff" -> category
                else -> {
                    Log.w(TAG, "Invalid category '$category', defaulting to maintenance")
                    Constants.CATEGORY_MAINTENANCE
                }
            }
            
            // Validate urgency
            val validUrgency = when (urgency) {
                "high" -> Constants.URGENCY_HIGH
                "low" -> Constants.URGENCY_LOW
                "medium" -> {
                    Log.w(TAG, "AI returned 'medium', converting to low")
                    Constants.URGENCY_LOW
                }
                else -> {
                    Log.w(TAG, "Invalid urgency '$urgency', defaulting to low")
                    Constants.URGENCY_LOW
                }
            }
            
            Log.d(TAG, "✅ Parsed image analysis - Category: $validCategory, Urgency: $validUrgency")
            
            ImageAnalysisResult(
                category = validCategory,
                urgency = validUrgency,
                problemDescription = problemDescription.ifEmpty { "Issue detected in image" },
                suggestedRepairSteps = suggestedRepairSteps.ifEmpty { "Please contact maintenance staff" },
                detectedLocation = detectedLocation
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ JSON parsing FAILED: ${e.message}", e)
            // Fallback to default values
            ImageAnalysisResult(
                category = Constants.CATEGORY_MAINTENANCE,
                urgency = Constants.URGENCY_LOW,
                problemDescription = "Unable to analyze image",
                suggestedRepairSteps = "Please contact maintenance staff"
            )
        }
    }
}

