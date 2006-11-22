/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.j2ee.scope.impl;

import com.ibm.wala.ecore.j2ee.scope.*;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class J2EEScopeFactoryImpl extends EFactoryImpl implements J2EEScopeFactory {
  /**
   * Creates the default factory implementation.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static J2EEScopeFactory init() {
    try {
      J2EEScopeFactory theJ2EEScopeFactory = (J2EEScopeFactory)EPackage.Registry.INSTANCE.getEFactory("http:///com/ibm/wala/wala.ecore.j2ee.scope"); 
      if (theJ2EEScopeFactory != null) {
        return theJ2EEScopeFactory;
      }
    }
    catch (Exception exception) {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new J2EEScopeFactoryImpl();
  }

  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public J2EEScopeFactoryImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EObject create(EClass eClass) {
    switch (eClass.getClassifierID()) {
      case J2EEScopePackage.EJ2EE_ANALYSIS_SCOPE: return createEJ2EEAnalysisScope();
      case J2EEScopePackage.EEAR_FILE: return createEEarFile();
      case J2EEScopePackage.EWAR_FILE: return createEWarFile();
      default:
        throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EJ2EEAnalysisScope createEJ2EEAnalysisScope() {
    EJ2EEAnalysisScopeImpl ej2EEAnalysisScope = new EJ2EEAnalysisScopeImpl();
    return ej2EEAnalysisScope;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EEarFile createEEarFile() {
    EEarFileImpl eEarFile = new EEarFileImpl();
    return eEarFile;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EWarFile createEWarFile() {
    EWarFileImpl eWarFile = new EWarFileImpl();
    return eWarFile;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public J2EEScopePackage getJ2EEScopePackage() {
    return (J2EEScopePackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  public static J2EEScopePackage getPackage() {
    return J2EEScopePackage.eINSTANCE;
  }

} //J2EEScopeFactoryImpl
