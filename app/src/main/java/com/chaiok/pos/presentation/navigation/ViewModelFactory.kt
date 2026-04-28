package com.chaiok.pos.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SimpleFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator.invoke() as VM
}
