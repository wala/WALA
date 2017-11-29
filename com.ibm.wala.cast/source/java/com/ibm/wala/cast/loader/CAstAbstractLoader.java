/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.loader;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.Warning;

/**
 * basic abstract class loader implementation
 *
 */
public abstract class CAstAbstractLoader implements IClassLoader {

  /**
   * types loaded by this
   */
  protected final Map<TypeName,IClass> types = HashMapFactory.make();

  protected final IClassHierarchy cha;

  protected final IClassLoader parent;

  /**
   * warnings generated while loading each module
   */
  private final Map<ModuleEntry, Set<Warning>> errors = new HashMap<>();
  
  public CAstAbstractLoader(IClassHierarchy cha, IClassLoader parent) {
    this.cha = cha;
    this.parent = parent;
  }

  public CAstAbstractLoader(IClassHierarchy cha) {
    this(cha, null);
  }

  public void addMessage(ModuleEntry module, Set<Warning> message) {
    if (! errors.containsKey(module)) {
      errors.put(module, new HashSet<Warning>());
    }
    
    errors.get(module).addAll(message);
  }

  public void addMessage(ModuleEntry module, Warning message) {
    if (! errors.containsKey(module)) {
      errors.put(module, new HashSet<Warning>());
    }
    
    errors.get(module).add(message);
  }

  private Iterator<ModuleEntry> getMessages(final byte severity) {
    return new MapIterator<>(new FilterIterator<>(errors.entrySet().iterator(), o -> {
       for(Warning w : o.getValue()) {
         if (w.getLevel() == severity) {
           return true;
         }
       }
       return false;
    }), Map.Entry::getKey);
  }
  
  public Iterator<ModuleEntry> getModulesWithParseErrors() {
     return getMessages(Warning.SEVERE);
  }

  public Iterator<ModuleEntry> getModulesWithWarnings() {
    return getMessages(Warning.MILD);
  }

  public Set<Warning> getMessages(ModuleEntry m) {
    return errors.get(m);
  }
  
  
  public void clearMessages() {
    errors.clear();
  }
  
  public IClass lookupClass(String className, IClassHierarchy cha) {
    assert this.cha == cha;
    return types.get(TypeName.string2TypeName(className));
  }

  @Override
  public IClass lookupClass(TypeName className) {
    return types.get(className);
  }

  @Override
  public Iterator<IClass> iterateAllClasses() {
    return types.values().iterator();
  }

  @Override
  public int getNumberOfClasses() {
    return types.size();
  }

  @Override
  public Atom getName() {
    return getReference().getName();
  }

  @Override
  public int getNumberOfMethods() {
    int i = 0;
    for (IClass cls : types.values()) {
      i += cls.getDeclaredMethods().size();
    }

    return i;
  }

  @Override
  public String getSourceFileName(IMethod method, int bcOffset) {
    if (!(method instanceof AstMethod)){
      return null;
    }
    Position pos = ((AstMethod)method).getSourcePosition(bcOffset);
    if (null == pos){
      return null;
    } 
    return pos.getURL().getFile();
  }
  
  @Override
  public String getSourceFileName(IClass klass) {
    return ((AstClass)klass).getSourcePosition().getURL().getFile();
  }
  
  @Override
  public Reader getSource(IClass klass) {
    try {
      return ((AstClass)klass).getSourcePosition().getReader();
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public Reader getSource(IMethod method, int bcOffset) {
    try {
      return ((AstMethod)method).getSourcePosition(bcOffset).getReader();
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public IClassLoader getParent() {
    assert parent != null;
    return parent;
  }

  @Override
  public void removeAll(Collection<IClass> toRemove) {
    Set<TypeName> keys = HashSetFactory.make();

    for (Map.Entry<TypeName, IClass> E : types.entrySet()) {
      if (toRemove.contains(E.getValue())) {
        keys.add(E.getKey());
      }
    }

    for (TypeName key : keys) {
      types.remove(key);
    }
  }

}
