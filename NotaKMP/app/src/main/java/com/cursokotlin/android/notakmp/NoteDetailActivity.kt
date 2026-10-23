package com.cursokotlin.android.notakmp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cursokotlin.android.notakmp.databinding.ActivityNoteDetailBinding
import com.cursokotlin.android.notakmp.storage.NoteStorage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var noteStorage: NoteStorage
    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.title_detail)
        noteStorage = NoteStorage(this)

        noteId = intent.getStringExtra("NOTE_ID")
        val note = noteId?.let { noteStorage.getNoteById(it) }

        if (note == null) {
            Toast.makeText(this, "Nota no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvDetailTitle.text = note.title
        binding.tvDetailBody.text = note.body

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.tvDetailDate.text = dateFormat.format(Date(note.createdAt))

        note.imagePath?.let { fileName ->
            try {
                val inputStream = openFileInput(fileName)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                binding.ivDetailImage.setImageBitmap(bitmap)
                binding.ivDetailImage.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.btnDeleteNote.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_note))
            .setMessage(getString(R.string.confirm_delete))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                noteId?.let { id ->
                    noteStorage.deleteNote(id)
                    Toast.makeText(this, getString(R.string.note_deleted), Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            }
            .show()
    }
}