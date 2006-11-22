/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.callGraph;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.callGraph.CallGraphPackage
 * @generated
 */
public interface CallGraphFactory extends EFactory {
  /**
   * The singleton instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  CallGraphFactory eINSTANCE = com.ibm.wala.ecore.java.callGraph.impl.CallGraphFactoryImpl.init();

  /**
   * Returns a new object of class '<em>ECall Graph</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>ECall Graph</em>'.
   * @generated
   */
  ECallGraph createECallGraph();

  /**
   * Returns the package supported by this factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the package supported by this factory.
   * @generated
   */
  CallGraphPackage getCallGraphPackage();

} //CallGraphFactory
