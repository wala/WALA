/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.basic;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.JGF;
import com.ibm.wala.util.graph.NumberedGraph;
import java.util.Map;
import net.javacrumbs.jsonunit.assertj.JsonAssert;
import net.javacrumbs.jsonunit.assertj.JsonListAssert;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class JGFTest {

  @Test
  public void testJFG() {
    NumberedGraph<String> G = GraphDataflowTest.buildGraph();
    JSONObject JG =
        JGF.toJGF(
            G,
            new JGF.EntityTypes<>() {

              @Override
              public JSONObject obj(String entity) {
                JSONObject x = new JSONObject();
                x.put("name", entity);
                return x;
              }

              @Override
              public String label(String entity) {
                return "" + G.getNumber(entity);
              }

              @Override
              public String label(Graph<String> entity) {
                return "test graph";
              }

              @Override
              public String label(String from, String to) {
                return from + " --> " + to;
              }
            });
    JsonAssert assertThatGraph = assertThatJson(JG).when(IGNORING_EXTRA_FIELDS);
    JsonAssert assertThatNodes = assertThatGraph.node("nodes");
    JsonListAssert assertThatEdges = assertThatGraph.node("edges").isArray();
    assertThat(G)
        .allSatisfy(
            node -> {
              int number = G.getNumber(node);
              assertThatNodes.node(number + ".metadata.name").isEqualTo(node);
              assertThat(G.getSuccNodes(node))
                  .toIterable()
                  .allSatisfy(
                      successor ->
                          assertThatEdges.contains(
                              Map.of(
                                  "source", "" + number, "target", "" + G.getNumber(successor))));
            });
  }
}
