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

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.adapters.PicturePagerAdapter
import com.perol.asdpl.pixivez.databinding.ActivityPictureBinding
import com.perol.asdpl.pixivez.objects.DataHolder
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlin.math.max

class PictureActivity : RinkActivity() {
    companion object {
        fun start(context: Context, id: Long, arrayList: LongArray? = LongArray(1) { id }) {
            val bundle = Bundle()
            bundle.putLongArray("illustidlist", arrayList)
            bundle.putLong("illustid", id)
            val intent = Intent(context, PictureActivity::class.java)
            intent.putExtras(bundle)
            context.startActivity(intent)
        }

        fun start(
            context: Context,
            id: Long,
            position: Int,
            limit: Int = 30,
            options: Bundle? = null
        ) {
            val bundle = Bundle()
            bundle.putInt("position", position - max(position - limit, 0))
            bundle.putLong("illustid", id)
            val intent = Intent(context, PictureActivity::class.java)
            intent.putExtras(bundle)
            context.startActivity(intent, options)
        }

        fun start(
            context: Context,
            illust: Illust,
            arrayList: LongArray? = LongArray(1) { illust.id }
        ) {
            val bundle = Bundle()
            bundle.putLongArray("illustidlist", arrayList)
            bundle.putParcelable("illust", illust)
            bundle.putLong("illustid", illust.id)
            val intent = Intent(context, PictureActivity::class.java)
            intent.putExtras(bundle)
            context.startActivity(intent)
        }
    }

    private var illustId: Long = 0
    private var illustIdList: LongArray? = null
    private var illustList: List<Illust>? = null
    private var nowPosition: Int = 0

    private lateinit var binding: ActivityPictureBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!PxEZApp.instance.pre
                .getBoolean("needstatusbar", false)
        ) {
            //WindowCompat.setDecorFitsSystemWindows(window, false)
            window.decorView.fitsSystemWindows = false
            //window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            //window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            // window.navigationBarColor = Color.TRANSPARENT
        } else {
            window.decorView.fitsSystemWindows = true
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        val bundle = this.intent.extras!!
        illustId = bundle.getLong("illustid")
        nowPosition = bundle.getInt("position", 0)

        when {
            bundle.containsKey("illustidlist") -> {
                illustIdList = bundle.getLongArray("illustidlist")
                nowPosition = illustIdList!!.indexOf(illustId)
                binding.viewpagePicture.adapter =
                    PicturePagerAdapter(supportFragmentManager, illustIdList!!)
            }

            DataHolder.checkIllustsList(nowPosition, illustId) -> {
                illustList = DataHolder.getIllustsList() // ?.toList()
                illustIdList = if (illustList != null) {
                    illustList!!.map { it.id }.toLongArray()
                } else {
                    LongArray(1) { illustId }
                }

                binding.viewpagePicture.adapter =
                    PicturePagerAdapter(supportFragmentManager, illustIdList, illustList)
                DataHolder.pictureAdapter = binding.viewpagePicture.adapter
            }

            else -> {
                illustIdList = LongArray(1) { illustId }
                nowPosition = 0 // illustIdList!!.indexOf(illustId)
                binding.viewpagePicture.adapter =
                    PicturePagerAdapter(supportFragmentManager, illustIdList)
            }
        }
        binding.viewpagePicture.currentItem = nowPosition

        if (PxEZApp.instance.pre.getBoolean("needactionbar", false)) {
            setSupportActionBar(binding.toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
        } else {
            binding.toolbar.visibility = View.GONE
        }

        binding.viewpagePicture.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                nowPosition = position
            }
        })
        supportPostponeEnterTransition()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_picture, menu)
        return true // super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                finishAfterTransition()

            R.id.action_share -> share()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        DataHolder.pictureAdapter = null
        super.onDestroy()
    }

    private fun share() {
        val textIntent = Intent(Intent.ACTION_SEND)
        // val illustId = illustIdList?.get(nowPosition)?: illustList?.get(nowPosition)?.id
        textIntent.type = "text/plain"
        textIntent.putExtra(
            Intent.EXTRA_TEXT,
            "https://www.pixiv.net/artworks/{illustIdList!![nowPosition]}"
        )
        startActivity(Intent.createChooser(textIntent, getString(R.string.share)))
    }
}
