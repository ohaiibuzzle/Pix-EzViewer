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

package com.perol.asdpl.pixivez.services

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.task.DownloadTask
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.data.AppDataRepo
import com.perol.asdpl.pixivez.networks.ServiceFactory.gson
import com.perol.asdpl.pixivez.objects.CrashHandler
import com.perol.asdpl.pixivez.objects.FastKVLogger
import com.perol.asdpl.pixivez.objects.FileUtil
import com.perol.asdpl.pixivez.objects.LanguageUtil
import com.perol.asdpl.pixivez.objects.Toasty
import io.fastkv.FastKVConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class PxEZApp : Application() {
    lateinit var pre: SharedPreferences
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Download.onTaskComplete
    fun taskComplete(task: DownloadTask?) {
        task?.let {
            val illustD = gson.decodeFromString<IllustD>(it.extendField)
            val title = illustD.title
            val sourceFile = File(it.filePath)
            if (sourceFile.isFile) {
                val needCreateFold = pre.getBoolean("needcreatefold", false)
                val name = illustD.userName?.toLegal()
                val targetFile = File(
                    "$storepath/" +
                        (if (R18Folder && sourceFile.name.startsWith("？")) R18FolderPath else "") +
                        if (needCreateFold) "${name}_${illustD.userId}" else "",
                    sourceFile.name.removePrefix("？")
                )
                val compatCheck = FileUtil.move(sourceFile, targetFile)
                MediaScannerConnection.scanFile(
                    this,
                    arrayOf(targetFile.path),
                    arrayOf(
                        MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(targetFile.extension)
                    )
                ) { _, _ ->
                    FileUtil.ListLog.add(illustD.id)
                    compatCheck()
                }

                if (ShowDownloadToast) {
                    Toasty.success(this, "${title}${getString(R.string.savesuccess)}")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // LeakCanary.install(this);
        pre = PreferenceManager.getDefaultSharedPreferences(this)
        CoroutineScope(Dispatchers.IO).launch {
            AppDataRepo.getUser()
        }
        Aria.init(this)
        Aria.download(this).register()

        Aria.get(this).apply {
            downloadConfig.apply {
//                queueMod=QueueMod.NOW.tag
                maxTaskNum = pre.getString("max_task_num", "2")!!.toInt()
                threadNum = pre.getString("thread_num", "2")!!.toInt()
            }
            appConfig.apply {
                isNotNetRetry = true
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            // Aria.download(this).removeAllTask(true)
            Aria.download(this).allCompleteTask?.forEach {
                if ((System.currentTimeMillis() - it.completeTime) > 10 * 60 * 1000) {
                    Aria.download(this).load(it.id).cancel()
                }
            }
            delay(10000)
            if (pre.getBoolean("resume_unfinished_task", true)
                // && Aria.download(this).allNotCompleteTask?.isNotEmpty()
            ) {
                // Toasty.normal(this, R.string.unfinished_task_title)
                Aria.download(this).allNotCompleteTask?.forEach {
                    if (it.state == 0) {
                        Aria.download(this).load(it.id).cancel()
                        delay(500)
                        // val illustD = gson.decodeFromString<IllustD>(it.str)
                        Aria.download(this).load(it.url)
                            .setFilePath(it.filePath) // 设置文件保存的完整路径
                            .ignoreFilePathOccupy()
                            .setExtendField(it.str)
                            .option(Works.option)
                            .create()
                        delay(300)
                    }
                }
            }
        }
        //initBugly(this)
        FastKVConfig.setLogger(FastKVLogger())
        FastKVConfig.setExecutor(Dispatchers.Default.asExecutor())
        AppCompatDelegate.setDefaultNightMode(
            pre.getString("dark_mode", "-1")!!.toInt()
        )
        animationEnable = pre.getBoolean("animation", false)
        ShowDownloadToast = pre.getBoolean("ShowDownloadToast", true)
        CollectMode = pre.getString("CollectMode", "0")?.toInt() ?: 0
        R18Private = pre.getBoolean("R18Private", true)
        R18Folder = pre.getBoolean("R18Folder", false)
        R18FolderPath = pre.getString("R18FolderPath", "xRestrict/")!!
        restrictSanity = pre.getInt("restrictSanity", 8)
        TagSeparator = pre.getString("TagSeparator", "#")!!
        storepath = pre.getString(
            "storepath1",
            Environment.getExternalStorageDirectory().absolutePath + File.separator + "PxEz"
        )!!
        saveformat = pre.getString("filesaveformat", "{illustid}({userid})_{title}_{part}{type}")!!
        if (pre.getBoolean("crashreport", true)) {
            CrashHandler.instance.init()
        }
        locale = LanguageUtil.getLocale() // System locale
        language = pre.getString("language", "-1")?.toIntOrNull()
            ?: LanguageUtil.localeToLang(locale) // try to detect language from system locale if not configured

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                ActivityCollector.collect(activity)
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            //override fun onActivityPostDestroyed(activity: Activity) {
            //    super.onActivityPostDestroyed(activity)
            //}

            override fun onActivityDestroyed(activity: Activity) {
                ActivityCollector.discard(activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                //
            }
        })
    }

    object ActivityCollector {
        @JvmStatic
        private val activityList = mutableListOf<Activity>()

        @JvmStatic
        fun collect(activity: Activity) {
            activityList.add(activity)
        }

        @JvmStatic
        fun discard(activity: Activity) {
            activityList.remove(activity)
        }

        @JvmStatic
        fun recreate() {
            for (i in activityList.size - 1 downTo 0) {
                activityList[i].recreate()
            }
        }
    }

    companion object {
        @JvmStatic
        var storepath = ""

        @JvmStatic
        var R18Folder: Boolean = false

        @JvmStatic
        var R18FolderPath = "xrestrict"

        @JvmStatic
        var restrictSanity: Int = 8

        @JvmStatic
        var saveformat = ""

        @JvmStatic
        var locale: Locale = Locale.SIMPLIFIED_CHINESE

        @JvmStatic
        var language: Int = 0

        @JvmStatic
        var animationEnable: Boolean = false

        @JvmStatic
        var R18Private: Boolean = true

        @JvmStatic
        var ShowDownloadToast: Boolean = true

        @JvmStatic
        var CollectMode: Int = 0

        @JvmStatic
        var TagSeparator: String = "#"

        lateinit var instance: PxEZApp
    }
}
