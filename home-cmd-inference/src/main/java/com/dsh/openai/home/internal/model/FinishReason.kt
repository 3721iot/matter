package com.dsh.openai.home.internal.model

@JvmInline
value class FinishReason(val reason: String) {
    companion object {
        @JvmStatic
        val FunctionCall: FinishReason = FinishReason("function_call")
        @JvmStatic
        val Stop: FinishReason = FinishReason("stop")
    }
}