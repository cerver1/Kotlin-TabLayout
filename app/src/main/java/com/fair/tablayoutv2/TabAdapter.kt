package com.fair.tablayoutv2

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class TabAdapter(fm: FragmentManager) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int) = when (position) {
        0 -> FirstFragment()
        1 -> SecondFragment()
        else -> ThirdFragment()
    }

    override fun getCount() = 3

    override fun getPageTitle(position: Int) =
        when (position) {
            0 -> "First Tab"
            1 -> "Second Tab"
            else -> "Third Tab"
    }
}