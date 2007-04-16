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
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.CacheReference;
import com.ibm.wala.util.Function;
import com.ibm.wala.util.MapIterator;
import com.ibm.wala.util.ReferenceCleanser;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * Simple implementation of a class hierarchy
 * 
 * @author sfink
 */
public class ClassHierarchy implements Iterable<IClass> {

  private static final boolean DEBUG = false;

  /**
   * Descriptor of root of class hierarchy (java.lang.Object for Java)
   */
  private final TypeReference rootDescriptor;

  /**
   * mapping from IClass to Node
   */
  private HashMap<IClass, Node> map = HashMapFactory.make();

  /**
   * root node of the class hierarchy
   */
  private Node root;

  /**
   * An object which defines class loaders.
   */
  private ClassLoaderFactory factory;

  /**
   * The loaders used to define this class hierarchy.
   * 
   * XXX is order significant??
   */
  private IClassLoader[] loaders;

  /**
   * An object which tracks analysis warnings
   */
  private WarningSet warnings;

  /**
   * A mapping from IClass -> Selector -> Set of IMethod
   */
  private HashMap<IClass, Object> targetCache = HashMapFactory.make();

  /**
   * Governing analysis scope
   */
  private final AnalysisScope scope;

  /**
   * A mapping from IClass (representing an interface) -> Set of IClass that
   * implement that interface
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
   * Return a set of IClasses that holds all superclasses of klass
   * 
   * @param klass
   *          class in question
   * @return Set the result set
   */
  private Set<IClass> computeSuperclasses(IClass klass) throws ClassHierarchyException {

    if (DEBUG) {
      Trace.println("computeSuperclasses: " + klass);
    }

    Set<IClass> result = HashSetFactory.make(3);
    klass = klass.getSuperclass();

    while (klass != null) {
      if (DEBUG) {
        Trace.println("got superclass " + klass);
      }
      result.add(klass);
      klass = klass.getSuperclass();

    }
    return result;
  }

  protected ClassHierarchy(AnalysisScope scope, ClassLoaderFactory factory, WarningSet warnings, IProgressMonitor monitor)
      throws ClassHierarchyException {
    this(scope, factory, warnings, TypeReference.JavaLangObject, monitor);
  }

  /**
   * @param scope
   * @param factory
   * @param warnings
   * @param rootDescriptor
   * @throws ClassHierarchyException
   */
  private ClassHierarchy(AnalysisScope scope, ClassLoaderFactory factory, WarningSet warnings, TypeReference rootDescriptor,
      IProgressMonitor progressMonitor) throws ClassHierarchyException {
    this.scope = scope;
    this.factory = factory;
    this.warnings = warnings;
    this.rootDescriptor = rootDescriptor;
    try {
      loaders = new IClassLoader[scope.getNumberOfLoaders()];
      int idx = 0;

      progressMonitor.beginTask(null, scope.getNumberOfLoaders());
      for (Iterator<ClassLoaderReference> it = scope.getLoaders(); it.hasNext();) {
        if (progressMonitor.isCanceled()) {
          throw new CancelCHAConstructionException();
        }

        IClassLoader icl = factory.getLoader(it.next(), this, scope);
        loaders[idx++] = icl;
        addAllClasses(icl, progressMonitor);

        progressMonitor.worked(1);
      }
    } catch (IOException e) {
      throw new ClassHierarchyException("factory.getLoader failed " + e);
    } finally {
      progressMonitor.done(); // In case an exception is thrown.
    }

    // perform numbering for subclass tests.
    numberTree();
    ReferenceCleanser.registerClassHierarchy(this);
  }

  /**
   * Add all classes in a class loader to the hiearchy.
   */
  private void addAllClasses(IClassLoader loader, IProgressMonitor progressMonitor) throws CancelCHAConstructionException {
    if (DEBUG) {
      Trace.println("Add all classes from loader " + loader);
    }
    Collection<IClass> toRemove = HashSetFactory.make();
    for (Iterator<IClass> it = loader.iterateAllClasses(); it.hasNext();) {
      if (progressMonitor.isCanceled()) {
        throw new CancelCHAConstructionException();
      }
      IClass klass = it.next();
      boolean added = addClass(klass);
      if (!added) {
        toRemove.add(klass);
      }
    }
    loader.removeAll(toRemove);
    Assertions.postcondition(root != null, "failed to load root of class hierarchy");
  }

  /**
   * @param klass
   * @return true if the add succeeded; false if it failed for some reason
   */
  public boolean addClass(IClass klass) {
    if (DEBUG) {
      Trace.println("Attempt to add class " + klass);
    }
    Set<IClass> loadedSuperclasses;
    Collection loadedSuperInterfaces;
    try {
      loadedSuperclasses = computeSuperclasses(klass);
      loadedSuperInterfaces = klass.isInterface() ? null : klass.getAllImplementedInterfaces();
    } catch (ClassHierarchyException e) {
      // a little cleanup
      if (klass instanceof ShrikeClass) {
        if (DEBUG) {
          Trace.println("Exception.  Clearing " + klass);
        }
      }
      warnings.add(ClassExclusion.create(klass.getReference(), e.getMessage()));
      return false;
    }
    Node node = findOrCreateNode(klass);

    if (klass.getReference().equals(rootDescriptor)) {
      // there is only one root
      Assertions._assert(root == null);
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
        if (supernode.getJavaClass().getReference().equals(rootDescriptor)) {
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
          warnings.add(ClassExclusion.create(iface.getReference(), e.getMessage()));
          continue;
        }
        if (DEBUG && Assertions.verifyAssertions) {
          if (!iface.isInterface()) {
            Assertions._assert(false, "not an interface: " + iface);
          }
          if (klass.isInterface()) {
            Assertions._assert(false, "an interface: " + klass);
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
   * Find the possible receivers of a call to a method reference
   * 
   * @param ref
   *          method reference
   * @return the set of IMethods that this call can resolve to.
   */
  public Iterator getPossibleTargets(MethodReference ref) {
    IClassLoader loader;
    try {
      loader = factory.getLoader(ref.getDeclaringClass().getClassLoader(), this, scope);
    } catch (IOException e) {
      throw new UnimplementedError("factory.getLoader failed " + e);
    }
    IClass declaredClass;
    declaredClass = loader.lookupClass(ref.getDeclaringClass().getName(), this);
    if (declaredClass == null) {
      return EmptyIterator.instance();
    }
    Set targets = findOrCreateTargetSet(declaredClass, ref);
    return (targets.iterator());
  }

  /**
   * Find the possible receivers of a call to a method reference
   * 
   * @param ref
   *          method reference
   * @return the set of IMethods that this call can resolve to.
   */
  @SuppressWarnings("unchecked")
  private Set findOrCreateTargetSet(IClass declaredClass, MethodReference ref) {
    Map<MethodReference, Set> classCache = (Map<MethodReference, Set>) CacheReference.get(targetCache.get(declaredClass));
    if (classCache == null) {
      classCache = HashMapFactory.make(3);
      targetCache.put(declaredClass, CacheReference.make(classCache));
    }
    Set result = classCache.get(ref);
    if (result == null) {
      result = computePossibleTargets(declaredClass, ref);
      classCache.put(ref, result);
    }
    return result;
  }

  /**
   * Find the possible receivers of a call to a method reference
   * 
   * @param ref
   *          method reference
   * @return the set of IMethods that this call can resolve to.
   */
  private Set<IMethod> computePossibleTargets(IClass declaredClass, MethodReference ref) {

    if (declaredClass.isInterface()) {
      HashSet<IMethod> result = HashSetFactory.make(3);
      Set impls = implementors.get(declaredClass);
      if (impls == null) {
        // give up and return no receivers
        return Collections.emptySet();
      }
      for (Iterator it = impls.iterator(); it.hasNext();) {
        IClass klass = (IClass) it.next();
        if (Assertions.verifyAssertions) {
          Assertions._assert(!klass.isInterface());
        }
        result.addAll(computeTargetsNotInterface(ref, klass));
      }
      return result;
    } else {
      return computeTargetsNotInterface(ref, declaredClass);
    }

  }

  /**
   * Get the targets for a method ref invoked on a class klass. The klass had
   * better not be an interface.
   * 
   * @param ref
   *          method to invoke
   * @param klass
   *          declaredClass of receiver
   * @return Set the set of method implementations that might receive the
   *         message
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
   * Return the unique receiver of an invocation of method on an object of type
   * m.getDeclaredClass
   * 
   * @param m
   * @return IMethod, or null if no appropriate receiver is found.
   */
  public IMethod resolveMethod(MethodReference m) {
    IClass receiver = lookupClass(m.getDeclaringClass());
    if (receiver == null) {
      return null;
    }
    Selector selector = m.getSelector();
    return resolveMethod(receiver, selector);
  }

  /**
   * @return the canonical FieldReference that represents a given field , or
   *         null if none found
   */
  public IField resolveField(FieldReference f) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(f != null);
    }
    IClass klass = lookupClass(f.getType());
    if (klass == null) {
      return null;
    }
    return resolveField(klass, f);
  }

  /**
   * @return the canonical FieldReference that represents a given field , or
   *         null if none found
   */
  public IField resolveField(IClass klass, FieldReference f) {
    return klass.getField(f.getName());
  }

  /**
   * Return the unique receiver of an invocation of method on an object of type
   * declaredClass
   * 
   * @param receiverClass
   *          type of receiver
   * @param selector
   *          method signature
   * @return Method resolved method abstraction
   */
  public IMethod resolveMethod(IClass receiverClass, Selector selector) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(receiverClass != null);
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
   * @param clazz
   *          class in question
   * @param selector
   *          method selector
   * @return the method if found, else null
   */
  private IMethod findMethod(IClass clazz, Selector selector) {
    return clazz.getMethod(selector);
  }

  /**
   * Get the set of subclasses of a class that provide implementations of a
   * method
   * 
   * @param node
   *          abstraction of class in question
   * @param selector
   *          method signature
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
   * Number the class hierarchy tree to support efficient subclass tests. After
   * numbering the tree, n1 is a child of n2 iff n2.left <= n1.left ^ n1.left <=
   * n2.right. Described as "relative numbering" by Vitek, Horspool, and Krall,
   * OOPSLA 97
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
   * @author sfink
   * 
   * internal representation of a node in the class hiearachy, representing one
   * java class.
   */
  private final class Node {

    private final IClass klass;

    private Set<Node> children = HashSetFactory.make(3);

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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
      return klass.hashCode() * 929;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
      return this == obj;
    }

  }

  /**
   * Returns the factory.
   * 
   * @return ClassLoaderFactory
   */
  public ClassLoaderFactory getFactory() {
    return factory;
  }

  /**
   * Method getLeastCommonSuperclass.
   * 
   * @param A
   * @param B
   */
  public IClass getLeastCommonSuperclass(IClass A, IClass B) {

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
   */
  public IClass lookupClass(TypeReference A) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(A != null, "lookupClass(null) is illegal");
    }
    ClassLoaderReference loaderRef = A.getClassLoader();
    for (int i = 0; i < loaders.length; i++) {
      if (loaders[i].getReference().equals(loaderRef)) {
        IClass klass = loaders[i].lookupClass(A.getName(), this);
        if (klass != null) {
          if (DEBUG) {
            Trace.println("lookupClass: got " + klass);
          }
          if (findNode(klass) != null) {
            // it's a scalar type in the class hierarchy
            return klass;
          }
          if (klass == null) {
            Assertions._assert(klass != null, "error looking up type " + A);
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

  // /**
  // * Is c a subclass of T?
  // *
  // * @return true if C == T or C is a subclass of T; false otherwise
  // */
  // public boolean isSubclassOf(TypeReference c, TypeReference T) {
  // return isSubclassOf(lookupClass(c), T);
  // }

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

  private final static Atom syntheticLoaderName = Atom.findOrCreateUnicodeAtom("Synthetic");

  private final static ClassLoaderReference syntheticLoaderRef = new ClassLoaderReference(syntheticLoaderName);

  public boolean isSyntheticClass(IClass c) {
    return c.getClassLoader() == getLoader(syntheticLoaderRef);
  }

  /**
   * Is c a subclass of T?
   */
  public boolean isSubclassOf(IClass c, IClass T) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(c != null, "null c");
      Assertions._assert(T != null, "null T");
    }

    if (c.isArrayClass()) {
      if (T.getReference() == TypeReference.JavaLangObject) {
        return true;
      } else if (T.getReference().isArrayType()) {
        TypeReference elementType = T.getReference().getArrayElementType();
        IClass elementKlass = lookupClass(elementType);
        if (elementKlass == null) {
          // uh oh.
          warnings.add(ClassHierarchyWarning.create("could not find " + elementType));
          return false;
        }
        IClass ce = ((ArrayClass) c).getElementClass();
        if (ce == null) {
          return false;
        }
        return isSubclassOf(ce, elementKlass);
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
        // some very wacky case, like a FakeRootClass
        return false;
      }
      Node n2 = map.get(T);
      if (n2 == null) {
        // some very wacky case, like a FakeRootClass
        return false;
      }
      if (n1.left == -1) {
        /**
         * not necessary any more, due to `new Function' in javaScript if
         * (Assertions.verifyAssertions)
         * Assertions._assert(isSyntheticClass(c));
         */
        return slowIsSubclass(c, T);
      } else if (n2.left == -1) {
        /**
         * not necessary any more, due to `new Function' in javaScript if
         * (Assertions.verifyAssertions)
         * Assertions._assert(isSyntheticClass(T));
         */
        return slowIsSubclass(c, T);
      } else {
        return (n2.left <= n1.left) && (n1.left <= n2.right);
      }
    }
  }

  /**
   * Does c implement T?
   * 
   * @param c
   * @param T
   * @return true iff T is an interface and c is a class that implements T,
   * 
   */
  public boolean implementsInterface(IClass c, TypeReference T) {
    IClass tClass = lookupClass(T);
    if (Assertions.verifyAssertions) {
      if (tClass == null) {
        Assertions._assert(false, "null klass for " + T);
      }
    }
    if (!tClass.isInterface()) {
      return false;
    }
    Set impls = implementors.get(tClass);
    if (impls != null && impls.contains(c)) {
      return true;
    }
    return false;
  }

  /**
   * Return set of all subclasses of type in the Class Hierarchy TODO: Tune this
   * implementation. Consider caching if necessary.
   * 
   * @return Set of IClasses
   */
  public Collection<IClass> computeSubClasses(TypeReference type) {
    IClass T = lookupClass(type);
    if (Assertions.verifyAssertions) {
      if (T == null) {
        Assertions._assert(T != null, "null class for type " + type);
      }
    }
    // a hack: TODO: work on better caching
    if (T.getReference().equals(TypeReference.JavaLangError)) {
      if (subclassesOfError == null) {
        subclassesOfError = computeSubClassesInternal(T);
      }
      return subclassesOfError;
    } else {
      return computeSubClassesInternal(T);
    }
  }

  /**
   * Solely for optimization; return a Collection<TypeReference> representing
   * the subclassesOfError
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
   * Return set of all subclasses of type in the Class Hierarchy TODO: Tune this
   * implementation. Consider caching if necessary.
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
   * @param type
   *          an interface
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

  /**
   * @return Iterator of IClass
   */
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
   * @return the number of classes that immediately extend klass.
   */
  public int getNumberOfImmediateSubclasses(IClass klass) {
    Node node = findNode(klass);
    return node.children.size();
  }

  /**
   * @param klass
   * @return the classes that immediately extend klass.
   */
  public Collection<IClass> getImmediateSubclasses(IClass klass) {
    Function<Node, IClass> node2Class = new Function<Node, IClass>() {
      public IClass apply(Node n) {
        return n.klass;
      }
    };
    return new Iterator2Collection<IClass>(new MapIterator<Node, IClass>(findNode(klass).children.iterator(), node2Class));
  }

  /**
   * @param scope
   * @param warnings
   * @return a ClassHierarchy object representing the analysis scope
   * @throws ClassHierarchyException
   */
  public static ClassHierarchy make(AnalysisScope scope, WarningSet warnings) throws ClassHierarchyException {
    return make(scope, new ClassLoaderFactoryImpl(scope.getExclusions(), warnings), warnings);
  }

  /**
   * temporarily marking this internal to avoid infinite sleep with
   * randomly chosen IProgressMonitor.
   * TODO: nanny for testgen
   */
  @Internal
  public static ClassHierarchy make(AnalysisScope scope, WarningSet warnings, IProgressMonitor monitor)
      throws ClassHierarchyException {
    return make(scope, new ClassLoaderFactoryImpl(scope.getExclusions(), warnings), warnings, monitor);
  }

  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, WarningSet warnings)
      throws ClassHierarchyException {
    return new ClassHierarchy(scope, factory, warnings, new NullProgressMonitor());
  }

  /**
   * temporarily marking this internal to avoid infinite sleep with
   * randomly chosen IProgressMonitor.
   * TODO: nanny for testgen
   */
  @Internal
  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, WarningSet warnings, IProgressMonitor monitor)
      throws ClassHierarchyException {
    return new ClassHierarchy(scope, factory, warnings, monitor);
  }

  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, WarningSet warnings,
      TypeReference rootDescriptor) throws ClassHierarchyException {
    return new ClassHierarchy(scope, factory, warnings, rootDescriptor, new NullProgressMonitor());
  }

  /**
   * temporarily marking this internal to avoid infinite sleep with
   * randomly chosen IProgressMonitor.
   * TODO: nanny for testgen
   */
  @Internal
  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, WarningSet warnings,
      TypeReference rootDescriptor, IProgressMonitor monitor) throws ClassHierarchyException {
    return new ClassHierarchy(scope, factory, warnings, rootDescriptor, monitor);
  }

  public IClass getRootClass() {
    return root.getJavaClass();
  }

  public int getNumber(IClass c) {
    return map.get(c).left;
  }

  /**
   * @author sfink
   * 
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
   */
  public boolean isAssignableFrom(IClass c1, IClass c2) {
    assert c1 != null;
    assert c2 != null;
    if (c1.isInterface()) {
      if (c2.isInterface()) {
        return isSubclassOf(c2, c1);
      } else {
        return implementsInterface(c2, c1.getReference());
      }
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
  public static boolean isInnerClass(IClass klass) {
    return klass.getName().toString().indexOf("$") > -1;
  }

}
