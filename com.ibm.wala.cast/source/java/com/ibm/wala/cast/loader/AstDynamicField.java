package com.ibm.wala.cast.loader;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.strings.Atom;

public class AstDynamicField implements IField {
  private final boolean isStatic;
  private final TypeReference descriptor;
  private final IClass cls;
  private final Atom name;

  public AstDynamicField(boolean isStatic, IClass cls, Atom name, TypeReference descriptor) {
    this.isStatic = isStatic;
    this.descriptor = descriptor;
    this.cls = cls;
    this.name = name;
  }

  @Override
  public String toString() {
    return "<field " + name + ">";
  }

  @Override
  public IClass getDeclaringClass() {
    return cls;
  }

  @Override
  public Atom getName() {
    return name;
  }

  @Override
  public TypeReference getFieldTypeReference() {
    return descriptor;
  }

  @Override
  public FieldReference getReference() {
    return FieldReference.findOrCreate(cls.getReference(), name, descriptor);
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public boolean isProtected() {
    return false;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public boolean isVolatile() {
    return false;
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return cls.getClassHierarchy();
  }

  @Override
  public Collection<Annotation> getAnnotations() {
    return Collections.emptySet();
  }
}
