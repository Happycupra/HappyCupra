package com.carlauncherc.launcher.ui

import android.app.Activity
import android.content.ComponentName
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.carlauncherc.launcher.R
import com.carlauncherc.launcher.core.Prefs
import com.carlauncherc.launcher.data.AppRepository
import com.carlauncherc.launcher.databinding.ActivityHomeBinding
import com.carlauncherc.launcher.util.IntentUtil
import kotlinx.coroutines.CoroutineScope

/**
 * Navigation and music quick-launch cards. Tap launches, long-press re-assigns.
 *
 * Every launch path ends in a picker rather than a dead end: on a head unit the "right"
 * package name is firmware dependent, so the user must always be able to point at whatever
 * their unit actually ships.
 */
class QuickCardsController(
    private val activity: Activity,
    private val binding: ActivityHomeBinding,
    private val repository: AppRepository,
    private val scope: CoroutineScope
) {

    fun bind() {
        binding.cardNav.setOnClickListener { launchStoredOrPick(it, Slot.NAV) }
        binding.cardNav.setOnLongClickListener {
            pick(Slot.NAV)
            true
        }

        binding.cardMusic.setOnClickListener { launchStoredOrPick(it, Slot.MUSIC) }
        binding.cardMusic.setOnLongClickListener {
            pick(Slot.MUSIC)
            true
        }

        refreshLabels()
    }

    fun refreshLabels() {
        bindSubtitle(binding.subtitleNav, Prefs.navPackage, R.string.card_not_set)
        bindSubtitle(binding.subtitleMusic, Prefs.musicPackage, R.string.card_not_set)

    }

    fun onPackageRemoved(packageName: String) {
        if (Prefs.navPackage?.let { IntentUtil.packageOf(it) } == packageName) Prefs.navPackage = null
        if (Prefs.musicPackage?.let { IntentUtil.packageOf(it) } == packageName) Prefs.musicPackage = null
        refreshLabels()
    }

    // --------------------------------------------------------------- nav and music

    private enum class Slot { NAV, MUSIC }

    private fun launchStoredOrPick(source: View, slot: Slot) {
        val stored = when (slot) {
            Slot.NAV -> Prefs.navPackage
            Slot.MUSIC -> Prefs.musicPackage
        }
        if (stored.isNullOrBlank()) {
            pick(slot)
            return
        }
        if (!IntentUtil.launchStored(activity, stored, source)) {
            clear(slot)
            toast(R.string.app_not_installed)
            pick(slot)
        }
    }

    private fun pick(slot: Slot) {
        val titleRes = when (slot) {
            Slot.NAV -> R.string.pick_nav_app
            Slot.MUSIC -> R.string.pick_music_app
        }
        AppPicker.show(activity, repository, scope, titleRes) { entry ->
            val flattened = entry.component.flattenToShortString()
            when (slot) {
                Slot.NAV -> Prefs.navPackage = flattened
                Slot.MUSIC -> Prefs.musicPackage = flattened
            }
            refreshLabels()
        }
    }

    private fun clear(slot: Slot) {
        when (slot) {
            Slot.NAV -> Prefs.navPackage = null
            Slot.MUSIC -> Prefs.musicPackage = null
        }
        refreshLabels()
    }

    // ---------------------------------------------------------------------- shared

    private fun bindSubtitle(view: TextView, stored: String?, emptyRes: Int) {
        if (stored.isNullOrBlank()) {
            view.setText(emptyRes)
            return
        }
        val packageName = IntentUtil.packageOf(stored)
        val entry = ComponentName.unflattenFromString(stored)?.let { repository.find(it) }
            ?: repository.findByPackage(packageName)

        view.text = when {
            entry != null -> entry.label
            repository.isInstalled(packageName) -> packageName
            else -> activity.getString(R.string.card_not_installed)
        }
    }

    private fun toast(resId: Int) {
        Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show()
    }
}
