/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis;

import com.ibm.wala.ecore.java.EJavaMethod;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>ELocal Pointer</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer#getValueNumber <em>Value Number</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer#getJavaMethod <em>Java Method</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage#getELocalPointer()
 * @model
 * @generated
 */
public interface ELocalPointer extends EPointer {
  /**
   * Returns the value of the '<em><b>Value Number</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Value Number</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Value Number</em>' attribute.
   * @see #setValueNumber(int)
   * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage#getELocalPointer_ValueNumber()
   * @model required="true"
   * @generated
   */
  int getValueNumber();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer#getValueNumber <em>Value Number</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Value Number</em>' attribute.
   * @see #getValueNumber()
   * @generated
   */
  void setValueNumber(int value);

  /**
   * Returns the value of the '<em><b>Java Method</b></em>' reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Java Method</em>' reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Java Method</em>' reference.
   * @see #setJavaMethod(EJavaMethod)
   * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage#getELocalPointer_JavaMethod()
   * @model required="true"
   * @generated
   */
  EJavaMethod getJavaMethod();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer#getJavaMethod <em>Java Method</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Java Method</em>' reference.
   * @see #getJavaMethod()
   * @generated
   */
  void setJavaMethod(EJavaMethod value);

} // ELocalPointer