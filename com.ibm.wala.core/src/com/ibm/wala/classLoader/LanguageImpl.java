/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.classLoader;

import java.util.Set;

import com.ibm.wala.util.collections.HashSetFactory;

/**
 * Common functionality for most {@link Language} implementations.
 */
public abstract class LanguageImpl implements Language {

  private Language baseLang;

  private Set<Language> derivedLangs= HashSetFactory.make();

  public LanguageImpl() { }

  public LanguageImpl(Language base) {
    baseLang= base;
    base.registerDerivedLanguage(this);
  }

  public Language getBaseLanguage() {
    return baseLang;
  }

  public Set<Language> getDerivedLanguages() {
    return derivedLangs;
  }

  public void registerDerivedLanguage(Language l) {
    derivedLangs.add(l);
    if (baseLang != null)
      baseLang.registerDerivedLanguage(l);
  }

  @Override
  public int hashCode() {
    return 1609 + 199 * getName().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LanguageImpl))
      return false;
    LanguageImpl other= (LanguageImpl) o;

    return getName().equals(other.getName());
  }

  @Override
  public String toString() {
    return getName().toString();
  }
}
