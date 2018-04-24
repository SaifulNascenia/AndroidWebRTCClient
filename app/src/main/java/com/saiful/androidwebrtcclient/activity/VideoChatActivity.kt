package com.saiful.androidwebrtcclient.activity

import android.app.ListActivity
import android.content.Context
import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import com.saiful.androidwebrtcclient.R
import com.saiful.androidwebrtcclient.adapter.ChatAdapter
import com.saiful.androidwebrtcclient.adt.ChatMessage
import com.saiful.androidwebrtcclient.util.Constants
import com.saiful.androidwebrtcclient.util.LogRTCListener

import org.json.JSONException
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.MediaStream
import org.webrtc.PeerConnectionFactory
import org.webrtc.VideoCapturer
import org.webrtc.VideoCapturerAndroid
import org.webrtc.VideoRenderer
import org.webrtc.VideoRendererGui
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

import java.util.LinkedList

import me.kevingleason.pnwebrtc.PnPeer
import me.kevingleason.pnwebrtc.PnRTCClient

/**
 * This chat will begin/subscribe to a video chat.
 * REQUIRED: The intent must contain a
 */
class VideoChatActivity : ListActivity() {

    private var pnRTCClient: PnRTCClient? = null
    private var localVideoSource: VideoSource? = null
    private var localRender: VideoRenderer.Callbacks? = null
    private var remoteRender: VideoRenderer.Callbacks? = null
    private var videoView: GLSurfaceView? = null
    private var mChatEditText: EditText? = null
    private var mChatList: ListView? = null
    private var mChatAdapter: ChatAdapter? = null
    private var mCallStatus: TextView? = null

    private var username: String? = null
    private var backPressed = false
    private var backPressedThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chat)

        val extras = intent.extras
        if (extras == null || !extras.containsKey(Constants.USER_NAME)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Need to pass username to VideoChatActivity in intent extras (Constants.USER_NAME).",
                    Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        this.username = extras.getString(Constants.USER_NAME, "")
        this.mChatList = listView
        this.mChatEditText = findViewById<View>(R.id.chat_input) as EditText
        this.mCallStatus = findViewById<View>(R.id.call_status) as TextView

        // Set up the List View for chatting
        val ll = LinkedList<ChatMessage>()
        mChatAdapter = ChatAdapter(this, ll)
        mChatList!!.adapter = mChatAdapter


        // First, we initiate the PeerConnectionFactory with our application context and some options.
        PeerConnectionFactory.initializeAndroidGlobals(
                this, // Context
                true, // Audio Enabled
                true, // Video Enabled
                true, null)// Hardware Acceleration Enabled
        // Render EGL Context

        val pcFactory = PeerConnectionFactory()
        this.pnRTCClient = PnRTCClient(Constants.PUB_KEY, Constants.SUB_KEY, this.username)

        // Returns the number of cams & front/back face device name
        val camNumber = VideoCapturerAndroid.getDeviceCount()
        val frontFacingCam = VideoCapturerAndroid.getNameOfFrontFacingDevice()
        val backFacingCam = VideoCapturerAndroid.getNameOfBackFacingDevice()

        // Creates a VideoCapturerAndroid instance for the device name
        val capturer = VideoCapturerAndroid.create(frontFacingCam)

        // First create a Video Source, then we can make a Video Track
        localVideoSource = pcFactory.createVideoSource(capturer, this.pnRTCClient!!.videoConstraints())
        val localVideoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource!!)

        // First we create an AudioSource then we can create our AudioTrack
        val audioSource = pcFactory.createAudioSource(this.pnRTCClient!!.audioConstraints())
        val localAudioTrack = pcFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource)

        // To create our VideoRenderer, we can use the included VideoRendererGui for simplicity
        // First we need to set the GLSurfaceView that it should render to
        this.videoView = findViewById<View>(R.id.gl_surface) as GLSurfaceView

        // Then we set that view, and pass a Runnable to run once the surface is ready
        VideoRendererGui.setView(videoView!!, null)

        // Now that VideoRendererGui is ready, we can get our VideoRenderer.
        // IN THIS ORDER. Effects which is on top or bottom
        remoteRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false)
        localRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)

        // We start out with an empty MediaStream object, created with help from our PeerConnectionFactory
        //  Note that LOCAL_MEDIA_STREAM_ID can be any string
        val mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID)

        // Now we can add our tracks.
        mediaStream.addTrack(localVideoTrack)
        mediaStream.addTrack(localAudioTrack)

        // First attach the RTC Listener so that callback events will be triggered
        this.pnRTCClient!!.attachRTCListener(DemoRTCListener())

        // Then attach your local media stream to the PnRTCClient.
        //  This will trigger the onLocalStream callback.
        this.pnRTCClient!!.attachLocalMediaStream(mediaStream)

        // Listen on a channel. This is your "phone number," also set the max chat users.
        this.pnRTCClient!!.listenOn("Kevin")
        this.pnRTCClient!!.setMaxConnections(1)

        // If the intent contains a number to dial, call it now that you are connected.
        //  Else, remain listening for a call.
        if (extras.containsKey(Constants.CALL_USER)) {
            val callUser = extras.getString(Constants.CALL_USER, "")
            connectToUser(callUser)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_video_chat, menu)
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

    override fun onPause() {
        super.onPause()
        this.videoView!!.onPause()
        this.localVideoSource!!.stop()
    }

    override fun onResume() {
        super.onResume()
        this.videoView!!.onResume()
        this.localVideoSource!!.restart()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this.localVideoSource != null) {
            this.localVideoSource!!.stop()
        }
        if (this.pnRTCClient != null) {
            this.pnRTCClient!!.onDestroy()
        }
    }

    override fun onBackPressed() {
        if (!this.backPressed) {
            this.backPressed = true
            Toast.makeText(this, "Press back again to end.", Toast.LENGTH_SHORT).show()
            this.backPressedThread = Thread(Runnable {
                try {
                    Thread.sleep(5000)
                    backPressed = false
                } catch (e: InterruptedException) {
                    Log.d("VCA-oBP", "Successfully interrupted")
                }
            })
            this.backPressedThread!!.start()
            return
        }
        if (this.backPressedThread != null)
            this.backPressedThread!!.interrupt()
        super.onBackPressed()
    }

    fun connectToUser(user: String) {
        this.pnRTCClient!!.connect(user)
    }

    fun hangup(view: View) {
        this.pnRTCClient!!.closeAllConnections()
        endCall()
    }

    private fun endCall() {
        startActivity(Intent(this@VideoChatActivity, MainActivity::class.java))
        finish()
    }


    fun sendMessage(view: View) {
        val message = mChatEditText!!.text.toString()
        if (message == "") return  // Return if empty
        val chatMsg = ChatMessage(this.username, message, System.currentTimeMillis())
        mChatAdapter!!.addMessage(chatMsg)
        val messageJSON = JSONObject()
        try {
            messageJSON.put(Constants.JSON_MSG_UUID, chatMsg.sender)
            messageJSON.put(Constants.JSON_MSG, chatMsg.message)
            messageJSON.put(Constants.JSON_TIME, chatMsg.timeStamp)
            this.pnRTCClient!!.transmitAll(messageJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        // Hide keyboard when you send a message.
        val focusView = this.currentFocus
        if (focusView != null) {
            val inputManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
        mChatEditText!!.setText("")
    }

    /**
     * LogRTCListener is used for debugging purposes, it prints all RTC messages.
     * DemoRTC is just a Log Listener with the added functionality to append screens.
     */
    private inner class DemoRTCListener : LogRTCListener() {
        override fun onLocalStream(localStream: MediaStream?) {
            super.onLocalStream(localStream) // Will log values
            this@VideoChatActivity.runOnUiThread(java.lang.Runnable {
                if (localStream!!.videoTracks.size == 0) return@Runnable
                localStream.videoTracks[0].addRenderer(VideoRenderer(localRender))
            })
        }

        override fun onAddRemoteStream(remoteStream: MediaStream?, peer: PnPeer?) {
            super.onAddRemoteStream(remoteStream, peer) // Will log values
            this@VideoChatActivity.runOnUiThread(java.lang.Runnable {
                Toast.makeText(this@VideoChatActivity, "Connected to " + peer!!.id, Toast.LENGTH_SHORT).show()
                try {
                    if (remoteStream!!.audioTracks.size == 0 || remoteStream.videoTracks.size == 0) return@Runnable
                    mCallStatus!!.visibility = View.GONE
                    remoteStream.videoTracks[0].addRenderer(VideoRenderer(remoteRender))
                    VideoRendererGui.update(remoteRender, 0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false)
                    VideoRendererGui.update(localRender, 72, 65, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }

        override fun onMessage(peer: PnPeer?, message: Any?) {
            super.onMessage(peer, message)  // Will log values
            if (message !is JSONObject) return  //Ignore if not JSONObject
            val jsonMsg = message as JSONObject?
            try {
                val uuid = jsonMsg!!.getString(Constants.JSON_MSG_UUID)
                val msg = jsonMsg.getString(Constants.JSON_MSG)
                val time = jsonMsg.getLong(Constants.JSON_TIME)
                val chatMsg = ChatMessage(uuid, msg, time)
                this@VideoChatActivity.runOnUiThread { mChatAdapter!!.addMessage(chatMsg) }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }

        override fun onPeerConnectionClosed(peer: PnPeer?) {
            super.onPeerConnectionClosed(peer)
            this@VideoChatActivity.runOnUiThread {
                mCallStatus!!.text = "Call Ended..."
                mCallStatus!!.visibility = View.VISIBLE
            }
            try {
                Thread.sleep(1500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            val intent = Intent(this@VideoChatActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    companion object {
        val VIDEO_TRACK_ID = "videoPN"
        val AUDIO_TRACK_ID = "audioPN"
        val LOCAL_MEDIA_STREAM_ID = "localStreamPN"
    }
}