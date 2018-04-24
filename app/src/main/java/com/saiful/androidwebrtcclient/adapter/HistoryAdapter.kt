package com.saiful.androidwebrtcclient.adapter

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView

import com.pubnub.api.Callback
import com.pubnub.api.Pubnub
import com.saiful.androidwebrtcclient.R
import com.saiful.androidwebrtcclient.activity.MainActivity
import com.saiful.androidwebrtcclient.adt.ChatUser
import com.saiful.androidwebrtcclient.adt.HistoryItem
import com.saiful.androidwebrtcclient.util.Constants

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.HashMap
import java.util.LinkedList



class HistoryAdapter(private val context: Context, private var values: MutableList<HistoryItem>?, private val mPubNub: Pubnub) : ArrayAdapter<HistoryItem>(context, R.layout.history_row_layout, android.R.id.text1, values) {
    private val inflater: LayoutInflater
    private val users: MutableMap<String, ChatUser>


    init {
        this.inflater = LayoutInflater.from(context)
        this.users = HashMap()
        updateHistory()
    }

    internal inner class ViewHolder {
        var user: TextView? = null
        var status: TextView? = null
        var time: TextView? = null
        var callBtn: ImageButton? = null
        var histItem: HistoryItem? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val hItem = this.values!![position]
        val holder: ViewHolder
        if (convertView == null) {
            holder = ViewHolder()
            convertView = inflater.inflate(R.layout.history_row_layout, parent, false)
            holder.user = convertView!!.findViewById<View>(R.id.history_name) as TextView
            holder.status = convertView.findViewById<View>(R.id.history_status) as TextView
            holder.time = convertView.findViewById<View>(R.id.history_time) as TextView
            holder.callBtn = convertView.findViewById<View>(R.id.history_call) as ImageButton
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.user!!.text = hItem.user.userId
        holder.time!!.text = formatTimeStamp(hItem.timeStamp!!)
        holder.status!!.text = hItem.user.status
        if (hItem.user.status == Constants.STATUS_OFFLINE)
            getUserStatus(hItem.user, holder.status)
        holder.callBtn!!.setOnClickListener { (context as MainActivity).dispatchCall(hItem.user.userId) }
        holder.histItem = hItem
        return convertView
    }

    override fun getCount(): Int {
        return this.values!!.size
    }

    fun removeButton(loc: Int) {
        this.values!!.removeAt(loc)
        notifyDataSetChanged()
    }

    private fun getUserStatus(user: ChatUser, statusView: TextView?) {
        val stdByUser = user.userId + Constants.STDBY_SUFFIX
        this.mPubNub.getState(stdByUser, user.userId, object : Callback() {
            override fun successCallback(channel: String?, message: Any?) {
                val jsonMsg = message as JSONObject?
                try {
                    if (!jsonMsg!!.has(Constants.JSON_STATUS)) return
                    val status = jsonMsg.getString(Constants.JSON_STATUS)
                    user.status = status
                    (getContext() as Activity).runOnUiThread { statusView!!.text = status }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        })
    }

    fun updateHistory() {
        val rtcHistory = LinkedList<HistoryItem>()
        val usrStdBy = this.mPubNub.uuid + Constants.STDBY_SUFFIX
        this.mPubNub.history(usrStdBy, 25, object : Callback() {
            override fun successCallback(channel: String?, message: Any?) {
                Log.d("HA-uH", "HISTORY: " + message!!.toString())
                try {
                    val historyArray = (message as JSONArray).getJSONArray(0)
                    for (i in 0 until historyArray.length()) {
                        val historyJson = historyArray.getJSONObject(i)
                        val userName = historyJson.getString(Constants.JSON_CALL_USER)
                        val timeStamp = historyJson.getLong(Constants.JSON_CALL_TIME)
                        var cUser = ChatUser(userName)
                        if (users.containsKey(userName)) {
                            cUser = users[userName]
                        } else {
                            users.put(userName, cUser)
                        }
                        rtcHistory.add(0, HistoryItem(cUser, timeStamp))
                    }
                    values = rtcHistory
                    updateAdapter()
                } catch (e: JSONException) {
                    // e.printStackTrace();
                }

            }
        })
    }

    private fun updateAdapter() {
        (context as Activity).runOnUiThread { notifyDataSetChanged() }
    }

    companion object {

        /**
         * Format the long System.currentTimeMillis() to a better looking timestamp. Uses a calendar
         * object to format with the user's current time zone.
         * @param timeStamp
         * @return
         */
        fun formatTimeStamp(timeStamp: Long): String {
            // Create a DateFormatter object for displaying date in specified format.
            val formatter = SimpleDateFormat("MMM d, h:mm a")

            // Create a calendar object that will convert the date and time value in milliseconds to date.
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeStamp
            return formatter.format(calendar.time)
        }
    }
}

