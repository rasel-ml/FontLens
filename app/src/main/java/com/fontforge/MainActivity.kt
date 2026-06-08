package com.fontforge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.fontforge.data.FontRepository
import com.fontforge.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FontRepository.load(this)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        // Manual bottom nav handling so all 3 tabs work
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_library -> {
                    navController.navigate(R.id.fontListFragment)
                    true
                }
                R.id.nav_favorites -> {
                    navController.navigate(R.id.favoritesFragment)
                    true
                }
                R.id.nav_settings -> {
                    navController.navigate(R.id.settingsFragment)
                    true
                }
                else -> false
            }
        }

        // Hide bottom nav when inside sub-screens
        val topDests = setOf(
            R.id.fontListFragment,
            R.id.favoritesFragment,
            R.id.settingsFragment
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = if (destination.id in topDests)
                android.view.View.VISIBLE
            else
                android.view.View.GONE
        }

        // Keep bottom nav selection in sync with current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.fontListFragment  -> binding.bottomNav.menu.findItem(R.id.nav_library)?.isChecked   = true
                R.id.favoritesFragment -> binding.bottomNav.menu.findItem(R.id.nav_favorites)?.isChecked = true
                R.id.settingsFragment  -> binding.bottomNav.menu.findItem(R.id.nav_settings)?.isChecked  = true
            }
        }
    }
}
