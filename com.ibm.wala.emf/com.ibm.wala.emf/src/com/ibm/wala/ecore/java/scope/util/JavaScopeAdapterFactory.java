/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope.util;

import com.ibm.wala.ecore.java.scope.*;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.scope.JavaScopePackage
 * @generated
 */
public class JavaScopeAdapterFactory extends AdapterFactoryImpl {
  /**
   * The cached model package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected static JavaScopePackage modelPackage;

  /**
   * Creates an instance of the adapter factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public JavaScopeAdapterFactory() {
    if (modelPackage == null) {
      modelPackage = JavaScopePackage.eINSTANCE;
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
  protected JavaScopeSwitch modelSwitch =
    new JavaScopeSwitch() {
      public Object caseEJavaAnalysisScope(EJavaAnalysisScope object) {
        return createEJavaAnalysisScopeAdapter();
      }
      public Object caseEClassLoader(EClassLoader object) {
        return createEClassLoaderAdapter();
      }
      public Object caseEModule(EModule object) {
        return createEModuleAdapter();
      }
      public Object caseEBuiltInModule(EBuiltInModule object) {
        return createEBuiltInModuleAdapter();
      }
      public Object caseEJarFile(EJarFile object) {
        return createEJarFileAdapter();
      }
      public Object caseEFile(EFile object) {
        return createEFileAdapter();
      }
      public Object caseEClassFile(EClassFile object) {
        return createEClassFileAdapter();
      }
      public Object caseESourceFile(ESourceFile object) {
        return createESourceFileAdapter();
      }
      public Object caseEClasspath(EClasspath object) {
        return createEClasspathAdapter();
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
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.scope.EClassLoader <em>EClass Loader</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.scope.EClassLoader
   * @generated
   */
  public Adapter createEClassLoaderAdapter() {
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
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.scope.EBuiltInModule <em>EBuilt In Module</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.scope.EBuiltInModule
   * @generated
   */
  public Adapter createEBuiltInModuleAdapter() {
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
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.scope.EFile <em>EFile</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.scope.EFile
   * @generated
   */
  public Adapter createEFileAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.scope.EClassFile <em>EClass File</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.scope.EClassFile
   * @generated
   */
  public Adapter createEClassFileAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.scope.ESourceFile <em>ESource File</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.scope.ESourceFile
   * @generated
   */
  public Adapter createESourceFileAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.scope.EClasspath <em>EClasspath</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.scope.EClasspath
   * @generated
   */
  public Adapter createEClasspathAdapter() {
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

} //JavaScopeAdapterFactory
