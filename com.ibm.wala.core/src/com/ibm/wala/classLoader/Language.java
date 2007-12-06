package com.ibm.wala.classLoader;

import java.util.Set;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;

public interface Language {

  public static Language JAVA = new LanguageImpl() {
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

    public boolean isNullType(TypeReference type) {
      return false;
    }
  };

  Atom getName();

  Language getBaseLanguage();

  void registerDerivedLanguage(Language l);

  Set<Language> getDerivedLanguages();

  TypeReference getRootType();

  TypeReference getConstantType(Object o);

  boolean isNullType(TypeReference type);

}
