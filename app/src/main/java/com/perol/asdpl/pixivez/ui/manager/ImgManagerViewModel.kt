/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
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

package com.perol.asdpl.pixivez.ui.manager

import android.os.Build
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.networks.ServiceFactory.gson
import com.perol.asdpl.pixivez.objects.FileInfo
import com.perol.asdpl.pixivez.objects.getFastKV
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.services.Works
import io.fastkv.FastKV
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.io.File


class RenameTask(fileInfo: FileInfo) {
    val file: FileInfo = fileInfo
    val pid: Int? = if (file.isPic()) file.pid else null
    val part: Int? = if (file.isPic()) fileInfo.part.toIntOrNull() else null
}
//TODO: clear
class ImgManagerViewModel : BaseViewModel() {
    val pre = PxEZApp.instance.pre
    var path = MutableLiveData<String>()
    var saveformat = pre.getString("ImgManagerSaveFormat", PxEZApp.saveformat)!!
    var TagSeparator = pre.getString("ImgManagerTagSeparator", PxEZApp.TagSeparator)!!
    var files: MutableList<FileInfo>? = null
    var task: List<RenameTask>? = null
    var length_filter = false
    var rename_once = false
    lateinit var adapter: ImgManagerAdapter
    lateinit var layoutManager: LinearLayoutManager
    val kv = getFastKV("ImgMgr")

    fun getInfo() = viewModelScope.launch {
        /*files!!.asFlow().map {
            if(!length_filter ||it.file.name.length>40)
                it.pid?.run {
                    retrofit.api.getIllust(this).let{rt->

                    }
                   CrashHandler.instance.d("imgMgr", "get$this")
                }
            return@map it
        }.collect()*/
        val taskmap = HashMap<Int, RenameTask>()
        task?.filter {
            (!length_filter || it.file.name.length < 50) && it.pid != null
        }?.forEach {
                taskmap[it.pid!!] = it
            if (kv.contains(it.pid.toString())) {
                kv.decodeSerializable<Illust>(it.pid.toString())
            } else {
                try {
                    retrofit.api.getIllust(it.pid).let {
                        kv.encode(it.illust.id.toString(), it.illust)
                        it.illust
                    }
                } catch (e: Exception) {
                    Log.e("imgMgr", "getIllust ${it.pid} : ${e.message} ")
                    null
                    }
            }?.let{ rt ->
                    //CrashHandler.instance.d("imgMgr","get"+this+"p"+it.part)
                    val it = taskmap[rt.id]!!
                    it.file.illust = rt
                    it.file.target =
                        Works.parseSaveFormat(rt, it.part, saveformat, TagSeparator)
                    it.file.checked = (it.file.target != it.file.name)
                    //CrashHandler.instance.d("imgMgr","get"+it.pid+"p"+it.part+"check"+it.file.checked )
                    if (rename_once) {
                        rename(it)
                    }
                    it
                }?.let {
                    //CrashHandler.instance.d("imgMgr","refresh"+it.pid+"p"+it.part)
                    val preIndex = files!!.indexOf(it.file)
                    if (preIndex >= layoutManager.findFirstVisibleItemPosition() &&
                        preIndex <= layoutManager.findLastVisibleItemPosition()
                    ) {
                        adapter.notifyItemChanged(preIndex)
                    }
                }
        }
        //CrashHandler.instance.d("imgMgr","all")
        File(path.value + File.separatorChar + "rename.log")
            .writeText(gson.encodeToString(task))
        taskmap.clear()
        launchUI {
            delay(200)
            adapter.notifyDataSetChanged()
        }
    }

    fun rename(it: RenameTask) {
        if (it.file.name == it.file.target || it.file.target == null) {
            return
        }
        val orig = File(it.file.path)
        val tar = "${orig.parent}${File.separator}${it.file.target}"
        it.file.name = it.file.target!!
        File(it.file.path).renameTo(File(tar))
        val preIndex = files!!.indexOf(it.file)
        launchUI {
            if (preIndex >= layoutManager.findFirstVisibleItemPosition() &&
                preIndex <= layoutManager.findLastVisibleItemPosition()
            ) {
                adapter.notifyItemChanged(preIndex)
            }
        }
    }

    fun renameAll() {
        Thread {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                task?.parallelStream()?.filter {
                    it.file.checked
                }?.forEach {
                    rename(it)
                }
            } else {
                task?.filter {
                    it.file.checked
                }?.forEach {
                    rename(it)
                }
            }
        }.start()
    }
}

private inline fun <reified T : Any> FastKV.encode(key: String, target: T?) =
    putString(key, gson.encodeToString(target))

private inline fun <reified T : Any> FastKV.decodeSerializable(key: String): T? =
    getString(key)?.let { gson.decodeFromString<T>(it) }
