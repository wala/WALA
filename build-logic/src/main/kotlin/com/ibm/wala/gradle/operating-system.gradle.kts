package com.ibm.wala.gradle

/**
 * Utility script for operating system detection in Gradle builds.
 *
 * This script provides variables to identify the current operating system, which can be used to
 * configure platform-specific build settings.
 */

/**
 * The name of the operating system as reported by the Java runtime.
 *
 * This value is obtained from
 * [the `os.name` system property](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/System.html#getProperties()).
 */
val osName: String by extra(System.getProperty("os.name"))

/**
 * Indicates whether the current operating system is Windows.
 *
 * This is determined by checking if the operating system name starts with `"Windows "`.
 */
@Suppress("unused") val isWindows by extra(osName.startsWith("Windows "))
