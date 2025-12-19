package com.example.complaintapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.complaintapp.data.Complaint
import com.example.complaintapp.repository.AuthRepository
import com.example.complaintapp.repository.ComplaintRepository
import com.example.complaintapp.util.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()
    private val complaintRepository = ComplaintRepository()
    private lateinit var rvRecentComplaints: RecyclerView
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Check authentication
        if (authRepository.getCurrentUser() == null) {
            redirectToLogin()
            return
        }
        
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        loadUserData()
        loadRecentComplaints()
    }

    private fun initViews() {
        rvRecentComplaints = findViewById(R.id.rvRecentComplaints)
        rvRecentComplaints.layoutManager = LinearLayoutManager(this)
        rvRecentComplaints.adapter = RecentComplaintAdapter(emptyList()) { complaint ->
            openComplaintDetail(complaint)
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val result = authRepository.getCurrentUserData()
            
            result.onSuccess { user ->
                findViewById<TextView>(R.id.tvUserName).text = user.name
            }.onFailure {
                // Keep default text if loading fails
            }
        }
    }

    private fun loadRecentComplaints() {
        val currentUser = authRepository.getCurrentUser() ?: return

        lifecycleScope.launch {
            val result = complaintRepository.getUserComplaints(currentUser.uid)
            
            result.onSuccess { complaints ->
                // Show recent complaints (last 3)
                val recentComplaints = complaints.take(3)
                displayRecentComplaints(recentComplaints)
            }.onFailure {
                // Silently fail - don't show error for empty list
            }
        }
    }

    private fun displayRecentComplaints(complaints: List<Complaint>) {
        val recentComplaints = complaints.take(3)
        (rvRecentComplaints.adapter as RecentComplaintAdapter).updateList(recentComplaints)
    }

    private fun openComplaintDetail(complaint: Complaint) {
        val intent = Intent(this, ComplaintDetailActivity::class.java).apply {
            putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_ID, complaint.id)
            putExtra(ComplaintDetailActivity.EXTRA_CATEGORY, complaint.category)
            putExtra(ComplaintDetailActivity.EXTRA_TITLE, complaint.title)
            putExtra(ComplaintDetailActivity.EXTRA_DESCRIPTION, complaint.description)
            putExtra(ComplaintDetailActivity.EXTRA_STATUS, complaint.status)
            putExtra(ComplaintDetailActivity.EXTRA_URGENCY, complaint.urgency)
            putExtra(ComplaintDetailActivity.EXTRA_LOCATION, complaint.location)
            putExtra(ComplaintDetailActivity.EXTRA_DATE, dateFormat.format(java.util.Date(complaint.createdAt)))
        }
        startActivity(intent)
    }

    // Adapter for Recent Complaints
    inner class RecentComplaintAdapter(
        private var complaints: List<Complaint>,
        private val onClick: (Complaint) -> Unit
    ) : RecyclerView.Adapter<RecentComplaintAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryIconFrame: FrameLayout = view.findViewById(R.id.categoryIconFrame)
            val ivCategoryIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvComplaintId: TextView = view.findViewById(R.id.tvComplaintId)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_complaint, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val complaint = complaints[position]

            holder.tvTitle.text = complaint.title
            holder.tvDate.text = dateFormat.format(java.util.Date(complaint.createdAt))
            holder.tvComplaintId.text = complaint.id

            // Set category
            val (iconRes, categoryName, bgRes) = getCategoryResources(complaint.category)
            holder.ivCategoryIcon.setImageResource(iconRes)
            holder.categoryIconFrame.setBackgroundResource(bgRes)
            holder.tvCategory.text = getString(categoryName)

            // Set status
            val (statusText, statusColor, statusBg) = getStatusResources(complaint.status)
            holder.tvStatus.text = getString(statusText)
            holder.tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, statusColor))
            holder.tvStatus.setBackgroundResource(statusBg)

            holder.itemView.setOnClickListener { onClick(complaint) }
        }

        override fun getItemCount() = complaints.size

        fun updateList(newList: List<Complaint>) {
            complaints = newList
            notifyDataSetChanged()
        }

        private fun getCategoryResources(category: String): Triple<Int, Int, Int> {
            return when (category) {
                Constants.CATEGORY_ELECTRICITY -> Triple(R.drawable.ic_electricity, R.string.electricity, R.drawable.bg_icon_electricity)
                Constants.CATEGORY_WATER -> Triple(R.drawable.ic_water, R.string.water, R.drawable.bg_icon_water)
                Constants.CATEGORY_MAINTENANCE -> Triple(R.drawable.ic_maintenance, R.string.maintenance, R.drawable.bg_icon_maintenance)
                Constants.CATEGORY_CLEANLINESS -> Triple(R.drawable.ic_cleanliness, R.string.cleanliness, R.drawable.bg_icon_cleanliness)
                Constants.CATEGORY_STAFF -> Triple(R.drawable.ic_staff, R.string.staff, R.drawable.bg_icon_staff)
                else -> Triple(R.drawable.ic_electricity, R.string.electricity, R.drawable.bg_icon_electricity)
            }
        }

        private fun getStatusResources(status: String): Triple<Int, Int, Int> {
            return when (status) {
                Constants.STATUS_PENDING -> Triple(R.string.pending, R.color.urgency_medium, R.drawable.bg_urgency_medium)
                Constants.STATUS_IN_PROGRESS -> Triple(R.string.in_progress, R.color.category_water, R.drawable.bg_status_in_progress)
                Constants.STATUS_RESOLVED -> Triple(R.string.resolved, R.color.urgency_low, R.drawable.bg_urgency_low)
                Constants.STATUS_CANCELLED -> Triple(R.string.cancelled, R.color.text_hint, R.drawable.bg_urgency_medium)
                else -> Triple(R.string.pending, R.color.urgency_medium, R.drawable.bg_urgency_medium)
            }
        }
    }

    private fun setupClickListeners() {
        // Submit Complaint Button - opens submit screen without pre-selected category
        findViewById<MaterialButton>(R.id.btnSubmitComplaint).setOnClickListener {
            openSubmitComplaint()
        }

        // Quick Action Cards
        findViewById<MaterialCardView>(R.id.cardViewHistory).setOnClickListener {
            startActivity(Intent(this, ViewHistoryActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardTrackStatus).setOnClickListener {
            startActivity(Intent(this, TrackStatusActivity::class.java))
        }

        // Category Cards - open submit screen with pre-selected category
        findViewById<MaterialCardView>(R.id.cardElectricity).setOnClickListener {
            openSubmitComplaint("electricity")
        }

        findViewById<MaterialCardView>(R.id.cardWater).setOnClickListener {
            openSubmitComplaint("water")
        }

        findViewById<MaterialCardView>(R.id.cardMaintenance).setOnClickListener {
            openSubmitComplaint("maintenance")
        }

        findViewById<MaterialCardView>(R.id.cardCleanliness).setOnClickListener {
            openSubmitComplaint("cleanliness")
        }

        findViewById<MaterialCardView>(R.id.cardStaff).setOnClickListener {
            openSubmitComplaint("staff")
        }

        // View All Recent Complaints
        findViewById<View>(R.id.tvViewAll).setOnClickListener {
            startActivity(Intent(this, ViewHistoryActivity::class.java))
        }

        // Logout Button
        findViewById<FrameLayout>(R.id.btnLogout).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                authRepository.logout()
                redirectToLogin()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun openSubmitComplaint(category: String? = null) {
        val intent = Intent(this, SubmitComplaintActivity::class.java)
        if (category != null) {
            intent.putExtra(SubmitComplaintActivity.EXTRA_CATEGORY, category)
        }
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
