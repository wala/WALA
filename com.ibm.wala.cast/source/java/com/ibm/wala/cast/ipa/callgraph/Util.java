/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ipa.callgraph;


import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

import java.io.*;
import java.net.*;
import java.util.*;

public class Util {

  public static SourceFileModule makeSourceModule(URL script, String dir, String name) {
    // DO NOT use File.separator here, since this name is matched against 
    // URLs.  It seems that, in DOS, URL.getFile() does not return a 
    // \-separated file name, but rather one with /'s.  Rather makes one 
    // wonder why the function is called get_File_ :(
    return makeSourceModule(script, dir + "/" + name);
  }

  public static SourceFileModule makeSourceModule(URL script, String scriptName) {
    String hackedName =
      script.getFile().replaceAll("%5c", "/").replaceAll("%20", " ");

    File scriptFile = new File( hackedName );

    Assertions._assert( hackedName.endsWith( scriptName ), 
      scriptName + " does not match file " + script.getFile());

    return new SourceFileModule( scriptFile, scriptName );
  }

  public static void dumpCG(PropagationCallGraphBuilder builder, CallGraph CG) {
    Trace.println( CG );

    for(Iterator x = CG.iterateNodes(); x.hasNext(); ) {
      CGNode N = (CGNode) x.next();
      Trace.println("\nIR of node " + N);
      IR ir = ((SSAContextInterpreter)CG.getInterpreter(N)).getIR(N, builder.getWarnings());
      if (ir != null) { 
	Trace.println( ir );
      } else {
	Trace.println( "no IR!" );
      }
    }

    PointerAnalysis PA = builder.getPointerAnalysis();
    for(Iterator x = PA.getPointerKeys().iterator(); x.hasNext(); ) {
      PointerKey n = (PointerKey)x.next();
      Trace.println(n + " --> " + PA.getPointsToSet(n));
    }
  }

}
