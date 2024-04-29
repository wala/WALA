/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.loader;

import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import java.util.HashMap;
import java.util.Map;

public abstract class AstDynamicPropertyClass extends AstClass {
  private final TypeReference defaultDescriptor;

  protected AstDynamicPropertyClass(
      CAstSourcePositionMap.Position sourcePosition,
      TypeName typeName,
      IClassLoader loader,
      short modifiers,
      Map<Selector, IMethod> declaredMethods,
      TypeReference defaultDescriptor) {
    super(sourcePosition, typeName, loader, modifiers, new HashMap<>(), declaredMethods);
    this.defaultDescriptor = defaultDescriptor;
  }

  @Override
  public IField getField(final Atom name) {
    IField x;
    if (declaredFields.containsKey(name)) {
      return declaredFields.get(name);
    } else if (getSuperclass() != null && (x = getSuperclass().getField(name)) != null) {
      return x;
    } else {
      final boolean isStatic = isStaticField(name);
      declaredFields.put(name, new AstDynamicField(isStatic, this, name, defaultDescriptor));

      return declaredFields.get(name);
    }
  }

  protected boolean isStaticField(Atom name) {
    return name.toString().startsWith("global ");
  }
}
