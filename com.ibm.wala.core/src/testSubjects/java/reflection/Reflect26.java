/*
 * Copyright (c) 2021 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

public class Reflect26 {

  // create a custom Annotation
  @Retention(RetentionPolicy.RUNTIME)
  @interface Annotation {
      // This annotation has two attributes.
      public String key();
    
      public String value();
  }

  @Annotation(key = "AvengersLeader", value = "CaptainAmerica")
  public class Marvel {
    @Annotation(key = "AvengersPlayer", value = "Hulk")
    public void getCustomAnnotation()
    {
      System.out.println("MARVEL!");
    }
  }

  /** Test of Method.getAnnotation */
  public static void main(String[] args)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
    Class<?> c = Marvel.class;
    Annotation annoC = c.getAnnotation(Annotation.class);
    System.out.println("Key Attribute of Class Annotation: " + annoC.key());
    System.out.println("Value Attribute of Class Annotation: " + annoC.value());
    
    Method[] methods = c.getMethods();
    Annotation anno = methods[0].getAnnotation(Annotation.class);
    System.out.println("Key Attribute of Method Annotation: " + anno.key());
    System.out.println("Value Attribute of Method Annotation: " + anno.value());
  }
}