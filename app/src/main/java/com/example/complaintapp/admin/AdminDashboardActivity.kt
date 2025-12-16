package com.example.complaintapp.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.complaintapp.LoginActivity
import com.example.complaintapp.R
import com.google.android.material.card.MaterialCardView

class AdminDashboardActivity : AppCompatActivity() {

    // Dummy data
    private val complaints = listOf(
        AdminComplaint("#CMP-001", "electricity", "Power outage in Room 304", "pending", "high", "Dec 16, 2024", "Ahmed Khan", "Room 304"),
        AdminComplaint("#CMP-002", "cleanliness", "Bathroom cleaning required", "resolved", "low", "Dec 15, 2024", "Sara Ali", "Room 201"),
        AdminComplaint("#CMP-003", "water", "Low water pressure", "in_progress", "medium", "Dec 14, 2024", "Hassan Malik", "Room 105"),
        AdminComplaint("#CMP-004", "maintenance", "Broken window lock", "resolved", "low", "Dec 13, 2024", "Fatima Noor", "Room 302"),
        AdminComplaint("#CMP-005", "staff", "Rude behavior by security", "pending", "high", "Dec 12, 2024", "Ali Raza", "Room 410"),
        AdminComplaint("#CMP-006", "electricity", "Fan not working", "pending", "medium", "Dec 11, 2024", "Zainab Khan", "Room 205"),
        AdminComplaint("#CMP-007", "water", "Leaking tap", "in_progress", "low", "Dec 10, 2024", "Omar Farooq", "Room 108"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_admin_dashboard)

        setupClickListeners()
        updateStats()
        updateCategoryStats()
        loadHighPriorityComplaints()
    }

    private fun setupClickListeners() {
        // Logout button
        findViewById<FrameLayout>(R.id.btnLogout).setOnClickListener {
            showLogoutDialog()
        }

        // All Complaints card
        findViewById<MaterialCardView>(R.id.cardAllComplaints).setOnClickListener {
            startActivity(Intent(this, AdminComplaintsActivity::class.java).apply {
                putExtra("filter", "all")
            })
        }

        // Pending Review card
        findViewById<MaterialCardView>(R.id.cardPendingReview).setOnClickListener {
            startActivity(Intent(this, AdminComplaintsActivity::class.java).apply {
                putExtra("filter", "pending")
            })
        }

        // View All High Priority
        findViewById<TextView>(R.id.tvViewAllHighPriority).setOnClickListener {
            startActivity(Intent(this, AdminComplaintsActivity::class.java).apply {
                putExtra("filter", "high")
            })
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout))
            .setMessage(getString(R.string.logout_confirm))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun updateStats() {
        findViewById<TextView>(R.id.tvTotalComplaints).text = complaints.size.toString()
        findViewById<TextView>(R.id.tvPendingCount).text = complaints.count { it.status == "pending" }.toString()
        findViewById<TextView>(R.id.tvHighUrgencyCount).text = complaints.count { it.urgency == "high" }.toString()
    }

    private fun updateCategoryStats() {
        val categoryStats = mapOf(
            "electricity" to complaints.count { it.category == "electricity" },
            "water" to complaints.count { it.category == "water" },
            "maintenance" to complaints.count { it.category == "maintenance" },
            "cleanliness" to complaints.count { it.category == "cleanliness" },
            "staff" to complaints.count { it.category == "staff" }
        )

        // Update each category row
        updateCategoryRow(R.id.categoryElectricity, R.drawable.ic_electricity, "Electricity", categoryStats["electricity"] ?: 0)
        updateCategoryRow(R.id.categoryWater, R.drawable.ic_water, "Water & Sanitation", categoryStats["water"] ?: 0)
        updateCategoryRow(R.id.categoryMaintenance, R.drawable.ic_maintenance, "Maintenance", categoryStats["maintenance"] ?: 0)
        updateCategoryRow(R.id.categoryCleanliness, R.drawable.ic_cleanliness, "Cleanliness", categoryStats["cleanliness"] ?: 0)
        updateCategoryRow(R.id.categoryStaff, R.drawable.ic_staff, "Staff Behavior", categoryStats["staff"] ?: 0)
    }

    private fun updateCategoryRow(viewId: Int, iconRes: Int, name: String, count: Int) {
        val view = findViewById<LinearLayout>(viewId)
        view.findViewById<ImageView>(R.id.ivCategoryIcon).setImageResource(iconRes)
        view.findViewById<TextView>(R.id.tvCategoryName).text = name
        view.findViewById<TextView>(R.id.tvCategoryCount).text = count.toString()
    }

    private fun loadHighPriorityComplaints() {
        val container = findViewById<LinearLayout>(R.id.highPriorityContainer)
        container.removeAllViews()

        val highPriority = complaints.filter { it.urgency == "high" }.take(3)

        highPriority.forEach { complaint ->
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_admin_complaint, container, false)

            itemView.findViewById<TextView>(R.id.tvComplaintId).text = complaint.id
            itemView.findViewById<TextView>(R.id.tvTitle).text = complaint.title
            itemView.findViewById<TextView>(R.id.tvRoom).text = complaint.room
            itemView.findViewById<TextView>(R.id.tvDate).text = complaint.date

            // Set category icon
            val (iconRes, bgRes) = getCategoryResources(complaint.category)
            itemView.findViewById<ImageView>(R.id.ivCategoryIcon).setImageResource(iconRes)
            itemView.findViewById<FrameLayout>(R.id.categoryIconFrame).setBackgroundResource(bgRes)

            // Set urgency
            val tvUrgency = itemView.findViewById<TextView>(R.id.tvUrgency)
            tvUrgency.text = complaint.urgency.uppercase()

            // Set status
            val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
            tvStatus.text = getStatusText(complaint.status)

            itemView.setOnClickListener {
                openComplaintDetail(complaint)
            }

            container.addView(itemView)
        }
    }

    private fun openComplaintDetail(complaint: AdminComplaint) {
        startActivity(Intent(this, AdminComplaintDetailActivity::class.java).apply {
            putExtra("complaint_id", complaint.id)
            putExtra("category", complaint.category)
            putExtra("title", complaint.title)
            putExtra("status", complaint.status)
            putExtra("urgency", complaint.urgency)
            putExtra("date", complaint.date)
            putExtra("student_name", complaint.studentName)
            putExtra("room", complaint.room)
        })
    }

    private fun getCategoryResources(category: String): Pair<Int, Int> {
        return when (category) {
            "electricity" -> Pair(R.drawable.ic_electricity, R.drawable.bg_icon_electricity)
            "water" -> Pair(R.drawable.ic_water, R.drawable.bg_icon_water)
            "maintenance" -> Pair(R.drawable.ic_maintenance, R.drawable.bg_icon_maintenance)
            "cleanliness" -> Pair(R.drawable.ic_cleanliness, R.drawable.bg_icon_cleanliness)
            "staff" -> Pair(R.drawable.ic_staff, R.drawable.bg_icon_staff)
            else -> Pair(R.drawable.ic_electricity, R.drawable.bg_icon_electricity)
        }
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "pending" -> getString(R.string.pending)
            "in_progress" -> getString(R.string.in_progress)
            "resolved" -> getString(R.string.resolved)
            else -> status
        }
    }

    // Data class
    data class AdminComplaint(
        val id: String,
        val category: String,
        val title: String,
        val status: String,
        val urgency: String,
        val date: String,
        val studentName: String,
        val room: String
    )
}

