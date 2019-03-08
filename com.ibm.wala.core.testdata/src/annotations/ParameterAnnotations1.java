/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package annotations;

public class ParameterAnnotations1 {

  public static void foo(@RuntimeVisableAnnotation String s) {}

  public static void bar(
      @AnnotationWithParams(
              enumParam = AnnotationEnum.VAL1,
              strArrParam = {"biz", "boz"},
              annotParam = @AnnotationWithSingleParam("sdfevs"),
              strParam = "sdfsevs",
              intParam = 25,
              klassParam = Integer.class)
          Integer i) {}

  public static void foo2(
      @RuntimeVisableAnnotation String s, @RuntimeInvisableAnnotation Integer i) {}

  public void foo3(@RuntimeVisableAnnotation String s, @RuntimeInvisableAnnotation Integer i) {}

  public void foo4(@RuntimeInvisableAnnotation @RuntimeVisableAnnotation String s, Integer i) {}
}
