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
package com.ibm.wala.cast.java.ipa.callgraph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys;
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.JavaClass;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.LexicalParent;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.util.debug.*;

public class JavaScopeMappingInstanceKeys extends ScopeMappingInstanceKeys {

  

  public JavaScopeMappingInstanceKeys(ClassHierarchy cha,
				      PropagationCallGraphBuilder builder, 
				      InstanceKeyFactory basic)
  {
    super(builder, basic);
    
  }

  protected LexicalParent[] getParents(InstanceKey base) {
    IClass cls = base.getConcreteType();
    if (cls instanceof JavaClass) {
      try {
	Set result = new HashSet();

	for(Iterator MS = cls.getAllMethods().iterator(); MS.hasNext(); ) {
	  IMethod m = (IMethod)MS.next();
	  if ((m instanceof AstMethod) && !m.isStatic()) {
	    AstMethod M = (AstMethod)m;
	    LexicalParent[] parents = M.getParents();
	    for(int i = 0; i < parents.length; i++) {
	      result.add( parents[i] );
	    }
	  }
	}
	
	if (! result.isEmpty()) {
	  if (AstTranslator.DEBUG_LEXICAL) 
	    Trace.println(base + " has parents: " + result);

	  return (LexicalParent[]) result.toArray(new LexicalParent[result.size()]);
	}
      } catch (ClassHierarchyException e) {
	Assertions.UNREACHABLE();
      }
    }

    if (AstTranslator.DEBUG_LEXICAL)
      Trace.println(base + " has no parents");

    return new LexicalParent[0];
  }

  protected boolean needsScopeMappingKey(InstanceKey base) {
    boolean result = getParents(base).length > 0;
    if (AstTranslator.DEBUG_LEXICAL) 
      Trace.println("does " + base + " need scope mapping? " + result);

    return result;
  }
}
