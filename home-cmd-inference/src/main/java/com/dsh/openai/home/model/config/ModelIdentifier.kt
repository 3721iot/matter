package com.dsh.openai.home.model.config

class ModelIdentifier(val id: String) {
    companion object {
        @JvmStatic
        val FourPointZero: ModelIdentifier = ModelIdentifier("gpt-4")
        @JvmStatic
        val FourPointZeroPreview: ModelIdentifier = ModelIdentifier("gpt-4-1106-preview")
        @JvmStatic
        val ThreePointFive16k: ModelIdentifier = ModelIdentifier("gpt-3.5-turbo-16k")
    }
}