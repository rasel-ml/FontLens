package com.fontlens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.fontlens.data.FontRepository
import com.fontlens.databinding.ActivityMainBinding
import com.fontlens.databinding.ItemDrawerFolderBinding
import com.fontlens.utils.FontLoader
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var backPressedOnce = false
    private val backToastHandler = android.os.Handler(android.os.Looper.getMainLooper())

    // True when launched by tapping a font file — suppresses library auto-reload
    var launchedFromIntent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FontRepository.load(this)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        binding.bottomNav.setOnItemSelectedListener { item ->
            val destId = when (item.itemId) {
                R.id.nav_library   -> R.id.fontListFragment
                R.id.nav_favorites -> R.id.favoritesFragment
                R.id.nav_settings  -> R.id.settingsFragment
                else -> return@setOnItemSelectedListener false
            }
            navController.navigate(destId, null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.fontListFragment, false)
                    .setLaunchSingleTop(true).build()
            )
            true
        }

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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START); return
                }
                val currentDest = navController.currentDestination?.id
                if (currentDest !in topDests) { navController.popBackStack(); return }
                if (currentDest == R.id.settingsFragment || currentDest == R.id.favoritesFragment) {
                    navController.navigate(R.id.fontListFragment, null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.fontListFragment, true)
                            .setLaunchSingleTop(true).build()
                    )
                    binding.bottomNav.menu.findItem(R.id.nav_library)?.isChecked = true
                    return
                }
                if (backPressedOnce) {
                    backToastHandler.removeCallbacksAndMessages(null); finish(); return
                }
                backPressedOnce = true
                Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                backToastHandler.postDelayed({ backPressedOnce = false }, 2000)
            }
        })

        // Handle font file opened from file manager
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            launchedFromIntent = true
            handleIncomingIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App already running — no need to suppress library reload
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return

        val name = getFileNameFromUri(uri)
        val ext  = name.substringAfterLast(".").lowercase()
        val fontExts = setOf("ttf", "otf", "woff", "woff2", "ttc")
        val mimeType = intent.type ?: contentResolver.getType(uri) ?: ""
        val isFontMime = mimeType.startsWith("font/") || mimeType.contains("font") ||
                (mimeType == "application/octet-stream" && ext in fontExts)

        if (!isFontMime && ext !in fontExts) {
            Toast.makeText(this, "Not a supported font file", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val items = FontLoader.loadFontsFromUris(this@MainActivity, listOf(uri))
            if (items.isEmpty()) {
                Toast.makeText(this@MainActivity, "Could not load font", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val font = items.first()
            FontRepository.addTempFont(font)

            val navHost = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHost.navController.navigate(
                R.id.previewFragment,
                Bundle().apply {
                    putString("fontId", font.id)
                    putBoolean("tempMode", true)
                }
            )
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx) ?: ""
        }
        return name.ifEmpty { uri.lastPathSegment ?: "" }
    }

    fun openDrawer()  { refreshDrawer(); binding.drawerLayout.openDrawer(GravityCompat.START) }
    fun closeDrawer() { binding.drawerLayout.closeDrawer(GravityCompat.START) }

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
                FontRepository.unmarkFolderLoaded(uri)
                getLibraryFragment()?.reloadFolder(uri)
            }
            fb.btnRemoveFolder.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Remove Folder")
                    .setMessage("Remove this folder from FontLens?\n\nFonts already loaded will remain until the app restarts.")
                    .setPositiveButton("Remove") { _, _ ->
                        FontRepository.removeSavedFolder(uri, this); refreshDrawer()
                    }
                    .setNegativeButton("Cancel", null).show()
            }
            container.addView(fb.root)
        }
    }

    private fun getLibraryFragment(): com.fontlens.ui.list.FontListFragment? =
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            ?.childFragmentManager?.fragments?.firstOrNull()
            as? com.fontlens.ui.list.FontListFragment

    private fun getFolderDisplayName(uri: Uri): String =
        try { "/" + (uri.lastPathSegment ?: uri.toString()).substringAfter(":") }
        catch (_: Exception) { uri.toString() }
}
