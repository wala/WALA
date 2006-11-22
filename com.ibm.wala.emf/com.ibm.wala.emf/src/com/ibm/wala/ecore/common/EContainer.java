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
 * A representation of the model object '<em><b>EContainer</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.common.EContainer#getContainees <em>Containees</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.common.CommonPackage#getEContainer()
 * @model
 * @generated
 */
public interface EContainer extends ECollection {
  /**
   * Returns the value of the '<em><b>Containees</b></em>' containment reference list.
   * The list contents are of type {@link org.eclipse.emf.ecore.EObject}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Containees</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Containees</em>' containment reference list.
   * @see com.ibm.wala.ecore.common.CommonPackage#getEContainer_Containees()
   * @model type="org.eclipse.emf.ecore.EObject" containment="true"
   * @generated
   */
  EList getContainees();

} // EContainer