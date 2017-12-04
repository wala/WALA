/******************************************************************************
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.util;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

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

/**
 * Utility class to serialize call graphs as JSON objects.
 * 
 * The serialised objects have the form
 * 
 * <pre>
 * {
 *   "&lt;callsite1&gt;": [ "&lt;callee1&gt;", "&lt;callee2&gt;", ... ],
 *   "&lt;callsite2&gt;": ... 
 * }
 * </pre>
 * 
 * where both call sites and callees are encoded as strings
 * of the form
 * 
 * <pre>
 * "&lt;filename&gt;@&lt;lineno&gt;:&lt;beginoff&gt;-&lt;endoff&gt;"
 * </pre>
 * 
 * Here, <code>filename</code> is the name of the containing
 * JavaScript file (not including its directory), and
 * <code>lineno</code>, <code>beginoff</code> and <code>endoff</code>
 * encode the source position of the call expression (for
 * call sites) or the function declaration/expression (for
 * callees) inside the file in terms of its starting line,
 * its starting offset (in characters from the beginning of the
 * file), and its end offset.
 * 
 * @author mschaefer
 */
public class CallGraph2JSON {
  public static boolean IGNORE_HARNESS = true;
  
	public static String serialize(CallGraph cg) {
		Map<String, Set<String>> edges = extractEdges(cg);
		return toJSON(edges);
	}

  public static Map<String, Set<String>> extractEdges(CallGraph cg) {
    Map<String, Set<String>> edges = HashMapFactory.make();
		for(CGNode nd : cg) {
			if(!isRealFunction(nd.getMethod()))
				continue;
			AstMethod method = (AstMethod)nd.getMethod();

			for(CallSiteReference callsite : Iterator2Iterable.make(nd.iterateCallSites())) {
        Set<IMethod> targets = Util.mapToSet(cg.getPossibleTargets(nd, callsite), CGNode::getMethod);
				serializeCallSite(method, callsite, targets, edges);
			}
		}
    return edges;
  }
	
  public static void serializeCallSite(AstMethod method, CallSiteReference callsite, Set<IMethod> targets,
      Map<String, Set<String>> edges) {
    Set<String> targetNames = MapUtil.findOrCreateSet(edges, ppPos(method.getSourcePosition(callsite.getProgramCounter())));
    for(IMethod target : targets) {
      target = getCallTargetMethod(target);
    	if(!isRealFunction(target))
    		continue;
    	targetNames.add(ppPos(((AstMethod)target).getSourcePosition()));
    }
  }
	
	private static IMethod getCallTargetMethod(IMethod method) {
    if(method.getName().equals(JavaScriptMethods.ctorAtom)) {
      method = method.getDeclaringClass().getMethod(AstMethodReference.fnSelector);
      if(method != null)
        return method;
    }
	  return method;
	}

	public static boolean isRealFunction(IMethod method) {
		if(method instanceof AstMethod) {
		  String methodName = method.getDeclaringClass().getName().toString();
	    
		  // exclude synthetic DOM modelling functions
	    if(methodName.contains("/make_node"))
	      return false;

      if(IGNORE_HARNESS) {
        for(String bootstrapFile : JavaScriptLoader.bootstrapFileNames)
          if(methodName.startsWith("L" + bootstrapFile + "/"))
            return false;
      }
 
	    return method.getName().equals(AstMethodReference.fnAtom);
		}
		return false;
	}
	
	private static String ppPos(Position pos) {
		String file = pos.getURL().getFile();
		file = file.substring(file.lastIndexOf('/')+1);
		
		int line = pos.getFirstLine(), start_offset = pos.getFirstOffset(), end_offset = pos.getLastOffset();
		return file + "@" + line + ":" + start_offset + "-" + end_offset;
	}
	
	public static String toJSON(Map<String, Set<String>> map) {
		StringBuffer res = new StringBuffer();
		res.append("{\n");
		res.append(joinWith(Util.mapToSet(map.entrySet(), e -> {
      StringBuffer res1 = new StringBuffer();
      if(e.getValue().size() > 0) {
        res1.append("    \"" + e.getKey() + "\": [\n");
        res1.append(joinWith(Util.mapToSet(e.getValue(), str -> "        \"" + str + "\""), ",\n"));
        res1.append("\n    ]");
      }
      return res1.length() == 0 ? null : res1.toString();
    }), ",\n"));
		res.append("\n}");
		return res.toString();
	}
	
	private static String joinWith(Iterable<String> lst, String sep) {
	  StringBuffer res = new StringBuffer();
	  ArrayList<String> strings = new ArrayList<>();
	  for(String s : lst)
	    if(s != null)
	      strings.add(s);
	      
	  boolean fst = true;
	  for(String s : strings) {
	    if(fst)
	      fst = false;
	    else
	      res.append(sep);
	    res.append(s);
	  }
	  return res.toString();
	}
}
