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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator

class TrackStatusActivity : AppCompatActivity() {

    private lateinit var rvActiveComplaints: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var etSearch: EditText

    private val activeComplaints = listOf(
        TrackableComplaint("#CMP-001", "electricity", "Power outage in Room 304", "Under Review", 50, "2h ago"),
        TrackableComplaint("#CMP-003", "water", "Low water pressure", "Assigned to Technician", 75, "1h ago"),
        TrackableComplaint("#CMP-005", "staff", "Rude behavior by security", "Pending Review", 25, "3h ago")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_track_status)

        initViews()
        setupClickListeners()
        setupRecyclerView()
    }

    private fun initViews() {
        rvActiveComplaints = findViewById(R.id.rvActiveComplaints)
        emptyState = findViewById(R.id.emptyState)
        etSearch = findViewById(R.id.etSearch)
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
        rvActiveComplaints.adapter = TrackAdapter(activeComplaints) { complaint ->
            openComplaintDetail(complaint)
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

    private fun openComplaintDetail(complaint: TrackableComplaint) {
        val intent = Intent(this, ComplaintDetailActivity::class.java).apply {
            putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_ID, complaint.id)
            putExtra(ComplaintDetailActivity.EXTRA_CATEGORY, complaint.category)
            putExtra(ComplaintDetailActivity.EXTRA_TITLE, complaint.title)
            putExtra(ComplaintDetailActivity.EXTRA_STATUS, "in_progress")
        }
        startActivity(intent)
    }

    // Data class
    data class TrackableComplaint(
        val id: String,
        val category: String,
        val title: String,
        val status: String,
        val progress: Int,
        val updatedTime: String
    )

    // Adapter
    inner class TrackAdapter(
        private var complaints: List<TrackableComplaint>,
        private val onClick: (TrackableComplaint) -> Unit
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
            holder.tvStatus.text = complaint.status
            holder.tvUpdatedTime.text = "Updated ${complaint.updatedTime}"
            holder.tvProgressPercent.text = "${complaint.progress}%"
            holder.progressBar.progress = complaint.progress

            // Set category icon
            val (iconRes, bgRes) = getCategoryResources(complaint.category)
            holder.ivCategoryIcon.setImageResource(iconRes)
            holder.categoryIconFrame.setBackgroundResource(bgRes)

            // Set status color based on progress
            val statusColor = when {
                complaint.progress < 50 -> R.color.urgency_medium
                complaint.progress < 100 -> R.color.category_water
                else -> R.color.urgency_low
            }
            holder.tvStatus.setTextColor(ContextCompat.getColor(this@TrackStatusActivity, statusColor))
            holder.statusDot.setBackgroundResource(
                if (complaint.progress < 50) R.drawable.bg_status_dot_pending
                else R.drawable.bg_status_dot_progress
            )

            holder.itemView.setOnClickListener { onClick(complaint) }
        }

        override fun getItemCount() = complaints.size

        fun updateList(newList: List<TrackableComplaint>) {
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

