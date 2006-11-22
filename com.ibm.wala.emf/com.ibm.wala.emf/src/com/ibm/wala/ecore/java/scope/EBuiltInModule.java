/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EBuilt In Module</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.scope.EBuiltInModule#getId <em>Id</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEBuiltInModule()
 * @model
 * @generated
 */
public interface EBuiltInModule extends EModule {
  /**
   * Returns the value of the '<em><b>Id</b></em>' attribute.
   * The literals are from the enumeration {@link com.ibm.wala.ecore.java.scope.EBuiltInResource}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Id</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Id</em>' attribute.
   * @see com.ibm.wala.ecore.java.scope.EBuiltInResource
   * @see #setId(EBuiltInResource)
   * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEBuiltInModule_Id()
   * @model required="true"
   * @generated
   */
  EBuiltInResource getId();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.scope.EBuiltInModule#getId <em>Id</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Id</em>' attribute.
   * @see com.ibm.wala.ecore.java.scope.EBuiltInResource
   * @see #getId()
   * @generated
   */
  void setId(EBuiltInResource value);

} // EBuiltInModule