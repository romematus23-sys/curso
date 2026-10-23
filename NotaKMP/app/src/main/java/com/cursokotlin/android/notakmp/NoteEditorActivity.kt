
package com.cursokotlin.android.notakmp

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.cursokotlin.android.notakmp.databinding.ActivityNoteEditorBinding
import com.cursokotlin.android.notakmp.model.Note
import com.cursokotlin.android.notakmp.storage.NoteStorage
import android.media.MediaPlayer

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var noteStorage: NoteStorage
    private var selectedImageUri: Uri? = null

    private var mediaPlayer: MediaPlayer? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivSelectedImage.setImageURI(it)
            binding.ivSelectedImage.visibility = android.view.View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.title_editor)
        noteStorage = NoteStorage(this)

        // Restaurar estado si hubo rotación
        savedInstanceState?.let {
            val uriString = it.getString("SELECTED_IMAGE_URI")
            if (uriString != null) {
                selectedImageUri = Uri.parse(uriString)
                binding.ivSelectedImage.setImageURI(selectedImageUri)
                binding.ivSelectedImage.visibility =
                    android.view.View.VISIBLE
            }
        }

        binding.btnAttachImage.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnSaveNote.setOnClickListener {
            saveNote()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedImageUri?.let {
            outState.putString("SELECTED_IMAGE_URI", it.toString())
        }
    }

    private fun saveNote() {
        val titleText = binding.etTitle.text.toString().trim()
        val bodyText = binding.etBody.text.toString().trim()

        if (titleText.isEmpty()) {
            binding.tilTitle.error = getString(R.string.error_empty_title)
            return
        }
        binding.tilTitle.error = null

        if (bodyText.isEmpty()) {
            binding.tilBody.error = getString(R.string.error_empty_body)
            return
        }
        binding.tilBody.error = null

        val imagePath = selectedImageUri?.let { uri ->
            copyImageToInternalStorage(uri)
        }

        val note = Note(
            title = titleText,
            body = bodyText,
            imagePath = imagePath
        )

        noteStorage.saveNote(note)

        // Reproducir sonido de confirmación
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.confirmation)
            mediaPlayer?.setOnCompletionListener { mp ->
                mp.release()
                if (mediaPlayer === mp) {
                    mediaPlayer = null
                }
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            // Si no hay archivo de audio, continuar sin error
        }

        Toast.makeText(
            this,
            getString(R.string.note_saved),
            Toast.LENGTH_SHORT
        ).show()

        setResult(RESULT_OK)
        finish()
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
                ?: return null

            val fileName = "note_img_${System.currentTimeMillis()}.jpg"
            val outputStream = openFileOutput(fileName, MODE_PRIVATE)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            fileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Liberar los recursos del reproductor al destruir la actividad
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
