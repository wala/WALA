package com.ibm.wala.cast.util;

import com.ibm.wala.util.Atom;

public interface TargetLanguageSelector<T, C> {

  T get(Atom language, C construct);

}
