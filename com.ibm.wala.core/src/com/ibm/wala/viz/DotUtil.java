/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.viz;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeDecorator;
import com.ibm.wala.util.warnings.WalaException;

/**
 * utilities for interfacing with DOT
 * 
 * @author sfink
 * 
 */
public class DotUtil {

  /**
   * @param g
   * @param labels
   * @throws WalaException
   * @throws IllegalArgumentException  if g is null
   */
  public static <T> void dotify(Graph<T> g, NodeDecorator labels, String dotFile, String psFile, String dotExe) throws WalaException {
    if (g == null) {
      throw new IllegalArgumentException("g is null");
    }
    File f = DotUtil.writeDotFile(g, labels, dotFile);
    spawnDot(dotExe, psFile, f);
  }
  
  public static void spawnDot(String dotExe, String psFile, File dotFile) throws WalaException {
    if (dotFile == null) {
      throw new IllegalArgumentException("dotFile is null");
    }
    String[] cmdarray = { dotExe, "-Tps", "-o", psFile, "-v", dotFile.getAbsolutePath() };
    System.out.println("spawning process " + Arrays.toString(cmdarray));
    try {
      Process p = Runtime.getRuntime().exec(cmdarray);
      BufferedInputStream output = new BufferedInputStream(p.getInputStream());
      BufferedInputStream error = new BufferedInputStream(p.getErrorStream());
      boolean repeat = true;
      while (repeat) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e1) {
          e1.printStackTrace();
          // just ignore and continue
        }
        if (output.available() > 0) {
          byte[] data = new byte[output.available()];
          int nRead = output.read(data);
          System.err.println("read " + nRead + " bytes from output stream");
        }
        if (error.available() > 0) {
          byte[] data = new byte[error.available()];
          int nRead = error.read(data);
          System.err.println("read " + nRead + " bytes from error stream");
        }
        try {
          p.exitValue();
          // if we get here, the process has terminated
          repeat = false;
          System.out.println("process terminated with exit code " + p.exitValue());
        } catch (IllegalThreadStateException e) {
          // this means the process has not yet terminated.
          repeat = true;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new WalaException("IOException in " + DotUtil.class);
    }
  }
  
  public static <T> File writeDotFile(Graph<T> g, NodeDecorator labels, String dotfile) throws WalaException {

    if (g == null) {
      throw new IllegalArgumentException("g is null");
    }
    StringBuffer dotStringBuffer = dotOutput(g, labels);

    // retrieve the filename parameter to this component, a String
    if (dotfile == null) {
      throw new WalaException("internal error: null filename parameter");
    }
    try {
      File f = new File(dotfile);
      FileWriter fw = new FileWriter(f);
      fw.write(dotStringBuffer.toString());
      fw.close();
      return f;

    } catch (Exception e) {
      throw new WalaException("Error writing dot file " + dotfile);
    }
  }

  /**
   * @return StringBuffer holding dot output representing G
   * @throws WalaException
   */
  private static <T> StringBuffer dotOutput(Graph<T> g, NodeDecorator labels) throws WalaException {
    StringBuffer result = new StringBuffer("digraph \"DirectedGraph\" {\n");

    String rankdir = getRankDir();
    if (rankdir != null) {
      result.append("rankdir=" + rankdir + ";");
    }
    result.append("center=true;fontsize=12;node [fontsize=12];edge [fontsize=12]; \n");

    Collection dotNodes = computeDotNodes(g);
    // if (getClustersInput().size() > 0) {
    // int i = 0;
    // for (Iterator it = getClustersInput().iterator(); it.hasNext(); ) {
    // ECluster cluster = (ECluster)it.next();
    // result.append("\nsubgraph cluster_" + i + " {\n");
    // result.append("label=\"");
    // result.append(cluster.getName());
    // result.append("\"\n");
    // i++;
    // for (Iterator it2 = cluster.getContents().iterator(); it2.hasNext(); ) {
    // Object n = it2.next();
    // outputNode(labels,result,n);
    // dotNodes.remove(n);
    // }
    // result.append("}\n");
    // }
    // }
    outputNodes(labels, result, dotNodes);

    for (Iterator<? extends T> it = g.iterator(); it.hasNext();) {
      T n = it.next();
      for (Iterator<? extends T> it2 = g.getSuccNodes(n); it2.hasNext();) {
        T s = it2.next();
        result.append(" ");
        result.append(getPort(n, labels));
        result.append(" -> ");
        result.append(getPort(s, labels));
        result.append(" \n");
      }
    }

    // if (usingClusters()) {
    // for (Iterator it = dotNodes.iterator(); it.hasNext();) {
    // ECluster n = (ECluster) it.next();
    // for (Iterator it2 = getRecordsGraphInput().getSuccNodes(n);
    // it2.hasNext();) {
    // Object s = it2.next();
    // result.append(" ");
    // result.append("\"" + getLabel(n, labels) + "\":f0");
    // result.append(" -> ");
    // result.append("\"" + getLabel(s, labels) + "\":f0");
    // result.append(" [color=blue]");
    // result.append(" \n");
    // }
    // }
    // }

    result.append("\n}");
    return result;
  }

  /**
   * @param labels
   * @param result
   * @param dotNodes
   * @throws WalaException
   */
  private static void outputNodes(NodeDecorator labels, StringBuffer result, Collection dotNodes) throws WalaException {
    for (Iterator it = dotNodes.iterator(); it.hasNext();) {
      outputNode(labels, result, it.next());
    }
  }

  private static void outputNode(NodeDecorator labels, StringBuffer result, Object n) throws WalaException {
    result.append("   ");
    result.append("\"");
    result.append(getLabel(n, labels));
    result.append("\"");
    result.append(decorateNode(n, labels));
  }

  /**
   * Compute the nodes to visualize .. these may be clusters
   * 
   */
  private static <T> Collection<T> computeDotNodes(Graph<T> g) throws WalaException {
    return Iterator2Collection.toCollection(g.iterator());
    // if (!usingClusters()) {
    // return new Iterator2Collection(getGraphInput().iterateNodes());
    // } else {
    // computeFieldInfo();
    // return new Iterator2Collection(getRecordsGraphInput().iterateNodes());
    // }
  }

  //
  // private boolean usingClusters() {
  // return getRecordsGraphInput() != null;
  // }

  private static String getRankDir() throws WalaException {
    // GraphLayout l = getLayout();
    // switch (l.getValue()) {
    // case GraphLayout.LEFT_TO_RIGHT:
    // return "LR";
    // case GraphLayout.TOP_TO_BOTTOM:
    // return null;
    // default:
    // throw new WalaException("Unexpected layout");
    //
    // }
    return null;
  }

  /**
   * @param n node to decorate
   * @param d decorating master
   */
  private static String decorateNode(Object n, NodeDecorator d) throws WalaException {
    StringBuffer result = new StringBuffer();

    // if (n instanceof ECluster) {
    // ECluster c = (ECluster) n;
    // result.append(" [shape=\"record\" color=\"blue\"");
    // result.append(" label = \"<f0> " + c.getName());
    // for (Iterator it = c.getContents().iterator(); it.hasNext();) {
    // Object field = it.next();
    // FieldInRecord info = (FieldInRecord) fieldInfo.get(field);
    // result.append(" | <f");
    // result.append(info.fieldNumber);
    // result.append("> ");
    // result.append(getLabel(field, d));
    // }
    // result.append("\"");
    // result.append("] \n");
    // } else {
    // result.append(" [shape=\"box\" color=\"blue\"");
    // result.append("] \n");
    // }
    result.append(" [shape=\"box\" color=\"blue\"");
    result.append("] \n");
    return result.toString();
  }

//  private Map fieldInfo = new HashMap();

  // private void computeFieldInfo() {
  // for (Iterator it = getRecordsGraphInput().iterateNodes(); it.hasNext();) {
  // ECluster c = (ECluster) it.next();
  // int i = 1;
  // for (Iterator it2 = c.getContents().iterator(); it2.hasNext();) {
  // Object field = it2.next();
  // fieldInfo.put(field, new FieldInRecord(c, i++));
  // }
  // }
  // }

  // private class FieldInRecord {
  // ECluster record;
  //
  // int fieldNumber;
  //
  // FieldInRecord(ECluster record, int fieldNumber) {
  // this.record = record;
  // this.fieldNumber = fieldNumber;
  // }
  // }
  //
  private static String getLabel(Object o, NodeDecorator d) throws WalaException {
    if (d == null) {
      return o.toString();
    } else {
      String result = d.getLabel(o);
      return result == null ? o.toString() : result;
    }
  }

  private static String getPort(Object o, NodeDecorator d) throws WalaException {
    // if (!usingClusters()) {
    return "\"" + getLabel(o, d) + "\"";
    // } else {
    // FieldInRecord info = (FieldInRecord) fieldInfo.get(o);
    // String clusterLabel = getLabel(info.record, d);
    // return "\"" + clusterLabel + "\":f" + info.fieldNumber;
    // }
  }

}
