package com.example.complaintapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.complaintapp.data.Complaint
import com.example.complaintapp.repository.ComplaintRepository
import com.example.complaintapp.util.Constants
import com.example.complaintapp.util.ImageUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

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

    private lateinit var complaintRepository: ComplaintRepository
    private lateinit var btnCancelComplaint: MaterialButton
    private lateinit var progressBar: ProgressBar
    private var currentComplaint: Complaint? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_complaint_detail)

        complaintRepository = ComplaintRepository()
        initViews()
        setupClickListeners()
        loadComplaintData()
    }

    private fun initViews() {
        btnCancelComplaint = findViewById(R.id.btnCancelComplaint)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnCancelComplaint.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }

    private fun loadComplaintData() {
        val complaintId = intent.getStringExtra(EXTRA_COMPLAINT_ID)
        
        if (complaintId.isNullOrEmpty()) {
            // Fallback to intent extras
            loadFromIntent()
            return
        }

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = complaintRepository.getComplaintById(complaintId)
            
            progressBar.visibility = View.GONE

            result.onSuccess { complaint ->
                currentComplaint = complaint
                displayComplaint(complaint)
            }.onFailure { exception ->
                Toast.makeText(this@ComplaintDetailActivity, "Failed to load complaint: ${exception.message}", Toast.LENGTH_SHORT).show()
                // Fallback to intent data
                loadFromIntent()
            }
        }
    }

    private fun loadFromIntent() {
        val complaintId = intent.getStringExtra(EXTRA_COMPLAINT_ID) ?: "#CMP-2024-001"
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "electricity"
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
        
        // Show/hide cancel button based on status
        updateCancelButtonVisibility(status)
        
        // Update timeline (using fallback timestamps since we don't have them from intent)
        val createdAt = System.currentTimeMillis()
        val updatedAt = createdAt
        updateTimeline(status, createdAt, updatedAt)
    }

    private fun displayComplaint(complaint: Complaint) {
        // Set complaint ID
        findViewById<TextView>(R.id.tvComplaintId).text = complaint.id
        
        // Set date
        findViewById<TextView>(R.id.tvDate).text = dateFormat.format(java.util.Date(complaint.createdAt))
        
        // Set description
        findViewById<TextView>(R.id.tvDescription).text = complaint.description
        
        // Set location
        findViewById<TextView>(R.id.tvLocation).text = complaint.location

        // Set category
        updateCategory(complaint.category)
        
        // Set urgency
        updateUrgency(complaint.urgency)
        
        // Set status
        updateStatus(complaint.status)
        
        // Show/hide cancel button based on status
        updateCancelButtonVisibility(complaint.status)
        
        // Display image and AI analysis if available
        displayImageAndAnalysis(complaint)
        
        // Update timeline
        updateTimeline(complaint)
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
            Constants.STATUS_PENDING -> tvStatus.text = getString(R.string.status_pending)
            Constants.STATUS_IN_PROGRESS -> tvStatus.text = getString(R.string.in_progress)
            Constants.STATUS_RESOLVED -> tvStatus.text = getString(R.string.resolved)
            Constants.STATUS_CANCELLED -> tvStatus.text = getString(R.string.cancelled)
            else -> tvStatus.text = getString(R.string.status_pending)
        }
    }

    private fun updateCancelButtonVisibility(status: String) {
        btnCancelComplaint.visibility = if (status == Constants.STATUS_PENDING) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun updateTimeline(complaint: Complaint) {
        updateTimeline(complaint.status, complaint.createdAt, complaint.updatedAt)
    }

    private fun updateTimeline(status: String, createdAt: Long, updatedAt: Long) {
        val tvSubmittedDate = findViewById<TextView>(R.id.tvSubmittedDate)
        val timelineItem2 = findViewById<View>(R.id.timelineItem2)
        val timelineDot2 = findViewById<View>(R.id.timelineDot2)
        val timelineLine2 = findViewById<View>(R.id.timelineLine2)
        val tvTimelineStatus2 = findViewById<TextView>(R.id.tvTimelineStatus2)
        val tvTimelineDate2 = findViewById<TextView>(R.id.tvTimelineDate2)
        val timelineItem3 = findViewById<View>(R.id.timelineItem3)
        val timelineDot3 = findViewById<View>(R.id.timelineDot3)
        val tvTimelineStatus3 = findViewById<TextView>(R.id.tvTimelineStatus3)
        val tvTimelineDate3 = findViewById<TextView>(R.id.tvTimelineDate3)

        // Set submitted date
        tvSubmittedDate.text = dateTimeFormat.format(java.util.Date(createdAt))

        when (status) {
            Constants.STATUS_PENDING -> {
                // Show: Submitted (active), Under Review (active), Resolved (inactive)
                timelineItem2.visibility = View.VISIBLE
                timelineDot2.setBackgroundResource(R.drawable.bg_timeline_dot_active)
                timelineLine2.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
                tvTimelineStatus2.text = getString(R.string.under_review)
                tvTimelineStatus2.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                tvTimelineStatus2.textSize = 14f
                tvTimelineDate2.text = dateTimeFormat.format(java.util.Date(updatedAt))
                
                timelineItem3.visibility = View.VISIBLE
                timelineDot3.setBackgroundResource(R.drawable.bg_timeline_dot_inactive)
                tvTimelineStatus3.text = getString(R.string.status_resolved)
                tvTimelineStatus3.setTextColor(ContextCompat.getColor(this, R.color.text_hint))
                tvTimelineDate3.text = getString(R.string.pending_text)
            }
            Constants.STATUS_IN_PROGRESS -> {
                // Show: Submitted (active), Under Review (active), Resolved (inactive)
                timelineItem2.visibility = View.VISIBLE
                timelineDot2.setBackgroundResource(R.drawable.bg_timeline_dot_active)
                timelineLine2.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
                tvTimelineStatus2.text = getString(R.string.in_progress)
                tvTimelineStatus2.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                tvTimelineStatus2.textSize = 14f
                tvTimelineDate2.text = dateTimeFormat.format(java.util.Date(updatedAt))
                
                timelineItem3.visibility = View.VISIBLE
                timelineDot3.setBackgroundResource(R.drawable.bg_timeline_dot_inactive)
                tvTimelineStatus3.text = getString(R.string.status_resolved)
                tvTimelineStatus3.setTextColor(ContextCompat.getColor(this, R.color.text_hint))
                tvTimelineDate3.text = getString(R.string.pending_text)
            }
            Constants.STATUS_RESOLVED -> {
                // Show: Submitted (active), Under Review (active), Resolved (active)
                timelineItem2.visibility = View.VISIBLE
                timelineDot2.setBackgroundResource(R.drawable.bg_timeline_dot_active)
                timelineLine2.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
                tvTimelineStatus2.text = getString(R.string.in_progress)
                tvTimelineStatus2.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                tvTimelineStatus2.textSize = 14f
                tvTimelineDate2.text = dateTimeFormat.format(java.util.Date(updatedAt))
                
                timelineItem3.visibility = View.VISIBLE
                timelineDot3.setBackgroundResource(R.drawable.bg_timeline_dot_active)
                tvTimelineStatus3.text = getString(R.string.resolved)
                tvTimelineStatus3.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                tvTimelineStatus3.textSize = 14f
                tvTimelineStatus3.setTypeface(null, android.graphics.Typeface.BOLD)
                tvTimelineDate3.text = dateTimeFormat.format(java.util.Date(updatedAt))
            }
            Constants.STATUS_CANCELLED -> {
                // Show: Submitted (active), Cancelled (active with red color)
                timelineItem2.visibility = View.VISIBLE
                timelineDot2.setBackgroundResource(R.drawable.bg_timeline_dot_active)
                timelineLine2.setBackgroundColor(ContextCompat.getColor(this, R.color.urgency_high))
                tvTimelineStatus2.text = getString(R.string.complaint_cancelled_timeline)
                tvTimelineStatus2.setTextColor(ContextCompat.getColor(this, R.color.urgency_high))
                tvTimelineStatus2.textSize = 14f
                tvTimelineStatus2.setTypeface(null, android.graphics.Typeface.BOLD)
                tvTimelineDate2.text = dateTimeFormat.format(java.util.Date(updatedAt))
                
                // Hide third timeline item for cancelled complaints
                timelineItem3.visibility = View.GONE
            }
        }
    }

    private fun showCancelConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.cancel_complaint))
            .setMessage(getString(R.string.cancel_complaint_confirm))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                cancelComplaint()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun cancelComplaint() {
        val complaintId = currentComplaint?.id ?: intent.getStringExtra(EXTRA_COMPLAINT_ID)
        
        if (complaintId.isNullOrEmpty()) {
            Toast.makeText(this, "Complaint ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnCancelComplaint.isEnabled = false

        lifecycleScope.launch {
            val result = complaintRepository.cancelComplaint(complaintId)
            
            progressBar.visibility = View.GONE
            btnCancelComplaint.isEnabled = true

            result.onSuccess {
                Toast.makeText(this@ComplaintDetailActivity, getString(R.string.complaint_cancelled), Toast.LENGTH_SHORT).show()
                // Reload complaint to update UI
                loadComplaintData()
            }.onFailure { exception ->
                Toast.makeText(
                    this@ComplaintDetailActivity,
                    exception.message ?: getString(R.string.error_occurred),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun displayImageAndAnalysis(complaint: Complaint) {
        val cardComplaintImage = findViewById<MaterialCardView>(R.id.cardComplaintImage)
        val ivComplaintImage = findViewById<ImageView>(R.id.ivComplaintImage)
        val cardAIAnalysis = findViewById<MaterialCardView>(R.id.cardAIAnalysis)
        val tvAIAnalysis = findViewById<TextView>(R.id.tvAIAnalysis)
        val tvSuggestedRepairs = findViewById<TextView>(R.id.tvSuggestedRepairs)

        // Display image if available
        if (complaint.imageBase64.isNotEmpty()) {
            val bitmap = ImageUtils.base64ToBitmap(complaint.imageBase64)
            if (bitmap != null) {
                ivComplaintImage.setImageBitmap(bitmap)
                cardComplaintImage.visibility = View.VISIBLE
            }
        } else {
            cardComplaintImage.visibility = View.GONE
        }

        // Display AI analysis if available
        if (complaint.aiImageAnalysis.isNotEmpty()) {
            // Parse AI analysis text (format: Category: ...\nUrgency: ...\nIssue: ...\nSuggested: ...)
            val analysisLines = complaint.aiImageAnalysis.split("\n")
            val issueText = analysisLines.find { it.startsWith("Issue:") }?.substringAfter("Issue:")?.trim() ?: ""
            val suggestedText = analysisLines.find { it.startsWith("Suggested:") }?.substringAfter("Suggested:")?.trim() ?: ""

            if (issueText.isNotEmpty() || suggestedText.isNotEmpty()) {
                tvAIAnalysis.text = issueText.ifEmpty { "AI analysis available" }
                tvSuggestedRepairs.text = suggestedText.ifEmpty { "No specific suggestions" }
                cardAIAnalysis.visibility = View.VISIBLE
            } else {
                cardAIAnalysis.visibility = View.GONE
            }
        } else {
            cardAIAnalysis.visibility = View.GONE
        }
    }
}

