package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.idbat.mobile.data.nfc.NfcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PocViewModel @Inject constructor(
    val nfcRepository: NfcRepository
) : ViewModel()
