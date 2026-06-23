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
package com.ibm.wala.ipa.summaries;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;
import java.util.Collections;

/**
 * A minimal standalone shell {@link IClass} for a type modeled only by a summary. Unlike {@link
 * BypassSyntheticClass}, which wraps an existing "real" type, a shell stands on its own and is
 * positioned in the hierarchy at an explicitly declared superclass. It carries no members; its
 * purpose is to occupy a place in the class hierarchy so that source subclasses resolve their base.
 * See <a href="https://github.com/wala/WALA/issues/1957">#1957</a>.
 */
public class SummaryClassShell extends SyntheticClass {

  /** The declared superclass, or {@code null} for the language root type. */
  private final TypeReference superReference;

  /**
   * @param type the type this shell represents
   * @param cha the governing class hierarchy
   * @param superReference the declared superclass, or {@code null} for the language root type
   */
  public SummaryClassShell(TypeReference type, IClassHierarchy cha, TypeReference superReference) {
    super(type, cha);
    this.superReference = superReference;
  }

  @Override
  public IClass getSuperclass() {
    IClassHierarchy cha = getClassHierarchy();
    if (superReference != null) {
      IClass superClass = cha.lookupClass(superReference);
      if (superClass != null) {
        return superClass;
      }
    }
    return cha.getRootClass();
  }

  @Override
  public Collection<IClass> getDirectInterfaces() {
    return Collections.emptySet();
  }

  @Override
  public Collection<IClass> getAllImplementedInterfaces() {
    return Collections.emptySet();
  }

  @Override
  public IMethod getMethod(Selector selector) {
    return null;
  }

  @Override
  public IField getField(Atom name) {
    return null;
  }

  @Override
  public IMethod getClassInitializer() {
    return null;
  }

  @Override
  public Collection<? extends IMethod> getDeclaredMethods() {
    return Collections.emptySet();
  }

  @Override
  public Collection<IField> getDeclaredInstanceFields() {
    return Collections.emptySet();
  }

  @Override
  public Collection<IField> getDeclaredStaticFields() {
    return Collections.emptySet();
  }

  @Override
  public Collection<IField> getAllInstanceFields() {
    return Collections.emptySet();
  }

  @Override
  public Collection<IField> getAllStaticFields() {
    return Collections.emptySet();
  }

  @Override
  public Collection<IField> getAllFields() {
    return Collections.emptySet();
  }

  @Override
  public Collection<? extends IMethod> getAllMethods() {
    return Collections.emptySet();
  }

  @Override
  public boolean isReferenceType() {
    return getReference().isReferenceType();
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public int getModifiers() {
    return 0;
  }

  @Override
  public String toString() {
    return "<SummaryShell " + getReference().getName() + '>';
  }
}
