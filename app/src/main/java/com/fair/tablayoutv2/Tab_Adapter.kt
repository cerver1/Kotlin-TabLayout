package com.fair.tablayoutv2

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.tabs.TabLayout

class Tab_Adapter(fm: FragmentManager): FragmentPagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> First_Fragment()
            1 -> Second_Fragment()
            else -> { return Third_Fragment()}
        }
    }
    override fun getCount(): Int {
       return 3
    }
    override fun getPageTitle(position: Int): CharSequence? {
        return when(position) {
            0 -> "First Tab"
            1 -> "Second Tab"
            else -> {return "Third Tab"}
        }
    }
}