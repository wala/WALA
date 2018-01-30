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
package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstMethod.LexicalParent;
import com.ibm.wala.cast.loader.CAstAbstractModuleLoader.DynamicMethodObject;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallString;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContextSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;

public class JavaScriptScopeMappingInstanceKeys extends ScopeMappingInstanceKeys {

  private final IClassHierarchy cha;
  private final IClass codeBody;

  public JavaScriptScopeMappingInstanceKeys(IClassHierarchy cha,
					    PropagationCallGraphBuilder builder, 
					    InstanceKeyFactory basic)
  {
    super(builder, basic);
    this.cha = cha;

    this.codeBody = cha.lookupClass(JavaScriptTypes.CodeBody);
  }

  protected LexicalParent[] getParents(InstanceKey base) {
    DynamicMethodObject function = (DynamicMethodObject)
      base.getConcreteType().getMethod(AstMethodReference.fnSelector);

    return function==null? new LexicalParent[0]: function.getParents();
  }

  @Override
  protected boolean needsScopeMappingKey(InstanceKey base) {
    return 
      cha.isSubclassOf(base.getConcreteType(), codeBody)
                        &&
      getParents(base).length > 0;
  }

  @Override
  protected Collection<CGNode> getConstructorCallers(ScopeMappingInstanceKey smik, Pair<String, String> name) {
    // in JavaScript, the 'new' instruction is wrapped in a synthetic constructor method.  we want the 
    // caller of that constructor method, which we obtain from the context for the constructor method
    final Context creatorContext = smik.getCreator().getContext();
    CGNode callerOfConstructor = (CGNode) creatorContext.get(ContextKey.CALLER);
    Collection<CGNode> result = null;
    if (callerOfConstructor != null) {
      return Collections.singleton(callerOfConstructor);        
    } else {
      CallString cs = (CallString) creatorContext.get(CallStringContextSelector.CALL_STRING);
      if (cs != null) {
        IMethod[] methods = cs.getMethods();
        assert methods.length == 1;
        IMethod m = methods[0];
        result = builder.getCallGraph().getNodes(m.getReference());        
      }
    }
    if (result == null) {
      IClassHierarchy cha = smik.getCreator().getClassHierarchy();
      MethodReference ref = MethodReference.findOrCreate(JavaScriptLoader.JS, TypeReference.findOrCreate(cha.getLoaders()[0].getReference(), name.snd), AstMethodReference.fnAtomStr, AstMethodReference.fnDesc.toString());
      final IMethod method = cha.resolveMethod(ref);
      if (method != null) {
        return builder.getCallGraph().getNodes(method.getReference());
      }
    }
    return result;
  }

  
}
