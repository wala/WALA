package com.ibm.wala.classLoader;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;

public interface Language {

  public static Language JAVA = new Language() {
    public Atom getName() {
      return ClassLoaderReference.Java;
    }

    public TypeReference getRootType() {
      return TypeReference.JavaLangObject;
    }

    public TypeReference getConstantType(Object o) {
      if (o instanceof String) {
        return TypeReference.JavaLangString;
      } else {
        return null;
      }
    }
  };

  Atom getName();

  TypeReference getRootType();

  TypeReference getConstantType(Object o);

}