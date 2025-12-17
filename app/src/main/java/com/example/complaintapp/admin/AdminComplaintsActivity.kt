package com.example.complaintapp.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.example.complaintapp.R
import com.example.complaintapp.data.Complaint
import com.example.complaintapp.repository.ComplaintRepository
import com.example.complaintapp.util.Constants
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AdminComplaintsActivity : AppCompatActivity() {

    private lateinit var rvComplaints: RecyclerView
    private lateinit var chipGroup: ChipGroup
    private lateinit var etSearch: EditText
    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar

    private val complaintRepository = ComplaintRepository()
    private var allComplaints = listOf<Complaint>()
    private var filteredComplaints = listOf<Complaint>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_admin_complaints)

        initViews()
        setupClickListeners()
        setupRecyclerView()

        // Check if filter was passed
        val filter = intent.getStringExtra("filter") ?: "all"
        applyInitialFilter(filter)
        loadComplaints()
    }

    private fun initViews() {
        rvComplaints = findViewById(R.id.rvComplaints)
        chipGroup = findViewById(R.id.chipGroup)
        etSearch = findViewById(R.id.etSearch)
        tvTitle = findViewById(R.id.tvTitle)
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
    }
    
    private fun loadComplaints() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val result = complaintRepository.getAllComplaints()
            
            progressBar.visibility = View.GONE
            
            result.onSuccess { complaints ->
                allComplaints = complaints
                filterComplaints()
            }.onFailure { exception ->
                Toast.makeText(this@AdminComplaintsActivity, "Failed to load complaints: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Filter chips
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentFilter = when (checkedIds[0]) {
                    R.id.chipAll -> "all"
                    R.id.chipHighUrgency -> "high"
                    R.id.chipPending -> "pending"
                    R.id.chipInProgress -> "in_progress"
                    R.id.chipResolved -> "resolved"
                    else -> "all"
                }
                filterComplaints()
            }
        }

        // Search
        etSearch.addTextChangedListener { filterComplaints() }
    }

    private fun setupRecyclerView() {
        rvComplaints.layoutManager = LinearLayoutManager(this)
        rvComplaints.adapter = AdminComplaintAdapter(emptyList()) { complaint ->
            openComplaintDetail(complaint)
        }
    }

    private fun applyInitialFilter(filter: String) {
        currentFilter = filter
        when (filter) {
            "all" -> chipGroup.check(R.id.chipAll)
            "high" -> {
                chipGroup.check(R.id.chipHighUrgency)
                tvTitle.text = getString(R.string.high_priority_complaints)
            }
            "pending" -> {
                chipGroup.check(R.id.chipPending)
                tvTitle.text = getString(R.string.pending_review)
            }
            else -> chipGroup.check(R.id.chipAll)
        }
        filterComplaints()
    }

    private fun filterComplaints() {
        val query = etSearch.text?.toString()?.lowercase() ?: ""

        var filtered = when (currentFilter) {
            "all" -> allComplaints
            "high" -> allComplaints.filter { it.urgency == Constants.URGENCY_HIGH }
            "pending" -> allComplaints.filter { it.status == Constants.STATUS_PENDING }
            "in_progress" -> allComplaints.filter { it.status == Constants.STATUS_IN_PROGRESS }
            "resolved" -> allComplaints.filter { it.status == Constants.STATUS_RESOLVED }
            else -> allComplaints
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.lowercase().contains(query) ||
                it.id.lowercase().contains(query) ||
                it.userRoom.lowercase().contains(query) ||
                it.userName.lowercase().contains(query)
            }
        }

        filteredComplaints = filtered
        (rvComplaints.adapter as AdminComplaintAdapter).updateList(filtered)
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

    // Adapter
    inner class AdminComplaintAdapter(
        private var complaints: List<Complaint>,
        private val onClick: (Complaint) -> Unit
    ) : RecyclerView.Adapter<AdminComplaintAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryIconFrame: FrameLayout = view.findViewById(R.id.categoryIconFrame)
            val ivCategoryIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
            val tvComplaintId: TextView = view.findViewById(R.id.tvComplaintId)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvRoom: TextView = view.findViewById(R.id.tvRoom)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvUrgency: TextView = view.findViewById(R.id.tvUrgency)
            val statusDot: View = view.findViewById(R.id.statusDot)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_complaint, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val complaint = complaints[position]

            holder.tvComplaintId.text = complaint.id
            holder.tvTitle.text = complaint.title
            holder.tvRoom.text = complaint.userRoom
            holder.tvDate.text = dateFormat.format(java.util.Date(complaint.createdAt))

            // Category
            val (iconRes, bgRes) = getCategoryResources(complaint.category)
            holder.ivCategoryIcon.setImageResource(iconRes)
            holder.categoryIconFrame.setBackgroundResource(bgRes)

            // Urgency
            holder.tvUrgency.text = complaint.urgency.uppercase()
            val (urgencyColor, urgencyBg) = getUrgencyResources(complaint.urgency)
            holder.tvUrgency.setTextColor(ContextCompat.getColor(this@AdminComplaintsActivity, urgencyColor))
            holder.tvUrgency.setBackgroundResource(urgencyBg)

            // Status
            val (statusText, statusColor, dotBg) = getStatusResources(complaint.status)
            holder.tvStatus.text = getString(statusText)
            holder.tvStatus.setTextColor(ContextCompat.getColor(this@AdminComplaintsActivity, statusColor))
            holder.statusDot.setBackgroundResource(dotBg)

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

        private fun getUrgencyResources(urgency: String): Pair<Int, Int> {
            return when (urgency) {
                "high" -> Pair(R.color.urgency_high, R.drawable.bg_urgency_high)
                "medium" -> Pair(R.color.urgency_medium, R.drawable.bg_urgency_medium)
                "low" -> Pair(R.color.urgency_low, R.drawable.bg_urgency_low)
                else -> Pair(R.color.urgency_medium, R.drawable.bg_urgency_medium)
            }
        }

        private fun getStatusResources(status: String): Triple<Int, Int, Int> {
            return when (status) {
                Constants.STATUS_PENDING -> Triple(R.string.pending, R.color.urgency_medium, R.drawable.bg_status_dot_pending)
                Constants.STATUS_IN_PROGRESS -> Triple(R.string.in_progress, R.color.category_water, R.drawable.bg_status_dot_progress)
                Constants.STATUS_RESOLVED -> Triple(R.string.resolved, R.color.urgency_low, R.drawable.bg_status_dot_resolved)
                else -> Triple(R.string.pending, R.color.urgency_medium, R.drawable.bg_status_dot_pending)
            }
        }
    }
}

