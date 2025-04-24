package com.dsh.openai.home.internal.utils.extensions

import com.aallam.openai.api.chat.FunctionCall

object FunctionCallExt {
    fun FunctionCall?.isNull(): Boolean {
        return this == null
    }
}