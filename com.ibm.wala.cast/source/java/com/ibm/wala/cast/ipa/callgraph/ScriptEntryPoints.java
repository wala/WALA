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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.AstCallGraph.ScriptFakeRoot;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WarningSet;

public abstract class ScriptEntryPoints implements Iterable<Entrypoint> {

  private final ClassHierarchy cha;

  private final IClass scriptType;

  private class ScriptEntryPoint extends Entrypoint {
    ScriptEntryPoint(IMethod scriptCodeBody) {
      super(scriptCodeBody);
    }

    public TypeReference[] getParameterTypes(int i) {
      Assertions._assert(i == 0);
      return new TypeReference[] { getMethod().getDeclaringClass().getReference() };
    }

    public int getNumberOfParameters() {
      return 1;
    }

    public SSAAbstractInvokeInstruction addCall(FakeRootMethod m, WarningSet warnings) {
      CallSiteReference site = makeSite(0);

      if (site == null) {
        return null;
      }

      int functionVn = makeArgument(m, 0, warnings);
      int paramVns[] = new int[getNumberOfParameters() - 1];
      for (int j = 0; j < paramVns.length; j++) {
        paramVns[j] = makeArgument(m, j + 1, warnings);
      }

      return ((ScriptFakeRoot) m).addDirectCall(functionVn, paramVns, site);
    }
  }

  public ScriptEntryPoints(ClassHierarchy cha, IClass scriptType) {
    this.cha = cha;
    this.scriptType = scriptType;
  }

  public Iterator<Entrypoint> iterator() {
    Set<Entrypoint> ES = new HashSet<Entrypoint>();
    Iterator<IClass> classes = scriptType.getClassLoader().iterateAllClasses();
    while (classes.hasNext()) {
      IClass cls = classes.next();
      if (cha.isSubclassOf(cls, scriptType)) {
        for (Iterator<IMethod> methods = cls.getDeclaredMethods().iterator(); methods.hasNext();) {
          ES.add(new ScriptEntryPoint(((IMethod) methods.next())));
        }
      }
    }

    return ES.iterator();
  }

}
