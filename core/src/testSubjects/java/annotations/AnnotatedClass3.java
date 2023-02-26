/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package annotations;

@AnnotationWithParams(strParam = "classStrParam")
public class AnnotatedClass3 {

  @AnnotationWithParams(
      enumParam = AnnotationEnum.VAL1,
      strArrParam = {"biz", "boz"},
      annotParam = @AnnotationWithSingleParam("sdfevs"),
      strParam = "sdfsevs",
      intParam = 25,
      klassParam = Integer.class)
  public static void foo() {}

  @AnnotationWithParams(strArrParam = {})
  public static void emptyArray() {}
}
