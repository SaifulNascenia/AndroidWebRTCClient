package com.saiful.androidwebrtcclient.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText

import com.saiful.androidwebrtcclient.R
import com.saiful.androidwebrtcclient.util.Constants


/**
 * Login Activity for the first time the app is opened, or when a user clicks the sign out button.
 * Saves the username in SharedPreferences.
 */
class LoginActivity : Activity() {

    private var mUsername: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mUsername = findViewById<View>(R.id.login_username) as EditText

        val extras = intent.extras
        if (extras != null) {
            val lastUsername = extras.getString("oldUsername", "")
            mUsername!!.setText(lastUsername)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_login, menu)
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

    /**
     * Takes the username from the EditText, check its validity and saves it if valid.
     * Then, redirects to the MainActivity.
     * @param view Button clicked to trigger call to joinChat
     */
    fun joinChat(view: View) {
        val username = mUsername!!.text.toString()
        if (!validUsername(username))
            return

        val sp = getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
        val edit = sp.edit()
        edit.putString(Constants.USER_NAME, username)
        edit.apply()

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    /**
     * Optional function to specify what a username in your chat app can look like.
     * @param username The name entered by a user.
     * @return is username valid
     */
    private fun validUsername(username: String): Boolean {
        if (username.length == 0) {
            mUsername!!.error = "Username cannot be empty."
            return false
        }
        if (username.length > 16) {
            mUsername!!.error = "Username too long."
            return false
        }
        return true
    }
}
