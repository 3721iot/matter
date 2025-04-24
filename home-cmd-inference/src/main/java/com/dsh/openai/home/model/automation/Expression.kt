package com.dsh.openai.home.model.automation


class Expression(val sign: String) {
    companion object {
        /**
         * The equal to sign
         */
        @JvmStatic
        val Equal: Expression = Expression("==")

        /**
         * The less than sign
         */
        @JvmStatic
        val LessThan: Expression = Expression("<")

        /**
         * The greater than
         */
        @JvmStatic
        val GreaterThan: Expression = Expression(">")

        /**
         * The less than or equal to sign
         */
        @JvmStatic
        val LessThanOrEqual: Expression = Expression("<=")

        /**
         * The greater than or equal to sign
         */
        @JvmStatic
        val GreaterThanOrEqual: Expression = Expression(">=")
    }
}