/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage
 * @generated
 */
public interface PointerAnalysisFactory extends EFactory {
  /**
   * The singleton instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  PointerAnalysisFactory eINSTANCE = com.ibm.wala.ecore.java.pointerAnalysis.impl.PointerAnalysisFactoryImpl.init();

  /**
   * Returns a new object of class '<em>EInstance Field</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EInstance Field</em>'.
   * @generated
   */
  EInstanceField createEInstanceField();

  /**
   * Returns a new object of class '<em>EArray Contents</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EArray Contents</em>'.
   * @generated
   */
  EArrayContents createEArrayContents();

  /**
   * Returns a new object of class '<em>EStatic Field</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EStatic Field</em>'.
   * @generated
   */
  EStaticField createEStaticField();

  /**
   * Returns a new object of class '<em>ELocal Pointer</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>ELocal Pointer</em>'.
   * @generated
   */
  ELocalPointer createELocalPointer();

  /**
   * Returns a new object of class '<em>EReturn Value Pointer</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EReturn Value Pointer</em>'.
   * @generated
   */
  EReturnValuePointer createEReturnValuePointer();

  /**
   * Returns a new object of class '<em>EJava Class Instance</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EJava Class Instance</em>'.
   * @generated
   */
  EJavaClassInstance createEJavaClassInstance();

  /**
   * Returns a new object of class '<em>EHeap Graph</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EHeap Graph</em>'.
   * @generated
   */
  EHeapGraph createEHeapGraph();

  /**
   * Returns the package supported by this factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the package supported by this factory.
   * @generated
   */
  PointerAnalysisPackage getPointerAnalysisPackage();

} //PointerAnalysisFactory
