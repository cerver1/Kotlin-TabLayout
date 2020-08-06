package com.fair.tablayoutv2

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentAdapter = TabAdapter(supportFragmentManager)
        ViewPager.adapter = fragmentAdapter

        TabLayout.setupWithViewPager(ViewPager)

        TabLayout.getTabAt(0)?.setIcon(R.drawable.ein)
        TabLayout.getTabAt(1)?.setIcon(R.drawable.zwei)
        TabLayout.getTabAt(2)?.setIcon(R.drawable.drei)
    }


    companion object {

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let{
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }}
            return if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir

        }
    }


}
