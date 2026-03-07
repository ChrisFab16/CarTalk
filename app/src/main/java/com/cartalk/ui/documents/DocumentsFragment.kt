package com.cartalk.ui.documents

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cartalk.CarTalkApplication
import com.cartalk.R
import com.cartalk.data.models.Document
import com.cartalk.data.models.DocumentType
import com.cartalk.data.repository.DocumentRepository
import com.cartalk.databinding.FragmentDocumentsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DocumentsFragment : Fragment() {

    private var _binding: FragmentDocumentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DocumentsViewModel by viewModels {
        val app = requireActivity().application as CarTalkApplication
        DocumentsViewModelFactory(app.documentRepository)
    }

    private lateinit var adapter: DocumentsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDocumentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DocumentsAdapter(
            onDocumentClick = { doc ->
                val action = DocumentsFragmentDirections.actionDocumentsToDetail(doc.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { doc -> viewModel.deleteDocument(doc.id) }
        )

        binding.rvDocuments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DocumentsFragment.adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.documents.collectLatest { docs ->
                adapter.submitList(docs)
                binding.tvEmpty.visibility = if (docs.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class DocumentsViewModel(private val repo: DocumentRepository) : ViewModel() {
    val documents = repo.allDocuments

    fun deleteDocument(id: Long) {
        kotlinx.coroutines.GlobalScope.launch {
            repo.deleteDocument(id)
        }
    }
}

class DocumentsViewModelFactory(private val repo: DocumentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DocumentsViewModel(repo) as T
    }
}
