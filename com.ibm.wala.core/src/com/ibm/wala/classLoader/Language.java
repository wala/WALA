package com.ibm.wala.classLoader;

import java.util.Set;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

public interface Language {

  public static Language JAVA = new LanguageImpl() {
    public Atom getName() {
      return ClassLoaderReference.Java;
    }

    public TypeReference getRootType() {
      return TypeReference.JavaLangObject;
    }

    public TypeReference getThrowableType() {
      return TypeReference.JavaLangThrowable;
    }

    public TypeReference getConstantType(Object o) {
      if (o instanceof String) {
        return TypeReference.JavaLangString;
      } else if (o instanceof IMethod) {
        IMethod m = (IMethod) o;
        return m.isInit() ? TypeReference.JavaLangReflectConstructor : TypeReference.JavaLangReflectMethod;
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

  TypeReference getThrowableType();

  TypeReference getConstantType(Object o);

  boolean isNullType(TypeReference type);

}
