package com.example.machinero.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.machinero.R
import com.example.machinero.data.NotificationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private val onClick: (NotificationEntity) -> Unit
) : ListAdapter<NotificationEntity, NotificationsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<NotificationEntity>() {
        override fun areItemsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem == newItem
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvBody: TextView = view.findViewById(R.id.tvBody)
        val tvMeta: TextView = view.findViewById(R.id.tvMeta)
    }

    private val timeFmt = SimpleDateFormat("HH:mm", Locale("sr", "RS"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvTitle.text = item.title
        holder.tvBody.text = item.body
        holder.tvTime.text = timeFmt.format(Date(item.receivedAtMillis))

        val meta = buildString {
            item.customer?.takeIf { it.isNotBlank() }?.let { append(it) }
            if (!item.status.isNullOrBlank()) {
                if (isNotEmpty()) append(" • ")
                append("status: ${item.status}")
            }
            if (!item.orderId.isNullOrBlank()) {
                if (isNotEmpty()) append(" • ")
                append("ID: ${item.orderId.takeLast(6).uppercase()}")
            }
        }
        holder.tvMeta.text = meta

        holder.itemView.setOnClickListener { onClick(item) }
    }
}
