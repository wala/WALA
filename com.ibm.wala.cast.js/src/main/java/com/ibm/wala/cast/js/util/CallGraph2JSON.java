/*
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.util;

import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.collections.Util;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to serialize call graphs as JSON objects.
 *
 * <p>The serialised objects have the form
 *
 * <pre>
 * {
 *   "&lt;callsite1&gt;": [ "&lt;callee1&gt;", "&lt;callee2&gt;", ... ],
 *   "&lt;callsite2&gt;": ...
 * }
 * </pre>
 *
 * where both call sites and callees are encoded as strings of the form
 *
 * <pre>
 * "&lt;filename&gt;@&lt;lineno&gt;:&lt;beginoff&gt;-&lt;endoff&gt;"
 * </pre>
 *
 * Here, {@code filename} is the name of the containing JavaScript file (not including its
 * directory), and {@code lineno}, {@code beginoff} and {@code endoff} encode the source position of
 * the call expression (for call sites) or the function declaration/expression (for callees) inside
 * the file in terms of its starting line, its starting offset (in characters from the beginning of
 * the file), and its end offset.
 *
 * @author mschaefer
 */
public class CallGraph2JSON {

  /** ignore any calls to, from, or within WALA's harness containing models of natives methods */
  private final boolean ignoreHarness;

  public CallGraph2JSON() {
    this(true);
  }

  public CallGraph2JSON(boolean ignoreHarness) {
    this.ignoreHarness = ignoreHarness;
  }

  public String serialize(CallGraph cg) {
    Map<String, Set<String>> edges = extractEdges(cg);
    return toJSON(edges);
  }

  public Map<String, Set<String>> extractEdges(CallGraph cg) {
    Map<String, Set<String>> edges = HashMapFactory.make();
    for (CGNode nd : cg) {
      if (!isValidFunctionFromSource(nd.getMethod())) {
        continue;
      }
      IMethod method = nd.getMethod();
      if (ignoreHarness && isHarnessMethod(method)) {
        continue;
      }
      for (CallSiteReference callsite : Iterator2Iterable.make(nd.iterateCallSites())) {
        Set<IMethod> targets =
            Util.mapToSet(cg.getPossibleTargets(nd, callsite), CGNode::getMethod);
        serializeCallSite(method, callsite, targets, edges);
      }
    }
    return edges;
  }

  public void serializeCallSite(
      IMethod caller,
      CallSiteReference callsite,
      Set<IMethod> targets,
      Map<String, Set<String>> edges) {
    Set<String> targetNames =
        MapUtil.findOrCreateSet(
            edges,
            getJSONRep(
                caller,
                caller instanceof AstMethod
                    ? ppPos(((AstMethod) caller).getSourcePosition(callsite.getProgramCounter()))
                    : null));
    for (IMethod target : targets) {
      IMethod trueTarget = getCallTargetMethod(target);
      if (trueTarget == null
          || !isValidFunctionFromSource(trueTarget)
          || (ignoreHarness && isHarnessMethod(trueTarget))) {
        continue;
      }
      targetNames.add(
          getJSONRep(
              trueTarget,
              isFunctionPrototypeCallOrApply(trueTarget)
                  ? null
                  : ppPos(((AstMethod) trueTarget).getSourcePosition())));
    }
  }

  private static String getJSONRep(IMethod method, String srcPos) {
    if (isHarnessMethod(method) || isFunctionPrototypeCallOrApply(method)) {
      // just use the method name; position is meaningless
      String typeName = method.getDeclaringClass().getName().toString();
      if (typeName.lastIndexOf('/') == -1) {
        throw new RuntimeException(typeName + " is weird on Windows");
      }
      return typeName.substring(typeName.lastIndexOf('/') + 1) + " (Native)";
    } else {
      return srcPos;
    }
  }

  private static IMethod getCallTargetMethod(IMethod method) {
    if (method.getName().equals(JavaScriptMethods.ctorAtom)) {
      method = method.getDeclaringClass().getMethod(AstMethodReference.fnSelector);
      if (method != null) return method;
    }
    return method;
  }

  private static boolean isValidFunctionFromSource(IMethod method) {
    if (method instanceof AstMethod) {
      String methodName = method.getDeclaringClass().getName().toString();

      // exclude synthetic DOM modelling functions
      if (methodName.contains("/make_node")) return false;

      return method.getName().equals(AstMethodReference.fnAtom);
    } else if (method.isWalaSynthetic()) {
      if (isFunctionPrototypeCallOrApply(method)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isFunctionPrototypeCallOrApply(IMethod method) {
    String methodName = method.getDeclaringClass().getName().toString();
    return methodName.equals("Lprologue.js/Function_prototype_call")
        || methodName.equals("Lprologue.js/Function_prototype_apply");
  }

  private static boolean isHarnessMethod(IMethod method) {
    String methodName = method.getDeclaringClass().getName().toString();
    for (String bootstrapFile : JavaScriptLoader.bootstrapFileNames) {
      if (methodName.startsWith('L' + bootstrapFile + '/')) {
        return true;
      }
    }
    return false;
  }

  private static String ppPos(Position pos) {
    String file = pos.getURL().getFile();
    file = file.substring(file.lastIndexOf('/') + 1);

    int line = pos.getFirstLine(),
        start_offset = pos.getFirstOffset(),
        end_offset = pos.getLastOffset();
    return file + '@' + line + ':' + start_offset + '-' + end_offset;
  }

  public static String toJSON(Map<String, Set<String>> map) {
    StringBuilder res = new StringBuilder();
    res.append("{\n");
    res.append(
        joinWith(
            Util.mapToSet(
                map.entrySet(),
                e -> {
                  StringBuilder res1 = new StringBuilder();
                  if (e.getValue().size() > 0) {
                    res1.append("    \"").append(e.getKey()).append("\": [\n");
                    res1.append(
                        joinWith(
                            Util.mapToSet(e.getValue(), str -> "        \"" + str + '"'), ",\n"));
                    res1.append("\n    ]");
                  }
                  return res1.length() == 0 ? null : res1.toString();
                }),
            ",\n"));
    res.append("\n}");
    return res.toString();
  }

  private static String joinWith(Iterable<String> lst, String sep) {
    StringBuilder res = new StringBuilder();
    ArrayList<String> strings = new ArrayList<>();
    for (String s : lst) if (s != null) strings.add(s);

    boolean fst = true;
    for (String s : strings) {
      if (fst) fst = false;
      else res.append(sep);
      res.append(s);
    }
    return res.toString();
  }
}
