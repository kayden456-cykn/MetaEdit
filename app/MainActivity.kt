package com.example.metaedit

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private var data: ByteArray? = null
    private var litOff = 0
    private var litCount = 0
    private var dataOff = 0
    private val edits = HashMap<Int, String>()
    private var shown = IntArray(0)
    private var token = 0
    private lateinit var status: TextView
    private lateinit var search: EditText
    private lateinit var adapter: BaseAdapter

    private fun rd(b: ByteArray, o: Int) = (b[o].toInt() and 255) or
        ((b[o + 1].toInt() and 255) shl 8) or
        ((b[o + 2].toInt() and 255) shl 16) or
        ((b[o + 3].toInt() and 255) shl 24)

    private fun wr(b: ByteArray, o: Int, v: Int) {
        b[o] = v.toByte(); b[o + 1] = (v shr 8).toByte()
        b[o + 2] = (v shr 16).toByte(); b[o + 3] = (v shr 24).toByte()
    }

    private fun original(i: Int): String {
        val d = data!!
        val len = rd(d, litOff + i * 8)
        val idx = rd(d, litOff + i * 8 + 4)
        return String(d, dataOff + idx, len, Charsets.UTF_8)
    }

    private fun current(i: Int) = edits[i] ?: original(i)

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 48, 16, 16) }
        val row = LinearLayout(this)
        val open = Button(this).apply { text = "Open"; setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }, 1) } }
        val save = Button(this).apply { text = "Save as global-metadata.dat"; setOnClickListener {
            if (data == null) toast("Open a file first")
            else startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, "global-metadata.dat") }, 2) } }
        row.addView(open, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(save, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f))
        status = TextView(this).apply { text = "Open a global-metadata.dat to begin." }
        search = EditText(this).apply { hint = "Search strings"; setSingleLine() }
        val list = ListView(this)
        adapter = object : BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(p: Int) = shown[p]
            override fun getItemId(p: Int) = shown[p].toLong()
            override fun getView(p: Int, v: View?, parent: ViewGroup): View {
                val t = (v as? TextView) ?: TextView(this@MainActivity).apply { setPadding(8, 14, 8, 14); maxLines = 3 }
                val i = shown[p]
                val txt = current(i)
                t.text = (if (edits.containsKey(i)) "* " else "") + "#$i  " + txt.take(200)
                return t
            }
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, p, _ -> editDialog(shown[p]) }
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(e: Editable?) = refilter()
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })
        root.addView(row); root.addView(status); root.addView(search)
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun toast(m: String) = runOnUiThread { Toast.makeText(this, m, Toast.LENGTH_LONG).show() }

    private fun refilter() {
        val q = search.text.toString()
        val my = ++token
        thread {
            val res = if (q.isEmpty()) IntArray(litCount) { it } else {
                val out = ArrayList<Int>()
                for (i in 0 until litCount) {
                    if (my != token) return@thread
                    if (current(i).contains(q, ignoreCase = true)) out.add(i)
                }
                out.toIntArray()
            }
            runOnUiThread { if (my == token) { shown = res; adapter.notifyDataSetChanged() } }
        }
    }

    private fun editDialog(i: Int) {
        val et = EditText(this).apply {
            setText(current(i))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        AlertDialog.Builder(this).setTitle("String #$i").setView(et)
            .setPositiveButton("Apply") { _, _ ->
                val n = et.text.toString()
                if (n == original(i)) edits.remove(i) else edits[i] = n
                adapter.notifyDataSetChanged(); updateStatus()
            }
            .setNeutralButton("Revert") { _, _ -> edits.remove(i); adapter.notifyDataSetChanged(); updateStatus() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun updateStatus() { status.text = "$litCount strings, ${edits.size} edited" }

    override fun onActivityResult(req: Int, res: Int, i: Intent?) {
        val uri = i?.data
        if (res != RESULT_OK || uri == null) return
        if (req == 1) load(uri) else if (req == 2) save(uri)
    }

    private fun load(uri: Uri) {
        status.text = "Loading..."
        thread {
            try {
                val b = contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                if (b.size < 32 || rd(b, 0) != 0xFAB11BAF.toInt()) { toast("Not a valid global-metadata.dat (bad header; may be encrypted)"); return@thread }
                val lo = rd(b, 8); val ls = rd(b, 12); val dof = rd(b, 16)
                if (lo <= 0 || ls < 0 || lo + ls > b.size || dof <= 0 || dof > b.size) { toast("Unsupported header layout"); return@thread }
                data = b; litOff = lo; litCount = ls / 8; dataOff = dof; edits.clear()
                runOnUiThread { search.setText(""); updateStatus(); token++; shown = IntArray(litCount) { it }; adapter.notifyDataSetChanged() }
            } catch (e: Exception) { toast("Load failed: ${e.message}") }
        }
    }

    // Edited strings are appended to the end of the file and their table entries repointed,
    // so no other section's offsets shift.
    private fun save(uri: Uri) {
        status.text = "Saving..."
        thread {
            try {
                val src = data!!
                val out = src.copyOf()
                val tail = ByteArrayOutputStream()
                var pos = src.size
                val pad = (4 - pos % 4) % 4
                repeat(pad) { tail.write(0) }; pos += pad
                for ((i, s) in edits) {
                    val b = s.toByteArray(Charsets.UTF_8)
                    wr(out, litOff + i * 8, b.size)
                    wr(out, litOff + i * 8 + 4, pos - dataOff)
                    tail.write(b); pos += b.size
                }
                contentResolver.openOutputStream(uri, "wt")!!.use { it.write(out); it.write(tail.toByteArray()) }
                toast("Saved (${edits.size} edits)")
                runOnUiThread { updateStatus() }
            } catch (e: Exception) { toast("Save failed: ${e.message}") }
        }
    }
}
