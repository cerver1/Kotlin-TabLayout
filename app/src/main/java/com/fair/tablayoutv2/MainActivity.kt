package com.fair.tablayoutv2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TableLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentAdapter = Tab_Adapter(supportFragmentManager)
        ViewPager.adapter = fragmentAdapter

        TabLayout.setupWithViewPager(ViewPager)

        TabLayout.getTabAt(0)?.setIcon(R.drawable.ic_launcher_background)
        TabLayout.getTabAt(1)?.setIcon(R.drawable.ic_launcher_background)
        TabLayout.getTabAt(2)?.setIcon(R.drawable.ic_launcher_background)
    }
}
