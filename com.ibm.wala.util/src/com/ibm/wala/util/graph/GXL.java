/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.graph;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public class GXL {

  public interface EntityTypes<T> {
    String type(T entity);

    String type(Graph<T> entity);

    String type(T from, T to);
  }

  public static <T> String toGXL(
      Graph<T> G,
      EntityTypes<T> types,
      String graphId,
      Function<T, String> nodeIds,
      Function<T, Map<String, String>> nodeProperties) {
    StringBuilder sb = new StringBuilder();

    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<!DOCTYPE gxl SYSTEM \"http://www.gupro.de/GXL/gxl-1.0.dtd\">\n");
    sb.append("<gxl xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n");
    sb.append("  <graph id=\"")
        .append(graphId)
        .append("\" edgemode=\"directed\" hypergraph=\"false\">\n");
    sb.append("    <type xlink:href=\"").append(types.type(G)).append("\"/>\n");

    for (T n : G) {
      sb.append("    <node id=\"").append(nodeIds.apply(n)).append("\">\n");
      sb.append("      <type xlink:href=\"").append(types.type(n)).append("\"/>\n");
      Map<String, String> props = nodeProperties.apply(n);
      if (props != null) {
        for (Map.Entry<String, String> e : props.entrySet()) {
          sb.append("      <attr name=\"").append(e.getKey()).append("\">\n");
          if (e.getValue() != null) {
            sb.append("        <string>").append(e.getValue()).append("</string>\n");
          } else {
            sb.append("        <string/>\n");
          }
          sb.append("      </attr>\n");
        }
      }
      sb.append("    </node>\n");
    }

    for (T n : G) {
      Iterator<T> ss = G.getSuccNodes(n);
      while (ss.hasNext()) {
        T s = ss.next();
        sb.append("    <edge from=\"")
            .append(nodeIds.apply(n))
            .append("\" to=\"")
            .append(nodeIds.apply(s))
            .append("\">\n");
        sb.append("      <type xlink:href=\"").append(types.type(n, s)).append("\"/>\n");

        sb.append("    </edge>\n");
      }
    }

    sb.append("  </graph>\n");
    sb.append("</gxl>\n");

    return sb.toString();
  }
}
