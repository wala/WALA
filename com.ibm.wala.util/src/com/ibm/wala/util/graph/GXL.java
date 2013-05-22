/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.graph;

import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.util.functions.Function;

public class GXL {

  public interface EntityTypes<T> {
    String type(T entity);
    String type(Graph<T> entity);
    String type(T from, T to);
  }
  
  public static <T> String toGXL(Graph<T> G, 
      EntityTypes<T> types,
      String graphId, 
      Function<T,String> nodeIds, 
      Function<T,Map<String,String>> nodeProperties) 
  {
    StringBuffer sb = new StringBuffer();
    
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<!DOCTYPE gxl SYSTEM \"http://www.gupro.de/GXL/gxl-1.0.dtd\">\n");
    sb.append("<gxl xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n");
    sb.append("  <graph id=\"" + graphId + "\" edgemode=\"directed\" hypergraph=\"false\">\n");
    sb.append("    <type xlink:href=\"" + types.type(G) + "\"/>\n");
    
    for(T n : G) {
      sb.append("    <node id=\"" + nodeIds.apply(n) + "\">\n");
      sb.append("      <type xlink:href=\"" + types.type(n) + "\"/>\n");
      Map<String,String> props = nodeProperties.apply(n);
      if (props != null) {
        for(Map.Entry<String,String> e : props.entrySet()) {
          sb.append("      <attr name=\"" + e.getKey() + "\">\n");
          if (e.getValue() != null) {
            sb.append("        <string>" + e.getValue() + "</string>\n");
          } else {
            sb.append("        <string/>\n");            
          }
          sb.append("      </attr>\n");
        }
      }
      sb.append("    </node>\n");
    }
    
    for(T n : G) {
      Iterator<T> ss = G.getSuccNodes(n);
      while (ss.hasNext()) {
        T s = ss.next();
        sb.append("    <edge from=\"" + nodeIds.apply(n) + "\" to=\"" + nodeIds.apply(s) + "\">\n");
        sb.append("      <type xlink:href=\"" + types.type(n, s) + "\"/>\n");
        
        sb.append("    </edge>\n");
      }
    }
    
    sb.append("  </graph>\n");
    sb.append("</gxl>\n");
    
    return sb.toString();
  }
}
