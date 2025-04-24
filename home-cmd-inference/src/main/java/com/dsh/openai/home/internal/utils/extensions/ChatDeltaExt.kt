package com.dsh.openai.home.internal.utils.extensions

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatDelta

@OptIn(BetaOpenAI::class)
object ChatDeltaExt {
    fun ChatDelta?.isNull(): Boolean {
        return null == this
    }

    fun ChatDelta?.isEmpty(): Boolean {
        return (null == this) || (this.content.isNullOrEmpty() && (null == this.functionCall))
    }
}