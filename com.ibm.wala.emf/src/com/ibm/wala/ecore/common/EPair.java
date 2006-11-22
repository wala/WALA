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
 * A representation of the model object '<em><b>EPair</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.common.EPair#getX <em>X</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.common.EPair#getY <em>Y</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.common.CommonPackage#getEPair()
 * @model
 * @generated
 */
public interface EPair extends EObject {
  /**
   * Returns the value of the '<em><b>X</b></em>' reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>X</em>' reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>X</em>' reference.
   * @see #setX(EObject)
   * @see com.ibm.wala.ecore.common.CommonPackage#getEPair_X()
   * @model required="true"
   * @generated
   */
  EObject getX();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.common.EPair#getX <em>X</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>X</em>' reference.
   * @see #getX()
   * @generated
   */
  void setX(EObject value);

  /**
   * Returns the value of the '<em><b>Y</b></em>' reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Y</em>' reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Y</em>' reference.
   * @see #setY(EObject)
   * @see com.ibm.wala.ecore.common.CommonPackage#getEPair_Y()
   * @model required="true"
   * @generated
   */
  EObject getY();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.common.EPair#getY <em>Y</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Y</em>' reference.
   * @see #getY()
   * @generated
   */
  void setY(EObject value);

} // EPair