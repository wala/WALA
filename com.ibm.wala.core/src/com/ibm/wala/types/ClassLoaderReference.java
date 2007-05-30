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
   * Canonical reference to primordial class loader
   */
  public final static ClassLoaderReference Primordial = new ClassLoaderReference(Atom.findOrCreateUnicodeAtom("Primordial"));

  /**
   * Canonical reference to extension class loader
   */
  public final static ClassLoaderReference Extension = new ClassLoaderReference(Atom.findOrCreateUnicodeAtom("Extension"));

  /**
   * Canonical reference to application class loader
   */
  public final static ClassLoaderReference Application = new ClassLoaderReference(Atom.findOrCreateUnicodeAtom("Application"));

  /**
   * A String which identifies this loader
   */
  private final Atom name;

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
  public ClassLoaderReference(Atom name) {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    this.name = name;
  }

  /**
   * @return the name of this class loader
   */
  public Atom getName() {
    return name;
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

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append(name.toString());
    result.append(" classloader");
    result.append("\n");
    return result.toString();
  }

}
