package com.cartalk.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.cartalk.CarTalkApplication
import com.cartalk.R
import com.cartalk.api.VisualType
import com.cartalk.databinding.FragmentChatBinding
import com.cartalk.utils.SpeechRecognizerManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels {
        val app = requireActivity().application as CarTalkApplication
        ChatViewModelFactory(app.claudeRepository, app.documentRepository, app.preferencesManager)
    }

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var speechManager: SpeechRecognizerManager
    private var isListening = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupInput()
        setupSpeech()
        observeState()
        setupMenu()

        if (!viewModel.isConfigured()) {
            Toast.makeText(requireContext(), "Please configure your API key in Settings", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).also { it.stackFromEnd = true }
            adapter = messageAdapter
        }
    }

    private fun setupInput() {
        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                binding.etInput.text?.clear()
            }
        }

        binding.btnMic.setOnClickListener {
            if (isListening) {
                speechManager.stopListening()
                isListening = false
                binding.btnMic.setImageResource(R.drawable.ic_mic)
            } else {
                checkAudioPermissionAndListen()
            }
        }

        binding.btnSaveNote.setOnClickListener {
            showSaveNoteDialog()
        }

        binding.btnSaveIdea.setOnClickListener {
            showCaptureIdeaDialog()
        }

        binding.btnDeepDive.setOnClickListener {
            showDeepDiveDialog()
        }

        binding.btnRecap.setOnClickListener {
            viewModel.generateRecap()
            Toast.makeText(requireContext(), "Generating recap document...", Toast.LENGTH_SHORT).show()
        }

        binding.btnClearChat.setOnClickListener {
            viewModel.clearChat()
        }

        // Visual panel close button
        binding.btnCloseVisual.setOnClickListener {
            viewModel.clearVisual()
        }
    }

    private fun setupSpeech() {
        speechManager = SpeechRecognizerManager(requireContext())
        speechManager.setCallbacks(
            onResult = { text ->
                isListening = false
                binding.btnMic.setImageResource(R.drawable.ic_mic)
                viewModel.sendMessage(text)
            },
            onError = { error ->
                isListening = false
                binding.btnMic.setImageResource(R.drawable.ic_mic)
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            },
            onListening = {
                binding.btnMic.setImageResource(R.drawable.ic_mic_active)
            }
        )
    }

    private fun setupMenu() {
        // Menu handled via toolbar actions if needed
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                // Update messages
                val displayMessages = state.messages.toMutableList()
                if (state.currentStreamText.isNotEmpty()) {
                    displayMessages.add(UiMessage(role = "assistant", content = state.currentStreamText + "▊"))
                }
                messageAdapter.submitList(displayMessages)
                if (displayMessages.isNotEmpty()) {
                    binding.rvMessages.smoothScrollToPosition(displayMessages.size - 1)
                }

                // Loading indicator
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                // Error
                state.error?.let { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }

                // Visual content panel
                if (state.visualContent != null) {
                    binding.visualPanel.visibility = View.VISIBLE
                    binding.tvVisualTitle.text = state.visualContent.title
                    binding.tvVisualDescription.text = state.visualContent.description

                    val typeLabel = when (state.visualContent.type) {
                        VisualType.LANDMARK -> "📍 Landmark"
                        VisualType.PERSON -> "👤 Person"
                        VisualType.MAP -> "🗺️ Location"
                        VisualType.GENERAL_IMAGE -> "🖼️ Visual"
                    }
                    binding.tvVisualType.text = typeLabel

                    // Load image from fixed Unsplash host with URL-encoded query only
                    state.visualContent.searchQuery?.let { query ->
                        val encoded = java.net.URLEncoder.encode(query, Charsets.UTF_8.name())
                        val url = "https://source.unsplash.com/400x300/?$encoded"
                        binding.ivVisual.load(url) {
                            placeholder(R.drawable.ic_image_placeholder)
                            error(R.drawable.ic_image_placeholder)
                        }
                    }
                } else {
                    binding.visualPanel.visibility = View.GONE
                }

                // Deep dive mode
                if (state.isDeepDiveMode) {
                    binding.tvDeepDiveBadge.visibility = View.VISIBLE
                    binding.tvDeepDiveBadge.text = "Deep Dive: ${state.currentTopic}"
                    binding.btnRecap.visibility = View.VISIBLE
                } else {
                    binding.tvDeepDiveBadge.visibility = View.GONE
                    binding.btnRecap.visibility = View.GONE
                }

                // Saved document notification
                state.lastSavedDocId?.let {
                    Toast.makeText(requireContext(), "Document saved! ✓", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkAudioPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO_PERMISSION)
        }
    }

    private fun startListening() {
        isListening = true
        binding.btnMic.setImageResource(R.drawable.ic_mic_active)
        speechManager.startListening()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQ_AUDIO_PERMISSION && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startListening()
        }
    }

    private fun showSaveNoteDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Note title (optional)"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Save Note")
            .setMessage("Save the last exchange as a note?")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val title = input.text?.toString()?.takeIf { it.isNotBlank() }
                viewModel.saveCurrentNote(title)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCaptureIdeaDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Describe your idea..."
            minLines = 3
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Capture Idea")
            .setMessage("What's the idea you want to save?")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val idea = input.text?.toString()?.trim() ?: ""
                if (idea.isNotEmpty()) {
                    viewModel.saveCapturedIdea(idea)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeepDiveDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Enter topic to explore..."
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Deep Dive")
            .setMessage("What topic would you like to explore in depth?")
            .setView(input)
            .setPositiveButton("Start") { _, _ ->
                val topic = input.text?.toString()?.trim() ?: ""
                if (topic.isNotEmpty()) {
                    viewModel.startDeepDive(topic)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechManager.destroy()
        _binding = null
    }

    companion object {
        private const val REQ_AUDIO_PERMISSION = 1001
    }
}
