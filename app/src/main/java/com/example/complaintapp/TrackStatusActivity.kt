package com.example.complaintapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.complaintapp.data.Complaint
import com.example.complaintapp.repository.AuthRepository
import com.example.complaintapp.repository.ComplaintRepository
import com.example.complaintapp.util.Constants
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class TrackStatusActivity : AppCompatActivity() {

    private lateinit var rvActiveComplaints: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var progressBar: ProgressBar

    private val authRepository = AuthRepository()
    private val complaintRepository = ComplaintRepository()
    private var activeComplaints = listOf<Complaint>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_track_status)

        initViews()
        setupClickListeners()
        setupRecyclerView()
        loadActiveComplaints()
    }

    private fun initViews() {
        rvActiveComplaints = findViewById(R.id.rvActiveComplaints)
        emptyState = findViewById(R.id.emptyState)
        etSearch = findViewById(R.id.etSearch)
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Search functionality
        etSearch.addTextChangedListener { text ->
            filterComplaints(text?.toString() ?: "")
        }
    }

    private fun setupRecyclerView() {
        rvActiveComplaints.layoutManager = LinearLayoutManager(this)
        rvActiveComplaints.adapter = TrackAdapter(emptyList()) { complaint ->
            openComplaintDetail(complaint)
        }
    }

    private fun loadActiveComplaints() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = complaintRepository.getActiveComplaints(currentUser.uid)
            
            progressBar.visibility = View.GONE

            result.onSuccess { complaints ->
                activeComplaints = complaints
                (rvActiveComplaints.adapter as TrackAdapter).updateList(complaints)
                
                if (complaints.isEmpty()) {
                    rvActiveComplaints.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                } else {
                    rvActiveComplaints.visibility = View.VISIBLE
                    emptyState.visibility = View.GONE
                }
            }.onFailure { exception ->
                Toast.makeText(this@TrackStatusActivity, "Failed to load complaints: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterComplaints(query: String) {
        val filteredList = if (query.isEmpty()) {
            activeComplaints
        } else {
            activeComplaints.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
            }
        }

        (rvActiveComplaints.adapter as TrackAdapter).updateList(filteredList)

        if (filteredList.isEmpty()) {
            rvActiveComplaints.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvActiveComplaints.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
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
    
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        
        return when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            else -> "Just now"
        }
    }
    
    private fun calculateProgress(status: String): Int {
        return when (status) {
            Constants.STATUS_PENDING -> 25
            Constants.STATUS_IN_PROGRESS -> 75
            Constants.STATUS_RESOLVED -> 100
            Constants.STATUS_CANCELLED -> 0
            else -> 0
        }
    }
    
    private fun getStatusText(status: String): String {
        return when (status) {
            Constants.STATUS_PENDING -> getString(R.string.status_pending)
            Constants.STATUS_IN_PROGRESS -> getString(R.string.in_progress)
            Constants.STATUS_RESOLVED -> getString(R.string.resolved)
            Constants.STATUS_CANCELLED -> getString(R.string.cancelled)
            else -> status
        }
    }

    // Adapter
    inner class TrackAdapter(
        private var complaints: List<Complaint>,
        private val onClick: (Complaint) -> Unit
    ) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryIconFrame: FrameLayout = view.findViewById(R.id.categoryIconFrame)
            val ivCategoryIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvComplaintId: TextView = view.findViewById(R.id.tvComplaintId)
            val tvProgressPercent: TextView = view.findViewById(R.id.tvProgressPercent)
            val progressBar: LinearProgressIndicator = view.findViewById(R.id.progressBar)
            val statusDot: View = view.findViewById(R.id.statusDot)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
            val tvUpdatedTime: TextView = view.findViewById(R.id.tvUpdatedTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_track_complaint, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val complaint = complaints[position]

            holder.tvTitle.text = complaint.title
            holder.tvComplaintId.text = complaint.id
            holder.tvStatus.text = getStatusText(complaint.status)
            holder.tvUpdatedTime.text = "Updated ${getTimeAgo(complaint.updatedAt)}"
            
            val progress = calculateProgress(complaint.status)
            holder.tvProgressPercent.text = "$progress%"
            holder.progressBar.progress = progress

            // Set category icon
            val (iconRes, bgRes) = getCategoryResources(complaint.category)
            holder.ivCategoryIcon.setImageResource(iconRes)
            holder.categoryIconFrame.setBackgroundResource(bgRes)

            // Set status color and dot based on status
            val (statusColor, statusDotBg) = when (complaint.status) {
                Constants.STATUS_PENDING -> Pair(R.color.urgency_medium, R.drawable.bg_status_dot_pending)
                Constants.STATUS_IN_PROGRESS -> Pair(R.color.category_water, R.drawable.bg_status_dot_progress)
                Constants.STATUS_RESOLVED -> Pair(R.color.urgency_low, R.drawable.bg_status_dot_resolved)
                Constants.STATUS_CANCELLED -> Pair(R.color.text_hint, R.drawable.bg_status_dot_pending)
                else -> Pair(R.color.urgency_medium, R.drawable.bg_status_dot_pending)
            }
            holder.tvStatus.setTextColor(ContextCompat.getColor(this@TrackStatusActivity, statusColor))
            holder.statusDot.setBackgroundResource(statusDotBg)

            holder.itemView.setOnClickListener { onClick(complaint) }
        }

        override fun getItemCount() = complaints.size

        fun updateList(newList: List<Complaint>) {
            complaints = newList
            notifyDataSetChanged()
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
    }
}

