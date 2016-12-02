/*******************************************************************************
 * Copyright (c) 2016 Joana IFC project,
 * Programming Paradigms Group,
 * Karlsruhe Institute of Technology (KIT).
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    martin.hecker@kit.edu, KIT - 
 *******************************************************************************/
package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE_USE})
public @interface TypeAnnotationTypeUse {
  String someKey() default "lol";
}
