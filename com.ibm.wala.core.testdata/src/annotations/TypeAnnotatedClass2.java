/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Martin Hecker, KIT - initial implementation
 *******************************************************************************/
package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

public class TypeAnnotatedClass2 extends @TypeAnnotationTypeUse Object {
  
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface A {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface B {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface C {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface D {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface E {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface F {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface G {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface H {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface I {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface J {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface K {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface L {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE_USE})
  @interface M {}
  
  @A Map<@B ? extends @C String, @D List<@E Object>>        field1;
  @I String @F [] @G [] @H []                               field2;
  @A List<@B Comparable<@F Object @C [] @D [] @E []>>       field3;
  @C Outer . @B Middle . @A Inner                           field4;
  Outer2 . Middle<@D Foo . @C Bar> . Inner<@B String @A []> field5;
}

class Outer {
  class Middle {
    class Inner {
    }
  }
}

class Outer2 {
  @SuppressWarnings("unused")
  class Middle<S> {
    class Inner<T> {
    }
  }
}

class Foo {
  class Bar {
  }
}
