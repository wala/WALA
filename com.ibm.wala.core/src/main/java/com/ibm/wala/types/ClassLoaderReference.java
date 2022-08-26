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
package com.ibm.wala.types;

import com.ibm.wala.core.util.strings.Atom;
import java.io.Serializable;

/**
 * Defines the meta-information that identifies a class loader. This is effectively a "name" for a
 * class loader.
 */
public class ClassLoaderReference implements Serializable {

  /* Serial version */
  private static final long serialVersionUID = -3256390509887654325L;

  /** Canonical name for the Java language */
  public static final Atom Java = Atom.findOrCreateUnicodeAtom("Java");

  /** Canonical reference to primordial class loader */
  public static final ClassLoaderReference Primordial =
      new ClassLoaderReference(Atom.findOrCreateUnicodeAtom("Primordial"), Java, null);

  /** Canonical reference to extension class loader */
  public static final ClassLoaderReference Extension =
      new ClassLoaderReference(Atom.findOrCreateUnicodeAtom("Extension"), Java, Primordial);

  /** Canonical reference to application class loader */
  public static final ClassLoaderReference Application =
      new ClassLoaderReference(Atom.findOrCreateUnicodeAtom("Application"), Java, Extension);

  /** A String which identifies this loader */
  private final Atom name;

  /** A String which identifies the language for this loader */
  private final Atom language;

  /** This class loader's parent */
  private final ClassLoaderReference parent;

  public ClassLoaderReference(Atom name, Atom language, ClassLoaderReference parent) {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    this.name = name;
    this.language = language;
    this.parent = parent;
  }

  /** @return the name of this class loader */
  public Atom getName() {
    return name;
  }

  /** @return the name of the language this class loader belongs to */
  public Atom getLanguage() {
    return language;
  }

  /** @return the parent of this loader in the loader hierarchy, or null if none */
  public ClassLoaderReference getParent() {
    return parent;
  }

  /** Note: names for class loader references must be unique. */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!getClass().equals(obj.getClass())) {
      return false;
    } else {
      ClassLoaderReference o = (ClassLoaderReference) obj;
      return name.equals(o.name);
    }
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name + " classloader\n";
  }
}
