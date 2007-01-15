/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.callGraph.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import com.ibm.wala.annotations.Internal;
import com.ibm.wala.ecore.java.callGraph.CallGraphFactory;
import com.ibm.wala.ecore.java.callGraph.CallGraphPackage;
import com.ibm.wala.ecore.java.callGraph.ECallGraph;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class CallGraphFactoryImpl extends EFactoryImpl implements CallGraphFactory {
  /**
   * Creates the default factory implementation.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static CallGraphFactory init() {
    try {
      CallGraphFactory theCallGraphFactory = (CallGraphFactory)EPackage.Registry.INSTANCE.getEFactory("http:///com/ibm/wala/wala.ecore.java.callGraph"); 
      if (theCallGraphFactory != null) {
        return theCallGraphFactory;
      }
    }
    catch (Exception exception) {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new CallGraphFactoryImpl();
  }

  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Internal
  public CallGraphFactoryImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EObject create(EClass eClass) {
    switch (eClass.getClassifierID()) {
      case CallGraphPackage.ECALL_GRAPH: return createECallGraph();
      default:
        throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ECallGraph createECallGraph() {
    ECallGraphImpl eCallGraph = new ECallGraphImpl();
    return eCallGraph;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public CallGraphPackage getCallGraphPackage() {
    return (CallGraphPackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  public static CallGraphPackage getPackage() {
    return CallGraphPackage.eINSTANCE;
  }

} //CallGraphFactoryImpl
