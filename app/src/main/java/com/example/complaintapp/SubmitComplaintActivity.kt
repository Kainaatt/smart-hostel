package com.example.complaintapp

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class SubmitComplaintActivity : AppCompatActivity() {

    private var selectedCategory: String? = null
    
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
        findViewById<MaterialButton>(R.id.btnSubmit).setOnClickListener {
            submitComplaint()
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

        // TODO: Send complaint to backend/NLP API
        // For now, just show success message
        Toast.makeText(this, getString(R.string.complaint_submitted), Toast.LENGTH_SHORT).show()
        
        // Go back to home
        finish()
    }
}

