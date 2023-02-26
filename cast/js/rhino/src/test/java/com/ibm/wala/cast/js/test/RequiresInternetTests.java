package com.ibm.wala.cast.js.test;

/**
 * JUnit category marker for tests that require Internet access
 *
 * <p>To exclude Internet-requiring tests, do any of the following:
 *
 * <ul>
 *   <li>add “{@code -PexcludeRequiresInternetTests}” to the Gradle command line,
 *   <li>add “{@code --offline}” to the Gradle command line, or
 *   <li>set the {@code $CI} environment variable to "{@code true}".
 * </ul>
 *
 * <p>Note that <a
 * href="https://docs.github.com/en/actions/learn-github-actions/environment-variables#default-environment-variables">GitHub
 * actions always set {@code $CI} to "{@code true}"</a>, so these tests will never run during GitHub
 * actions.
 */
public interface RequiresInternetTests {}
