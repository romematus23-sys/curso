package com.cursokotlin.android.notakmp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cursokotlin.android.notakmp.adapter.NoteAdapter
import com.cursokotlin.android.notakmp.databinding.ActivityMainBinding
import com.cursokotlin.android.notakmp.model.Note
import com.cursokotlin.android.notakmp.storage.NoteStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteStorage: NoteStorage

    private val editorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadNotes()
        }
    }

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadNotes()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteStorage = NoteStorage(this)

        setupRecyclerView()
        setupFab()
        loadNotes()
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(emptyList()) { note ->
            val intent = Intent(this, NoteDetailActivity::class.java)
            intent.putExtra("NOTE_ID", note.id)
            detailLauncher.launch(intent)
        }
        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = noteAdapter
        }
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            val intent = Intent(this, NoteEditorActivity::class.java)
            editorLauncher.launch(intent)
        }
    }

    private fun loadNotes() {
        val notes = noteStorage.getAllNotes()
        noteAdapter.updateNotes(notes)
        binding.tvEmptyState.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        binding.rvNotes.visibility = if (notes.isEmpty()) View.GONE else View.VISIBLE
    }
}