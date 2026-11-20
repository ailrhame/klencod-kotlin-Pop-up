package com.pop.fox

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private var directLabel: TextView? = null
    private var intentLabel: TextView? = null
    private var copyDirectBtn: Button? = null
    private var copyIntentBtn: Button? = null
    private var directLink: String = ""
    private var intentLink: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUI()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun buildUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "Pop-up"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        })

        val urlInput = EditText(this).apply {
            hint = "URL الهدف"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        root.addView(urlInput)

        val countInput = EditText(this).apply {
            hint = "عدد النوافذ المنبثقة"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        root.addView(countInput)

        val msgInput = EditText(this).apply {
            hint = "الرسالة التي تظهر في النافذة"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
        }
        root.addView(msgInput)

        val genBtn = Button(this).apply { text = "تأكيد" }
        root.addView(genBtn)

        root.addView(TextView(this).apply {
            text = "رابط قابل للنقر (شاركه كـ <a href>):"
            textSize = 12f
            setPadding(0, 32, 0, 4)
        })

        directLabel = TextView(this).apply {
            textSize = 12f
            setTextIsSelectable(true)
        }
        root.addView(directLabel)

        copyDirectBtn = Button(this).apply {
            text = "نسخ رابط <a href>"
            visibility = View.GONE
        }
        root.addView(copyDirectBtn)

        root.addView(TextView(this).apply {
            text = "رابط للصق في شريط عناوين Chrome:"
            textSize = 12f
            setPadding(0, 32, 0, 4)
        })

        intentLabel = TextView(this).apply {
            textSize = 12f
            setTextIsSelectable(true)
        }
        root.addView(intentLabel)

        copyIntentBtn = Button(this).apply {
            text = "نسخ رابط intent://"
            visibility = View.GONE
        }
        root.addView(copyIntentBtn)

        genBtn.setOnClickListener {
            val url = urlInput.text.toString().trim()
            val count = countInput.text.toString().trim().toIntOrNull() ?: 0
            val msg = msgInput.text.toString()
            if (count < 1) {
                Toast.makeText(this, "أدخل عدداً صحيحاً موجباً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val u64 = encodeB64(url)
            val m64 = encodeB64(msg)
            val query = "open?c=$count&m=$m64&u=$u64"

            directLink = "popgen://$query"
            intentLink = "intent://$query#Intent;scheme=popgen;package=com.pop.fox;end"

            directLabel?.text = directLink
            intentLabel?.text = intentLink
            copyDirectBtn?.visibility = View.VISIBLE
            copyIntentBtn?.visibility = View.VISIBLE
        }

        copyDirectBtn?.setOnClickListener { copy(directLink, "تم نسخ <a href>") }
        copyIntentBtn?.setOnClickListener { copy(intentLink, "تم نسخ intent://") }
    }

    private fun copy(text: String, msg: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("link", text))
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun encodeB64(s: String): String =
        Base64.encodeToString(
            s.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

    private fun decodeB64(s: String): String = try {
        String(
            Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
            Charsets.UTF_8
        )
    } catch (_: Exception) {
        ""
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val data = intent.data ?: return
        if (data.scheme != "popgen" || data.host != "open") return

        val count = data.getQueryParameter("c")?.toIntOrNull() ?: 0
        val msg = decodeB64(data.getQueryParameter("m") ?: "")
        val url = decodeB64(data.getQueryParameter("u") ?: "")
        if (count < 1) return

        window.decorView.post { showPopups(count, msg, url) }
    }

    private fun showPopups(total: Int, msg: String, targetUrl: String) {
        var shown = 0
        fun next() {
            if (shown >= total) {
                if (targetUrl.isNotEmpty()) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                    } catch (_: Exception) {
                        Toast.makeText(this, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
                    }
                }
                return
            }
            shown++
            AlertDialog.Builder(this)
                .setTitle("$shown / $total")
                .setMessage(if (msg.isEmpty()) " " else msg)
                .setCancelable(false)
                .setPositiveButton("OK") { _, _ -> next() }
                .show()
        }
        next()
    }
}