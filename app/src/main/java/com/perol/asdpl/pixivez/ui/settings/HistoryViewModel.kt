/*
 * MIT License
 *
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

package com.perol.asdpl.pixivez.ui.settings

import androidx.lifecycle.MutableLiveData
import com.perol.asdpl.pixivez.base.BaseViewModel
import com.perol.asdpl.pixivez.data.HistoryDatabase
import com.perol.asdpl.pixivez.data.entity.HistoryEntity
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel : BaseViewModel() {
    val history = MutableLiveData<MutableList<HistoryEntity>>()
    private val historyDatabase = HistoryDatabase.getInstance(PxEZApp.instance)

    fun first() {
        CoroutineScope(Dispatchers.IO).launch {
            val history = historyDatabase.viewHistoryDao().getAll() as MutableList
            withContext(Dispatchers.Main) {
                this@HistoryViewModel.history.value = history
            }
        }
    }

    fun clearHistory() {
        CoroutineScope(Dispatchers.IO).launch {
            historyDatabase.viewHistoryDao().clear()
        }
    }

    fun deleteSelect(i: Int, after: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            historyDatabase.viewHistoryDao().delete(history.value!![i])
            withContext(Dispatchers.Main) {
                history.value!!.removeAt(i)
                after()
            }
        }
    }
}
