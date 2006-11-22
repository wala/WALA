/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EInstance Field</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.EInstanceField#getFieldName <em>Field Name</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage#getEInstanceField()
 * @model
 * @generated
 */
public interface EInstanceField extends EPointer {
  /**
   * Returns the value of the '<em><b>Field Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Field Name</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Field Name</em>' attribute.
   * @see #setFieldName(String)
   * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage#getEInstanceField_FieldName()
   * @model
   * @generated
   */
  String getFieldName();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.pointerAnalysis.EInstanceField#getFieldName <em>Field Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Field Name</em>' attribute.
   * @see #getFieldName()
   * @generated
   */
  void setFieldName(String value);

} // EInstanceField