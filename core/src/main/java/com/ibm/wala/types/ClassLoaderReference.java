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
import java.io.Serial;
import java.io.Serializable;

/**
 * Defines the meta-information that identifies a class loader. This is effectively a "name" for a
 * class loader.
 *
 * @param name A String which identifies this loader
 * @param language A String which identifies the language for this loader
 * @param parent This class loader's parent
 */
public record ClassLoaderReference(Atom name, Atom language, ClassLoaderReference parent)
    implements Serializable {

  /* Serial version */
  @Serial private static final long serialVersionUID = -3256390509887654325L;

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

  public ClassLoaderReference {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
  }

  /**
   * @deprecated Use {@link #name()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public Atom getName() {
    return name();
  }

  /**
   * @deprecated Use {@link #language()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public Atom getLanguage() {
    return language();
  }

  /**
   * @deprecated Use {@link #parent()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public ClassLoaderReference getParent() {
    return parent();
  }

  // intentional: names for class loader references are unique, so equality is
  // name-based only (ignoring language and parent)
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

  // intentional: name-based only
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name + " classloader\n";
  }
}
