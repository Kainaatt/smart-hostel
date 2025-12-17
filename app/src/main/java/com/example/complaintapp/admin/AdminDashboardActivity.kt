package com.example.complaintapp.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.complaintapp.LoginActivity
import com.example.complaintapp.R
import com.example.complaintapp.data.Complaint
import com.example.complaintapp.repository.AuthRepository
import com.example.complaintapp.repository.ComplaintRepository
import com.example.complaintapp.util.Constants
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AdminDashboardActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()
    private val complaintRepository = ComplaintRepository()
    private var allComplaints = listOf<Complaint>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_admin_dashboard)

        setupClickListeners()
        loadComplaints()
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
                authRepository.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun loadComplaints() {
        lifecycleScope.launch {
            val result = complaintRepository.getAllComplaints()
            
            result.onSuccess { complaints ->
                allComplaints = complaints
                updateStats()
                updateCategoryStats()
                loadHighPriorityComplaints()
            }.onFailure { exception ->
                Toast.makeText(this@AdminDashboardActivity, "Failed to load complaints: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStats() {
        findViewById<TextView>(R.id.tvTotalComplaints).text = allComplaints.size.toString()
        findViewById<TextView>(R.id.tvPendingCount).text = allComplaints.count { it.status == Constants.STATUS_PENDING }.toString()
        findViewById<TextView>(R.id.tvHighUrgencyCount).text = allComplaints.count { it.urgency == Constants.URGENCY_HIGH }.toString()
    }

    private fun updateCategoryStats() {
        val categoryStats = mapOf(
            Constants.CATEGORY_ELECTRICITY to allComplaints.count { it.category == Constants.CATEGORY_ELECTRICITY },
            Constants.CATEGORY_WATER to allComplaints.count { it.category == Constants.CATEGORY_WATER },
            Constants.CATEGORY_MAINTENANCE to allComplaints.count { it.category == Constants.CATEGORY_MAINTENANCE },
            Constants.CATEGORY_CLEANLINESS to allComplaints.count { it.category == Constants.CATEGORY_CLEANLINESS },
            Constants.CATEGORY_STAFF to allComplaints.count { it.category == Constants.CATEGORY_STAFF }
        )

        // Update each category row
        updateCategoryRow(R.id.categoryElectricity, R.drawable.ic_electricity, "Electricity", categoryStats[Constants.CATEGORY_ELECTRICITY] ?: 0)
        updateCategoryRow(R.id.categoryWater, R.drawable.ic_water, "Water & Sanitation", categoryStats[Constants.CATEGORY_WATER] ?: 0)
        updateCategoryRow(R.id.categoryMaintenance, R.drawable.ic_maintenance, "Maintenance", categoryStats[Constants.CATEGORY_MAINTENANCE] ?: 0)
        updateCategoryRow(R.id.categoryCleanliness, R.drawable.ic_cleanliness, "Cleanliness", categoryStats[Constants.CATEGORY_CLEANLINESS] ?: 0)
        updateCategoryRow(R.id.categoryStaff, R.drawable.ic_staff, "Staff Behavior", categoryStats[Constants.CATEGORY_STAFF] ?: 0)
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

        val highPriority = allComplaints.filter { it.urgency == Constants.URGENCY_HIGH }.take(3)

        highPriority.forEach { complaint ->
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_admin_complaint, container, false)

            itemView.findViewById<TextView>(R.id.tvComplaintId).text = complaint.id
            itemView.findViewById<TextView>(R.id.tvTitle).text = complaint.title
            itemView.findViewById<TextView>(R.id.tvRoom).text = complaint.userRoom
            itemView.findViewById<TextView>(R.id.tvDate).text = dateFormat.format(java.util.Date(complaint.createdAt))

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

    private fun openComplaintDetail(complaint: Complaint) {
        startActivity(Intent(this, AdminComplaintDetailActivity::class.java).apply {
            putExtra("complaint_id", complaint.id)
            putExtra("category", complaint.category)
            putExtra("title", complaint.title)
            putExtra("description", complaint.description)
            putExtra("status", complaint.status)
            putExtra("urgency", complaint.urgency)
            putExtra("location", complaint.location)
            putExtra("user_name", complaint.userName)
            putExtra("user_room", complaint.userRoom)
            putExtra("admin_notes", complaint.adminNotes)
            putExtra("created_at", complaint.createdAt)
            putExtra("updated_at", complaint.updatedAt)
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
            Constants.STATUS_PENDING -> getString(R.string.pending)
            Constants.STATUS_IN_PROGRESS -> getString(R.string.in_progress)
            Constants.STATUS_RESOLVED -> getString(R.string.resolved)
            else -> status
        }
    }
}

