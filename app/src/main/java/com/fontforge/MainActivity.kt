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

        binding.bottomNav.setupWithNavController(navController)

        // Keep bottom nav in sync when navigating via back stack
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val topDests = setOf(R.id.fontListFragment, R.id.favoritesFragment, R.id.settingsFragment)
            binding.bottomNav.visibility = if (destination.id in topDests)
                android.view.View.VISIBLE else android.view.View.GONE
        }
    }
}
