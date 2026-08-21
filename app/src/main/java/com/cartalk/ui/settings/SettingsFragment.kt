package com.cartalk.ui.settings

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cartalk.CarTalkApplication
import com.cartalk.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as CarTalkApplication
        val prefs = app.preferencesManager

        refreshUi()

        binding.btnSetApiKey.setOnClickListener {
            if (!prefs.isSecureStorageAvailable) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Secure storage unavailable")
                    .setMessage(
                        "Encrypted storage could not be initialized on this device. " +
                            "CarTalk will not save an API key in plaintext. Try restarting the app, " +
                            "or check that the device Keystore is working."
                    )
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }

            val input = android.widget.EditText(requireContext()).apply {
                hint = "sk-ant-..."
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                // Do not prefill the saved secret
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Claude API Key")
                .setMessage("Enter your Anthropic API key.\nGet one at console.anthropic.com")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val key = input.text?.toString()?.trim() ?: ""
                    if (key.startsWith("sk-ant-") || key.startsWith("sk-")) {
                        try {
                            prefs.setApiKey(key)
                            app.claudeRepository.onApiKeyChanged()
                            refreshUi()
                            Toast.makeText(requireContext(), "API key saved!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(),
                                "Could not save API key securely: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Invalid API key format", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear") { _, _ ->
                    prefs.clearApiKey()
                    app.claudeRepository.onApiKeyChanged()
                    refreshUi()
                    Toast.makeText(requireContext(), "API key cleared", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        binding.switchTts.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTtsEnabled(isChecked)
        }

        binding.switchDeepThinking.setOnCheckedChangeListener { _, isChecked ->
            prefs.setDeepThinkingEnabled(isChecked)
        }

        binding.switchAutoVisual.setOnCheckedChangeListener { _, isChecked ->
            prefs.setAutoVisualEnabled(isChecked)
        }

        binding.btnEditSystemPrompt.setOnClickListener {
            if (!prefs.isSecureStorageAvailable) {
                Toast.makeText(requireContext(), "Secure storage unavailable", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val input = android.widget.EditText(requireContext()).apply {
                hint = "Custom instructions for Claude..."
                minLines = 4
                setText(prefs.getCustomSystemPrompt() ?: "")
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Custom System Prompt")
                .setMessage("Override the default CarTalk assistant instructions")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    prefs.setCustomSystemPrompt(input.text?.toString())
                    Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Reset Default") { _, _ ->
                    prefs.setCustomSystemPrompt(null)
                    Toast.makeText(requireContext(), "Reset to default", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        binding.spinnerModel.apply {
            val models = arrayOf("claude-opus-4-6", "claude-sonnet-4-6", "claude-haiku-4-5")
            val adapter = android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                models
            )
            this.adapter = adapter
            val currentModel = prefs.getModel()
            setSelection(models.indexOfFirst { it == currentModel }.coerceAtLeast(0))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    prefs.setModel(models[pos])
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }

        binding.btnGetApiKey.setOnClickListener {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://console.anthropic.com")
            )
            startActivity(intent)
        }
    }

    private fun refreshUi() {
        val prefs = (requireActivity().application as CarTalkApplication).preferencesManager

        binding.tvApiKeyStatus.text = when {
            !prefs.isSecureStorageAvailable ->
                "Secure storage unavailable — API key cannot be saved"
            prefs.hasApiKey() ->
                "Configured"
            else ->
                "Not configured — required to use CarTalk"
        }

        binding.switchTts.isChecked = prefs.isTtsEnabled()
        binding.switchDeepThinking.isChecked = prefs.isDeepThinkingEnabled()
        binding.switchAutoVisual.isChecked = prefs.isAutoVisualEnabled()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
