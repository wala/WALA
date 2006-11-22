/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java;

import com.ibm.wala.ecore.common.EObjectWithContainerId;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EJava Method</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.EJavaMethod#getMethodName <em>Method Name</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.EJavaMethod#getDescriptor <em>Descriptor</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.EJavaMethod#getJavaClass <em>Java Class</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.EJavaMethod#getSignature <em>Signature</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.JavaPackage#getEJavaMethod()
 * @model
 * @generated
 */
public interface EJavaMethod extends EObjectWithContainerId {
  /**
   * Returns the value of the '<em><b>Method Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Method Name</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Method Name</em>' attribute.
   * @see #setMethodName(String)
   * @see com.ibm.wala.ecore.java.JavaPackage#getEJavaMethod_MethodName()
   * @model required="true"
   * @generated
   */
  String getMethodName();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.EJavaMethod#getMethodName <em>Method Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Method Name</em>' attribute.
   * @see #getMethodName()
   * @generated
   */
  void setMethodName(String value);

  /**
   * Returns the value of the '<em><b>Descriptor</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Descriptor</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Descriptor</em>' attribute.
   * @see #setDescriptor(String)
   * @see com.ibm.wala.ecore.java.JavaPackage#getEJavaMethod_Descriptor()
   * @model required="true"
   * @generated
   */
  String getDescriptor();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.EJavaMethod#getDescriptor <em>Descriptor</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Descriptor</em>' attribute.
   * @see #getDescriptor()
   * @generated
   */
  void setDescriptor(String value);

  /**
   * Returns the value of the '<em><b>Java Class</b></em>' reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Java Class</em>' reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Java Class</em>' reference.
   * @see #setJavaClass(EJavaClass)
   * @see com.ibm.wala.ecore.java.JavaPackage#getEJavaMethod_JavaClass()
   * @model required="true"
   * @generated
   */
  EJavaClass getJavaClass();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.EJavaMethod#getJavaClass <em>Java Class</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Java Class</em>' reference.
   * @see #getJavaClass()
   * @generated
   */
  void setJavaClass(EJavaClass value);

  /**
   * Returns the value of the '<em><b>Signature</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Signature</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Signature</em>' attribute.
   * @see com.ibm.wala.ecore.java.JavaPackage#getEJavaMethod_Signature()
   * @model transient="true" changeable="false" volatile="true" derived="true"
   * @generated
   */
  String getSignature();

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @model kind="operation" ordered="false"
   * @generated
   */
  boolean isClinit();

} // EJavaMethod