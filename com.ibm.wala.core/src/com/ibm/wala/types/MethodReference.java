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

import java.util.HashMap;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.shrike.ShrikeUtil;
import com.ibm.wala.util.strings.Atom;

/**
 * A class to represent the reference in a class file to a method.
 */
public final class MethodReference extends MemberReference {
  /**
   * Used to canonicalize MethodReferences a mapping from Key -&gt; MethodReference
   */
  final private static HashMap<Key, MethodReference> dictionary = HashMapFactory.make();

  public final static Atom newInstanceAtom = Atom.findOrCreateUnicodeAtom("newInstance");

  private final static Descriptor newInstanceDesc = Descriptor.findOrCreateUTF8(Language.JAVA, "()Ljava/lang/Object;");

  public final static MethodReference JavaLangClassNewInstance = findOrCreate(TypeReference.JavaLangClass, newInstanceAtom,
      newInstanceDesc);

  private final static Atom ctorNewInstanceAtom = Atom.findOrCreateUnicodeAtom("newInstance");

  private final static Descriptor ctorNewInstanceDesc = Descriptor.findOrCreateUTF8(Language.JAVA, "([Ljava/lang/Object;)Ljava/lang/Object;");

  public final static MemberReference JavaLangReflectCtorNewInstance = findOrCreate(TypeReference.JavaLangReflectConstructor,
      ctorNewInstanceAtom, ctorNewInstanceDesc);

  public final static Atom forNameAtom = Atom.findOrCreateUnicodeAtom("forName");

  private final static Descriptor forNameDesc = Descriptor.findOrCreateUTF8(Language.JAVA, "(Ljava/lang/String;)Ljava/lang/Class;");

  public final static MethodReference JavaLangClassForName = findOrCreate(TypeReference.JavaLangClass, forNameAtom, forNameDesc);

  public final static Atom initAtom = Atom.findOrCreateUnicodeAtom("<init>");

  public final static Descriptor defaultInitDesc = Descriptor.findOrCreateUTF8(Language.JAVA, "()V");

  public final static Selector initSelector = new Selector(initAtom, defaultInitDesc);

  public final static Atom clinitName = Atom.findOrCreateUnicodeAtom("<clinit>");

  public final static Selector clinitSelector = new Selector(clinitName, defaultInitDesc);

  public final static Atom finalizeName = Atom.findOrCreateUnicodeAtom("finalize");

  public final static Selector finalizeSelector = new Selector(finalizeName, defaultInitDesc);

  public final static Atom runAtom = Atom.findOrCreateUnicodeAtom("run");

  public final static Descriptor runDesc = Descriptor.findOrCreateUTF8(Language.JAVA, "()Ljava/lang/Object;");

  public final static Selector runSelector = new Selector(runAtom, runDesc);

  public final static Atom equalsAtom = Atom.findOrCreateUnicodeAtom("equals");

  public final static Descriptor equalsDesc = Descriptor.findOrCreateUTF8(Language.JAVA, "(Ljava/lang/Object;)Z");

  public final static Selector equalsSelector = new Selector(equalsAtom, equalsDesc);

  public final static MethodReference lambdaMetafactory = 
      findOrCreate(
          TypeReference.LambdaMetaFactory, 
          Atom.findOrCreateUnicodeAtom("metafactory"), 
          Descriptor.findOrCreateUTF8("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"));
  
  /**
   * types of parameters to this method.
   */
  final private TypeReference[] parameterTypes;

  /**
   * return type.
   */
  private final TypeReference returnType;

  /**
   * Name and descriptor
   */
  private final Selector selector;

  /**
   * Find or create the canonical MethodReference instance for the given tuple.
   * 
   * @param mn the name of the member
   * @param md the descriptor of the member
   */
  public static synchronized MethodReference findOrCreate(TypeReference tref, Atom mn, Descriptor md) {
    if (tref == null) {
      throw new IllegalArgumentException("null tref");
    }
    Key key = new Key(tref, mn, md);

    MethodReference val = dictionary.get(key);
    if (val != null)
      return val;
    val = new MethodReference(key);
    dictionary.put(key, val);
    return val;
  }

  /**
   * Find or create the canonical MethodReference instance for the given tuple.
   * 
   * @param tref the type reference
   * @param selector the selector for the method
   * @throws IllegalArgumentException if selector is null
   */
  public static synchronized MethodReference findOrCreate(TypeReference tref, Selector selector) {
    if (selector == null) {
      throw new IllegalArgumentException("selector is null");
    }
    return findOrCreate(tref, selector.getName(), selector.getDescriptor());
  }

  public static MethodReference findOrCreate(TypeReference t, String methodName, String descriptor) throws IllegalArgumentException {
    return findOrCreate(Language.JAVA, t, methodName, descriptor);
  }
  
  public static MethodReference findOrCreate(Language l, TypeReference t, String methodName, String descriptor) throws IllegalArgumentException {
    Descriptor d = Descriptor.findOrCreateUTF8(l, descriptor);
    return findOrCreate(t, Atom.findOrCreateUnicodeAtom(methodName), d);
  }
  
  public static MethodReference findOrCreate(ClassLoaderReference loader, String methodClass, String methodName,
      String methodSignature) throws IllegalArgumentException {
    return findOrCreate(Language.JAVA, loader, methodClass, methodName, methodSignature);
  }

  public static MethodReference findOrCreate(Language l, ClassLoaderReference loader, String methodClass, String methodName,
      String methodSignature) throws IllegalArgumentException {
    TypeReference t = ShrikeUtil.makeTypeReference(loader, methodClass);
    Atom name = Atom.findOrCreateUnicodeAtom(methodName);
    Descriptor d = Descriptor.findOrCreateUTF8(l, methodSignature);
    return findOrCreate(t, name, d);
  }

  /**
   * @return the descriptor component of this member reference
   */
  public final Descriptor getDescriptor() {
    return selector.getDescriptor();
  }

  @Override
  public final String toString() {
    return "< " + getDeclaringClass().getClassLoader().getName() + ", " + getDeclaringClass().getName() + ", " + selector + " >";
  }

  MethodReference(Key key) {
    super(key.type, key.name, key.hashCode());
    selector = new Selector(key.name, key.descriptor);
    TypeName[] parameterNames = key.descriptor.getParameters();
    if (parameterNames != null) {
      parameterTypes = new TypeReference[parameterNames.length];
    } else {
      parameterTypes = null;
    }
    ClassLoaderReference loader = getDeclaringClass().getClassLoader();
    if (parameterNames != null) {
      for (int i = 0; i < parameterNames.length; i++) {
        parameterTypes[i] = TypeReference.findOrCreate(loader, parameterNames[i]);
      }
    }

    TypeName r = key.descriptor.getReturnType();
    returnType = TypeReference.findOrCreate(getDeclaringClass().getClassLoader(), r);
  }

  /**
   * @return return type of the method
   */
  public final TypeReference getReturnType() {
    return returnType;
  }

  /**
   * @return ith parameter to the method. This does NOT include the implicit "this" pointer.
   */
  public final TypeReference getParameterType(int i) throws IllegalArgumentException {
    if (parameterTypes == null || i >= parameterTypes.length) {
      throw new IllegalArgumentException("illegal parameter number " + i + " for " + this);
    }
    return parameterTypes[i];
  }

  public boolean isInit() {
    return getName().equals(MethodReference.initAtom);
  }

  /**
   * @return something like: com.foo.bar.createLargeOrder(IILjava/lang/String;Ljava/sql/Date;)Ljava/lang/Integer;
   */
  @Override
  public String getSignature() {
    // TODO: check that we're not calling this often.
    String s = getDeclaringClass().getName().toString().substring(1).replace('/', '.') + "." + getName() + getDescriptor();
    return s;
  }

  /**
   * @return something like: createLargeOrder(IILjava.lang.String;SLjava.sql.Date;)Ljava.lang.Integer;
   */
  public Selector getSelector() {
    return selector;
  }

  /**
   * This method does NOT include the implicit "this" parameter
   */
  public int getNumberOfParameters() {
    return parameterTypes == null ? 0 : parameterTypes.length;
  }

  /**
   * An identifier/selector for methods.
   */
  protected static class Key {
    private final TypeReference type;

    private final Atom name;

    private final Descriptor descriptor;

    Key(TypeReference type, Atom name, Descriptor descriptor) {
      this.type = type;
      this.name = name;
      this.descriptor = descriptor;
    }

    @Override
    public final int hashCode() {

      return 7001 * type.hashCode() + 7013 * name.hashCode() + descriptor.hashCode();
    }

    @Override
    public final boolean equals(Object other) {
      assert other != null && this.getClass().equals(other.getClass());
      Key that = (Key) other;
      return type.equals(that.type) && name.equals(that.name) && descriptor.equals(that.descriptor);
    }
  }

}
