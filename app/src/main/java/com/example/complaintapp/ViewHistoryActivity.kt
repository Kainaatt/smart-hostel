package com.example.complaintapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ViewHistoryActivity : AppCompatActivity() {

    private lateinit var rvComplaints: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var chipGroup: ChipGroup
    
    private val allComplaints = listOf(
        Complaint("#CMP-001", "electricity", "Power outage in Room 304", "pending", "high", "Dec 16, 2024"),
        Complaint("#CMP-002", "cleanliness", "Bathroom cleaning required", "resolved", "low", "Dec 15, 2024"),
        Complaint("#CMP-003", "water", "Low water pressure", "in_progress", "medium", "Dec 14, 2024"),
        Complaint("#CMP-004", "maintenance", "Broken window lock", "resolved", "low", "Dec 13, 2024"),
        Complaint("#CMP-005", "staff", "Rude behavior by security", "pending", "high", "Dec 12, 2024")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_view_history)

        initViews()
        setupClickListeners()
        setupRecyclerView()
        updateStats()
    }

    private fun initViews() {
        rvComplaints = findViewById(R.id.rvComplaints)
        emptyState = findViewById(R.id.emptyState)
        chipGroup = findViewById(R.id.chipGroup)
    }

    private fun setupClickListeners() {
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Filter chips
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds[0]
                filterComplaints(chipId)
            }
        }
    }

    private fun setupRecyclerView() {
        rvComplaints.layoutManager = LinearLayoutManager(this)
        rvComplaints.adapter = ComplaintAdapter(allComplaints) { complaint ->
            openComplaintDetail(complaint)
        }
    }

    private fun filterComplaints(chipId: Int) {
        val filteredList = when (chipId) {
            R.id.chipAll -> allComplaints
            R.id.chipPending -> allComplaints.filter { it.status == "pending" }
            R.id.chipInProgress -> allComplaints.filter { it.status == "in_progress" }
            R.id.chipResolved -> allComplaints.filter { it.status == "resolved" }
            else -> allComplaints
        }

        (rvComplaints.adapter as ComplaintAdapter).updateList(filteredList)
        
        if (filteredList.isEmpty()) {
            rvComplaints.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvComplaints.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun updateStats() {
        findViewById<TextView>(R.id.tvTotalCount).text = allComplaints.size.toString()
        findViewById<TextView>(R.id.tvPendingCount).text = allComplaints.count { it.status == "pending" }.toString()
        findViewById<TextView>(R.id.tvResolvedCount).text = allComplaints.count { it.status == "resolved" }.toString()
    }

    private fun openComplaintDetail(complaint: Complaint) {
        val intent = Intent(this, ComplaintDetailActivity::class.java).apply {
            putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_ID, complaint.id)
            putExtra(ComplaintDetailActivity.EXTRA_CATEGORY, complaint.category)
            putExtra(ComplaintDetailActivity.EXTRA_TITLE, complaint.title)
            putExtra(ComplaintDetailActivity.EXTRA_STATUS, complaint.status)
            putExtra(ComplaintDetailActivity.EXTRA_URGENCY, complaint.urgency)
            putExtra(ComplaintDetailActivity.EXTRA_DATE, complaint.date)
        }
        startActivity(intent)
    }

    // Data class
    data class Complaint(
        val id: String,
        val category: String,
        val title: String,
        val status: String,
        val urgency: String,
        val date: String
    )

    // Adapter
    inner class ComplaintAdapter(
        private var complaints: List<Complaint>,
        private val onClick: (Complaint) -> Unit
    ) : RecyclerView.Adapter<ComplaintAdapter.ViewHolder>() {

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
            holder.tvDate.text = complaint.date
            holder.tvComplaintId.text = complaint.id

            // Set category
            val (iconRes, categoryName, bgRes) = getCategoryResources(complaint.category)
            holder.ivCategoryIcon.setImageResource(iconRes)
            holder.categoryIconFrame.setBackgroundResource(bgRes)
            holder.tvCategory.text = getString(categoryName)

            // Set status
            val (statusText, statusColor, statusBg) = getStatusResources(complaint.status)
            holder.tvStatus.text = getString(statusText)
            holder.tvStatus.setTextColor(ContextCompat.getColor(this@ViewHistoryActivity, statusColor))
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
                "electricity" -> Triple(R.drawable.ic_electricity, R.string.electricity, R.drawable.bg_icon_electricity)
                "water" -> Triple(R.drawable.ic_water, R.string.water, R.drawable.bg_icon_water)
                "maintenance" -> Triple(R.drawable.ic_maintenance, R.string.maintenance, R.drawable.bg_icon_maintenance)
                "cleanliness" -> Triple(R.drawable.ic_cleanliness, R.string.cleanliness, R.drawable.bg_icon_cleanliness)
                "staff" -> Triple(R.drawable.ic_staff, R.string.staff, R.drawable.bg_icon_staff)
                else -> Triple(R.drawable.ic_electricity, R.string.electricity, R.drawable.bg_icon_electricity)
            }
        }

        private fun getStatusResources(status: String): Triple<Int, Int, Int> {
            return when (status) {
                "pending" -> Triple(R.string.pending, R.color.urgency_medium, R.drawable.bg_urgency_medium)
                "in_progress" -> Triple(R.string.in_progress, R.color.category_water, R.drawable.bg_status_in_progress)
                "resolved" -> Triple(R.string.resolved, R.color.urgency_low, R.drawable.bg_urgency_low)
                else -> Triple(R.string.pending, R.color.urgency_medium, R.drawable.bg_urgency_medium)
            }
        }
    }
}

