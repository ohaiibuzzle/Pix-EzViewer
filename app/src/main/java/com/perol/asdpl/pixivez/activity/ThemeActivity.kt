/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
 * Copyright (c) 2019 Perol_Notsfsssf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package com.perol.asdpl.pixivez.activity

import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.GridItem
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.databinding.ActivityThemeBinding
import com.perol.asdpl.pixivez.objects.ThemeUtil
import com.perol.asdpl.pixivez.services.PxEZApp

class ThemeActivity : RinkActivity() {
    class ThemeFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pre_theme)
            findPreference<Preference>("dark_mode")!!.setOnPreferenceChangeListener { preference, newValue ->
                AppCompatDelegate.setDefaultNightMode(newValue.toString().toInt())
                PxEZApp.instance.setTheme(R.style.AppThemeBase_pink)
                true
            }
            findPreference<Preference>("material3")?.apply{
                setOnPreferenceChangeListener { preference, newValue ->
                    ThemeUtil.resetColor(requireActivity())
                    snackbarApplyConfig()
                    //PxEZApp.ActivityCollector.recreate()
                    true
                }
            }
            findPreference<Preference>("dynamicColor")?.apply{
                if (!PxEZApp.instance.pre.getBoolean("material3", true)){
                    isEnabled = false
                }
                if (!DynamicColors.isDynamicColorAvailable()){
                    summary = getString(R.string.dynamicColorAPIAlert)
                    isEnabled = false
                }
                else {
                    setOnPreferenceChangeListener { preference, newValue ->
                        ThemeUtil.resetColor(requireActivity())
                        PxEZApp.ActivityCollector.recreate()
                        true
                    }
                }
            }
            findPreference<Preference>("theme")?.apply {
                //icon = ColorDrawable(ThemeUtil.getColorPrimary(requireContext()))
                if (PxEZApp.instance.pre.getBoolean("dynamicColor", false)){
                    isEnabled = false
                    summary = "Dynamic"
                }
                else {
                    val colorItems = listOf(
                        BackgroundGridItem(R.color.colorPrimary, "Primary"),
                        BackgroundGridItem(R.color.md_blue_300, "Blue"),
                        BackgroundGridItem(R.color.pink, "Pink"),
                        BackgroundGridItem(R.color.miku, "Miku"),
                        BackgroundGridItem(R.color.md_purple_500, "Purple"),
                        BackgroundGridItem(R.color.md_cyan_300, "Cyan"),
                        BackgroundGridItem(R.color.md_green_300, "Green"),
                        BackgroundGridItem(R.color.md_indigo_300, "Indigo"),
                        BackgroundGridItem(R.color.md_red_500, "Red"),
                        BackgroundGridItem(R.color.now, "Pale green")
                    )
                    summary =
                        colorItems[
                            PxEZApp.instance.pre.getInt("colorint", 0)
                        ].title
                    setOnPreferenceClickListener {
                            MaterialDialog(
                                requireContext(),
                                BottomSheet(LayoutMode.WRAP_CONTENT)
                            ).show {
                                lateinit var action: () -> Unit

                                title(R.string.title_change_theme)
                                val gridItems = gridItems(colorItems) { _, index, item ->
                                    it.summary = item.title
                                    PxEZApp.instance.pre.edit {
                                        putInt("colorint", index)
                                    }
                                    ThemeUtil.resetColor(requireActivity())
                                    action = {
                                        PxEZApp.ActivityCollector.recreate()
                                    }
                                }
                                onDismiss {
                                    action.invoke()
                                }
                                cornerRadius(16.0F)
                                negativeButton(android.R.string.cancel)
                                positiveButton(R.string.action_apply)
                                lifecycleOwner(this@ThemeFragment)
                            }
                            true
                        }
                }
            }
        }

        private fun snackbarApplyConfig() {
            Snackbar.make(requireView(), getString(R.string.title_change_theme), Snackbar.LENGTH_SHORT)
                .setAction(R.string.restart_now) {
                    PxEZApp.ActivityCollector.recreate()
                }
                .show()
        }
    }

    private class BackgroundGridItem(@ColorRes private val color: Int, override val title: String) :
        GridItem {

        override fun populateIcon(imageView: ImageView) {
            imageView.apply {
                setBackgroundColor(ContextCompat.getColor(imageView.context, color))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    marginEnd = 4.dp
                    marginStart = 4.dp
                }
            }
        }

        private val Int.dp: Int get() = toFloat().dp.toInt()

        private val Float.dp: Float
            get() = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                this,
                Resources.getSystem().displayMetrics
            )
    }

    private lateinit var binding: ActivityThemeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction().replace(R.id.fragment_theme, ThemeFragment()).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
