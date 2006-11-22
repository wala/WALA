/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.common;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EObject With Container Id</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.common.EObjectWithContainerId#getId <em>Id</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.common.CommonPackage#getEObjectWithContainerId()
 * @model
 * @generated
 */
public interface EObjectWithContainerId extends EObject {
  /**
   * Returns the value of the '<em><b>Id</b></em>' attribute.
   * The default value is <code>"-1"</code>.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Id</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Id</em>' attribute.
   * @see #setId(int)
   * @see com.ibm.wala.ecore.common.CommonPackage#getEObjectWithContainerId_Id()
   * @model default="-1" id="true" required="true"
   * @generated
   */
  int getId();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.common.EObjectWithContainerId#getId <em>Id</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Id</em>' attribute.
   * @see #getId()
   * @generated
   */
  void setId(int value);

} // EObjectWithContainerId