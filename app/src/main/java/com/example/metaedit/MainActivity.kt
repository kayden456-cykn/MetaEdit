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
                    if (mýäôÑ½­¸¤ÉÑÕÉ¹Ñ¡É(¥¡ÕÉÉ¹Ð¡¤¤¹½¹Ñ¥¹Ì¡Ä°¥¹½ÉÍôÑÉÕ¤¤½ÕÐ¹¡¤¤(ô(½ÕÐ¹Ñ½%¹ÑÉÉä ¤(ô(ÉÕ¹=¹U¥Q¡Éì¥¡µäôôÑ½­¸¤ìÍ¡½Ý¸ôÉÌìÁÑÈ¹¹½Ñ¥åÑMÑ¡¹ ¤ôô(ô(ô((ÁÉ¥ÙÑÕ¸¥Ñ¥±½¡¤è%¹Ð¤ì(Ù°Ðô¥ÑQáÐ¡Ñ¡¥Ì¤¹ÁÁ±äì(ÍÑQáÐ¡ÕÉÉ¹Ð¡¤¤¤(¥¹ÁÕÑQåÁô%¹ÁÕÑQåÁ¹QeA}1MM}QaP½È%¹ÁÕÑQåÁ¹QeA}QaQ}1}5U1Q%}1%9(ô(±ÉÑ¥±½¹	Õ¥±È¡Ñ¡¥Ì¤¹ÍÑQ¥Ñ± MÑÉ¥¹¤¤¹ÍÑY¥Ü¡Ð¤(¹ÍÑA½Í¥Ñ¥Ù	ÕÑÑ½¸ ÁÁ±ä¤ì|°|´ø(Ù°¸ôÐ¹ÑáÐ¹Ñ½MÑÉ¥¹ ¤(¥¡¸ôô½É¥¥¹°¡¤¤¤¥ÑÌ¹Éµ½Ù¡¤¤±Í¥ÑÍm¥tô¸(ÁÑÈ¹¹½Ñ¥åÑMÑ¡¹ ¤ìÕÁÑMÑÑÕÌ ¤(ô(¹ÍÑ9ÕÑÉ±	ÕÑÑ½¸ IÙÉÐ¤ì|°|´ø¥ÑÌ¹Éµ½Ù¡¤¤ìÁÑÈ¹¹½Ñ¥åÑMÑ¡¹ ¤ìÕÁÑMÑÑÕÌ ¤ô(¹ÍÑ9Ñ¥Ù	ÕÑÑ½¸ ¹°°¹Õ±°¤¹Í¡½Ü ¤(ô((ÁÉ¥ÙÑÕ¸ÕÁÑMÑÑÕÌ ¤ìÍÑÑÕÌ¹ÑáÐô±¥Ñ½Õ¹ÐÍÑÉ¥¹Ì°í¥ÑÌ¹Í¥éô¥Ñô((½ÙÉÉ¥Õ¸½¹Ñ¥Ù¥ÑåIÍÕ±Ð¡ÉÄè%¹Ð°ÉÌè%¹Ð°¤è%¹Ñ¹Ðü¤ì(Ù°ÕÉ¤ô¤ü¹Ñ(¥¡ÉÌôIMU1Q}=,ñðÕÉ¤ôô¹Õ±°¤ÉÑÕÉ¸(¥¡ÉÄôôÄ¤±½¡ÕÉ¤¤±Í¥¡ÉÄôôÈ¤ÍÙ¡ÕÉ¤¤(ô((ÁÉ¥ÙÑÕ¸±½¡ÕÉ¤èUÉ¤¤ì(ÍÑÑÕÌ¹ÑáÐô1½¥¹¸¸¸(Ñ¡Éì(ÑÉäì(Ù°ô½¹Ñ¹ÑIÍ½±ÙÈ¹½Á¹%¹ÁÕÑMÑÉ´¡ÕÉ¤¤¹ÕÍì¥Ð¹É	åÑÌ ¤ô(¥¡¹Í¥éðÌÈñðÉ¡°À¤ôÁáÄÅ	¹Ñ½%¹Ð ¤¤ìÑ½ÍÐ 9½ÐÙ±¥±½°µµÑÑ¹Ð¡¡Èìµä¹ÉåÁÑ¤¤ìÉÑÕÉ¹Ñ¡Éô(Ù°±¼ôÉ¡°à¤ìÙ°±ÌôÉ¡°ÄÈ¤ìÙ°½ôÉ¡°ÄØ¤(¥¡±¼ðôÀñð±ÌðÀñð±¼¬±Ìø¹Í¥éñð½ðôÀñð½ø¹Í¥é¤ìÑ½ÍÐ U¹ÍÕÁÁ½ÉÑ¡È±å½ÕÐ¤ìÉÑÕÉ¹Ñ¡Éô(Ñôì±¥Ñ=ô±¼ì±¥Ñ½Õ¹Ðô±Ì¼àìÑ=ô½ì¥ÑÌ¹±È ¤(ÉÕ¹=¹U¥Q¡ÉìÍÉ ¹ÍÑQáÐ ¤ìÕÁÑMÑÑÕÌ ¤ìÑ½­¸¬¬ìÍ¡½Ý¸ô%¹ÑÉÉä¡±¥Ñ½Õ¹Ð¤ì¥ÐôìÁÑÈ¹¹½Ñ¥åÑMÑ¡¹ ¤ô(ôÑ ¡èáÁÑ¥½¸¤ìÑ½ÍÐ 1½¥±èí¹µÍÍô¤ô(ô(ô((¼¼¥ÑÍÑÉ¥¹ÌÉÁÁ¹Ñ¼Ñ¡¹½Ñ¡¥±¹Ñ¡¥ÈÑ±¹ÑÉ¥ÌÉÁ½¥¹Ñ°(¼¼Í¼¹¼½Ñ¡ÈÍÑ¥½¸Ì½ÍÑÌÍ¡¥Ð¸(ÁÉ¥ÙÑÕ¸ÍÙ¡ÕÉ¤èUÉ¤¤ì(ÍÑÑÕÌ¹ÑáÐôMÙ¥¹¸¸¸(Ñ¡Éì(ÑÉäì(Ù°ÍÉôÑ(Ù°½ÕÐôÍÉ¹½Áå= ¤(Ù°Ñ¥°ô	åÑÉÉå=ÕÑÁÕÑMÑÉ´ ¤(ÙÈÁ½ÌôÍÉ¹Í¥é(Ù°Áô Ð´Á½ÌÐ¤Ð(ÉÁÐ¡Á¤ìÑ¥°¹ÝÉ¥Ñ À¤ôìÁ½Ì¬ôÁ(½È ¡¤°Ì¤¥¸¥ÑÌ¤ì(Ù°ôÌ¹Ñ½	åÑÉÉä¡¡ÉÍÑÌ¹UQ|à¤(ÝÈ¡½ÕÐ°±¥Ñ=¬¤¨à°¹Í¥é¤(ÝÈ¡½ÕÐ°±¥Ñ=¬¤¨à¬Ð°Á½Ì´Ñ=¤(Ñ¥°¹ÝÉ¥Ñ¡¤ìÁ½Ì¬ô¹Í¥é(ô(½¹Ñ¹ÑIÍ½±ÙÈ¹½Á¹=ÕÑÁÕÑMÑÉ´¡ÕÉ¤°ÝÐ¤¹ÕÍì¥Ð¹ÝÉ¥Ñ¡½ÕÐ¤ì¥Ð¹ÝÉ¥Ñ¡Ñ¥°¹Ñ½	åÑÉÉä ¤¤ô(Ñ½ÍÐ MÙ í¥ÑÌ¹Í¥éô¥ÑÌ¤¤(ÉÕ¹=¹U¥Q¡ÉìÕÁÑMÑÑÕÌ ¤ô(ôÑ ¡èáÁÑ¥½¸¤ìÑ½ÍÐ MÙ¥±èí¹µÍÍô¤ô(ô(ô)ô(ÿÿÿ