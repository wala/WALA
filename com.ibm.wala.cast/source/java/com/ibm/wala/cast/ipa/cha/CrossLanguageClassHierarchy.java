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
package com.ibm.wala.cast.ipa.cha;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.ComposedIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * This class hierarchy represents a family of disjoint class hierarchies, one
 * for each of a selection of languages. The implementation creates a separate
 * ClassHierarchy object for each language, and this overall IClassHierarchy
 * implementation delegates to the appropriate language class hierarchy based on
 * the language associated with the class loader of the given TypeReference or
 * IClass object.
 * 
 * Note that, because of this delegating structure and representation of
 * multiple languages, the getRootClass API call does not make sense for this
 * hierarchy. In general, any code that wants to use multiple language must deal
 * with the fact that there is no longer a single root type. Each individual
 * language is still expected to have a root type, however.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class CrossLanguageClassHierarchy implements IClassHierarchy {

  private final ClassLoaderFactory loaderFactory;

  private final AnalysisScope analysisScope;

  private final Map<Atom, IClassHierarchy> hierarchies;

  @Override
  public ClassLoaderFactory getFactory() {
    return loaderFactory;
  }

  @Override
  public AnalysisScope getScope() {
    return analysisScope;
  }

  private IClassHierarchy getHierarchy(Atom language) {
    return hierarchies.get(language);
  }

  private IClassHierarchy getHierarchy(IClassLoader loader) {
    return getHierarchy(loader.getLanguage().getName());
  }

  private IClassHierarchy getHierarchy(ClassLoaderReference loader) {
    return getHierarchy(loader.getLanguage());
  }

  private IClassHierarchy getHierarchy(IClass cls) {
    return getHierarchy(cls.getClassLoader());
  }

  private IClassHierarchy getHierarchy(TypeReference cls) {
    return getHierarchy(cls.getClassLoader());
  }

  private IClassHierarchy getHierarchy(MethodReference ref) {
    return getHierarchy(ref.getDeclaringClass());
  }

  private IClassHierarchy getHierarchy(FieldReference ref) {
    return getHierarchy(ref.getDeclaringClass());
  }

  @Override
  public IClassLoader[] getLoaders() {
    Set<IClassLoader> loaders = HashSetFactory.make();
    for (Iterator ldrs = analysisScope.getLoaders().iterator(); ldrs.hasNext();) {
      loaders.add(getLoader((ClassLoaderReference) ldrs.next()));
    }

    return loaders.toArray(new IClassLoader[loaders.size()]);
  }

  @Override
  public IClassLoader getLoader(ClassLoaderReference loaderRef) {
    return getHierarchy(loaderRef).getLoader(loaderRef);
  }

  @Override
  public boolean addClass(IClass klass) {
    return getHierarchy(klass).addClass(klass);
  }

  @Override
  public int getNumberOfClasses() {
    int total = 0;
    for (Iterator ldrs = analysisScope.getLoaders().iterator(); ldrs.hasNext();) {
      total += getLoader((ClassLoaderReference) ldrs.next()).getNumberOfClasses();
    }

    return total;
  }

  @Override
  public boolean isRootClass(IClass c) {
    return getHierarchy(c).isRootClass(c);
  }

  @Override
  public IClass getRootClass() {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public int getNumber(IClass c) {
    return getHierarchy(c).getNumber(c);
  }

  @Override
  public Set<IMethod> getPossibleTargets(MethodReference ref) {
    return getHierarchy(ref).getPossibleTargets(ref);
  }

  @Override
  public Set<IMethod> getPossibleTargets(IClass receiverClass, MethodReference ref) {
    return getHierarchy(ref).getPossibleTargets(receiverClass, ref);
  }

  @Override
  public IMethod resolveMethod(MethodReference m) {
    return getHierarchy(m).resolveMethod(m);
  }

  @Override
  public IField resolveField(FieldReference f) {
    return getHierarchy(f).resolveField(f);
  }

  @Override
  public IField resolveField(IClass klass, FieldReference f) {
    return getHierarchy(klass).resolveField(klass, f);
  }

  @Override
  public IMethod resolveMethod(IClass receiver, Selector selector) {
    return getHierarchy(receiver).resolveMethod(receiver, selector);
  }

  @Override
  public IClass lookupClass(TypeReference A) {
    return getHierarchy(A).lookupClass(A);
  }

//  public boolean isSyntheticClass(IClass c) {
//    return getHierarchy(c).isSyntheticClass(c);
//  }

  @Override
  public boolean isInterface(TypeReference type) {
    return getHierarchy(type).isInterface(type);
  }

  @Override
  public IClass getLeastCommonSuperclass(IClass A, IClass B) {
    return getHierarchy(A).getLeastCommonSuperclass(A, B);
  }

  @Override
  public TypeReference getLeastCommonSuperclass(TypeReference A, TypeReference B) {
    return getHierarchy(A).getLeastCommonSuperclass(A, B);
  }

  @Override
  public boolean isSubclassOf(IClass c, IClass T) {
    return getHierarchy(c).isSubclassOf(c, T);
  }

  @Override
  public boolean implementsInterface(IClass c, IClass i) {
    return getHierarchy(c).implementsInterface(c, i);
  }

  @Override
  public Collection<IClass> computeSubClasses(TypeReference type) {
    return getHierarchy(type).computeSubClasses(type);
  }

  @Override
  public Collection<TypeReference> getJavaLangRuntimeExceptionTypes() {
    return getHierarchy(TypeReference.JavaLangRuntimeException).getJavaLangErrorTypes();
  }

  @Override
  public Collection<TypeReference> getJavaLangErrorTypes() {
    return getHierarchy(TypeReference.JavaLangError).getJavaLangErrorTypes();
  }

  @Override
  public Set<IClass> getImplementors(TypeReference type) {
    return getHierarchy(type).getImplementors(type);
  }

  @Override
  public int getNumberOfImmediateSubclasses(IClass klass) {
    return getHierarchy(klass).getNumberOfImmediateSubclasses(klass);
  }

  @Override
  public Collection<IClass> getImmediateSubclasses(IClass klass) {
    return getHierarchy(klass).getImmediateSubclasses(klass);
  }

  @Override
  public boolean isAssignableFrom(IClass c1, IClass c2) {
    return getHierarchy(c1).isAssignableFrom(c1, c2);
  }

  @Override
  public Iterator<IClass> iterator() {
    return new ComposedIterator<ClassLoaderReference, IClass>(analysisScope.getLoaders().iterator()) {
      @Override
      public Iterator<IClass> makeInner(ClassLoaderReference o) {
        IClassLoader ldr = getLoader(o);
        return ldr.iterateAllClasses();
      }
    };
  }

  private CrossLanguageClassHierarchy(AnalysisScope scope, ClassLoaderFactory factory, Map<Atom, IClassHierarchy> hierarchies) {
    this.analysisScope = scope;
    this.loaderFactory = factory;
    this.hierarchies = hierarchies;
  }

  public static CrossLanguageClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory)
      throws ClassHierarchyException {
    Set<Language> languages = scope.getBaseLanguages();
    Map<Atom, IClassHierarchy> hierarchies = HashMapFactory.make();
    for (Iterator ls = languages.iterator(); ls.hasNext();) {
      Language L = (Language) ls.next();
      Set<Language> ll = HashSetFactory.make(L.getDerivedLanguages());
      ll.add(L);
      hierarchies.put(L.getName(), ClassHierarchyFactory.make(scope, factory, ll));
    }

    return new CrossLanguageClassHierarchy(scope, factory, hierarchies);
  }

/** BEGIN Custom change: unresolved classes */
  @Override
  public Set<TypeReference> getUnresolvedClasses() {
    return HashSetFactory.make();
  }
/** END Custom change: unresolved classes */

}
