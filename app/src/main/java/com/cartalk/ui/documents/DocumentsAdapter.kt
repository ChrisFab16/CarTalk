package com.cartalk.ui.documents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cartalk.R
import com.cartalk.data.models.Document
import com.cartalk.data.models.DocumentType
import java.text.SimpleDateFormat
import java.util.*

class DocumentsAdapter(
    private val onDocumentClick: (Document) -> Unit,
    private val onDeleteClick: (Document) -> Unit
) : ListAdapter<Document, DocumentsAdapter.DocumentViewHolder>(DocumentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position), onDocumentClick, onDeleteClick)
    }

    class DocumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tv_doc_title)
        private val tvType: TextView = view.findViewById(R.id.tv_doc_type)
        private val tvDate: TextView = view.findViewById(R.id.tv_doc_date)
        private val tvPreview: TextView = view.findViewById(R.id.tv_doc_preview)
        private val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)

        fun bind(doc: Document, onClick: (Document) -> Unit, onDelete: (Document) -> Unit) {
            tvTitle.text = doc.title
            tvType.text = when (doc.type) {
                DocumentType.NOTE -> "📝 Note"
                DocumentType.RECAP -> "📋 Recap"
                DocumentType.PLAN -> "🗓️ Plan"
                DocumentType.GENERAL -> "📄 Document"
            }
            tvDate.text = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                .format(Date(doc.updatedAt))
            tvPreview.text = doc.content.take(120).replace('\n', ' ')

            itemView.setOnClickListener { onClick(doc) }
            btnDelete.setOnClickListener { onDelete(doc) }
        }
    }
}

class DocumentDiffCallback : DiffUtil.ItemCallback<Document>() {
    override fun areItemsTheSame(oldItem: Document, newItem: Document) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Document, newItem: Document) = oldItem == newItem
}
