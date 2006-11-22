/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.common;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>ENot Container</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.common.ENotContainer#getElements <em>Elements</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.common.CommonPackage#getENotContainer()
 * @model
 * @generated
 */
public interface ENotContainer extends ECollection {
  /**
   * Returns the value of the '<em><b>Elements</b></em>' reference list.
   * The list contents are of type {@link org.eclipse.emf.ecore.EObject}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Elements</em>' reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Elements</em>' reference list.
   * @see com.ibm.wala.ecore.common.CommonPackage#getENotContainer_Elements()
   * @model type="org.eclipse.emf.ecore.EObject"
   * @generated
   */
  EList getElements();

} // ENotContainer