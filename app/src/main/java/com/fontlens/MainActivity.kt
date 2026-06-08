package com.fontlens

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import com.fontlens.data.FontRepository
import com.fontlens.databinding.ActivityMainBinding
import com.fontlens.databinding.ItemDrawerFolderBinding

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

        // Bottom nav
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_library   -> { navController.navigate(R.id.fontListFragment);   true }
                R.id.nav_favorites -> { navController.navigate(R.id.favoritesFragment);  true }
                R.id.nav_settings  -> { navController.navigate(R.id.settingsFragment);   true }
                else -> false
            }
        }

        // Hide bottom nav on sub-screens
        val topDests = setOf(R.id.fontListFragment, R.id.favoritesFragment, R.id.settingsFragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in topDests) View.VISIBLE else View.GONE
            when (destination.id) {
                R.id.fontListFragment  -> binding.bottomNav.menu.findItem(R.id.nav_library)?.isChecked   = true
                R.id.favoritesFragment -> binding.bottomNav.menu.findItem(R.id.nav_favorites)?.isChecked = true
                R.id.settingsFragment  -> binding.bottomNav.menu.findItem(R.id.nav_settings)?.isChecked  = true
            }
        }
    }

    /** Called by FontListFragment toolbar hamburger icon */
    fun openDrawer() {
        refreshDrawer()
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    fun refreshDrawer() {
        val container = binding.folderListContainer
        val tvEmpty   = binding.tvDrawerEmpty
        container.removeAllViews()

        val folders = FontRepository.getSavedFolderUris()
        tvEmpty.visibility = if (folders.isEmpty()) View.VISIBLE else View.GONE

        val inflater = LayoutInflater.from(this)
        folders.forEach { uri ->
            val fb = ItemDrawerFolderBinding.inflate(inflater, container, false)
            fb.tvFolderPath.text = getFolderDisplayName(uri)

            fb.btnReload.setOnClickListener {
                closeDrawer()
                // Notify library fragment to reload this folder
                FontRepository.unmarkFolderLoaded(uri)
                (supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment)
                    ?.childFragmentManager
                    ?.fragments
                    ?.firstOrNull() as? com.fontlens.ui.list.FontListFragment)
                    ?.reloadFolder(uri)
            }

            fb.btnRemoveFolder.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Remove Folder")
                    .setMessage("Remove this folder from FontLens?\n\nFonts already loaded will remain in the library until the app restarts.")
                    .setPositiveButton("Remove") { _, _ ->
                        FontRepository.removeSavedFolder(uri, this)
                        refreshDrawer()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            container.addView(fb.root)
        }
    }

    private fun getFolderDisplayName(uri: Uri): String {
        return try {
            val path = uri.lastPathSegment ?: uri.toString()
            // Convert "primary:Fonts/MyFonts" → "/Fonts/MyFonts"
            path.substringAfter(":").let { "/$it" }
        } catch (_: Exception) {
            uri.toString()
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }
}
