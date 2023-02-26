package com.ibm.wala.cast.loader;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import java.util.Collection;
import java.util.Collections;

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
    return "<field " + name + '>';
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cls == null) ? 0 : cls.hashCode());
    result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
    result = prime * result + (isStatic ? 1231 : 1237);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    AstDynamicField other = (AstDynamicField) obj;
    if (cls == null) {
      if (other.cls != null) return false;
    } else if (!cls.equals(other.cls)) return false;
    if (descriptor == null) {
      if (other.descriptor != null) return false;
    } else if (!descriptor.equals(other.descriptor)) return false;
    if (isStatic != other.isStatic) return false;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    return true;
  }
}
