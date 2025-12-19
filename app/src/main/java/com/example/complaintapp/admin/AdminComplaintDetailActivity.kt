package com.example.complaintapp.admin

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.complaintapp.R
import com.example.complaintapp.repository.ComplaintRepository
import com.example.complaintapp.util.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AdminComplaintDetailActivity : AppCompatActivity() {

    private lateinit var rgStatus: RadioGroup
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnUpdateStatus: MaterialButton
    private lateinit var progressBar: ProgressBar
    
    private val complaintRepository = ComplaintRepository()
    private var complaintId: String = ""
    private var currentStatus = Constants.STATUS_PENDING
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_admin_complaint_detail)

        complaintId = intent.getStringExtra("complaint_id") ?: ""
        
        initViews()
        setupClickListeners()
        loadComplaintData()
    }

    private fun initViews() {
        rgStatus = findViewById(R.id.rgStatus)
        etNotes = findViewById(R.id.etNotes)
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus)
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnUpdateStatus.setOnClickListener {
            updateComplaintStatus()
        }
    }

    private fun loadComplaintData() {
        if (complaintId.isEmpty()) {
            // Load from intent extras (fallback)
            loadFromIntent()
            return
        }

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = complaintRepository.getComplaintById(complaintId)
            
            progressBar.visibility = View.GONE

            result.onSuccess { complaint ->
                displayComplaint(complaint)
            }.onFailure { exception ->
                Toast.makeText(this@AdminComplaintDetailActivity, "Failed to load complaint: ${exception.message}", Toast.LENGTH_SHORT).show()
                // Fallback to intent data
                loadFromIntent()
            }
        }
    }

    private fun loadFromIntent() {
        val category = intent.getStringExtra("category") ?: Constants.CATEGORY_ELECTRICITY
        val title = intent.getStringExtra("title") ?: "Complaint"
        val description = intent.getStringExtra("description") ?: title
        val status = intent.getStringExtra("status") ?: Constants.STATUS_PENDING
        val urgency = intent.getStringExtra("urgency") ?: Constants.URGENCY_LOW
        val location = intent.getStringExtra("location") ?: ""
        val userName = intent.getStringExtra("user_name") ?: "Student"
        val userRoom = intent.getStringExtra("user_room") ?: "Room"
        val adminNotes = intent.getStringExtra("admin_notes") ?: ""
        val createdAt = intent.getLongExtra("created_at", System.currentTimeMillis())

        currentStatus = status

        // Set complaint ID
        findViewById<TextView>(R.id.tvComplaintId).text = complaintId.ifEmpty { "N/A" }
        
        // Set urgency badge
        val tvUrgency = findViewById<TextView>(R.id.tvUrgency)
        tvUrgency.text = urgency.uppercase()
        
        // Set student info
        findViewById<TextView>(R.id.tvStudentName).text = userName
        findViewById<TextView>(R.id.tvRoom).text = userRoom
        
        // Set complaint details
        findViewById<TextView>(R.id.tvDate).text = dateFormat.format(java.util.Date(createdAt))
        findViewById<TextView>(R.id.tvDescription).text = description
        
        // Set admin notes if available
        if (adminNotes.isNotEmpty()) {
            etNotes.setText(adminNotes)
        }
        
        // Set category
        updateCategory(category)
        
        // Set current status in radio group
        when (status) {
            Constants.STATUS_PENDING -> rgStatus.check(R.id.rbPending)
            Constants.STATUS_IN_PROGRESS -> rgStatus.check(R.id.rbInProgress)
            Constants.STATUS_RESOLVED -> rgStatus.check(R.id.rbResolved)
        }
    }

    private fun displayComplaint(complaint: com.example.complaintapp.data.Complaint) {
        currentStatus = complaint.status

        // Set complaint ID
        findViewById<TextView>(R.id.tvComplaintId).text = complaint.id
        
        // Set urgency badge
        val tvUrgency = findViewById<TextView>(R.id.tvUrgency)
        tvUrgency.text = complaint.urgency.uppercase()
        
        // Set student info
        findViewById<TextView>(R.id.tvStudentName).text = complaint.userName
        findViewById<TextView>(R.id.tvRoom).text = complaint.userRoom
        
        // Set complaint details
        findViewById<TextView>(R.id.tvDate).text = dateFormat.format(java.util.Date(complaint.createdAt))
        findViewById<TextView>(R.id.tvDescription).text = complaint.description
        
        // Set admin notes if available
        if (complaint.adminNotes.isNotEmpty()) {
            etNotes.setText(complaint.adminNotes)
        }
        
        // Set category
        updateCategory(complaint.category)
        
        // Set current status in radio group
        when (complaint.status) {
            Constants.STATUS_PENDING -> rgStatus.check(R.id.rbPending)
            Constants.STATUS_IN_PROGRESS -> rgStatus.check(R.id.rbInProgress)
            Constants.STATUS_RESOLVED -> rgStatus.check(R.id.rbResolved)
        }
    }

    private fun updateCategory(category: String) {
        val (iconRes, nameRes, bgRes) = when (category) {
            Constants.CATEGORY_ELECTRICITY -> Triple(R.drawable.ic_electricity, R.string.electricity, R.drawable.bg_icon_electricity)
            Constants.CATEGORY_WATER -> Triple(R.drawable.ic_water, R.string.water, R.drawable.bg_icon_water)
            Constants.CATEGORY_MAINTENANCE -> Triple(R.drawable.ic_maintenance, R.string.maintenance, R.drawable.bg_icon_maintenance)
            Constants.CATEGORY_CLEANLINESS -> Triple(R.drawable.ic_cleanliness, R.string.cleanliness, R.drawable.bg_icon_cleanliness)
            Constants.CATEGORY_STAFF -> Triple(R.drawable.ic_staff, R.string.staff, R.drawable.bg_icon_staff)
            else -> Triple(R.drawable.ic_electricity, R.string.electricity, R.drawable.bg_icon_electricity)
        }

        findViewById<ImageView>(R.id.ivCategoryIcon).setImageResource(iconRes)
        findViewById<FrameLayout>(R.id.categoryIconFrame).setBackgroundResource(bgRes)
        findViewById<TextView>(R.id.tvCategory).setText(nameRes)
    }

    private fun updateComplaintStatus() {
        val newStatus = when (rgStatus.checkedRadioButtonId) {
            R.id.rbPending -> Constants.STATUS_PENDING
            R.id.rbInProgress -> Constants.STATUS_IN_PROGRESS
            R.id.rbResolved -> Constants.STATUS_RESOLVED
            else -> currentStatus
        }

        val notes = etNotes.text?.toString()?.trim() ?: ""

        if (complaintId.isEmpty()) {
            Toast.makeText(this, "Invalid complaint ID", Toast.LENGTH_SHORT).show()
            return
        }

        btnUpdateStatus.isEnabled = false
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = complaintRepository.updateComplaintStatus(complaintId, newStatus, notes)
            
            btnUpdateStatus.isEnabled = true
            progressBar.visibility = View.GONE

            result.onSuccess {
                val statusText = when (newStatus) {
                    Constants.STATUS_PENDING -> getString(R.string.status_pending)
                    Constants.STATUS_IN_PROGRESS -> getString(R.string.in_progress)
                    Constants.STATUS_RESOLVED -> getString(R.string.resolved)
                    Constants.STATUS_CANCELLED -> getString(R.string.cancelled)
                    else -> newStatus
                }

                Toast.makeText(
                    this@AdminComplaintDetailActivity,
                    getString(R.string.status_updated_to, statusText),
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }.onFailure { exception ->
                Toast.makeText(
                    this@AdminComplaintDetailActivity,
                    "Failed to update: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

