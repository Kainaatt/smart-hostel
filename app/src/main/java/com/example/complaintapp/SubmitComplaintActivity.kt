package com.example.complaintapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.complaintapp.ai.GeminiService
import com.example.complaintapp.data.Complaint
import com.example.complaintapp.repository.AuthRepository
import com.example.complaintapp.repository.ComplaintRepository
import com.example.complaintapp.util.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SubmitComplaintActivity : AppCompatActivity() {

    private var selectedCategory: String? = null
    private var selectedUrgency: String = "low"
    
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
    private lateinit var progressBar: ProgressBar
    private lateinit var tvAnalyzing: TextView

    private val authRepository = AuthRepository()
    private val complaintRepository = ComplaintRepository()
    private val geminiService = GeminiService()
    
    private var analysisJob: Job? = null

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_submit_complaint)

        initViews()
        setupClickListeners()
        
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
    
    private fun analyzeComplaint(description: String) {
        lifecycleScope.launch {
            tvAnalyzing.visibility = View.VISIBLE
            
            val result = geminiService.analyzeComplaint(description)
            
            tvAnalyzing.visibility = View.GONE
            
            result.onSuccess { analysis ->
                // Auto-select category if not already selected
                if (selectedCategory == null) {
                    selectCategory(analysis.category)
                }
                
                // Set urgency
                selectedUrgency = analysis.urgency
                
                Toast.makeText(
                    this@SubmitComplaintActivity,
                    "Detected: ${analysis.category} (${analysis.urgency} urgency)",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                // Silently fail - user can still select manually
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

    private fun submitComplaint() {
        val complaintText = etComplaint.text?.toString()?.trim() ?: ""
        val roomText = etRoom.text?.toString()?.trim() ?: ""

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

                val complaint = Complaint(
                    userId = currentUser.uid,
                    userName = user.name,
                    userRoom = roomText.ifEmpty { user.room },
                    category = selectedCategory!!,
                    urgency = selectedUrgency,
                    title = title,
                    description = complaintText,
                    location = roomText.ifEmpty { user.room },
                    status = Constants.STATUS_PENDING
                )

                // Save to Firestore
                val saveResult = complaintRepository.saveComplaint(complaint)
                
                btnSubmit.isEnabled = true
                progressBar.visibility = View.GONE

                saveResult.onSuccess {
                    Toast.makeText(this@SubmitComplaintActivity, getString(R.string.complaint_submitted), Toast.LENGTH_SHORT).show()
                    finish()
                }.onFailure { exception ->
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

