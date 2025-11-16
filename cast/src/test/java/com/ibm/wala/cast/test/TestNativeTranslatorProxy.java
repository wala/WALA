package com.ibm.wala.cast.test;

import org.junit.jupiter.api.Test;

/**
 * A proxy for {@link TestNativeTranslator} to help JUnit discover its test methods.
 *
 * <p>{@link TestNativeTranslator} is treated as a test fixture, not a test, for use by the Gradle
 * {@code :cast:smoke_test:checkSmokeTest} task. However, JUnit only looks for {@link Test}
 * annotations in test code. This trivial subclass ensures that the {@link #testNativeCAst()} test
 * will be discovered and run as part of the Gradle {@code :cast:test} task.
 */
public class TestNativeTranslatorProxy extends TestNativeTranslator {}
