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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallString;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContextSelector;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MapUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

  /**
   * if true, output JSON that keeps distinct method clones in the underlying call graph separate
   */
  private final boolean exposeContexts;

  public CallGraph2JSON() {
    this(true);
  }

  public CallGraph2JSON(boolean ignoreHarness) {
    this(ignoreHarness, false);
  }

  public CallGraph2JSON(boolean ignoreHarness, boolean exposeContexts) {
    this.ignoreHarness = ignoreHarness;
    this.exposeContexts = exposeContexts;
  }

  public String serialize(CallGraph cg) {
    Map<String, Map<String, Set<String>>> edges = extractEdges(cg);
    return toJSON(edges);
  }

  /**
   * Extract the edges of the given callgraph as a map over strings that is easy to serialize. The
   * map keys are locations of methods. The map values are themselves maps, from call site locations
   * within a method to the (locations of) potential target methods for the call sites.
   */
  public Map<String, Map<String, Set<String>>> extractEdges(CallGraph cg) {
    Map<String, Map<String, Set<String>>> edges = HashMapFactory.make();
    for (CGNode nd : cg) {
      if (!isValidFunctionFromSource(nd.getMethod())) {
        continue;
      }
      IMethod method = nd.getMethod();
      if (ignoreHarness && isHarnessMethod(method)) {
        continue;
      }
      Map<String, Set<String>> edgesForMethod =
          MapUtil.findOrCreateMap(edges, getJSONRepForNode(nd.getMethod(), nd.getContext()));
      for (CallSiteReference callsite : Iterator2Iterable.make(nd.iterateCallSites())) {
        serializeCallSite(nd, callsite, cg.getPossibleTargets(nd, callsite), edgesForMethod);
      }
    }
    return edges;
  }

  public void serializeCallSite(
      CGNode nd, CallSiteReference callsite, Set<CGNode> targets, Map<String, Set<String>> edges) {
    Set<String> targetNames =
        MapUtil.findOrCreateSet(
            edges, getJSONRepForCallSite(nd.getMethod(), nd.getContext(), callsite));
    for (CGNode target : targets) {
      IMethod trueTarget = getCallTargetMethod(target.getMethod());
      if (trueTarget == null
          || !isValidFunctionFromSource(trueTarget)
          || (ignoreHarness && isHarnessMethod(trueTarget))) {
        continue;
      }
      targetNames.add(getJSONRepForNode(trueTarget, target.getContext()));
    }
  }

  private String getJSONRepForNode(IMethod method, Context context) {
    String result;
    if (isHarnessMethod(method) || isFunctionPrototypeCallOrApply(method)) {
      // just use the method name; position is meaningless
      result = getNativeMethodName(method);
    } else {
      AstMethod astMethod = (AstMethod) method;
      result = astMethod.getSourcePosition().prettyPrint();
    }
    if (exposeContexts) {
      result += getContextString(context);
    }
    return result;
  }

  private String getContextString(Context context) {
    if (context.equals(Everywhere.EVERYWHERE)) {
      return "";
    } else if (context instanceof CallStringContext) {
      CallStringContext cs = (CallStringContext) context;
      CallString callString = (CallString) cs.get(CallStringContextSelector.CALL_STRING);
      CallSiteReference csRef = callString.getCallSiteRefs()[0];
      IMethod callerMethod = callString.getMethods()[0];
      return " [" + getJSONRepForCallSite(callerMethod, Everywhere.EVERYWHERE, csRef) + "]";
    } else {
      throw new RuntimeException(context.toString());
    }
  }

  private static String getNativeMethodName(IMethod method) {
    String typeName = method.getDeclaringClass().getName().toString();
    return typeName.substring(typeName.lastIndexOf('/') + 1) + " (Native)";
  }

  private String getJSONRepForCallSite(IMethod method, Context context, CallSiteReference site) {
    String result;
    if (isHarnessMethod(method) || isFunctionPrototypeCallOrApply(method)) {
      result = getNativeMethodName(method);
    } else {
      AstMethod astMethod = (AstMethod) method;
      result = astMethod.getSourcePosition(site.getProgramCounter()).prettyPrint();
    }
    if (exposeContexts) {
      result += getContextString(context);
    }
    return result;
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

  /**
   * Converts a call graph map produced by {@link #extractEdges(CallGraph)} to JSON, eliding call
   * sites with no targets.
   */
  public static String toJSON(Map<String, Map<String, Set<String>>> map) {
    // strip out call sites with no targets
    Map<String, Map<String, Set<String>>> filtered = new HashMap<>();
    for (Map.Entry<String, Map<String, Set<String>>> entry : map.entrySet()) {
      String methodLoc = entry.getKey();
      Map<String, Set<String>> callSites = entry.getValue();
      Map<String, Set<String>> filteredSites =
          callSites.entrySet().stream()
              .filter(e -> !e.getValue().isEmpty())
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      if (!filteredSites.isEmpty()) {
        filtered.put(methodLoc, filteredSites);
      }
    }
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(filtered);
  }
}
