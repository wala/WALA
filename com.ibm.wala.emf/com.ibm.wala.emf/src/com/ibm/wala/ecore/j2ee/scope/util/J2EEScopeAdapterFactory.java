/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.j2ee.scope.util;

import com.ibm.wala.ecore.j2ee.scope.*;

import com.ibm.wala.ecore.java.scope.EJarFile;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.ecore.java.scope.EModule;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.j2ee.scope.J2EEScopePackage
 * @generated
 */
public class J2EEScopeAdapterFactory extends AdapterFactoryImpl {
  /**
   * The cached model package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected static J2EEScopePackage modelPackage;

  /**
   * Creates an instance of the adapter factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public J2EEScopeAdapterFactory() {
    if (modelPackage == null) {
      modelPackage = J2EEScopePackage.eINSTANCE;
    }
  }

  /**
   * Returns whether this factory is applicable for the type of the object.
   * <!-- begin-user-doc -->
   * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
   * <!-- end-user-doc -->
   * @return whether this factory is applicable for the type of the object.
   * @generated
   */
  public boolean isFactoryForType(Object object) {
    if (object == modelPackage) {
      return true;
    }
    if (object instanceof EObject) {
      return ((EObject)object).eClass().getEPackage() == modelPackage;
    }
    return false;
  }

  /**
   * The switch the delegates to the <code>createXXX</code> methods.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected J2EEScopeSwitch modelSwitch =
    new J2EEScopeSwitch() {
      public Object caseEJ2EEAnalysisScope(EJ2EEAnalysisScope object) {
        return createEJ2EEAnalysisScopeAdapter();
      }
      public Object caseEEarFile(EEarFile object) {
        return createEEarFileAdapter();
      }
      public Object caseEWarFile(EWarFile object) {
        return createEWarFileAdapter();
      }
      public Object caseEJavaAnalysisScope(EJavaAnalysisScope object) {
        return createEJavaAnalysisScopeAdapter();
      }
      public Object caseEModule(EModule object) {
        return createEModuleAdapter();
      }
      public Object caseEJarFile(EJarFile object) {
        return createEJarFileAdapter();
      }
      public Object defaultCase(EObject object) {
        return createEObjectAdapter();
      }
    };

  /**
   * Creates an adapter for the <code>target</code>.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param target the object to adapt.
   * @return the adapter for the <code>target</code>.
   * @generated
   */
  public Adapter createAdapter(Notifier target) {
    return (Adapter)modelSwitch.doSwitch((EObject)target);
  }


  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.j2ee.scope.EJ2EEAnalysisScope <em>EJ2EE Analysis Scope</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.j2ee.scope.EJ2EEAnalysisScope
   * @generated
   */
  public Adapter createEJ2EEAnalysisScopeAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.j2ee.scope.EEarFile <em>EEar File</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.j2ee.scope.EEarFile
   * @generated
   */
  public Adapter createEEarFileAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.j2ee.scope.EWarFile <em>EWar File</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.j2ee.scope.EWarFile
   * @generated
   */
  public Adapter createEWarFileAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.scope.EJavaAnalysisScope <em>EJava Analysis Scope</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.scope.EJavaAnalysisScope
   * @generated
   */
  public Adapter createEJavaAnalysisScopeAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.scope.EModule <em>EModule</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.scope.EModule
   * @generated
   */
  public Adapter createEModuleAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.scope.EJarFile <em>EJar File</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.scope.EJarFile
   * @generated
   */
  public Adapter createEJarFileAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for the default case.
   * <!-- begin-user-doc -->
   * This default implementation returns null.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @generated
   */
  public Adapter createEObjectAdapter() {
    return null;
  }

} //J2EEScopeAdapterFactory
