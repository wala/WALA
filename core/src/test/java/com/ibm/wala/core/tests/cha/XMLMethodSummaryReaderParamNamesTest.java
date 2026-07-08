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
package com.ibm.wala.core.tests.cha;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.XMLMethodSummaryReader;
import com.ibm.wala.types.MethodReference;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link XMLMethodSummaryReader} retains legitimate parameter names that merely begin
 * with {@code arg} (e.g. {@code args}, {@code argv}) in a summary's value-name table, while still
 * dropping the synthetic positional symbols ({@code arg0}, {@code arg1}, ...). A parameter whose
 * name is dropped has no value name, so a keyword argument of that name cannot bind to it.
 */
public class XMLMethodSummaryReaderParamNamesTest extends WalaTestCase {

  /** A method whose declared parameter names include ones beginning with {@code arg}. */
  private static final String XML_WITH_ARG_PARAM_NAMES =
      """
      <summary-spec>
        <classloader name="Synthetic">
          <class name="Run" allocatable="true">
            <method name="do" descriptor="()LRoot;" numArgs="4" paramNames="self fn args argv">
              <return value="arg1"/>
            </method>
          </class>
        </classloader>
      </summary-spec>
      """;

  /** A method that declares no parameter names, so its value-name table should be empty. */
  private static final String XML_WITHOUT_PARAM_NAMES =
      """
      <summary-spec>
        <classloader name="Synthetic">
          <class name="NoNames" allocatable="true">
            <method name="do" descriptor="()LRoot;" numArgs="3">
              <return value="arg1"/>
            </method>
          </class>
        </classloader>
      </summary-spec>
      """;

  private static AnalysisScope scope() throws IOException {
    return CallGraphTestUtil.makeJ2SEAnalysisScope(
        TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
  }

  private static MethodSummary onlySummary(String xml) throws IOException {
    XMLMethodSummaryReader reader =
        new XMLMethodSummaryReader(
            new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), scope());
    Map<MethodReference, MethodSummary> summaries = reader.getSummaries();
    assertThat(summaries).hasSize(1);
    return summaries.values().iterator().next();
  }

  /**
   * A parameter named {@code args} or {@code argv} keeps its value name; only the synthetic {@code
   * arg0}/{@code arg1} symbols are filtered, and those are overwritten by the declared names
   * anyway.
   */
  @Test
  public void testArgPrefixedParameterNamesAreRetained() throws IOException {
    Map<Integer, Atom> valueNames = onlySummary(XML_WITH_ARG_PARAM_NAMES).getValueNames();

    // v0 is reserved for "unknown"; v1 is the first parameter, and so on.
    assertThat(valueNames).containsEntry(1, Atom.findOrCreateUnicodeAtom("self"));
    assertThat(valueNames).containsEntry(2, Atom.findOrCreateUnicodeAtom("fn"));
    assertThat(valueNames).containsEntry(3, Atom.findOrCreateUnicodeAtom("args"));
    assertThat(valueNames).containsEntry(4, Atom.findOrCreateUnicodeAtom("argv"));
  }

  /**
   * Without declared parameter names, the synthetic {@code arg0}/{@code arg1} symbols are unnamed.
   */
  @Test
  public void testSyntheticArgSymbolsAreNotNamed() throws IOException {
    assertThat(onlySummary(XML_WITHOUT_PARAM_NAMES).getValueNames()).isEmpty();
  }
}
