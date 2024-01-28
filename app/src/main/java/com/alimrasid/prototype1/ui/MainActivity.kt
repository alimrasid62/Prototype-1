package com.alimrasid.prototype1.ui

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import com.alimrasid.prototype1.R
import android.content.Intent
import androidx.fragment.app.Fragment
import com.alimrasid.prototype1.ui.detection.DetectorActivity
import com.alimrasid.prototype1.ui.home.HomeFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    companion object {
        const val MINIMUM_CONFIDENCE_TF_OD_API = 0.5f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNav)

        val fab: FloatingActionButton = findViewById(R.id.fabCamera)
        fab.setOnClickListener {
            startActivity(Intent(this, DetectorActivity::class.java))
        }

        bottomNav.setOnNavigationItemSelectedListener { item ->
            var fragment: Fragment? = null

            when (item.itemId) {
                R.id.home_nav -> fragment = HomeFragment()
                R.id.profile_nav -> fragment = ProfileFragment()
                R.id.camera_nav -> startActivity(Intent(this@MainActivity, DetectorActivity::class.java))
            }
            loadFragment(fragment)
        }
    }

    private fun loadFragment(fragment: Fragment?): Boolean {
        if (fragment != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.mainContainer, fragment)
                .commit()
            return true
        }
        return false
    }
}
