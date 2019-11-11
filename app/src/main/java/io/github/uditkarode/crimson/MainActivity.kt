package io.github.uditkarode.crimson

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.androidnetworking.interfaces.StringRequestListener
import com.onesignal.OneSignal
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var host: String
    lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidNetworking.initialize(this@MainActivity)

        OneSignal.startInit(this)
            .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
            .unsubscribeWhenNotificationsAreDisabled(true)
            .init()

        ViewPump.init(
            ViewPump.builder()
                .addInterceptor(
                    CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                            .setDefaultFontPath("RobotoMono-Regular.ttf")
                            .setFontAttrId(R.attr.fontPath)
                            .build()
                    )
                ).build()
        )

        setContentView(R.layout.activity_main)
        sp = getSharedPreferences("kernelStats", 0)
        telegramFooter.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/crimsonviolet"))) }
        githubFooter.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/uditkarode/crimson"))) }
        head.setOnClickListener {
            startActivity(Intent(this@MainActivity, LatestVersions::class.java))
        }

        val kernelVersion = readKernelVersion()

        if(areStatsStored()){
            codeNameTv.text = sp.getString("codeName", "Clear app data")
            cafTagTv.text = sp.getString("cafTag", "Clear app data")
            linuxVersionTv.text = sp.getString("linuxVersion", "Clear app data")
            buildDateTv.text = sp.getString("buildDate", "Clear app data")
        }

        if(!kernelVersion.contains(Constants.KERNEL_NAME.toLowerCase(Locale.getDefault()))){
            Toast.makeText(this@MainActivity, "You are not using the Crimson kernel - get it now! ONLY VIOLET", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@MainActivity, LatestVersions::class.java))
            finish()
        } else {
            if(!kernelVersion.contains("minimal")){
                Toast.makeText(this@MainActivity, "This application is not for P tag kernels for now. Only latest links will be shown.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@MainActivity, LatestVersions::class.java))
                finish()
            }
        }

        val buildDate = SimpleDateFormat("MMM dd HH yyyy", Locale.ENGLISH).parse(kernelVersion.substring(kernelVersion.lastIndexOf("PREEMPT")).substring(12, 20) + " ${Constants.CURRENT_YEAR}")

        AndroidNetworking
            .get(Constants.HOST_REFERENCE)
            .doNotCacheResponse()
            .build()
            .getAsString(object: StringRequestListener {
                override fun onResponse(response: String) {
                    host = response
                    checkForUpdates(buildDate)
                }

                override fun onError(anError: ANError?) {
                    Toast.makeText(this@MainActivity, "Please check your internet connection.", Toast.LENGTH_LONG).show()
                    finish()
                }
            })
    }

    private fun stopLoading(updateAvailable: Boolean, downloadLink: String = ""){
        loader.visibility = View.GONE

        if(updateAvailable){
            updateStatusImg.setImageDrawable(getDrawable(R.drawable.crossfix))
            updateStatusTv.text = "An update is available!"
            updateButton.visibility = View.VISIBLE
            updateButton.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadLink)))
            }
        }

        content.visibility = View.VISIBLE
    }

    private fun areStatsStored() = sp.getString("codeName", "rip") != "rip"

    private fun checkForUpdates(buildDate: Date){
        AndroidNetworking
            .get("$host/latest.json")
            .doNotCacheResponse()
            .build()
            .getAsJSONObject(object: JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    val latestDate = SimpleDateFormat("MMM dd HH yyyy", Locale.ENGLISH).parse(response.getString("builtAt") + " ${Constants.CURRENT_YEAR}")
                    val latestFDate = SimpleDateFormat("dd/MM/yyyy hh a", Locale.ENGLISH).format(latestDate)
                    val codeName = response.getString("codeName")
                    val cafTag = response.getString("cafTag")
                    val linuxVersion = response.getString("linuxVersion")
                    val buildFdate = SimpleDateFormat("dd/MM/yyyy hh a", Locale.ENGLISH).format(latestDate)
                    val editor = sp.edit()

                    if(latestDate.after(buildDate)){
                        editor.putString("codeName", codeName)
                        editor.putString("cafTag", cafTag)
                        editor.putString("linuxVersion", linuxVersion)
                        editor.putString("buildDate", latestFDate)
                        editor.apply()

                        codeNameTv.text = codeName
                        cafTagTv.text = cafTag
                        linuxVersionTv.text = linuxVersion
                        buildDateTv.text = buildFdate

                        updateBuildDateTv.text = "built at: $buildFdate"
                        updateBuildDateTv.visibility = View.VISIBLE
                        stopLoading(true, response.getString("downloadLink"))
                    } else {
                        stopLoading(false)
                        if(!areStatsStored()){
                            editor.putString("codeName", codeName)
                            editor.putString("cafTag", cafTag)
                            editor.putString("linuxVersion", linuxVersion)
                            editor.putString("buildDate", latestFDate)
                            editor.apply()

                            codeNameTv.text = codeName
                            cafTagTv.text = cafTag
                            linuxVersionTv.text = linuxVersion
                            buildDateTv.text = latestFDate
                        }
                    }
                }

                override fun onError(anError: ANError?) {
                    Toast.makeText(this@MainActivity, "Please check your internet connection! ${anError?.errorDetail.toString()}", Toast.LENGTH_LONG).show()
                    finish()
                }
            })
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    private fun readKernelVersion(): String {
        try {
            val p = Runtime.getRuntime().exec("uname -av")
            val `is`: InputStream? = if (p.waitFor() == 0) {
                p.inputStream
            } else {
                p.errorStream
            }
            val br = BufferedReader(
                InputStreamReader(`is`),
                32
            )
            val line = br.readLine()
            br.close()
            return line
        } catch (ex: Exception) {
            return "ERROR: " + ex.message
        }

    }
}