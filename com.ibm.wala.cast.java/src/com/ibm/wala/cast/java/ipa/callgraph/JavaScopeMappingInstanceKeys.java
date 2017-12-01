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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys;
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.JavaClass;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.LexicalParent;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;

public class JavaScopeMappingInstanceKeys extends ScopeMappingInstanceKeys {

  public JavaScopeMappingInstanceKeys(PropagationCallGraphBuilder builder, InstanceKeyFactory basic) {
    super(builder, basic);

  }

  protected LexicalParent[] getParents(InstanceKey base) {
    IClass cls = base.getConcreteType();
    if (isPossiblyLexicalClass(cls)) {
      Set<LexicalParent> result = HashSetFactory.make();

      for (IMethod m : cls.getAllMethods()) {
        if ((m instanceof AstMethod) && !m.isStatic()) {
          AstMethod M = (AstMethod) m;
          LexicalParent[] parents = M.getParents();
          for (LexicalParent parent : parents) {
            result.add(parent);
          }
        }
      }

      if (!result.isEmpty()) {
        if (AstTranslator.DEBUG_LEXICAL)
          System.err.println((base + " has parents: " + result));

        return result.toArray(new LexicalParent[result.size()]);
      }

    }

    if (AstTranslator.DEBUG_LEXICAL)
      System.err.println((base + " has no parents"));

    return new LexicalParent[0];
  }

  protected boolean isPossiblyLexicalClass(IClass cls) {
    return cls instanceof JavaClass;
  }

  @Override
  protected boolean needsScopeMappingKey(InstanceKey base) {
    boolean result = getParents(base).length > 0;
    if (AstTranslator.DEBUG_LEXICAL)
      System.err.println(("does " + base + " need scope mapping? " + result));

    return result;
  }
  
  @Override
  protected Collection<CGNode> getConstructorCallers(ScopeMappingInstanceKey smik, Pair<String, String> name) {
    // for Java, the creator node is exactly what we want
    return Collections.singleton(smik.getCreator());
  }

}
