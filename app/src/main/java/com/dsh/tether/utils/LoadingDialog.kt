package com.dsh.tether.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.dsh.tether.databinding.DialogLoadingBinding
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LoadingDialog @Inject constructor(@ApplicationContext context: Context) {

    /**
     * The dialog
     */
    private val dialog: Dialog = Dialog(context)

    /**
     * The dialog binder
     */
    private var binding = DialogLoadingBinding.inflate(dialog.layoutInflater)

    /**
     * Shows the loading dialog
     */
    fun show() {
        dialog.setContentView(binding.root)
        binding.piLoading.visibility = View.VISIBLE
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.show()
    }

    /**
     * dismisses the loading dialog
     */
    fun dismiss() {
        if (dialog.isShowing) {
            binding.piLoading.visibility = View.INVISIBLE
            dialog.dismiss()
        }
    }
}