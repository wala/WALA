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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AstDynamicPropertyClass extends AstClass {
  private final TypeReference defaultDescriptor;

  protected AstDynamicPropertyClass(
      CAstSourcePositionMap.Position sourcePosition,
      TypeName typeName,
      IClassLoader loader,
      short modifiers,
      Map<Selector, IMethod> declaredMethods,
      TypeReference defaultDescriptor) {
    // Concurrent because getField creates fields on lookup, so a query can write the map another
    // thread is iterating.
    super(sourcePosition, typeName, loader, modifiers, new ConcurrentHashMap<>(), declaredMethods);
    this.defaultDescriptor = defaultDescriptor;
  }

  @Override
  public IField getField(final Atom name) {
    IField x;
    IField field = declaredFields.get(name);
    if (field != null) {
      return field;
    } else if (getSuperclass() != null && (x = getSuperclass().getField(name)) != null) {
      return x;
    } else {
      // Atomic, so concurrent lookups of the same new name all return the same field.
      return declaredFields.computeIfAbsent(
          name, n -> new AstDynamicField(isStaticField(n), this, n, defaultDescriptor));
    }
  }

  protected boolean isStaticField(Atom name) {
    return name.toString().startsWith("global ");
  }
}
