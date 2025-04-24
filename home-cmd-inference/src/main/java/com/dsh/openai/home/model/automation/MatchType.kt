package com.dsh.openai.home.model.automation

class MatchType(val type: String) {
    companion object {
        /**
         * Used to signal that all conditions should be matched
         */
        @JvmStatic
        val All: MatchType = MatchType("all")

        /**
         * Used to signal that matching any condition in the list of conditions is considered valid
         */
        @JvmStatic
        val Any: MatchType = MatchType("any")
    }
}