/*
 * Copyright (c) 2022 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.librarymodelsloader;

import static com.uber.nullaway.LibraryModels.MethodRef.methodRef;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.uber.nullaway.LibraryModels;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

@AutoService(LibraryModels.class)
public class LibraryModelsLoader implements LibraryModels {

  public final String NULLABLE_METHOD_LIST_FILE_NAME = "nullable-methods.tsv";
  public final ImmutableSet<MethodRef> nullableMethods;

  // Assuming this constructor will be called when picked by service loader
  public LibraryModelsLoader() {
    this.nullableMethods = parseTSVFileFromResourcesToMethodRef(NULLABLE_METHOD_LIST_FILE_NAME);
  }

  /**
   * Loads a file from resources and parses the content into set of {@link
   * com.uber.nullaway.LibraryModels.MethodRef}.
   *
   * @param name File name in resources.
   * @return ImmutableSet of content in the passed file. Returns empty if the file does not exist.
   */
  private ImmutableSet<MethodRef> parseTSVFileFromResourcesToMethodRef(String name) {
    // Check if resource exists
    if (getClass().getResource(name) == null) {
      return ImmutableSet.of();
    }
    try (InputStream is = getClass().getResourceAsStream(name)) {
      if (is == null) {
        return ImmutableSet.of();
      }
      ImmutableSet.Builder<MethodRef> contents = ImmutableSet.builder();
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
      String line = reader.readLine();
      while (line != null) {
        String[] values = line.split("\\t");
        contents.add(methodRef(values[0], values[1]));
        line = reader.readLine();
      }
      return contents.build();
    } catch (IOException e) {
      throw new RuntimeException("Error while reading content of resource: " + name, e);
    }
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> failIfNullParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> nonNullParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> nullImpliesTrueParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> nullImpliesFalseParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> nullImpliesNullParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSet<MethodRef> nullableReturns() {
    return nullableMethods;
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> explicitlyNullableParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSet<MethodRef> nonNullReturns() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> castToNonNullMethods() {
    return ImmutableSetMultimap.of();
  }
}
