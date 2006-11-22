/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.graph;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.graph.GraphPackage
 * @generated
 */
public interface GraphFactory extends EFactory {
  /**
   * The singleton instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  GraphFactory eINSTANCE = com.ibm.wala.ecore.graph.impl.GraphFactoryImpl.init();

  /**
   * Returns a new object of class '<em>EGraph</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EGraph</em>'.
   * @generated
   */
  EGraph createEGraph();

  /**
   * Returns a new object of class '<em>ETree</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>ETree</em>'.
   * @generated
   */
  ETree createETree();

  /**
   * Returns the package supported by this factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the package supported by this factory.
   * @generated
   */
  GraphPackage getGraphPackage();

} //GraphFactory
