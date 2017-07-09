/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package annotations;

public @interface AnnotationWithParams {

  String strParam() default "strdef";
  int intParam() default 0;
  Class<?> klassParam() default Object.class;
  AnnotationEnum enumParam() default AnnotationEnum.VAL2;
  String[] strArrParam() default {"foo","baz"};
  int[] intArrParam() default {3,4};
  AnnotationWithSingleParam annotParam() default @AnnotationWithSingleParam("fsf");
}
