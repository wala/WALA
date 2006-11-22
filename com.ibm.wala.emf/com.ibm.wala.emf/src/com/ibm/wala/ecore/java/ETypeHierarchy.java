/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java;

import com.ibm.wala.ecore.common.ERelation;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EType Hierarchy</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.ETypeHierarchy#getClasses <em>Classes</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.ETypeHierarchy#getInterfaces <em>Interfaces</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.ETypeHierarchy#getImplements <em>Implements</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.JavaPackage#getETypeHierarchy()
 * @model
 * @generated
 */
public interface ETypeHierarchy extends EObject {
  /**
   * Returns the value of the '<em><b>Classes</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Classes</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Classes</em>' containment reference.
   * @see #setClasses(EClassHierarchy)
   * @see com.ibm.wala.ecore.java.JavaPackage#getETypeHierarchy_Classes()
   * @model containment="true" required="true"
   * @generated
   */
  EClassHierarchy getClasses();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.ETypeHierarchy#getClasses <em>Classes</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Classes</em>' containment reference.
   * @see #getClasses()
   * @generated
   */
  void setClasses(EClassHierarchy value);

  /**
   * Returns the value of the '<em><b>Interfaces</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Interfaces</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Interfaces</em>' containment reference.
   * @see #setInterfaces(EInterfaceHierarchy)
   * @see com.ibm.wala.ecore.java.JavaPackage#getETypeHierarchy_Interfaces()
   * @model containment="true" required="true"
   * @generated
   */
  EInterfaceHierarchy getInterfaces();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.ETypeHierarchy#getInterfaces <em>Interfaces</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Interfaces</em>' containment reference.
   * @see #getInterfaces()
   * @generated
   */
  void setInterfaces(EInterfaceHierarchy value);

  /**
   * Returns the value of the '<em><b>Implements</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Implements</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Implements</em>' containment reference.
   * @see #setImplements(ERelation)
   * @see com.ibm.wala.ecore.java.JavaPackage#getETypeHierarchy_Implements()
   * @model containment="true" required="true"
   * @generated
   */
  ERelation getImplements();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.ETypeHierarchy#getImplements <em>Implements</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Implements</em>' containment reference.
   * @see #getImplements()
   * @generated
   */
  void setImplements(ERelation value);

} // ETypeHierarchy