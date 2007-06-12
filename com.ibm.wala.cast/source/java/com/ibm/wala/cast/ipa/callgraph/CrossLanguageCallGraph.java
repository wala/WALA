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

import com.ibm.wala.cast.util.*;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.util.Atom;
import com.ibm.wala.shrikeBT.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.warnings.*;

import java.util.*;

/**
 *  A CallGraph implementation adapted to work for graphs that contain
 * code entities from multiple languages, and hence multiple specialized
 * forms of IR.  The root node delegates to one of several language-specific
 * root nodes, allowing each language to use its own specialized IR 
 * constructs for entry points.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class CrossLanguageCallGraph extends AstCallGraph {
  
  public CrossLanguageCallGraph(
	   TargetLanguageSelector<AbstractRootMethod, CrossLanguageCallGraph> roots,
	   IClassHierarchy cha, 
	   AnalysisOptions options)
  {
    super(cha, options);
    this.roots = roots;
  }

  private final TargetLanguageSelector<AbstractRootMethod, CrossLanguageCallGraph> roots;

  private final Set languageRootNodes = new HashSet();

  private final Map languageRoots = new HashMap();

  public AbstractRootMethod getLanguageRoot(Atom language) {
    if (! languageRoots.containsKey(language)) {
      AbstractRootMethod languageRoot = roots.get(language, this);
      
      CGNode languageRootNode = 
	findOrCreateNode(languageRoot, Everywhere.EVERYWHERE);

      languageRootNodes.add( languageRootNode );

      CallSiteReference site = 
	CallSiteReference.make(1, 
			       languageRoot.getReference(),
			       IInvokeInstruction.Dispatch.STATIC);
      
      CGNode fakeRootNode = getFakeRootNode();
      CrossLanguageFakeRoot fakeRootMethod = 
	(CrossLanguageFakeRoot) fakeRootNode.getMethod();

      site =
	fakeRootMethod.addInvocationInternal(new int[0], site).getCallSite();

      fakeRootNode.addTarget(site, languageRootNode);
      
      languageRoots.put(language, languageRoot);
    }

    return (AbstractRootMethod) languageRoots.get(language);
  }

  public class CrossLanguageFakeRoot extends ScriptFakeRoot {
  
    public CrossLanguageFakeRoot(IClass declaringClass, 
				 IClassHierarchy cha, 
				 AnalysisOptions options)
    {
      super(FakeRootMethod.rootMethod, declaringClass, cha, options);
    }

    public CrossLanguageFakeRoot(IClassHierarchy cha, 
				 AnalysisOptions options)
    {
      super(FakeRootMethod.rootMethod, cha, options);
    }

    public int addPhi(TypeReference type, int[] values) {
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addPhi(type, values);
    }

    public int addGetInstance(FieldReference ref, int object) {
      TypeReference type = ref.getDeclaringClass();
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addGetInstance(ref, object);
    }

    public int addGetStatic(FieldReference ref) {
      TypeReference type = ref.getDeclaringClass();
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addGetStatic(ref);
    }

    public int addCheckcast(TypeReference type, int rv) {
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addCheckcast(type, rv);
    }

    public SSANewInstruction 
      addAllocation(TypeReference type, WarningSet warnings)
    {
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addAllocation(type, warnings);
    }

    public SSAInvokeInstruction 
      addInvocation(int[] params, CallSiteReference site) 
    {
      TypeReference type = site.getDeclaredTarget().getDeclaringClass();
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addInvocation(params, site);
    }

    public SSAInvokeInstruction 
      addInvocationInternal(int[] params, CallSiteReference site) 
    {
      return super.addInvocation(params, site);
    }

    public AstLexicalRead addGlobalRead(TypeReference type, String name) {
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return ((AstFakeRoot)root).addGlobalRead(type, name);
    }    

    public SSAAbstractInvokeInstruction 
      addDirectCall(int functionVn, int[] argVns, CallSiteReference callSite) 
    {
      TypeReference type = callSite.getDeclaredTarget().getDeclaringClass();
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return
	((ScriptFakeRoot)root).addDirectCall(functionVn, argVns, callSite);
    }	
  }

  Iterator getLanguageRoots() {
    return languageRootNodes.iterator();
  }

  protected CGNode makeFakeRootNode() {
    return 
      findOrCreateNode(
	new CrossLanguageFakeRoot(cha, options),
	Everywhere.EVERYWHERE);
  }
}
