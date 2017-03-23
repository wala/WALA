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

import java.util.List;
import java.util.Set;

public class TypeAnnotatedClass1 extends @TypeAnnotationTypeUse Object {
  
  
  @TypeAnnotationTypeUse
  List<Set<@TypeAnnotationTypeUse TypeAnnotatedClass1>> field;
  
  @TypeAnnotationTypeUse Integer foo(@TypeAnnotationTypeUse int a, @TypeAnnotationTypeUse Object b) {
    
    @TypeAnnotationTypeUse
    int x = 3;
    
    @TypeAnnotationTypeUse
    Object y = new Object();
    
    if (y instanceof @TypeAnnotationTypeUse(someKey = "lul") String) {
      x = 7;
    }
    
    try {
      throw new NullPointerException();
    } catch (@TypeAnnotationTypeUse RuntimeException e) {
      x = 911;
    }
    
    return x;
  }
}
