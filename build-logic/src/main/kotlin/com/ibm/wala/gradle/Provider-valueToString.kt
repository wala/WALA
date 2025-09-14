package com.ibm.wala.gradle

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.process.ProcessForkOptions

/**
 * Returns a proxy [Provider] whose [toString] uses the original [Provider]'s value at the moment
 * the [String] is requested.
 *
 * Several Gradle APIs accept [Any] value but will ultimately convert that value to a [String] using
 * [toString]. For example, [ProcessForkOptions.executable] follows this pattern, as does the second
 * argument to the two-argument overload of [AbstractExecTask.environment]. We can use this
 * extension property to postpone evaluating a [Provider] until the [String] representation of its
 * value is actually needed.
 */
val <T : Any> Provider<T>.valueToString
  get() =
      object : Provider<T> by this {
        override fun toString() = get().toString()
      }
