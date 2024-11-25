/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.tools.traces

import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * The utility class to wait a condition with customized options. The default retry policy is 5
 * times with interval 1 second.
 *
 * @param <T> The type of the object to validate.
 *
 * <p>Sample:</p> <pre> // Simple case. if (Condition.waitFor("true value", () -> true)) {
 *
 * ```
 *     println("Success");
 * ```
 *
 * } // Wait for customized result with customized validation. String result =
 * WaitForCondition.Builder(supplier = () -> "Result string")
 *
 * ```
 *         .withCondition(str -> str.equals("Expected string"))
 *         .withRetryIntervalMs(500)
 *         .withRetryLimit(3)
 *         .onFailure(str -> println("Failed on " + str)))
 *         .build()
 *         .waitFor()
 * ```
 *
 * </pre>
 *
 * @param condition If it returns true, that means the condition is satisfied.
 */
class WaitCondition<T>
private constructor(
    private val supplier: Supplier<T>,
    private val condition: Condition<T>,
    private val retryLimit: Int,
    private val onLog: BiConsumer<String, Boolean>?,
    private val onFailure: Consumer<T>?,
    private val onRetry: Consumer<T>?,
    private val onSuccess: Consumer<T>?,
    private val onStart: Consumer<String>?,
    private val onEnd: Consumer<String>?,
) {
    /** @return `false` if the condition does not satisfy within the time limit. */
    fun waitFor(): Boolean {
        onStart?.accept("waitFor")
        try {
            return doWaitFor()
        } finally {
            onEnd?.accept("done waitFor")
        }
    }

    private fun doWaitFor(): Boolean {
        onLog?.accept("***Waiting for $condition", false)
        var currState: T? = null
        var success = false
        for (i in 0..retryLimit) {
            val result = doWaitForRetry(i)
            success = result.first
            currState = result.second
            if (success) {
                break
            } else if (i < retryLimit) {
                onRetry?.accept(currState)
            }
        }

        return if (success) {
            true
        } else {
            doNotifyFailure(currState)
            false
        }
    }

    private fun doWaitForRetry(retryNr: Int): Pair<Boolean, T> {
        onStart?.accept("doWaitForRetry")
        try {
            val currState = supplier.get()
            return if (condition.isSatisfied(currState)) {
                onLog?.accept("***Waiting for $condition ... Success!", false)
                onSuccess?.accept(currState)
                Pair(true, currState)
            } else {
                val detailedMessage = condition.getMessage(currState)
                onLog?.accept("***Waiting for $detailedMessage... retry=${retryNr + 1}", true)
                Pair(false, currState)
            }
        } finally {
            onEnd?.accept("done doWaitForRetry")
        }
    }

    private fun doNotifyFailure(currState: T?) {
        val detailedMessage =
            if (currState != null) {
                condition.getMessage(currState)
            } else {
                condition.toString()
            }
        onLog?.accept("***Waiting for $detailedMessage ... Failed!", true)
        if (onFailure != null) {
            require(currState != null) { "Missing last result for failure notification" }
            onFailure.accept(currState)
        }
    }

    class Builder<T>(private var retryLimit: Int, private val supplier: Supplier<T>) {
        private val conditions = mutableListOf<Condition<T>>()
        private var onStart: Consumer<String>? = null
        private var onEnd: Consumer<String>? = null
        private var onFailure: Consumer<T>? = null
        private var onRetry: Consumer<T>? = null
        private var onSuccess: Consumer<T>? = null
        private var onLog: BiConsumer<String, Boolean>? = null

        fun withCondition(condition: Condition<T>) = apply { conditions.add(condition) }

        fun withCondition(message: String, condition: Predicate<T>) = apply {
            withCondition(Condition(message, condition))
        }

        private fun spreadConditionList(): List<Condition<T>> =
            conditions.flatMap {
                if (it is ConditionList<T>) {
                    it.conditions
                } else {
                    listOf(it)
                }
            }

        /**
         * Executes the action when the condition does not satisfy within the time limit. The passed
         * object to the consumer will be the last result from the supplier.
         */
        fun onFailure(onFailure: Consumer<T>): Builder<T> = apply { this.onFailure = onFailure }

        fun onLog(onLog: BiConsumer<String, Boolean>): Builder<T> = apply { this.onLog = onLog }

        fun onRetry(onRetry: Consumer<T>? = null): Builder<T> = apply { this.onRetry = onRetry }

        fun onStart(onStart: Consumer<String>? = null): Builder<T> = apply {
            this.onStart = onStart
        }

        fun onEnd(onEnd: Consumer<String>? = null): Builder<T> = apply { this.onEnd = onEnd }

        fun onSuccess(onRetry: Consumer<T>? = null): Builder<T> = apply { this.onSuccess = onRetry }

        fun build(): WaitCondition<T> =
            WaitCondition(
                supplier,
                ConditionList(spreadConditionList()),
                retryLimit,
                onLog,
                onFailure,
                onRetry,
                onSuccess,
                onStart,
                onEnd,
            )
    }
}
