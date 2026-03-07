package com.cartalk.ui.documents

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cartalk.CarTalkApplication
import com.cartalk.data.models.Document
import com.cartalk.databinding.FragmentDocumentDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class DocumentDetailFragment : Fragment() {

    private var _binding: FragmentDocumentDetailBinding? = null
    private val binding get() = _binding!!

    private val args: DocumentDetailFragmentArgs by navArgs()
    private var document: Document? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDocumentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadDocument()
        setupButtons()
    }

    private fun loadDocument() {
        val app = requireActivity().application as CarTalkApplication
        viewLifecycleOwner.lifecycleScope.launch {
            val doc = app.documentRepository.getDocumentById(args.documentId)
            document = doc
            if (doc != null) {
                binding.tvTitle.text = doc.title
                binding.tvContent.text = doc.content
                doc.topic?.let { topic ->
                    binding.tvTopic.visibility = View.VISIBLE
                    binding.tvTopic.text = "Topic: $topic"
                }
            } else {
                Toast.makeText(requireContext(), "Document not found", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun setupButtons() {
        val app = requireActivity().application as CarTalkApplication

        binding.btnReadAloud.setOnClickListener {
            document?.let { doc ->
                app.ttsManager.speak(doc.content)
                Toast.makeText(requireContext(), "Reading aloud...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShare.setOnClickListener {
            document?.let { doc ->
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, doc.title)
                    putExtra(android.content.Intent.EXTRA_TEXT, "${doc.title}\n\n${doc.content}")
                }
                startActivity(android.content.Intent.createChooser(intent, "Share Document"))
            }
        }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete \"${document?.title}\"?")
                .setPositiveButton("Delete") { _, _ ->
                    document?.let { doc ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            app.documentRepository.deleteDocument(doc.id)
                            findNavController().popBackStack()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnAppend.setOnClickListener {
            showAppendDialog()
        }
    }

    private fun showAppendDialog() {
        val app = requireActivity().application as CarTalkApplication
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Add more notes..."
            minLines = 3
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Append to Document")
            .setView(input)
            .setPositiveButton("Append") { _, _ ->
                val addition = input.text?.toString()?.trim() ?: ""
                if (addition.isNotEmpty()) {
                    document?.let { doc ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            app.documentRepository.appendToDocument(doc.id, addition)
                            loadDocument()
                            Toast.makeText(requireContext(), "Appended!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
