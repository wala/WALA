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
package com.ibm.wala.cast.js.test;

import com.ibm.wala.util.debug.Trace;

import com.ibm.wala.util.collections.*;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.warnings.WarningSet;

import java.util.*;

import junit.framework.Assert;

public abstract class TestCallGraphShape extends WalaTestCase {

  protected static class Name {
    String name;
    int instructionIndex;
    int vn;
    Name(int vn, int instructionIndex, String name) {
      this.vn = vn;
      this.name = name;
      this.instructionIndex = instructionIndex;
    }
  }

  protected void verifyNameAssertions(CallGraph CG, Object[][] assertionData) {
    WarningSet W = new WarningSet();
    for(int i = 0; i < assertionData.length; i++) {
      Iterator NS = Util.getNodes(CG, (String)assertionData[i][0]).iterator();
      while ( NS.hasNext() ) {
	CGNode N = (CGNode) NS.next();
	IR ir = ((SSAContextInterpreter)CG.getInterpreter(N)).getIR(N, W);
	Name[] names = (Name[]) assertionData[i][1];
	for(int j = 0; j < names.length; j++) {

	  Trace.println("looking for " + names[j].name + ", " + names[j].vn + " in " + N);

	  String[] localNames = ir.getLocalNames( names[j].instructionIndex, names[j].vn );

	  boolean found = false;
	  for(int k = 0; k < localNames.length; k++) {
	    if ( localNames[k].equals(names[j].name) ) {
	      found = true;
	    }
	  }

	  Assert.assertTrue("no name " + names[j].name + " for " + N, found);
	}
      }
    }
  }

  protected void verifyGraphAssertions(CallGraph CG, Object[][] assertionData) {    Trace.println( CG );

    for(int i = 0; i < assertionData.length; i++) {
	
    check_target: 
      for(int j = 0; j < ((String[])assertionData[i][1]).length; j++) {
	Iterator srcs = 
	    (assertionData[i][0] instanceof String)?
	    Util.getNodes(CG, (String)assertionData[i][0]).iterator():
	    new NonNullSingletonIterator( CG.getFakeRootNode() );
	Assert.assertTrue("cannot find " + assertionData[i][0], srcs.hasNext());
      
	while (srcs.hasNext()) {
	  CGNode src = (CGNode)srcs.next();
	  for(Iterator sites = src.iterateSites(); sites.hasNext(); ) {
	    CallSiteReference sr = (CallSiteReference) sites.next();
	  
	    Iterator dsts = Util.getNodes(CG, ((String[])assertionData[i][1])[j]).iterator();
	    Assert.assertTrue("cannot find " + ((String[])assertionData[i][1])[j], dsts.hasNext());
            
	    while (dsts.hasNext()) {
	      CGNode dst = (CGNode)dsts.next();
	      for(Iterator tos = src.getPossibleTargets(sr).iterator();
		  tos.hasNext(); )
	      {
		if (tos.next().equals(dst)) {
		  Trace.println("found expected " + src + " --> " + dst + " at " + sr);
		  continue check_target;
		}
	      }
	    }
	  }
	}
	
	Assert.assertTrue("cannot find edge " + assertionData[i][0] + " ---> " + ((String[])assertionData[i][1])[j], false);
      }
    }
  }

  protected static final Object ROOT = new Object();

}

