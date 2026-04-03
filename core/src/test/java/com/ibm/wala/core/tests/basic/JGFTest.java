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

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.JGF;
import com.ibm.wala.util.graph.NumberedGraph;

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
    JSONObject nodes = JG.getJSONObject("nodes");
    JSONArray edges = JG.getJSONArray("edges");
    for (String n : G) {
      assert nodes
          .getJSONObject("" + G.getNumber(n))
          .getJSONObject("metadata")
          .getString("name")
          .equals(n);
      G.getSuccNodes(n)
          .forEachRemaining(
              s -> {
                boolean found = false;
                for (int i = 0; i < edges.length(); i++) {
                  JSONObject e = edges.getJSONObject(i);
                  if (e.getString("source").equals("" + G.getNumber(n))
                      && e.getString("target").equals("" + G.getNumber(s))) {
                    found = true;
                  }
                }
                assert found;
              });
    }
  }
}
