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

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys;
import com.ibm.wala.cast.js.loader.JavaScriptLoader.JavaScriptMethodObject;
import com.ibm.wala.cast.js.types.*;
import com.ibm.wala.cast.loader.AstMethod.LexicalParent;
import com.ibm.wala.cast.types.*;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.Selector;

public class JavaScriptScopeMappingInstanceKeys extends ScopeMappingInstanceKeys {

  private final ClassHierarchy cha;
  private final IClass codeBody;

  public JavaScriptScopeMappingInstanceKeys(ClassHierarchy cha,
					    PropagationCallGraphBuilder builder, 
					    InstanceKeyFactory basic)
  {
    super(builder, basic);
    this.cha = cha;

    this.codeBody = cha.lookupClass(JavaScriptTypes.CodeBody);
  }

  protected LexicalParent[] getParents(InstanceKey base) {
    JavaScriptMethodObject function = (JavaScriptMethodObject)
      base.getConcreteType().getMethod(AstMethodReference.fnSelector);

    return function==null? new LexicalParent[0]: function.getParents();
  }

  protected boolean needsScopeMappingKey(InstanceKey base) {
    return 
      cha.isSubclassOf(base.getConcreteType(), codeBody)
                        &&
      getParents(base).length > 0;
  }

}
