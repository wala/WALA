/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.types;

import com.ibm.wala.util.Atom;

/**
 * Defines the meta-information that identifies a class loader. This is
 * effectively a "name" for a class loader.
 * 
 * @author sfink
 * 
 */
public class ClassLoaderReference {

  /**
   * Canonical name for the Java language
   */
  public final static Atom Java = Atom.findOrCreateUnicodeAtom("Java");

  /**
   * Canonical reference to primordial class loader
   */
  public final static ClassLoaderReference Primordial = new ClassLoaderReference(Atom.findOrCreateUnicodeAtom("Primordial"), Java);

  /**
   * Canonical reference to extension class loader
   */
  public final static ClassLoaderReference Extension = new ClassLoaderReference(Atom.findOrCreateUnicodeAtom("Extension"), Java);

  /**
   * Canonical reference to application class loader
   */
  public final static ClassLoaderReference Application = new ClassLoaderReference(Atom.findOrCreateUnicodeAtom("Application"), Java);

  /**
   * A String which identifies this loader
   */
  private final Atom name;

  /**
   * A String which identifies the language for this loader
   */
  private final Atom language;

  /**
   * This class loader's parent
   */
  private ClassLoaderReference parent;

  /**
   * Default constructor
   * 
   * @param name
   *          String (actually Atom) name identifying the loader
   */
  public ClassLoaderReference(Atom name, Atom language) {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    this.name = name;
    this.language = language;
  }

  /**
   * @return the name of this class loader
   */
  public Atom getName() {
    return name;
  }

  /**
   * @return the name of this class loader
   */
  public Atom getLanguage() {
    return language;
  }

  /**
   * @return the parent of this loader in the loader hierarchy, or null if none
   */
  public ClassLoaderReference getParent() {
    return parent;
  }

  /**
   * TODO: I hate that this exists.
   * @param parent
   *          the parent of this loader in the loader hierarchy,
   */
  public void setParent(ClassLoaderReference parent) {
    this.parent = parent;
  }

  /**
   * Note: names for class loader references must be unique.
   * 
   * @see java.lang.Object#equals(Object)
   */
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
    StringBuffer result = new StringBuffer();
    result.append(name.toString());
    result.append(" classloader");
    result.append("\n");
    return result.toString();
  }

}
