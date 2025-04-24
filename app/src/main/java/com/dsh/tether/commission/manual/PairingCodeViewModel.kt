package com.dsh.tether.commission.manual

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dsh.tether.model.ValidationTaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PairingCodeViewModel @Inject constructor() : ViewModel() {

    /**
     * Validation status live data
     */
    private val _validationStatus = MutableLiveData<ValidationTaskStatus>()
    val validationStatus : LiveData<ValidationTaskStatus>
            get() = _validationStatus

    fun validateManualCode(payload: String){
        val pairingCode = payload.replace("-", "")
        Timber.d("Pairing code: [${pairingCode} , length ${pairingCode.length}]")
        if(pairingCode.length != 11 && pairingCode.length != 21){
            val message = "Matter only supports 11- or 21-digit codes"
            _validationStatus.postValue(ValidationTaskStatus.Failed(message))
        }else{
            _validationStatus.postValue(ValidationTaskStatus.Passed(pairingCode))
        }
    }
}