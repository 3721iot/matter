package com.dsh.openai.home

import android.content.Context
import com.dsh.openai.home.internal.InferenceEngineImpl
import com.dsh.openai.home.model.config.InferenceConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class HomeCommandInference @Inject constructor() {
    companion object {
        /**
         * Getter for the inference engine
         *
         * @param context the application context
         * @param config the inference engine config
         * @param listener the inference listener
         * @return the inference engine
         */
        @JvmStatic
        @Deprecated(
            message = "Use getClient(context, config, toolsListener, resultListener)",
            replaceWith = ReplaceWith(
                "getClient(context, config, toolsListener, resultListener)"
            ),
            level = DeprecationLevel.ERROR
        )
        fun getClient(
            @ApplicationContext context: Context,
            config: InferenceConfig,
            listener: InferenceListener
        ) : InferenceEngine {
            return InferenceEngineImpl.getInstance()
        }

        /**
         * Getter for the inference engine
         *
         * @param context the application context
         * @param config the inference engine config
         * @param toolsListener the inference engine tools listener
         * @param resultListener the inference engine results listener
         * @return the inference engine
         */
        @JvmStatic
        fun getClient(
            @ApplicationContext context: Context,
            config: InferenceConfig,
            toolsListener: InferenceToolsListener,
            resultListener: InferenceResultListener,
        ) : InferenceEngine {
            return InferenceEngineImpl.getInstance(context, config, toolsListener, resultListener)
        }
    }
}