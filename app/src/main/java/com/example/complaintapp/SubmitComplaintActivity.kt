package com.example.complaintapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.complaintapp.ai.GeminiService
import com.example.complaintapp.ai.ImageAnalysisResult
import com.example.complaintapp.data.Complaint
import com.example.complaintapp.repository.AuthRepository
import com.example.complaintapp.repository.ComplaintRepository
import com.example.complaintapp.util.Constants
import com.example.complaintapp.util.ImageUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class SubmitComplaintActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SubmitComplaint"
        const val EXTRA_CATEGORY = "extra_category"
    }

    private var selectedCategory: String? = null
    private var selectedUrgency: String? = null  // Start with null to know if AI has run
    private var aiAnalysisCompleted = false
    
    // Category cards
    private lateinit var cardElectricity: MaterialCardView
    private lateinit var cardWater: MaterialCardView
    private lateinit var cardMaintenance: MaterialCardView
    private lateinit var cardCleanliness: MaterialCardView
    private lateinit var cardStaff: MaterialCardView
    
    // UI Elements
    private lateinit var selectedCategoryContainer: LinearLayout
    private lateinit var ivCategoryIcon: ImageView
    private lateinit var tvSelectedCategory: TextView
    private lateinit var categoryGrid: GridLayout
    private lateinit var etComplaint: TextInputEditText
    private lateinit var etRoom: TextInputEditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnAddPhoto: MaterialButton
    private lateinit var btnRemovePhoto: ImageButton
    private lateinit var cardImagePreview: MaterialCardView
    private lateinit var ivImagePreview: ImageView
    private lateinit var tvAnalyzingImage: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvAnalyzing: TextView

    private val authRepository = AuthRepository()
    private val complaintRepository = ComplaintRepository()
    private val geminiService = GeminiService()
    
    private var analysisJob: Job? = null
    private var selectedImageBitmap: Bitmap? = null
    private var imageBase64: String = ""
    private var imageAnalysisResult: ImageAnalysisResult? = null
    private var cameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_submit_complaint)

        initViews()
        setupClickListeners()
        setupImagePickers()
        
        // Check if category was passed from MainActivity
        val preSelectedCategory = intent.getStringExtra(EXTRA_CATEGORY)
        if (preSelectedCategory != null) {
            selectCategory(preSelectedCategory)
        }
    }

    private fun initViews() {
        selectedCategoryContainer = findViewById(R.id.selectedCategoryContainer)
        ivCategoryIcon = findViewById(R.id.ivCategoryIcon)
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory)
        categoryGrid = findViewById(R.id.categoryGrid)
        etComplaint = findViewById(R.id.etComplaint)
        etRoom = findViewById(R.id.etRoom)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnAddPhoto = findViewById(R.id.btnAddPhoto)
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto)
        cardImagePreview = findViewById(R.id.cardImagePreview)
        ivImagePreview = findViewById(R.id.ivImagePreview)
        tvAnalyzingImage = findViewById(R.id.tvAnalyzingImage)
        tvAnalyzing = findViewById(R.id.tvAnalyzing)
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
        
        cardElectricity = findViewById(R.id.cardElectricity)
        cardWater = findViewById(R.id.cardWater)
        cardMaintenance = findViewById(R.id.cardMaintenance)
        cardCleanliness = findViewById(R.id.cardCleanliness)
        cardStaff = findViewById(R.id.cardStaff)
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Category cards
        cardElectricity.setOnClickListener { selectCategory("electricity") }
        cardWater.setOnClickListener { selectCategory("water") }
        cardMaintenance.setOnClickListener { selectCategory("maintenance") }
        cardCleanliness.setOnClickListener { selectCategory("cleanliness") }
        cardStaff.setOnClickListener { selectCategory("staff") }

        // Submit button
        btnSubmit.setOnClickListener {
            submitComplaint()
        }

        // Add photo button
        btnAddPhoto.setOnClickListener {
            showImagePickerDialog()
        }

        // Remove photo button
        btnRemovePhoto.setOnClickListener {
            removeImage()
        }

        // Add text watcher for AI analysis
        etComplaint.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim() ?: ""
                if (text.length >= 20) {
                    // Cancel previous analysis
                    analysisJob?.cancel()
                    // Debounce: wait 1 second after user stops typing
                    analysisJob = lifecycleScope.launch {
                        delay(1000)
                        analyzeComplaint(text)
                    }
                }
            }
        })
    }
    
    /**
     * Determines the final urgency level using:
     * 1. AI analysis result (if available)
     * 2. Keyword-based fallback detection
     */
    private fun determineFinalUrgency(complaintText: String): String {
        val textLower = complaintText.lowercase()
        
        // High urgency keywords - safety hazards, emergencies
        val highUrgencyKeywords = listOf(
            "hazard", "danger", "dangerous", "emergency", "urgent", "severe",
            "short circuit", "short-circuit", "exposed wire", "naked wire", 
            "fire", "burning", "smoke", "spark", "electric shock", "electrocution",
            "flooding", "flood", "leak", "gas leak", "no water", "no electricity",
            "broken glass", "injury", "injured", "health", "safety", "risk",
            "immediate", "critical", "serious"
        )
        
        val keywordDetectedHigh = highUrgencyKeywords.any { keyword -> textLower.contains(keyword) }
        Log.d(TAG, "Keyword detection - High urgency keywords found: $keywordDetectedHigh")
        
        // If AI completed and returned a result, use it (but verify with keywords)
        if (aiAnalysisCompleted && selectedUrgency != null) {
            Log.d(TAG, "Using AI result: $selectedUrgency")
            
            // If AI said LOW but keywords suggest HIGH, override to HIGH
            if (selectedUrgency == Constants.URGENCY_LOW && keywordDetectedHigh) {
                Log.w(TAG, "⚠️ AI said LOW but keywords suggest HIGH - overriding to HIGH")
                return Constants.URGENCY_HIGH
            }
            
            return selectedUrgency!!
        }
        
        // AI didn't complete or failed - use keyword detection
        Log.d(TAG, "AI not available, using keyword-based detection")
        return if (keywordDetectedHigh) {
            Log.d(TAG, "Keyword detection: HIGH urgency")
            Constants.URGENCY_HIGH
        } else {
            Log.d(TAG, "Keyword detection: LOW urgency (default)")
            Constants.URGENCY_LOW
        }
    }
    
    private fun analyzeComplaint(description: String) {
        Log.d(TAG, "Starting AI analysis for description: ${description.take(50)}...")
        
        lifecycleScope.launch {
            tvAnalyzing.visibility = View.VISIBLE
            
            val result = geminiService.analyzeComplaint(description)
            
            tvAnalyzing.visibility = View.GONE
            
            result.onSuccess { analysis ->
                Log.d(TAG, "✅ AI Analysis SUCCESS - Category: ${analysis.category}, Urgency: ${analysis.urgency}")
                aiAnalysisCompleted = true
                
                // Auto-select category if not already selected
                if (selectedCategory == null) {
                    Log.d(TAG, "Auto-selecting category: ${analysis.category}")
                    selectCategory(analysis.category)
                }
                
                // Set urgency
                selectedUrgency = analysis.urgency
                Log.d(TAG, "Urgency set to: $selectedUrgency")
                
                Toast.makeText(
                    this@SubmitComplaintActivity,
                    "AI Detected: ${analysis.category.uppercase()} (${analysis.urgency.uppercase()} urgency)",
                    Toast.LENGTH_LONG
                ).show()
            }.onFailure { error ->
                Log.e(TAG, "❌ AI Analysis FAILED: ${error.message}", error)
                aiAnalysisCompleted = false
                
                Toast.makeText(
                    this@SubmitComplaintActivity,
                    "AI analysis failed: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun selectCategory(category: String) {
        selectedCategory = category
        
        // Reset all cards
        resetAllCards()
        
        // Highlight selected card
        val selectedCard = when (category) {
            "electricity" -> cardElectricity
            "water" -> cardWater
            "maintenance" -> cardMaintenance
            "cleanliness" -> cardCleanliness
            "staff" -> cardStaff
            else -> null
        }
        
        selectedCard?.let {
            it.strokeColor = ContextCompat.getColor(this, R.color.primary)
            it.strokeWidth = 2 * resources.displayMetrics.density.toInt()
            it.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_container))
        }
        
        // Update header chip
        updateHeaderChip(category)
    }

    private fun resetAllCards() {
        val cards = listOf(cardElectricity, cardWater, cardMaintenance, cardCleanliness, cardStaff)
        val outlineColor = ContextCompat.getColor(this, R.color.outline)
        val surfaceColor = ContextCompat.getColor(this, R.color.surface)
        
        cards.forEach { card ->
            card.strokeColor = outlineColor
            card.strokeWidth = resources.displayMetrics.density.toInt()
            card.setCardBackgroundColor(surfaceColor)
        }
    }

    private fun updateHeaderChip(category: String) {
        selectedCategoryContainer.visibility = View.VISIBLE
        
        val (iconRes, nameRes, colorRes) = when (category) {
            "electricity" -> Triple(R.drawable.ic_electricity, R.string.electricity, R.color.category_electricity)
            "water" -> Triple(R.drawable.ic_water, R.string.water, R.color.category_water)
            "maintenance" -> Triple(R.drawable.ic_maintenance, R.string.maintenance, R.color.category_maintenance)
            "cleanliness" -> Triple(R.drawable.ic_cleanliness, R.string.cleanliness, R.color.category_cleanliness)
            "staff" -> Triple(R.drawable.ic_staff, R.string.staff, R.color.category_staff)
            else -> return
        }
        
        ivCategoryIcon.setImageResource(iconRes)
        ivCategoryIcon.setColorFilter(ContextCompat.getColor(this, R.color.white))
        tvSelectedCategory.setText(nameRes)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            onImageSelected(cameraImageUri!!)
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onImageSelected(it) }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupImagePickers() {
        // Setup is done via launchers above
    }

    private fun showImagePickerDialog() {
        val options = arrayOf(getString(R.string.take_photo), getString(R.string.choose_from_gallery))
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.attach_photo))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        try {
            val imageFile = File(getExternalFilesDir(null), "camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera: ${e.message}", e)
            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun onImageSelected(uri: Uri) {
        lifecycleScope.launch {
            try {
                val bitmap = ImageUtils.getScaledBitmap(uri, this@SubmitComplaintActivity)
                if (bitmap != null) {
                    selectedImageBitmap = bitmap
                    ivImagePreview.setImageBitmap(bitmap)
                    cardImagePreview.visibility = View.VISIBLE
                    btnAddPhoto.text = getString(R.string.change_photo)
                    
                    // Compress and convert to Base64
                    val compressedBitmap = ImageUtils.compressBitmap(bitmap)
                    imageBase64 = ImageUtils.bitmapToBase64(compressedBitmap)
                    
                    // Analyze image
                    analyzeImage(compressedBitmap)
                } else {
                    Toast.makeText(this@SubmitComplaintActivity, getString(R.string.failed_to_load_image), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}", e)
                Toast.makeText(this@SubmitComplaintActivity, "Error processing image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun analyzeImage(bitmap: Bitmap) {
        tvAnalyzingImage.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val result = geminiService.analyzeComplaintImage(bitmap)
            
            tvAnalyzingImage.visibility = View.GONE
            
            result.onSuccess { analysis ->
                imageAnalysisResult = analysis
                
                // Auto-select category if not already selected
                if (selectedCategory == null) {
                    selectCategory(analysis.category)
                }
                
                // Set urgency
                selectedUrgency = analysis.urgency
                
                // Show AI suggestions
                val suggestions = buildString {
                    append("AI Detected: ${analysis.category.uppercase()} (${analysis.urgency.uppercase()} urgency)\n")
                    if (analysis.problemDescription.isNotEmpty()) {
                        append("\nIssue: ${analysis.problemDescription}")
                    }
                    if (analysis.suggestedRepairSteps.isNotEmpty()) {
                        append("\n\nSuggested: ${analysis.suggestedRepairSteps}")
                    }
                }
                
                Toast.makeText(
                    this@SubmitComplaintActivity,
                    "AI analysis complete",
                    Toast.LENGTH_LONG
                ).show()
                
                // Optionally auto-fill description if empty
                if (etComplaint.text?.toString()?.trim().isNullOrEmpty() && analysis.problemDescription.isNotEmpty()) {
                    etComplaint.setText(analysis.problemDescription)
                }
            }.onFailure { error ->
                Log.e(TAG, "Image analysis failed: ${error.message}", error)
                Toast.makeText(
                    this@SubmitComplaintActivity,
                    getString(R.string.image_analysis_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun removeImage() {
        selectedImageBitmap = null
        imageBase64 = ""
        imageAnalysisResult = null
        cardImagePreview.visibility = View.GONE
        btnAddPhoto.text = getString(R.string.attach_photo)
    }

    private fun submitComplaint() {
        val complaintText = etComplaint.text?.toString()?.trim() ?: ""
        val roomText = etRoom.text?.toString()?.trim() ?: ""

        Log.d(TAG, "========== SUBMIT COMPLAINT START ==========")
        Log.d(TAG, "Complaint text: $complaintText")
        Log.d(TAG, "Selected category: $selectedCategory")
        Log.d(TAG, "AI analysis completed: $aiAnalysisCompleted")
        Log.d(TAG, "Selected urgency from AI: $selectedUrgency")

        // Validation
        if (selectedCategory == null) {
            Toast.makeText(this, getString(R.string.please_select_category), Toast.LENGTH_SHORT).show()
            return
        }

        if (complaintText.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_describe_issue), Toast.LENGTH_SHORT).show()
            etComplaint.requestFocus()
            return
        }

        if (complaintText.length < 10) {
            Toast.makeText(this, getString(R.string.complaint_too_short), Toast.LENGTH_SHORT).show()
            etComplaint.requestFocus()
            return
        }

        // Get current user
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Determine final urgency - use AI result, or fallback to keyword detection
        val finalUrgency = determineFinalUrgency(complaintText)
        Log.d(TAG, "Final urgency to be saved: $finalUrgency")

        // Get user data
        lifecycleScope.launch {
            btnSubmit.isEnabled = false
            progressBar.visibility = View.VISIBLE

            val userResult = authRepository.getCurrentUserData()
            
            userResult.onSuccess { user ->
                // Create complaint
                val title = if (complaintText.length > 50) {
                    complaintText.substring(0, 50) + "..."
                } else {
                    complaintText
                }

                // Build AI analysis text from image analysis if available
                val aiAnalysisText = imageAnalysisResult?.let { result ->
                    buildString {
                        append("Category: ${result.category}\n")
                        append("Urgency: ${result.urgency}\n")
                        if (result.problemDescription.isNotEmpty()) {
                            append("Issue: ${result.problemDescription}\n")
                        }
                        if (result.suggestedRepairSteps.isNotEmpty()) {
                            append("Suggested: ${result.suggestedRepairSteps}")
                        }
                    }
                } ?: ""

                val complaint = Complaint(
                    userId = currentUser.uid,
                    userName = user.name,
                    userRoom = roomText.ifEmpty { user.room },
                    category = selectedCategory!!,
                    urgency = finalUrgency,
                    title = title,
                    description = complaintText,
                    location = roomText.ifEmpty { user.room },
                    status = Constants.STATUS_PENDING,
                    imageBase64 = imageBase64,
                    aiImageAnalysis = aiAnalysisText
                )
                
                Log.d(TAG, "Complaint object created with urgency: ${complaint.urgency}")

                // Save to Firestore
                Log.d(TAG, "Saving complaint to Firestore...")
                val saveResult = complaintRepository.saveComplaint(complaint)
                
                btnSubmit.isEnabled = true
                progressBar.visibility = View.GONE

                saveResult.onSuccess {
                    Log.d(TAG, "✅ Complaint saved successfully!")
                    Log.d(TAG, "========== SUBMIT COMPLAINT SUCCESS ==========")
                    Toast.makeText(this@SubmitComplaintActivity, getString(R.string.complaint_submitted), Toast.LENGTH_SHORT).show()
                    finish()
                }.onFailure { exception ->
                    Log.e(TAG, "❌ Failed to save complaint: ${exception.message}", exception)
                    Toast.makeText(
                        this@SubmitComplaintActivity,
                        "Failed to submit: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                btnSubmit.isEnabled = true
                progressBar.visibility = View.GONE
                Toast.makeText(this@SubmitComplaintActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

