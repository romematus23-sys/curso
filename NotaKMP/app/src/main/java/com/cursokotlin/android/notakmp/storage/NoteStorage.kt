package com.cursokotlin.android.notakmp.storage

import android.content.Context
import android.content.SharedPreferences
import com.cursokotlin.android.notakmp.model.Note
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NoteStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "notakmp_prefs"
        private const val KEY_NOTES = "notes_list"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAllNotes(): List<Note> {
        val json = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        val type = object : TypeToken<List<Note>>() {}.type
        return try {
            val notes: List<Note> = gson.fromJson(json, type)
            notes.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getNoteById(id: String): Note? {
        return getAllNotes().find { it.id == id }
    }

    fun saveNote(note: Note) {
        val currentNotes = getAllNotes().toMutableList()
        currentNotes.add(note)
        saveAllNotes(currentNotes)
    }

    fun deleteNote(id: String) {
        val currentNotes = getAllNotes().toMutableList()
        currentNotes.removeAll { it.id == id }
        saveAllNotes(currentNotes)
    }

    private fun saveAllNotes(notes: List<Note>) {
        val json = gson.toJson(notes)
        prefs.edit().putString(KEY_NOTES, json).apply()
    }
}