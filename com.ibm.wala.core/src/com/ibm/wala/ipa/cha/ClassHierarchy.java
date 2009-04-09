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
package com.ibm.wala.ipa.cha;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.wala.annotations.Internal;
import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.functions.Function;
import com.ibm.wala.util.ref.CacheReference;
import com.ibm.wala.util.ref.ReferenceCleanser;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

/**
 * Simple implementation of a class hierarchy
 */
public class ClassHierarchy implements IClassHierarchy {

  private static final boolean DEBUG = false;

  /**
   * Languages that contribute classes to the set represented in this hierarchy. The languages may for example be related by
   * inheritance (e.g. X10 derives from Java, and shares a common type hierarchy rooted at java.lang.Object).
   */
  private final Set<Language> languages = HashSetFactory.make();

  final private HashMap<IClass, Node> map = HashMapFactory.make();

  /**
   * {@link TypeReference} for the root type
   */
  private TypeReference rootTypeRef;

  /**
   * root node of the class hierarchy
   */
  private Node root;

  /**
   * An object which defines class loaders.
   */
  final private ClassLoaderFactory factory;

  /**
   * The loaders used to define this class hierarchy.
   * 
   * XXX is order significant??
   */
  final private IClassLoader[] loaders;

  /**
   * A mapping from IClass -> Selector -> Set of IMethod
   */
  final private HashMap<IClass, Object> targetCache = HashMapFactory.make();

  /**
   * Governing analysis scope
   */
  private final AnalysisScope scope;

  /**
   * A mapping from IClass (representing an interface) -> Set of IClass that implement that interface
   */
  private final Map<IClass, Set<IClass>> implementors = HashMapFactory.make();

  /**
   * A temporary hack : TODO: do intelligent caching somehow
   */
  private Collection<IClass> subclassesOfError;

  /**
   * A temporary hack : TODO: do intelligent caching somehow
   */
  private Collection<TypeReference> subTypeRefsOfError;

  /**
   * A temporary hack : TODO: do intelligent caching somehow
   */
  private Collection<IClass> runtimeExceptionClasses;

  /**
   * A temporary hack : TODO: do intelligent caching somehow
   */
  private Collection<TypeReference> runtimeExceptionTypeRefs;

  /**
   * Return a set of IClasses that holds all superclasses of klass
   * 
   * @param klass class in question
   * @return Set the result set
   */
  private Set<IClass> computeSuperclasses(IClass klass) throws ClassHierarchyException {
    if (DEBUG) {
      System.err.println("computeSuperclasses: " + klass);
    }

    Set<IClass> result = HashSetFactory.make(3);
    klass = klass.getSuperclass();

    while (klass != null) {
      if (DEBUG) {
        System.err.println("got superclass " + klass);
      }
      result.add(klass);
      klass = klass.getSuperclass();
      if (klass != null && klass.getReference().getName().equals(rootTypeRef.getName())) {
        if (!klass.getReference().getClassLoader().equals(rootTypeRef.getClassLoader())) {
          throw new ClassHierarchyException("class " + klass + " is invalid, unexpected classloader");
        }
      }
    }
    return result;
  }

  private ClassHierarchy(AnalysisScope scope, ClassLoaderFactory factory, Language language, IProgressMonitor progressMonitor)
      throws ClassHierarchyException, IllegalArgumentException {
    this(scope, factory, Collections.singleton(language), progressMonitor);
  }

  private ClassHierarchy(AnalysisScope scope, ClassLoaderFactory factory, IProgressMonitor progressMonitor)
      throws ClassHierarchyException, IllegalArgumentException {
    this(scope, factory, scope.getLanguages(), progressMonitor);
  }

  private ClassHierarchy(AnalysisScope scope, ClassLoaderFactory factory, Collection<Language> languages,
      IProgressMonitor progressMonitor) throws ClassHierarchyException, IllegalArgumentException {
    // now is a good time to clear the warnings globally.
    // TODO: think of a better way to guard against warning leaks.
    Warnings.clear();

    if (factory == null) {
      throw new IllegalArgumentException();
    }
    if (scope.getLanguages().size() == 0) {
      throw new IllegalArgumentException("AnalysisScope must contain at least 1 language");
    }
    this.scope = scope;
    this.factory = factory;
    Set<Atom> langNames = HashSetFactory.make();
    for (Language lang : languages) {
      this.languages.add(lang);
      this.languages.addAll(lang.getDerivedLanguages());
      langNames.add(lang.getName());
    }
    for (Language lang : this.languages) {
      if (lang.getRootType() != null) {
        if (this.rootTypeRef != null) {
          throw new IllegalArgumentException("AnalysisScope must have only 1 root type");
        } else {
          this.rootTypeRef = lang.getRootType();
        }
      }
    }
    try {
      int numLoaders = 0;
      for (ClassLoaderReference ref : scope.getLoaders()) {
        if (langNames.contains(ref.getLanguage())) {
          numLoaders++;
        }
      }

      loaders = new IClassLoader[numLoaders];
      int idx = 0;

      if (progressMonitor != null) {
        progressMonitor.beginTask("Build Class Hierarchy", numLoaders);
      }
      for (ClassLoaderReference ref : scope.getLoaders()) {
        if (progressMonitor != null) {
          if (progressMonitor.isCanceled()) {
            throw new CancelCHAConstructionException();
          }
        }

        if (langNames.contains(ref.getLanguage())) {
          IClassLoader icl = factory.getLoader(ref, this, scope);
          loaders[idx++] = icl;

          if (progressMonitor != null) {
            progressMonitor.worked(1);
          }
        }
      }

      for (IClassLoader icl : loaders) {
        addAllClasses(icl, progressMonitor);

        if (progressMonitor != null) {
          progressMonitor.worked(1);
        }
      }
      
    } catch (IOException e) {
      throw new ClassHierarchyException("factory.getLoader failed " + e);
    } finally {
      if (progressMonitor != null) {
        progressMonitor.done(); // In case an exception is thrown.
      }
    }

    if (root == null) {
      throw new ClassHierarchyException("failed to load root " + rootTypeRef + " of class hierarchy");
    }

    // perform numbering for subclass tests.
    numberTree();
    ReferenceCleanser.registerClassHierarchy(this);
  }

  /**
   * Add all classes in a class loader to the hierarchy.
   */
  private void addAllClasses(IClassLoader loader, IProgressMonitor progressMonitor) throws CancelCHAConstructionException {
    if (DEBUG) {
      Trace.println("Add all classes from loader " + loader);
    }
    Collection<IClass> toRemove = HashSetFactory.make();
    for (Iterator<IClass> it = loader.iterateAllClasses(); it.hasNext();) {
      if (progressMonitor != null) {
        if (progressMonitor.isCanceled()) {
          throw new CancelCHAConstructionException();
        }
      }
      IClass klass = it.next();
      boolean added = addClass(klass);
      if (!added) {
        toRemove.add(klass);
      }
    }
    loader.removeAll(toRemove);

  }

  /**
   * @return true if the add succeeded; false if it failed for some reason
   * @throws IllegalArgumentException if klass is null
   */
  public boolean addClass(IClass klass) {
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    if (klass.getReference().getName().equals(rootTypeRef.getName())) {
      if (!klass.getReference().getClassLoader().equals(rootTypeRef.getClassLoader())) {
        throw new IllegalArgumentException("class " + klass + " is invalid, unexpected classloader");
      }
    }
    if (DEBUG) {
      Trace.println("Attempt to add class " + klass);
    }
    Set<IClass> loadedSuperclasses;
    Collection loadedSuperInterfaces;
    try {
      loadedSuperclasses = computeSuperclasses(klass);
      loadedSuperInterfaces = klass.getAllImplementedInterfaces();
    } catch (ClassHierarchyException e) {
      // a little cleanup
      if (klass instanceof ShrikeClass) {
        if (DEBUG) {
          Trace.println("Exception.  Clearing " + klass);
        }
      }
      Warnings.add(ClassExclusion.create(klass.getReference(), e.getMessage()));
      return false;
    }
    Node node = findOrCreateNode(klass);

    if (klass.getReference().equals(this.rootTypeRef)) {
      // there is only one root
      if (Assertions.verifyAssertions) {
        Assertions._assert(root == null);
      }
      root = node;
    }

    Set workingSuperclasses = HashSetFactory.make(loadedSuperclasses);
    while (node != null) {
      IClass c = node.getJavaClass();
      IClass superclass = null;
      try {
        superclass = c.getSuperclass();
      } catch (ClassHierarchyException e1) {
        Assertions.UNREACHABLE();
      }
      if (superclass != null) {
        workingSuperclasses.remove(superclass);
        Node supernode = findOrCreateNode(superclass);
        if (DEBUG) {
          Trace.println("addChild " + node.getJavaClass() + " to " + supernode.getJavaClass());
        }
        supernode.addChild(node);
        if (supernode.getJavaClass().getReference().equals(rootTypeRef)) {
          node = null;
        } else {
          node = supernode;
        }
      } else {
        node = null;
      }
    }

    if (loadedSuperInterfaces != null) {
      for (Iterator it3 = loadedSuperInterfaces.iterator(); it3.hasNext();) {
        IClass iface = (IClass) it3.next();
        try {
          // make sure we'll be able to load the interface!
          computeSuperclasses(iface);
        } catch (ClassHierarchyException e) {
          Warnings.add(ClassExclusion.create(iface.getReference(), e.getMessage()));
          continue;
        }
        if (DEBUG && Assertions.verifyAssertions) {
          if (!iface.isInterface()) {
            Assertions._assert(false, "not an interface: " + iface);
          }
        }
        recordImplements(klass, iface);
      }
    }
    return true;
  }

  /**
   * Record that a klass implements a particular interface
   * 
   * @param klass
   * @param iface
   */
  private void recordImplements(IClass klass, IClass iface) {
    Set<IClass> impls = MapUtil.findOrCreateSet(implementors, iface);
    impls.add(klass);
  }

  /**
   * Find the possible targets of a call to a method reference. Note that if the reference is to an instance initialization method,
   * we assume the method was called with invokespecial rather than invokevirtual.
   * 
   * @param ref method reference
   * @return the set of IMethods that this call can resolve to.
   * @throws IllegalArgumentException if ref is null
   */
  public Collection<IMethod> getPossibleTargets(MethodReference ref) {
    if (ref == null) {
      throw new IllegalArgumentException("ref is null");
    }
    IClassLoader loader;
    try {
      loader = factory.getLoader(ref.getDeclaringClass().getClassLoader(), this, scope);
    } catch (IOException e) {
      throw new UnimplementedError("factory.getLoader failed " + e);
    }
    IClass declaredClass;
    declaredClass = loader.lookupClass(ref.getDeclaringClass().getName());
    if (declaredClass == null) {
      return Collections.emptySet();
    }
    Set<IMethod> targets = HashSetFactory.make();
    targets.addAll(findOrCreateTargetSet(declaredClass, ref));
    return (targets);
  }

  /**
   * Find the possible targets of a call to a method reference
   * 
   * @param ref method reference
   * @return the set of IMethods that this call can resolve to.
   */
  @SuppressWarnings("unchecked")
  private Set<IMethod> findOrCreateTargetSet(IClass declaredClass, MethodReference ref) {
    Map<MethodReference, Set<IMethod>> classCache = (Map<MethodReference, Set<IMethod>>) CacheReference.get(targetCache
        .get(declaredClass));
    if (classCache == null) {
      classCache = HashMapFactory.make(3);
      targetCache.put(declaredClass, CacheReference.make(classCache));
    }
    Set<IMethod> result = classCache.get(ref);
    if (result == null) {
      result = getPossibleTargets(declaredClass, ref);
      classCache.put(ref, result);
    }
    return result;
  }

  /**
   * Find the possible receivers of a call to a method reference
   * 
   * @param ref method reference
   * @return the set of IMethods that this call can resolve to.
   */
  public Set<IMethod> getPossibleTargets(IClass declaredClass, MethodReference ref) {

    if (ref.getName().equals(MethodReference.initAtom)) {
      // for an object init method, use the method alone as a possible target,
      // rather than inspecting subclasses
      IMethod resolvedMethod = resolveMethod(ref);
      assert resolvedMethod != null;
      return Collections.singleton(resolvedMethod);
    }
    if (declaredClass.isInterface()) {
      HashSet<IMethod> result = HashSetFactory.make(3);
      Set impls = implementors.get(declaredClass);
      if (impls == null) {
        // give up and return no receivers
        return Collections.emptySet();
      }
      for (Iterator it = impls.iterator(); it.hasNext();) {
        IClass klass = (IClass) it.next();
        if (!klass.isInterface() && !klass.isAbstract()) {
          result.addAll(computeTargetsNotInterface(ref, klass));
        }
      }
      return result;
    } else {
      return computeTargetsNotInterface(ref, declaredClass);
    }

  }

  /**
   * Get the targets for a method ref invoked on a class klass. The klass had better not be an interface.
   * 
   * @param ref method to invoke
   * @param klass declaringClass of receiver
   * @return Set the set of method implementations that might receive the message
   */
  private Set<IMethod> computeTargetsNotInterface(MethodReference ref, IClass klass) {

    Node n = findNode(klass);
    HashSet<IMethod> result = HashSetFactory.make(3);
    // if n is null, then for some reason this class is excluded
    // from the analysis. Return a result of no targets.
    if (n == null)
      return result;

    Selector selector = ref.getSelector();

    // try to resolve the method by walking UP the class hierarchy
    IMethod resolved = resolveMethod(klass, selector);

    if (resolved != null) {
      result.add(resolved);
    }

    // find any receivers that override the method with inheritance
    result.addAll(computeOverriders(n, selector));

    return result;
  }

  /**
   * Return the unique receiver of an invocation of method on an object of type m.getDeclaredClass
   * 
   * @param m
   * @return IMethod, or null if no appropriate receiver is found.
   * @throws IllegalArgumentException if m is null
   */
  public IMethod resolveMethod(MethodReference m) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    IClass receiver = lookupClass(m.getDeclaringClass());
    if (receiver == null) {
      return null;
    }
    Selector selector = m.getSelector();
    return resolveMethod(receiver, selector);
  }

  /**
   * @return the canonical IField that represents a given field , or null if none found
   * @throws IllegalArgumentException if f is null
   */
  public IField resolveField(FieldReference f) {
    if (f == null) {
      throw new IllegalArgumentException("f is null");
    }
    IClass klass = lookupClass(f.getDeclaringClass());
    if (klass == null) {
      return null;
    }
    return resolveField(klass, f);
  }

  /**
   * @return the canonical IField that represents a given field , or null if none found
   * @throws IllegalArgumentException if f is null
   * @throws IllegalArgumentException if klass is null
   */
  public IField resolveField(IClass klass, FieldReference f) {
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    if (f == null) {
      throw new IllegalArgumentException("f is null");
    }
    return klass.getField(f.getName());
  }

  /**
   * Return the unique target of an invocation of method on an object of type declaringClass
   * 
   * @param receiverClass type of receiver
   * @param selector method signature
   * @return Method resolved method abstraction
   * @throws IllegalArgumentException if receiverClass is null
   */
  public IMethod resolveMethod(IClass receiverClass, Selector selector) {
    if (receiverClass == null) {
      throw new IllegalArgumentException("receiverClass is null");
    }
    IMethod result = findMethod(receiverClass, selector);
    if (result != null) {
      return result;
    } else {
      IClass superclass = null;
      try {
        superclass = receiverClass.getSuperclass();
      } catch (ClassHierarchyException e) {
        Assertions.UNREACHABLE();
      }
      if (superclass == null) {
        if (DEBUG) {
          Trace.println("resolveMethod(" + selector + ") failed: method not found");
        }
        return null;
      } else {
        if (DEBUG) {
          Trace.println("Attempt to resolve for " + receiverClass + " in superclass: " + superclass + " " + selector);
        }
        return resolveMethod(superclass, selector);
      }
    }
  }

  /**
   * Does a particular class contain (implement) a particular method?
   * 
   * @param clazz class in question
   * @param selector method selector
   * @return the method if found, else null
   */
  private IMethod findMethod(IClass clazz, Selector selector) {
    return clazz.getMethod(selector);
  }

  /**
   * Get the set of subclasses of a class that provide implementations of a method
   * 
   * @param node abstraction of class in question
   * @param selector method signature
   * @return Set set of IMethods that override the method
   */
  private Set<IMethod> computeOverriders(Node node, Selector selector) {
    HashSet<IMethod> result = HashSetFactory.make(3);
    for (Iterator<Node> it = node.getChildren(); it.hasNext();) {

      Node child = it.next();
      IMethod m = findMethod(child.getJavaClass(), selector);
      if (m != null) {
        result.add(m);
      }
      result.addAll(computeOverriders(child, selector));
    }
    return result;
  }

  private Node findNode(IClass klass) {
    return map.get(klass);
  }

  private Node findOrCreateNode(IClass klass) {
    Node result = map.get(klass);
    if (result == null) {
      result = new Node(klass);
      map.put(klass, result);
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer(100);
    recursiveStringify(root, result);
    return result.toString();
  }

  private void recursiveStringify(Node n, StringBuffer buffer) {
    buffer.append(n.toString()).append("\n");
    for (Iterator<Node> it = n.getChildren(); it.hasNext();) {
      Node child = it.next();
      recursiveStringify(child, buffer);
    }
  }

  /**
   * Number the class hierarchy tree to support efficient subclass tests. After numbering the tree, n1 is a child of n2 iff n2.left
   * <= n1.left ^ n1.left <= n2.right. Described as "relative numbering" by Vitek, Horspool, and Krall, OOPSLA 97
   * 
   * TODO: this implementation is recursive; un-recursify if needed
   */
  private int nextNumber = 1;

  private void numberTree() {
    if (Assertions.verifyAssertions) {
      Assertions._assert(root != null);
    }
    visitForNumbering(root);
  }

  private void visitForNumbering(Node N) {
    N.left = nextNumber++;
    for (Iterator<Node> it = N.children.iterator(); it.hasNext();) {
      Node C = it.next();
      visitForNumbering(C);
    }
    N.right = nextNumber++;
  }

  /**
   * internal representation of a node in the class hiearachy, representing one java class.
   */
  private static final class Node {

    private final IClass klass;

    final private Set<Node> children = HashSetFactory.make(3);

    // the following two fields are used for efficient subclass tests.
    // After numbering the tree, n1 is a child of n2 iff
    // n2.left <= n1.left ^ n1.left <= n2.right.
    // Described as "relative numbering" by Vitek, Horspool, and Krall, OOPSLA
    // 97
    private int left = -1;

    private int right = -1;

    Node(IClass klass) {
      this.klass = klass;
    }

    boolean isInterface() {
      return klass.isInterface();
    }

    IClass getJavaClass() {
      return klass;
    }

    void addChild(Node child) {
      children.add(child);
    }

    Iterator<Node> getChildren() {
      return children.iterator();
    }

    @Override
    public String toString() {
      StringBuffer result = new StringBuffer(100);
      result.append(klass.toString()).append(":");
      for (Iterator<Node> i = children.iterator(); i.hasNext();) {
        Node n = i.next();
        result.append(n.klass.toString());
        if (i.hasNext())
          result.append(",");
      }
      return result.toString();
    }

    @Override
    public int hashCode() {
      return klass.hashCode() * 929;
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj;
    }

  }

  public ClassLoaderFactory getFactory() {
    return factory;
  }

  /**
   * @throws IllegalArgumentException if A is null
   */
  public IClass getLeastCommonSuperclass(IClass A, IClass B) {

    if (A == null) {
      throw new IllegalArgumentException("A is null");
    }
    TypeReference tempA = A.getReference();
    if (A.equals(B)) {
      return A;
    } else if (tempA.equals(TypeReference.Null)) {
      return B;
    } else if (B.getReference().equals(TypeReference.Null)) {
      return A;
    } else if (B.getReference().equals(TypeReference.JavaLangObject)) {
      return B;
    } else {
      if (Assertions.verifyAssertions) {
        Node n = map.get(B);
        if (n == null) {
          Assertions._assert(n != null, "null n for " + B);
        }
      }
      Set<IClass> superB;
      try {
        superB = getSuperclasses(B);
      } catch (ClassHierarchyException e1) {
        e1.printStackTrace();
        Assertions.UNREACHABLE();
        superB = null;
      }
      while (A != null) {
        if (superB.contains(A))
          return A;
        try {
          A = A.getSuperclass();
        } catch (ClassHierarchyException e) {
          Assertions.UNREACHABLE();
        }
      }
      Assertions.UNREACHABLE("getLeastCommonSuperclass " + tempA + " " + B);
      return null;
    }
  }

  private Set<IClass> getSuperclasses(IClass c) throws ClassHierarchyException {
    HashSet<IClass> result = HashSetFactory.make(3);
    while (c.getSuperclass() != null) {
      result.add(c.getSuperclass());
      c = c.getSuperclass();
    }
    return result;
  }

  public TypeReference getLeastCommonSuperclass(TypeReference A, TypeReference B) {
    if (A == null) {
      throw new IllegalArgumentException("A is null");
    }
    if (A.equals(B))
      return A;
    IClass AClass = lookupClass(A);
    IClass BClass = lookupClass(B);
    if (AClass == null || BClass == null) {
      // One of the classes is not in scope. Give up.
      return TypeReference.JavaLangObject;
    }
    return getLeastCommonSuperclass(AClass, BClass).getReference();
  }

  /**
   * Load a class using one of the loaders specified for this class hierarchy
   * 
   * @return null if can't find the class.
   * @throws IllegalArgumentException if A is null
   */
  public IClass lookupClass(TypeReference A) {
    if (A == null) {
      throw new IllegalArgumentException("A is null");
    }
    ClassLoaderReference loaderRef = A.getClassLoader();
    for (int i = 0; i < loaders.length; i++) {
      if (loaders[i].getReference().equals(loaderRef)) {
        IClass klass = loaders[i].lookupClass(A.getName());
        if (klass != null) {
          if (findNode(klass) != null) {
            // it's a scalar type in the class hierarchy
            return klass;
          }
          if (klass.isArrayClass()) {
            // check that we know how to handle the element type
            TypeReference t = klass.getReference().getInnermostElementType();
            if (t.isPrimitiveType()) {
              return klass;
            } else {
              IClass tClass = lookupClass(t);
              if (tClass != null) {
                return klass;
              } else {
                return null;
              }
            }
          }
        }
      }
    }
    return null;
  }

  private boolean slowIsSubclass(IClass sub, IClass sup) {
    if (sub == sup)
      return true;
    else
      try {
        IClass parent = sub.getSuperclass();
        if (parent == null)
          return false;
        else
          return slowIsSubclass(parent, sup);
      } catch (ClassHierarchyException e) {
        Assertions.UNREACHABLE();
        return false;
      }
  }

  /**
   * Is c a subclass of T?
   * 
   * @throws IllegalArgumentException if c is null
   */
  public boolean isSubclassOf(IClass c, IClass T) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(T != null, "null T");
    }

    if (c.isArrayClass()) {
      if (T.getReference() == TypeReference.JavaLangObject) {
        return true;
      } else if (T.getReference().isArrayType()) {
        TypeReference elementType = T.getReference().getArrayElementType();
        if (elementType.isPrimitiveType()) {
          return elementType.equals(c.getReference().getArrayElementType());
        } else {
          IClass elementKlass = lookupClass(elementType);
          if (elementKlass == null) {
            // uh oh.
            Warnings.add(ClassHierarchyWarning.create("could not find " + elementType));
            return false;
          }
          IClass ce = ((ArrayClass) c).getElementClass();
          if (ce == null) {
            return false;
          }
          if (elementKlass.isInterface()) {
            return implementsInterface(ce, elementKlass);
          } else {
            return isSubclassOf(ce, elementKlass);
          }
        }
      } else {
        return false;
      }
    } else {
      if (T.getReference().isArrayType()) {
        return false;
      }
      if (c.getReference().equals(T.getReference())) {
        return true;
      }
      Node n1 = map.get(c);
      if (n1 == null) {
        // some wacky case, like a FakeRootClass
        return false;
      }
      Node n2 = map.get(T);
      if (n2 == null) {
        // some wacky case, like a FakeRootClass
        return false;
      }
      if (n1.left == -1) {
        return slowIsSubclass(c, T);
      } else if (n2.left == -1) {
        return slowIsSubclass(c, T);
      } else {
        return (n2.left <= n1.left) && (n1.left <= n2.right);
      }
    }
  }

  /**
   * Does c implement i?
   * 
   * @return true iff i is an interface and c is a class that implements i, or c is an interface that extends i.
   */
  public boolean implementsInterface(IClass c, IClass i) {
    if (i == null) {
      throw new IllegalArgumentException("Cannot ask implementsInterface with i == null");
    }
    if (c == null) {
      throw new IllegalArgumentException("Cannot ask implementsInterface with c == null");
    }
    if (!i.isInterface()) {
      return false;
    }
    if (c.equals(i)) {
      return true;
    }
    if (c.isArrayClass()) {
      // arrays implement Cloneable and Serializable
      return i.equals(lookupClass(TypeReference.JavaLangCloneable)) || i.equals(lookupClass(TypeReference.JavaIoSerializable));
    }
    Set impls = implementors.get(i);
    if (impls != null && impls.contains(c)) {
      return true;
    }
    return false;
  }

  /**
   * Return set of all subclasses of type in the Class Hierarchy TODO: Tune this implementation. Consider caching if necessary.
   */
  public Collection<IClass> computeSubClasses(TypeReference type) {
    IClass t = lookupClass(type);
    if (t == null) {
      throw new IllegalArgumentException("could not find class for TypeReference " + type);
    }
    // a hack: TODO: work on better caching
    if (t.getReference().equals(TypeReference.JavaLangError)) {
      if (subclassesOfError == null) {
        subclassesOfError = computeSubClassesInternal(t);
      }
      return subclassesOfError;
    } else if (t.getReference().equals(TypeReference.JavaLangRuntimeException)) {
      if (runtimeExceptionClasses == null) {
        runtimeExceptionClasses = computeSubClassesInternal(t);
      }
      return runtimeExceptionClasses;
    } else {
      return computeSubClassesInternal(t);
    }
  }

  /**
   * Solely for optimization; return a Collection<TypeReference> representing the subclasses of Error
   * 
   * kind of ugly. a better scheme?
   */
  public Collection<TypeReference> getJavaLangErrorTypes() {
    if (subTypeRefsOfError == null) {
      computeSubClasses(TypeReference.JavaLangError);
      subTypeRefsOfError = HashSetFactory.make(subclassesOfError.size());
      for (Iterator it = subclassesOfError.iterator(); it.hasNext();) {
        IClass klass = (IClass) it.next();
        subTypeRefsOfError.add(klass.getReference());
      }
    }
    return Collections.unmodifiableCollection(subTypeRefsOfError);
  }

  /**
   * Solely for optimization; return a Collection<TypeReference> representing the subclasses of RuntimeException
   * 
   * kind of ugly. a better scheme?
   */
  public Collection<TypeReference> getJavaLangRuntimeExceptionTypes() {
    if (runtimeExceptionTypeRefs == null) {
      computeSubClasses(TypeReference.JavaLangRuntimeException);
      runtimeExceptionTypeRefs = HashSetFactory.make(runtimeExceptionClasses.size());
      for (Iterator it = runtimeExceptionClasses.iterator(); it.hasNext();) {
        IClass klass = (IClass) it.next();
        runtimeExceptionTypeRefs.add(klass.getReference());
      }
    }
    return Collections.unmodifiableCollection(runtimeExceptionTypeRefs);
  }

  /**
   * Return set of all subclasses of type in the Class Hierarchy TODO: Tune this implementation. Consider caching if necessary.
   * 
   * @return Set of IClasses
   */
  private Set<IClass> computeSubClassesInternal(IClass T) {
    if (T.isArrayClass()) {
      return Collections.singleton(T);
    }
    Node node = findNode(T);
    if (Assertions.verifyAssertions) {
      if (node == null) {
        Assertions._assert(node != null, "null node for class " + T);
      }
    }
    HashSet<IClass> result = HashSetFactory.make(3);
    result.add(T);
    for (Iterator<Node> it = node.getChildren(); it.hasNext();) {
      Node child = it.next();
      result.addAll(computeSubClasses(child.klass.getReference()));
    }
    return result;
  }

  public boolean isInterface(TypeReference type) {
    IClass T = lookupClass(type);
    if (Assertions.verifyAssertions) {
      if (T == null) {
        Assertions._assert(T != null, "Null lookup for " + type);
      }
    }
    return T.isInterface();
  }

  /**
   * TODO: tune this if necessary
   * 
   * @param type an interface
   * @return Set of IClass that represent implementors of the interface
   */
  public Set<IClass> getImplementors(TypeReference type) {
    IClass T = lookupClass(type);
    Set<IClass> result = implementors.get(T);
    if (result == null) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(result);
  }

  public Iterator<IClass> iterator() {
    return map.keySet().iterator();
  }

  /**
   * @return The number of classes present in the class hierarchy.
   */
  public int getNumberOfClasses() {
    return map.keySet().size();
  }

  public IClassLoader[] getLoaders() {
    return loaders;
  }

  public IClassLoader getLoader(ClassLoaderReference loaderRef) {
    for (int i = 0; i < loaders.length; i++) {
      if (loaders[i].getReference().equals(loaderRef)) {
        return loaders[i];
      }
    }
    Assertions.UNREACHABLE();
    return null;
  }

  public AnalysisScope getScope() {
    return scope;
  }

  /**
   * @param klass
   * @return the number of classes that immediately extend klass. if klass is an array class A[][]...[], we return number of
   *         immediate subclasses of A. If A is primitive, we return 0.
   */
  public int getNumberOfImmediateSubclasses(IClass klass) {
    if (klass.isArrayClass()) {
      IClass innermost = getInnermostTypeOfArrayClass(klass);
      return innermost == null ? 0 : getNumberOfImmediateSubclasses(innermost);
    }
    Node node = findNode(klass);
    return node.children.size();
  }

  /**
   * @param klass
   * @return the classes that immediately extend klass. if klass is an array class A[][]...[], we return array classes B[][]...[]
   *         (same dimensionality) where B is an immediate subclass of A. If A is primitive, we return the empty set.
   */
  public Collection<IClass> getImmediateSubclasses(IClass klass) {
    if (klass.isArrayClass()) {
      return getImmediateArraySubclasses(klass);
    }
    Function<Node, IClass> node2Class = new Function<Node, IClass>() {
      public IClass apply(Node n) {
        return n.klass;
      }
    };
    return Iterator2Collection.toSet(new MapIterator<Node, IClass>(findNode(klass).children.iterator(), node2Class));
  }

  private Collection<IClass> getImmediateArraySubclasses(IClass klass) {
    IClass innermost = getInnermostTypeOfArrayClass(klass);
    if (innermost == null) {
      return Collections.emptySet();
    }
    Collection<IClass> innermostSubclasses = getImmediateSubclasses(innermost);
    int dim = klass.getReference().getDimensionality();
    Collection<IClass> result = HashSetFactory.make();
    for (IClass k : innermostSubclasses) {
      TypeReference ref = k.getReference();
      for (int i = 0; i < dim; i++) {
        ref = ref.getArrayTypeForElementType();
      }
      result.add(lookupClass(ref));
    }
    return result;
  }

  /**
   * for an array class, get the innermost type, or null if it's primitive
   */
  private IClass getInnermostTypeOfArrayClass(IClass klass) {
    TypeReference result = klass.getReference();
    while (result.isArrayType()) {
      result = result.getArrayElementType();
    }
    return result.isPrimitiveType() ? null : lookupClass(result);
  }

  /**
   * @return a ClassHierarchy object representing the analysis scope
   * @throws ClassHierarchyException
   */
  public static ClassHierarchy make(AnalysisScope scope) throws ClassHierarchyException {
    if (scope == null) {
      throw new IllegalArgumentException("null scope");
    }
    return make(scope, new ClassLoaderFactoryImpl(scope.getExclusions()));
  }

  /**
   * temporarily marking this internal to avoid infinite sleep with randomly chosen IProgressMonitor.
   */
  @Internal
  public static ClassHierarchy make(AnalysisScope scope, IProgressMonitor monitor) throws ClassHierarchyException {
    if (scope == null) {
      throw new IllegalArgumentException("null scope");
    }
    return make(scope, new ClassLoaderFactoryImpl(scope.getExclusions()), monitor);
  }

  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory) throws ClassHierarchyException {
    if (scope == null) {
      throw new IllegalArgumentException("null scope");
    }
    if (factory == null) {
      throw new IllegalArgumentException("null factory");
    }
    return new ClassHierarchy(scope, factory, new NullProgressMonitor());
  }

  /**
   * temporarily marking this internal to avoid infinite sleep with randomly chosen IProgressMonitor.
   */
  @Internal
  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, IProgressMonitor monitor)
      throws ClassHierarchyException {
    return new ClassHierarchy(scope, factory, monitor);
  }

  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, Set<Language> languages)
      throws ClassHierarchyException {
    return new ClassHierarchy(scope, factory, languages, new NullProgressMonitor());
  }

  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, Language language)
      throws ClassHierarchyException {
    return new ClassHierarchy(scope, factory, language, new NullProgressMonitor());
  }

  /**
   * temporarily marking this internal to avoid infinite sleep with randomly chosen IProgressMonitor. TODO: nanny for testgen
   */
  @Internal
  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, Language language, IProgressMonitor monitor)
      throws ClassHierarchyException {
    if (factory == null) {
      throw new IllegalArgumentException("null factory");
    }
    return new ClassHierarchy(scope, factory, language, monitor);
  }

  public IClass getRootClass() {
    return root.getJavaClass();
  }

  public boolean isRootClass(IClass c) throws IllegalArgumentException {
    if (c == null) {
      throw new IllegalArgumentException("c == null");
    }
    return c.equals(root.getJavaClass());
  }

  public int getNumber(IClass c) {
    return map.get(c).left;
  }

  /**
   * A warning for when we fail to resolve the type for a checkcast
   */
  private static class ClassExclusion extends Warning {

    final TypeReference klass;

    final String message;

    ClassExclusion(TypeReference klass, String message) {
      super(Warning.MODERATE);
      this.klass = klass;
      this.message = message;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + klass + " " + message;
    }

    public static ClassExclusion create(TypeReference klass, String message) {
      return new ClassExclusion(klass, message);
    }
  }

  /**
   * Does an expression c1 x := c2 y typecheck?
   * 
   * i.e. is c2 a subtype of c1?
   * 
   * @throws IllegalArgumentException if c1 is null
   * @throws IllegalArgumentException if c2 is null
   */
  public boolean isAssignableFrom(IClass c1, IClass c2) {
    if (c2 == null) {
      throw new IllegalArgumentException("c2 is null");
    }
    if (c1 == null) {
      throw new IllegalArgumentException("c1 is null");
    }
    if (c1.isInterface()) {
      return implementsInterface(c2, c1);
    } else {
      if (c2.isInterface()) {
        return c1.equals(getRootClass());
      } else {
        return isSubclassOf(c2, c1);
      }
    }
  }

  /**
   * Does it look like an inner class? [TODO: is this definitive?]
   */
  @Deprecated
  public static boolean isInnerClass(IClass klass) throws NullPointerException {
    if (klass == null) {
      throw new IllegalArgumentException("null klass");
    }
    return klass.getName().toString().indexOf("$") > -1;
  }

}
