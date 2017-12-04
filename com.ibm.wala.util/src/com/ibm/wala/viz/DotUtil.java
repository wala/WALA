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
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.Graph;

/**
 * utilities for interfacing with DOT
 */
public class DotUtil {

  /**
   * possible output formats for dot
   * 
   */
  public static enum DotOutputType {
    PS("ps"),
    SVG("svg"),
    PDF("pdf"),
    EPS("eps");

    public final String suffix;

    DotOutputType(final String suffix) {
      this.suffix = suffix;
    }
  }

  private static DotOutputType outputType = DotOutputType.PDF;
  
  private static int fontSize = 6;
  private static String fontColor = "black";
  private static String fontName = "Arial";

  public static void setOutputType(DotOutputType outType) {
    outputType = outType;
  }

  public static DotOutputType getOutputType() {
    return outputType;
  }

  private static String outputTypeCmdLineParam() {
    return "-T" + outputType.suffix;
  }

  /**
   * Some versions of dot appear to croak on long labels. Reduce this if so.
   */
  private final static int MAX_LABEL_LENGTH = Integer.MAX_VALUE;


  /**
   * @param <T> the type of a graph node
   */
  public static <T> void dotify(Graph<T> g, NodeDecorator<T> labels, String dotFile, String outputFile, String dotExe)
    throws WalaException {
    dotify(g, labels, null, dotFile, outputFile, dotExe);
  }

  /**
   * @param <T> the type of a graph node
   */
  public static <T> void dotify(Graph<T> g, NodeDecorator<T> labels, String title, String dotFile, String outputFile, String dotExe)
      throws WalaException {
    if (g == null) {
      throw new IllegalArgumentException("g is null");
    }
    File f = DotUtil.writeDotFile(g, labels, title, dotFile);
    if (dotExe != null) {
      spawnDot(dotExe, outputFile, f);
    }
  }

  public static void spawnDot(String dotExe, String outputFile, File dotFile) throws WalaException {
    if (dotFile == null) {
      throw new IllegalArgumentException("dotFile is null");
    }
    String[] cmdarray = { dotExe, outputTypeCmdLineParam(), "-o", outputFile, "-v", dotFile.getAbsolutePath() };
    System.out.println("spawning process " + Arrays.toString(cmdarray));
    BufferedInputStream output = null;
    BufferedInputStream error = null;
    try {
      Process p = Runtime.getRuntime().exec(cmdarray);
      output = new BufferedInputStream(p.getInputStream());
      error = new BufferedInputStream(p.getErrorStream());
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
    } finally {
      if (output != null) {
        try {
          output.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (error != null) {
        try {
          error.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static <T> File writeDotFile(Graph<T> g, NodeDecorator<T> labels, String title, String dotfile) throws WalaException {

    if (g == null) {
      throw new IllegalArgumentException("g is null");
    }
    StringBuffer dotStringBuffer = dotOutput(g, labels, title);

    // retrieve the filename parameter to this component, a String
    if (dotfile == null) {
      throw new WalaException("internal error: null filename parameter");
    }
    try {
      File f = new File(dotfile);
      try (Writer fw = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
        fw.write(dotStringBuffer.toString());
      }
      return f;

    } catch (Exception e) {
      throw new WalaException("Error writing dot file " + dotfile, e);
    }
  }

  /**
   * @return StringBuffer holding dot output representing G
   * @throws WalaException
   */
  public static <T> StringBuffer dotOutput(Graph<T> g, NodeDecorator<T> labels, String title) throws WalaException {
    StringBuffer result = new StringBuffer("digraph \"DirectedGraph\" {\n");

    if (title != null) {
      result.append("graph [label = \""+title+"\", labelloc=t, concentrate = true];");
    } else {
      result.append("graph [concentrate = true];");
    }
    
    String rankdir = getRankDir();
    if (rankdir != null) {
      result.append("rankdir=" + rankdir + ";");
    }
    String fontsizeStr = "fontsize=" + fontSize;
    String fontcolorStr = (fontColor != null) ? ",fontcolor="+fontColor : "";
    String fontnameStr = (fontName != null) ? ",fontname="+fontName : "";
         
    result.append("center=true;");
    result.append(fontsizeStr);
    result.append(";node [ color=blue,shape=\"box\"");
    result.append(fontsizeStr);
    result.append(fontcolorStr);
    result.append(fontnameStr);
    result.append("];edge [ color=black,");
    result.append(fontsizeStr);
    result.append(fontcolorStr);
    result.append(fontnameStr);
    result.append("]; \n");

    Collection<T> dotNodes = computeDotNodes(g);

    outputNodes(labels, result, dotNodes);

    for (T n : g) {
      for (T s : Iterator2Iterable.make(g.getSuccNodes(n))) {
        result.append(" ");
        result.append(getPort(n, labels));
        result.append(" -> ");
        result.append(getPort(s, labels));
        result.append(" \n");
      }
    }

    result.append("\n}");
    return result;
  }

  private static <T> void outputNodes(NodeDecorator<T> labels, StringBuffer result, Collection<T> dotNodes) throws WalaException {
    for (T t : dotNodes) {
      outputNode(labels, result, t);
    }
  }

  private static <T> void outputNode(NodeDecorator<T> labels, StringBuffer result, T n) throws WalaException {
    result.append("   ");
    result.append("\"");
    result.append(getLabel(n, labels));
    result.append("\"");
    result.append(decorateNode(n, labels));
  }

  /**
   * Compute the nodes to visualize
   */
  private static <T> Collection<T> computeDotNodes(Graph<T> g) {
    return Iterator2Collection.toSet(g.iterator());
  }

  private static String getRankDir() {
    return null;
  }

  /**
   * @param n node to decorate
   * @param d decorating master
   */
  private static <T> String decorateNode(T n, NodeDecorator<T> d) throws WalaException {
    StringBuffer result = new StringBuffer();
    result.append(" [ label=\"");
    result.append(getLabel(n, d));
    result.append("\"]\n");
    return result.toString();
  }

  private static <T> String getLabel(T n, NodeDecorator<T> d) throws WalaException {
    String result = null;
    if (d == null) {
      result = n.toString();
    } else {
      result = d.getLabel(n);
      result = result == null ? n.toString() : result;
    }
    if (result.length() >= MAX_LABEL_LENGTH) {
      result = result.substring(0, MAX_LABEL_LENGTH - 3) + "...";
    }
    return result;
  }

  private static <T> String getPort(T n, NodeDecorator<T> d) throws WalaException {
    return "\"" + getLabel(n, d) + "\"";

  }

  public static int getFontSize() {
    return fontSize;
  }

  public static void setFontSize(int fontSize) {
    DotUtil.fontSize = fontSize;
  }

}
