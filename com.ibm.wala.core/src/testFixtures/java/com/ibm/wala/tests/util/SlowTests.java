package com.ibm.wala.tests.util;

/**
 * JUnit category marker for slow tests
 *
 * <p>Add “{@code -PexcludeSlowTests}” to the Gradle command line for faster test turnaround at the
 * expense of worse test coverage.
 */
public interface SlowTests {}
