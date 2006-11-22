/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;

/**
 * 
 * a instance field pointer key key that carries a type filter
 * 
 * @author sfink
 */
public class InstanceFieldKeyWithFilter extends InstanceFieldKey implements FilteredPointerKey {

  private final IClass filter;

  public InstanceFieldKeyWithFilter(ClassHierarchy cha, InstanceKey instance, IField field) {
    super(instance, field);
    IClass fieldType = cha.lookupClass(field.getFieldTypeReference());
    if (fieldType == null) {
      // TODO: assertions.unreachable()
      this.filter = cha.lookupClass(TypeReference.JavaLangObject);
    } else {
      this.filter = fieldType;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PointerKey#getTypeFilter()
   */
  public IClass getTypeFilter() {
    return filter;
  }

}
