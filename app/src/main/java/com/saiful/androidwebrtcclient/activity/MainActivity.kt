package com.saiful.androidwebrtcclient.activity

import android.app.ListActivity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import com.pubnub.api.Callback
import com.pubnub.api.Pubnub
import com.pubnub.api.PubnubError
import com.pubnub.api.PubnubException
import com.saiful.androidwebrtcclient.R
import com.saiful.androidwebrtcclient.adapter.HistoryAdapter
import com.saiful.androidwebrtcclient.adt.HistoryItem
import com.saiful.androidwebrtcclient.util.Constants

import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList


class MainActivity : ListActivity() {
    private var mSharedPreferences: SharedPreferences? = null
    private var username: String? = null
    private var stdByChannel: String? = null
    private var mPubNub: Pubnub? = null

    private var mHistoryList: ListView? = null
    private var mHistoryAdapter: HistoryAdapter? = null
    private var mCallNumET: EditText? = null
    private var mUsernameTV: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
        if (!this.mSharedPreferences!!.contains(Constants.USER_NAME)) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        this.username = this.mSharedPreferences!!.getString(Constants.USER_NAME, "")
        this.stdByChannel = this.username!! + Constants.STDBY_SUFFIX

        this.mHistoryList = listView
        this.mCallNumET = findViewById<View>(R.id.call_num) as EditText
        this.mUsernameTV = findViewById<View>(R.id.main_username) as TextView

        this.mUsernameTV!!.text = this.username
        initPubNub()

        this.mHistoryAdapter = HistoryAdapter(this, ArrayList(), this.mPubNub)
        this.mHistoryList!!.adapter = this.mHistoryAdapter
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        when (id) {
            R.id.action_settings -> return true
            R.id.action_sign_out -> {
                signOut()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        if (this.mPubNub != null) {
            this.mPubNub!!.unsubscribeAll()
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (this.mPubNub == null) {
            initPubNub()
        } else {
            subscribeStdBy()
        }
    }

    /**
     * Subscribe to standby channel so that it doesn't interfere with the WebRTC Signaling.
     */
    fun initPubNub() {
        this.mPubNub = Pubnub(Constants.PUB_KEY, Constants.SUB_KEY)
        this.mPubNub!!.uuid = this.username
        subscribeStdBy()
    }

    /**
     * Subscribe to standby channel
     */
    private fun subscribeStdBy() {
        try {
            this.mPubNub!!.subscribe(this.stdByChannel, object : Callback() {
                override fun successCallback(channel: String?, message: Any?) {
                    Log.d("MA-iPN", "MESSAGE: " + message!!.toString())
                    if (message !is JSONObject) return  // Ignore if not JSONObject
                    val jsonMsg = message as JSONObject?
                    try {
                        if (!jsonMsg!!.has(Constants.JSON_CALL_USER)) return      //Ignore Signaling messages.
                        val user = jsonMsg.getString(Constants.JSON_CALL_USER)
                        dispatchIncomingCall(user)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                }

                override fun connectCallback(channel: String?, message: Any?) {
                    Log.d("MA-iPN", "CONNECTED: " + message!!.toString())
                    setUserStatus(Constants.STATUS_AVAILABLE)
                }

                override fun errorCallback(channel: String, error: PubnubError) {
                    Log.d("MA-iPN", "ERROR: " + error.toString())
                }
            })
        } catch (e: PubnubException) {
            Log.d("HERE", "HEREEEE")
            e.printStackTrace()
        }

    }

    /**
     * Take the user to a video screen. USER_NAME is a required field.
     * @param view button that is clicked to trigger toVideo
     */
    fun makeCall(view: View) {
        val callNum = mCallNumET!!.text.toString()
        if (callNum.isEmpty() || callNum == this.username) {
            showToast("Enter a valid user ID to call.")
            return
        }
        dispatchCall(callNum)
    }

    /**TODO: Debate who calls who. Should one be on standby? Or use State API for busy/available
     * Check that user is online. If they are, dispatch the call by publishing to their standby
     * channel. If the publish was successful, then change activities over to the video chat.
     * The called user will then have the option to accept of decline the call. If they accept,
     * they will be brought to the video chat activity as well, to connect video/audio. If
     * they decline, a hangup will be issued, and the VideoChat adapter's onHangup callback will
     * be invoked.
     * @param callNum Number to publish a call to.
     */
    fun dispatchCall(callNum: String) {
        val callNumStdBy = callNum + Constants.STDBY_SUFFIX
        this.mPubNub!!.hereNow(callNumStdBy, object : Callback() {
            override fun successCallback(channel: String?, message: Any?) {
                Log.d("MA-dC", "HERE_NOW: " + " CH - " + callNumStdBy + " " + message!!.toString())
                try {
                    val occupancy = (message as JSONObject).getInt(Constants.JSON_OCCUPANCY)
                    if (occupancy == 0) {
                        showToast("User is not online!")
                        return
                    }
                    val jsonCall = JSONObject()
                    jsonCall.put(Constants.JSON_CALL_USER, username)
                    jsonCall.put(Constants.JSON_CALL_TIME, System.currentTimeMillis())
                    mPubNub!!.publish(callNumStdBy, jsonCall, object : Callback() {
                        override fun successCallback(channel: String?, message: Any?) {
                            Log.d("MA-dC", "SUCCESS: " + message!!.toString())
                            val intent = Intent(this@MainActivity, VideoChatActivity::class.java)
                            intent.putExtra(Constants.USER_NAME, username)
                            intent.putExtra(Constants.CALL_USER, callNum)  // Only accept from this number?
                            startActivity(intent)
                        }
                    })
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        })
    }

    /**
     * Handle incoming calls. TODO: Implement an accept/reject functionality.
     * @param userId
     */
    private fun dispatchIncomingCall(userId: String) {
        showToast("Call from: " + userId)
        val intent = Intent(this@MainActivity, IncomingCallActivity::class.java)
        intent.putExtra(Constants.USER_NAME, username)
        intent.putExtra(Constants.CALL_USER, userId)
        startActivity(intent)
    }

    private fun setUserStatus(status: String) {
        try {
            val state = JSONObject()
            state.put(Constants.JSON_STATUS, status)
            this.mPubNub!!.setState(this.stdByChannel, this.username, state, object : Callback() {
                override fun successCallback(channel: String?, message: Any?) {
                    Log.d("MA-sUS", "State Set: " + message!!.toString())
                }
            })
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    private fun getUserStatus(userId: String) {
        val stdByUser = userId + Constants.STDBY_SUFFIX
        this.mPubNub!!.getState(stdByUser, userId, object : Callback() {
            override fun successCallback(channel: String?, message: Any?) {
                Log.d("MA-gUS", "User Status: " + message!!.toString())
            }
        })
    }

    /**
     * Ensures that toast is run on the UI thread.
     * @param message
     */
    private fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
    }

    /**
     * Log out, remove username from SharedPreferences, unsubscribe from PubNub, and send user back
     * to the LoginActivity
     */
    fun signOut() {
        this.mPubNub!!.unsubscribeAll()
        val edit = this.mSharedPreferences!!.edit()
        edit.remove(Constants.USER_NAME)
        edit.apply()
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("oldUsername", this.username)
        startActivity(intent)
    }
}
