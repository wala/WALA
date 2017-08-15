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

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.AstCallGraph.ScriptFakeRoot;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;

public abstract class ScriptEntryPoints implements Iterable<Entrypoint> {

  protected final IClassHierarchy cha;

  protected final IClass scriptType;

  public class ScriptEntryPoint extends Entrypoint {
    public ScriptEntryPoint(IMethod scriptCodeBody) {
      super(scriptCodeBody);
    }
      
    @Override
    public CallSiteReference makeSite(int programCounter) {
      return makeScriptSite(getMethod(), programCounter);
    }

    @Override
    public TypeReference[] getParameterTypes(int i) {
      assert i == 0;
      if (getMethod().isStatic()) {
	return new TypeReference[0];
      } else {
	return new TypeReference[] { getMethod().getDeclaringClass().getReference() };
      }
    }

    @Override
    public int getNumberOfParameters() {
      return getMethod().isStatic()? 0: 1;
    }

      
    @Override
    public SSAAbstractInvokeInstruction addCall(AbstractRootMethod m){
      CallSiteReference site = makeSite(0);

      if (site == null) {
        return null;
      }

      int functionVn = getMethod().isStatic()? -1: makeArgument(m, 0);
      int paramVns[] = new int[Math.min(0, getNumberOfParameters() - 1)];
      for (int j = 0; j < paramVns.length; j++) {
        paramVns[j] = makeArgument(m, j + 1);
      }

      return ((ScriptFakeRoot) m).addDirectCall(functionVn, paramVns, site);
    }
  }

  public ScriptEntryPoints(IClassHierarchy cha, IClass scriptType) {
    this.cha = cha;
    this.scriptType = scriptType;
  }

  protected abstract CallSiteReference makeScriptSite(IMethod m, int pc);

  protected boolean keep() {
    return true;
  }
  
  @Override
  public Iterator<Entrypoint> iterator() {
    Set<Entrypoint> ES = HashSetFactory.make();
    Iterator<IClass> classes = scriptType.getClassLoader().iterateAllClasses();
    while (classes.hasNext()) {
      IClass cls = classes.next();
      if (cha.isSubclassOf(cls, scriptType) && !cls.isAbstract()) {
        for (IMethod method : cls.getDeclaredMethods()) {
          if (keep()) {
            ES.add(new ScriptEntryPoint(method));
          }
        }
      }
    }

    return ES.iterator();
  }

  public Entrypoint make(String scriptName) {
    IClass cls = cha.lookupClass(TypeReference.findOrCreate(scriptType.getClassLoader().getReference(), scriptName));
    assert cls != null && cha.isSubclassOf(cls, scriptType) && !cls.isAbstract() : String.valueOf(cls) + " for " + scriptName;
    for (IMethod method : cls.getDeclaredMethods()) {
      if (keep()) {
        return new ScriptEntryPoint(method);
      }
    }
    
    assert false;
    return null;
  }
}
