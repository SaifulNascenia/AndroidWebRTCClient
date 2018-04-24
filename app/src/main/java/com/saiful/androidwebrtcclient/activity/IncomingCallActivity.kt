package com.saiful.androidwebrtcclient.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast

import com.pubnub.api.Callback
import com.pubnub.api.Pubnub
import com.saiful.androidwebrtcclient.R
import com.saiful.androidwebrtcclient.util.Constants

import org.json.JSONObject

import me.kevingleason.pnwebrtc.PnPeerConnectionClient


class IncomingCallActivity : Activity() {
    private var mSharedPreferences: SharedPreferences? = null
    private var username: String? = null
    private var callUser: String? = null

    private var mPubNub: Pubnub? = null
    private var mCallerID: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        this.mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
        if (!this.mSharedPreferences!!.contains(Constants.USER_NAME)) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        this.username = this.mSharedPreferences!!.getString(Constants.USER_NAME, "")

        val extras = intent.extras
        if (extras == null || !extras.containsKey(Constants.CALL_USER)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Need to pass username to IncomingCallActivity in intent extras (Constants.CALL_USER).",
                    Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        this.callUser = extras.getString(Constants.CALL_USER, "")
        this.mCallerID = findViewById<View>(R.id.caller_id) as TextView
        this.mCallerID!!.text = this.callUser

        this.mPubNub = Pubnub(Constants.PUB_KEY, Constants.SUB_KEY)
        this.mPubNub!!.uuid = this.username
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_incoming_call, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)

    }

    fun acceptCall(view: View) {
        val intent = Intent(this@IncomingCallActivity, VideoChatActivity::class.java)
        intent.putExtra(Constants.USER_NAME, this.username)
        intent.putExtra(Constants.CALL_USER, this.callUser)
        startActivity(intent)
    }

    /**
     * Publish a hangup command if rejecting call.
     * @param view
     */
    fun rejectCall(view: View) {
        val hangupMsg = PnPeerConnectionClient.generateHangupPacket(this.username)
        this.mPubNub!!.publish(this.callUser, hangupMsg, object : Callback() {
            override fun successCallback(channel: String?, message: Any?) {
                val intent = Intent(this@IncomingCallActivity, MainActivity::class.java)
                startActivity(intent)
            }
        })
    }

    override fun onStop() {
        super.onStop()
        if (this.mPubNub != null) {
            this.mPubNub!!.unsubscribeAll()
        }
    }
}
