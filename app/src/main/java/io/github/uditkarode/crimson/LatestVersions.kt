package io.github.uditkarode.crimson

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.androidnetworking.interfaces.StringRequestListener
import kotlinx.android.synthetic.main.latest_version.*
import org.json.JSONObject

class LatestVersions: AppCompatActivity() {

    lateinit var host: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidNetworking.initialize(this@LatestVersions)
        setContentView(R.layout.latest_version)

        telegramFooterLV.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/crimsonviolet"))) }
        githubFooterLV.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/uditkarode/crimson"))) }

        AndroidNetworking
            .get(Constants.HOST_REFERENCE)
            .doNotCacheResponse()
            .build()
            .getAsString(object: StringRequestListener {
                override fun onResponse(response: String) {
                    host = response
                    getLinks()
                }

                override fun onError(anError: ANError?) {
                    Toast.makeText(this@LatestVersions, "Please check your internet connection.", Toast.LENGTH_LONG).show()
                    finish()
                }
            })
    }

    private fun getLinks(){
        AndroidNetworking
            .get("$host/links.json")
            .doNotCacheResponse()
            .build()
            .getAsJSONObject(object: JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    lPCodename.text = response.getString("latestPCodename")
                    lQCodename.text = response.getString("latestQCodename")
                    pTagLatest.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(response.getString("latestPLink"))))
                    }
                    qTagLatest.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(response.getString("latestQLink"))))
                    }
                    pTagLatest.visibility = View.VISIBLE
                    qTagLatest.visibility = View.VISIBLE
                }

                override fun onError(anError: ANError?) {
                    Toast.makeText(this@LatestVersions, "Please check your internet connection!", Toast.LENGTH_LONG).show()
                    finish()
                }
            })
    }

    override fun onBackPressed() {
        finish()
    }
}