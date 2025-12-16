package com.example.complaintapp.admin

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.complaintapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AdminComplaintDetailActivity : AppCompatActivity() {

    private lateinit var rgStatus: RadioGroup
    private lateinit var etNotes: TextInputEditText
    
    private var currentStatus = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_admin_complaint_detail)

        initViews()
        setupClickListeners()
        loadComplaintData()
    }

    private fun initViews() {
        rgStatus = findViewById(R.id.rgStatus)
        etNotes = findViewById(R.id.etNotes)
    }

    private fun setupClickListeners() {
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnUpdateStatus).setOnClickListener {
            updateComplaintStatus()
        }
    }

    private fun loadComplaintData() {
        // Get data from intent
        val complaintId = intent.getStringExtra("complaint_id") ?: "#CMP-001"
        val category = intent.getStringExtra("category") ?: "electricity"
        val title = intent.getStringExtra("title") ?: "Power outage"
        val status = intent.getStringExtra("status") ?: "pending"
        val urgency = intent.getStringExtra("urgency") ?: "high"
        val date = intent.getStringExtra("date") ?: "Dec 16, 2024"
        val studentName = intent.getStringExtra("student_name") ?: "Student"
        val room = intent.getStringExtra("room") ?: "Room 304"

        currentStatus = status

        // Set complaint ID
        findViewById<TextView>(R.id.tvComplaintId).text = complaintId
        
        // Set urgency badge
        val tvUrgency = findViewById<TextView>(R.id.tvUrgency)
        tvUrgency.text = urgency.uppercase()
        
        // Set student info
        findViewById<TextView>(R.id.tvStudentName).text = studentName
        findViewById<TextView>(R.id.tvRoom).text = room
        
        // Set complaint details
        findViewById<TextView>(R.id.tvDate).text = date
        findViewById<TextView>(R.id.tvDescription).text = "$title - This is a detailed description of the complaint submitted by the student."
        
        // Set category
        updateCategory(category)
        
        // Set current status in radio group
        when (status) {
            "pending" -> rgStatus.check(R.id.rbPending)
            "in_progress" -> rgStatus.check(R.id.rbInProgress)
            "resolved" -> rgStatus.check(R.id.rbResolved)
        }
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

    private fun updateComplaintStatus() {
        val newStatus = when (rgStatus.checkedRadioButtonId) {
            R.id.rbPending -> "pending"
            R.id.rbInProgress -> "in_progress"
            R.id.rbResolved -> "resolved"
            else -> currentStatus
        }

        val notes = etNotes.text?.toString()?.trim() ?: ""

        // TODO: Send update to backend
        // For now, just show success message

        val statusText = when (newStatus) {
            "pending" -> getString(R.string.status_pending)
            "in_progress" -> getString(R.string.in_progress)
            "resolved" -> getString(R.string.resolved)
            else -> newStatus
        }

        Toast.makeText(
            this,
            getString(R.string.status_updated_to, statusText),
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }
}

