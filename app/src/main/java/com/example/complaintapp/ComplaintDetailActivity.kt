package com.example.complaintapp

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

class ComplaintDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COMPLAINT_ID = "extra_complaint_id"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_URGENCY = "extra_urgency"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_DATE = "extra_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_complaint_detail)

        setupClickListeners()
        loadComplaintData()
    }

    private fun setupClickListeners() {
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadComplaintData() {
        // Get data from intent (or use defaults for demo)
        val complaintId = intent.getStringExtra(EXTRA_COMPLAINT_ID) ?: "#CMP-2024-001"
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "electricity"
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Power outage in Room 304"
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) 
            ?: "Power outage in Room 304. The electricity has been out since morning and it's affecting my studies. Please fix this as soon as possible."
        val status = intent.getStringExtra(EXTRA_STATUS) ?: "pending"
        val urgency = intent.getStringExtra(EXTRA_URGENCY) ?: "high"
        val location = intent.getStringExtra(EXTRA_LOCATION) ?: "Room 304, Block A"
        val date = intent.getStringExtra(EXTRA_DATE) ?: "Dec 16, 2024"

        // Set complaint ID
        findViewById<TextView>(R.id.tvComplaintId).text = complaintId
        
        // Set date
        findViewById<TextView>(R.id.tvDate).text = date
        
        // Set description
        findViewById<TextView>(R.id.tvDescription).text = description
        
        // Set location
        findViewById<TextView>(R.id.tvLocation).text = location

        // Set category
        updateCategory(category)
        
        // Set urgency
        updateUrgency(urgency)
        
        // Set status
        updateStatus(status)
    }

    private fun updateCategory(category: String) {
        val (iconRes, nameRes, bgRes) = when (category) {
            "electricity" -> Triple(R.drawable.ic_electricity, R.string.electricity, R.drawable.bg_icon_electricity)
            "water" -> Triple(R.drawable.ic_water, R.string.water, R.drawable.bg_icon_water)
            "maintenance" -> Triple(R.drawable.ic_maintenance, R.string.maintenance, R.drawable.bg_icon_maintenance)
            "cleanliness" -> Triple(R.drawable.ic_cleanliness, R.string.cleanliness, R.drawable.bg_icon_cleanliness)
            "staff" -> Triple(R.drawable.ic_staff, R.string.staff, R.drawable.bg_icon_staff)
            else -> Triple(R.drawable.ic_electricity, R.string.electricity, R.drawable.bg_icon_electricity)
        }

        findViewById<ImageView>(R.id.ivCategoryIcon).setImageResource(iconRes)
        findViewById<FrameLayout>(R.id.categoryIconFrame).setBackgroundResource(bgRes)
        findViewById<TextView>(R.id.tvCategory).setText(nameRes)
    }

    private fun updateUrgency(urgency: String) {
        val tvUrgency = findViewById<TextView>(R.id.tvUrgency)
        
        when (urgency) {
            "high" -> {
                tvUrgency.text = getString(R.string.high)
                tvUrgency.setTextColor(ContextCompat.getColor(this, R.color.urgency_high))
                tvUrgency.setBackgroundResource(R.drawable.bg_urgency_high)
            }
            "medium" -> {
                tvUrgency.text = getString(R.string.medium)
                tvUrgency.setTextColor(ContextCompat.getColor(this, R.color.urgency_medium))
                tvUrgency.setBackgroundResource(R.drawable.bg_urgency_medium)
            }
            "low" -> {
                tvUrgency.text = getString(R.string.low)
                tvUrgency.setTextColor(ContextCompat.getColor(this, R.color.urgency_low))
                tvUrgency.setBackgroundResource(R.drawable.bg_urgency_low)
            }
        }
    }

    private fun updateStatus(status: String) {
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        
        when (status) {
            "pending" -> tvStatus.text = getString(R.string.status_pending)
            "in_progress" -> tvStatus.text = getString(R.string.in_progress)
            "resolved" -> tvStatus.text = getString(R.string.resolved)
        }
    }
}

