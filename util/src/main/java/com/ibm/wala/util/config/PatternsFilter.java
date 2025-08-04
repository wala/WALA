/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.NullMarked;

/**
 * A filter that accepts any {@link String} that matches any of a sequence of {@link Pattern}s.
 *
 * <p>{@link Pattern}s are matched as per {@link Matcher#matches()}. Thus, a {@link String} is in
 * this set if at least one {@link Pattern} matches the <em>entire</em> {@link String}.
 */
@NullMarked
public class PatternsFilter implements StringFilter {

  /** Serial version */
  private static final long serialVersionUID = 2613051147552054190L;

  /** The regular expressions that define set membership. */
  private final List<String> regexps;

  /** A membership test as provided by {@link Pattern#asMatchPredicate()}. */
  private transient Predicate<String> isMatch;

  private static Stream<String> discardComments(Stream<String> lines) {
    return lines.filter(line -> !line.startsWith("#"));
  }

  private static List<String> collectWithoutComments(Stream<String> lines) {
    return discardComments(lines).collect(Collectors.toUnmodifiableList());
  }

  private PatternsFilter(final List<String> regexps) {
    this.regexps = regexps;
    isMatch = compileStringPredicate();
  }

  /**
   * Creates a {@code PatternsFilter} with the given regular expressions, one per {@link Stream}
   * element.
   *
   * <p>Any element that starts with {@code "#"} is discarded as a comment.
   *
   * <p>This method does not take ownership of the given {@link Stream}. If that argument needs to
   * be closed after use, then the caller is responsible for doing so.
   *
   * @param lines the regular expressions to addAll, one per line
   */
  public PatternsFilter(final Stream<String> lines) {
    this(collectWithoutComments(lines));
  }

  /**
   * Creates a {@code PatternsFilter} with the given regular expressions, one per line.
   *
   * <p>Any line that starts with {@code "#"} is discarded as a comment.
   *
   * <p>This method does not take ownership of the given {@link InputStream}. If that argument needs
   * to be closed after use, then the caller is responsible for doing so.
   *
   * @param input the regular expressions to addAll, one per line
   */
  public PatternsFilter(final InputStream input) {
    this(new BufferedReader(new InputStreamReader(input)).lines());
  }

  /**
   * Creates a {@code PatternsFilter} with the given regular expressions, one per line in the named
   * file.
   *
   * <p>Any line that starts with {@code "#"} is discarded as a comment.
   *
   * @param path the path to a file containing regular expressions to addAll, one per line
   */
  public PatternsFilter(final Path path) throws IOException {
    try (var lines = Files.lines(path)) {
      regexps = collectWithoutComments(lines);
    }
    isMatch = compileStringPredicate();
  }

  private Predicate<String> compileStringPredicate() {
    final Predicate<String> isMatch;
    isMatch =
        Pattern.compile(
                regexps.stream()
                    .map(regexp -> String.format("(?:%s)", regexp))
                    .collect(Collectors.joining("|")))
            .asMatchPredicate();
    return isMatch;
  }

  /**
   * Creates a {@code PatternsFilter} with the given regular expressions, one per line in the given
   * file.
   *
   * <p>Any line that starts with {@code "#"} is discarded as a comment.
   *
   * @param file a file containing regular expressions to addAll, one per line
   */
  public PatternsFilter(final File file) throws IOException {
    this(file.toPath());
  }

  /** Helper for collecting regular expressions to form a {@link PatternsFilter}. */
  public static class Builder {

    private final List<String> regexps = new ArrayList<>();

    private Builder() {}

    /**
     * Adds the given regular expression to the collection to be used for an eventual {@link
     * PatternsFilter}.
     *
     * @param regexp the regular expression to addAll
     * @return {@code this}, to allow method chaining
     */
    public Builder add(@Language("RegExp") final String regexp) {
      regexps.add(regexp);
      return this;
    }

    /**
     * Adds the given regular expressions to the collection to be used for an eventual {@link
     * PatternsFilter}.
     *
     * <p>Any line that starts with {@code "#"} is discarded as a comment.
     *
     * @param input the regular expressions to addAll, one per line
     * @return {@code this}, to allow method chaining
     */
    public Builder addAll(final InputStream input) {
      discardComments(new BufferedReader(new InputStreamReader(input)).lines()).forEach(this::add);
      return this;
    }

    /**
     * Creates a {@link PatternsFilter} with the accumulated regular expressions.
     *
     * @return a new {@link PatternsFilter} instance
     */
    public PatternsFilter build() {
      return new PatternsFilter(Collections.unmodifiableList(regexps));
    }
  }

  /**
   * Prepares to build a {@code PatternsFilter}.
   *
   * @return a {@link Builder} that can collect regular expressions and create a new {@code
   *     SetOfClassPatterns}
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean test(final String klassName) {
    return isMatch.test(klassName);
  }

  @Override
  public List<String> toJson() {
    return regexps;
  }

  private void readObject(ObjectInputStream input) throws ClassNotFoundException, IOException {
    input.defaultReadObject();
    isMatch = compileStringPredicate();
  }
}
