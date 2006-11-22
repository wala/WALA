/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.perf;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.perf.PerfPackage
 * @generated
 */
public interface PerfFactory extends EFactory {
  /**
   * The singleton instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  PerfFactory eINSTANCE = com.ibm.wala.ecore.perf.impl.PerfFactoryImpl.init();

  /**
   * Returns a new object of class '<em>EPhase Timing</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EPhase Timing</em>'.
   * @generated
   */
  EPhaseTiming createEPhaseTiming();

  /**
   * Returns the package supported by this factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the package supported by this factory.
   * @generated
   */
  PerfPackage getPerfPackage();

} //PerfFactory
