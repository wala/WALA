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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.util.Arrays;
import reflection.Reflect26.MarvelAnnotation.Priority;

public class Reflect26 {

  /** Test of Method.getAnnotation */
  public static void main(String[] args)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException,
          IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
          SecurityException {
    Class<?> c = Marvel.class;
    MarvelAnnotation annoC = c.getAnnotation(MarvelAnnotation.class);
    System.out.println("Role Attribute of Class Annotation: " + annoC.role());
    System.out.println("Name Attribute of Class Annotation: " + annoC.name());
    System.out.println("Tags Attribute of Class Annotation: " + Arrays.toString(annoC.tags()));
    System.out.println("Team Attribute of Class Annotation: " + annoC.team());
    System.out.println("Priority Attribute of Class Annotation: " + annoC.priority());

    Constructor<?>[] co = c.getDeclaredConstructors();
    MarvelAnnotation annoCo = co[0].getAnnotation(MarvelAnnotation.class);
    System.out.println("Role Attribute of Constructor Annotation: " + annoCo.role());
    System.out.println("Name Attribute of Constructor Annotation: " + annoCo.name());
    System.out.println(
        "Tags Attribute of Constructor Annotation: " + Arrays.toString(annoCo.tags()));
    System.out.println("Team Attribute of Constructor Annotation: " + annoCo.team());
    System.out.println("Priority Attribute of Constructor Annotation: " + annoCo.priority());

    Field[] fields = c.getDeclaredFields();
    MarvelAnnotation annoF = fields[0].getAnnotation(MarvelAnnotation.class);
    System.out.println("Role Attribute of Field Annotation: " + annoF.role());
    System.out.println("Name Attribute of Field Annotation: " + annoF.name());
    System.out.println("Tags Attribute of Field Annotation: " + Arrays.toString(annoF.tags()));
    System.out.println("Team Attribute of Field Annotation: " + annoF.team());
    System.out.println("Priority Attribute of Field Annotation: " + annoF.priority());

    Method[] methods = c.getMethods();
    MarvelAnnotation annoM = methods[0].getAnnotation(MarvelAnnotation.class);
    System.out.println("Role Attribute of Method Annotation: " + annoM.role());
    System.out.println("Name Attribute of Method Annotation: " + annoM.name());
    System.out.println("Tags Attribute of Method Annotation: " + Arrays.toString(annoM.tags()));
    System.out.println("Team Attribute of Method Annotation: " + annoM.team());
    System.out.println("Priority Attribute of Method Annotation: " + annoM.priority());

    Parameter[] parameters = methods[0].getParameters();
    MarvelAnnotation annoP = parameters[0].getAnnotation(MarvelAnnotation.class);
    System.out.println("Role Attribute of Parameter Annotation: " + annoP.role());
    System.out.println("Name Attribute of Parameter Annotation: " + annoP.name());
    System.out.println("Tags Attribute of Parameter Annotation: " + Arrays.toString(annoP.tags()));
    System.out.println("Team Attribute of Parameter Annotation: " + annoP.team());
    System.out.println("Priority Attribute of Parameter Annotation: " + annoP.priority());
  }

  // create a custom Annotation
  @Retention(RetentionPolicy.RUNTIME)
  @interface MarvelAnnotation {
    String role();

    String name();

    int team() default 3;

    Priority priority() default Priority.MEDIUM;

    String[] tags() default {""};

    enum Priority {
      LOW,
      MEDIUM,
      HIGH
    }
  }

  @MarvelAnnotation(
      role = "AvengersLeader",
      name = "CaptainAmerica",
      tags = {"A", "B"})
  public class Marvel {

    @MarvelAnnotation(role = "AvengersPlayer", name = "Thor")
    public String textField;

    @MarvelAnnotation(role = "AvengersPlayer", name = "IronMan", team = 5)
    public Marvel() {
      textField = "MARVEL";
    }

    @MarvelAnnotation(role = "AvengersPlayer", name = "Hulk", priority = Priority.HIGH)
    public void getCustomAnnotation(
        @MarvelAnnotation(role = "AvengersPlayer", name = "Spiderman") String text) {
      System.out.println(textField + text);
    }
  }
}
