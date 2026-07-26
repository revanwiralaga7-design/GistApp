package com.gistapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.gistapp.R
import com.gistapp.databinding.ActivityMainBinding
import com.gistapp.ui.auth.AuthActivity
import com.gistapp.ui.create.CreateGistActivity
import com.gistapp.ui.gistlist.GistListFragment
import com.gistapp.util.TokenManager

/**
 * Main Activity dengan BottomNavigation untuk:
 * - Gists Saya (perlu login)
 * - Gists Publik (bisa tanpa login)
 * - FAB untuk buat gist baru
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setSupportActionBar(binding.toolbar)

        // Default: tampilkan My Gists
        if (savedInstanceState == null) {
            loadFragment(GistListFragment.newInstance(isPublic = false), "My Gists")
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_my_gists -> {
                    loadFragment(GistListFragment.newInstance(isPublic = false), "My Gists")
                    true
                }
                R.id.nav_public -> {
                    loadFragment(GistListFragment.newInstance(isPublic = true), "Public Gists")
                    true
                }
                else -> false
            }
        }

        binding.fabCreate.setOnClickListener {
            if (tokenManager.hasToken()) {
                startActivity(Intent(this, CreateGistActivity::class.java))
            } else {
                // Arahkan ke login
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                tokenManager.clearToken()
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            R.id.action_about -> {
                // TODO: About dialog
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        binding.toolbar.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
