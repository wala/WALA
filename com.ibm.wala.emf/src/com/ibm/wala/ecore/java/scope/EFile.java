/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EFile</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.scope.EFile#getUrl <em>Url</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEFile()
 * @model
 * @generated
 */
public interface EFile extends EModule {
  /**
   * Returns the value of the '<em><b>Url</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Url</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Url</em>' attribute.
   * @see #setUrl(String)
   * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEFile_Url()
   * @model required="true"
   * @generated
   */
  String getUrl();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.scope.EFile#getUrl <em>Url</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Url</em>' attribute.
   * @see #getUrl()
   * @generated
   */
  void setUrl(String value);

} // EFile