package com.saiful.androidwebrtcclient.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.widget.ArrayAdapter
import android.widget.TextView

import com.saiful.androidwebrtcclient.R
import com.saiful.androidwebrtcclient.adt.ChatMessage

import java.text.SimpleDateFormat
import java.util.Calendar



class ChatAdapter(private val context: Context, private val values: MutableList<ChatMessage>) : ArrayAdapter<ChatMessage>(context, R.layout.chat_message_row_layout, android.R.id.text1, values) {
    private val inflater: LayoutInflater

    init {
        this.inflater = LayoutInflater.from(context)
    }

    internal inner class ViewHolder {
        var sender: TextView? = null
        var message: TextView? = null
        var timeStamp: TextView? = null
        var chatMsg: ChatMessage? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val chatMsg: ChatMessage
        if (position >= values.size) {
            chatMsg = ChatMessage("", "", 0)
        } // Catch Edge Case
        else {
            chatMsg = this.values[position]
        }
        val holder: ViewHolder
        if (convertView == null) {
            holder = ViewHolder()
            convertView = inflater.inflate(R.layout.chat_message_row_layout, parent, false)
            holder.sender = convertView!!.findViewById<View>(R.id.chat_user) as TextView
            holder.message = convertView.findViewById<View>(R.id.chat_message) as TextView
            holder.timeStamp = convertView.findViewById<View>(R.id.chat_timestamp) as TextView
            convertView.tag = holder
            Log.d("Adapter", "Recreating fadeout.")
        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.sender!!.text = chatMsg.sender + ": "
        holder.message!!.text = chatMsg.message
        holder.timeStamp!!.text = formatTimeStamp(chatMsg.timeStamp)
        holder.chatMsg = chatMsg
        setFadeOut3(convertView, chatMsg)
        return convertView
    }

    override fun getCount(): Int {
        return this.values.size
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getItemId(position: Int): Long {
        return if (position >= values.size) {
            -1
        } else values[position].hashCode().toLong()
    }

    fun removeMsg(loc: Int) {
        this.values.removeAt(loc)
        notifyDataSetChanged()
    }

    fun addMessage(chatMsg: ChatMessage) {
        this.values.add(chatMsg)
        notifyDataSetChanged()
    }

    private fun setFadeOut2(view: View, message: ChatMessage) {
        Log.i("AdapterFade", "Caling Fade2")
        view.animate().setDuration(1000).setStartDelay(2000).alpha(0f)
                .withEndAction {
                    if (values.contains(message))
                        values.remove(message)
                    notifyDataSetChanged()
                    view.alpha = 1f
                }
    }

    private fun setFadeOut3(view: View, message: ChatMessage) {
        Log.i("AdapterFade", "Caling Fade3")
        val elapsed = System.currentTimeMillis() - message.timeStamp
        if (elapsed >= FADE_TIMEOUT) {
            if (values.contains(message))
                values.remove(message)
            notifyDataSetChanged()
            return
        }
        view.animate().setStartDelay(FADE_TIMEOUT - elapsed).setDuration(1500).alpha(0f)
                .withEndAction {
                    if (values.contains(message)) {
                        values.remove(message)
                    }
                    notifyDataSetChanged()
                    view.alpha = 1f
                }
    }


    private fun setFadeOut(view: View, message: ChatMessage) {
        val elapsed = System.currentTimeMillis() - message.timeStamp
        if (elapsed >= FADE_TIMEOUT) {
            if (values.contains(message))
                values.remove(message)
            notifyDataSetChanged()
            return
        }

        view.setHasTransientState(true)
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator() //and this
        fadeOut.startOffset = FADE_TIMEOUT - elapsed
        fadeOut.duration = 1000

        val animation = AnimationSet(false)
        animation.addAnimation(fadeOut)
        animation.repeatCount = 1

        view.animation = animation
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                if (values.contains(message)) {
                    values.remove(message)
                }
                notifyDataSetChanged()
                view.alpha = 1f
                view.setHasTransientState(false)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })


    }

    companion object {
        private val FADE_TIMEOUT: Long = 3000

        /**
         * Format the long System.currentTimeMillis() to a better looking timestamp. Uses a calendar
         * object to format with the user's current time zone.
         * @param timeStamp
         * @return
         */
        fun formatTimeStamp(timeStamp: Long): String {
            // Create a DateFormatter object for displaying date in specified format.
            val formatter = SimpleDateFormat("h:mm.ss a")

            // Create a calendar object that will convert the date and time value in milliseconds to date.
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeStamp
            return formatter.format(calendar.time)
        }
    }

}