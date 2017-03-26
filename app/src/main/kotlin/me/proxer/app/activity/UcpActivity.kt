package me.proxer.app.activity

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import butterknife.bindView
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper
import me.proxer.app.R
import me.proxer.app.fragment.ucp.UcpOverviewFragment
import org.jetbrains.anko.startActivity

class UcpActivity : MainActivity() {

    companion object {
        fun navigateTo(context: Activity) {
            context.startActivity<UcpActivity>()
        }
    }

    private var sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val viewPager: ViewPager by bindView(R.id.viewPager)
    private val tabs: TabLayout by bindView(R.id.tabs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tabs)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.section_ucp)

        viewPager.adapter = sectionsPagerAdapter

        TabLayoutHelper(tabs, viewPager).apply { isAutoAdjustTabModeEnabled = true }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) :
            FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> UcpOverviewFragment.newInstance()
//                1 -> UcpToptenFragment.newInstance()
//                2 -> HistoryFragment.newInstance()
                else -> throw RuntimeException("Unknown index passed")
            }
        }

        override fun getCount() = 1 //3

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> getString(R.string.section_ucp_overview)
//                1 -> getString(R.string.fragment_ucp_topten_title)
//                2 -> getString(R.string.fragment_history_title)
                else -> throw RuntimeException("Unknown index passed")
            }
        }
    }
}
