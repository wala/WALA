package com.ibm.wala.classLoader;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

import java.util.Collection;
import java.util.Collections;

/**
 * dummy class representing a missing superclass
 */
public class PhantomClass extends SyntheticClass {

  /**
   * @param T type reference describing this class
   * @param cha
   */
  public PhantomClass(TypeReference T, IClassHierarchy cha) {
    super(T, cha);
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public int getModifiers() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public IClass getSuperclass() {
    return getClassHierarchy().getRootClass();
  }

  @Override
  public Collection<? extends IClass> getDirectInterfaces() {
    return Collections.emptySet();
  }

  @Override
  public Collection<IClass> getAllImplementedInterfaces() {
    return Collections.emptySet();
  }

  @Override
  public IMethod getMethod(Selector selector) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IField getField(Atom name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IMethod getClassInitializer() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<? extends IMethod> getDeclaredMethods() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<IField> getAllInstanceFields() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<IField> getAllStaticFields() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<IField> getAllFields() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<? extends IMethod> getAllMethods() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<IField> getDeclaredInstanceFields() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<IField> getDeclaredStaticFields() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isReferenceType() {
    return true;
  }
}
