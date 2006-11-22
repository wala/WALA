/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.common;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>ECollection</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.common.ECollection#getContents <em>Contents</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.common.CommonPackage#getECollection()
 * @model interface="true" abstract="true"
 * @generated
 */
public interface ECollection extends EObject {
  /**
   * Returns the value of the '<em><b>Contents</b></em>' reference list.
   * The list contents are of type {@link org.eclipse.emf.ecore.EObject}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Contents</em>' reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Contents</em>' reference list.
   * @see com.ibm.wala.ecore.common.CommonPackage#getECollection_Contents()
   * @model type="org.eclipse.emf.ecore.EObject" transient="true" volatile="true"
   * @generated
   */
  EList getContents();

} // ECollection