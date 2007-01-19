/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope.util;

import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import com.ibm.wala.annotations.Internal;
import com.ibm.wala.ecore.java.scope.EBuiltInModule;
import com.ibm.wala.ecore.java.scope.EClassFile;
import com.ibm.wala.ecore.java.scope.EClassLoader;
import com.ibm.wala.ecore.java.scope.EClasspath;
import com.ibm.wala.ecore.java.scope.EFile;
import com.ibm.wala.ecore.java.scope.EJarFile;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.ecore.java.scope.EModule;
import com.ibm.wala.ecore.java.scope.ESourceFile;
import com.ibm.wala.ecore.java.scope.JavaScopePackage;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.scope.JavaScopePackage
 * @generated
 */
@Internal
public class JavaScopeSwitch {
  /**
   * The cached model package
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected static JavaScopePackage modelPackage;

  /**
   * Creates an instance of the switch.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public JavaScopeSwitch() {
    if (modelPackage == null) {
      modelPackage = JavaScopePackage.eINSTANCE;
    }
  }

  /**
   * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the first non-null result returned by a <code>caseXXX</code> call.
   * @generated
   */
  public Object doSwitch(EObject theEObject) {
    return doSwitch(theEObject.eClass(), theEObject);
  }

  /**
   * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the first non-null result returned by a <code>caseXXX</code> call.
   * @generated
   */
  protected Object doSwitch(EClass theEClass, EObject theEObject) {
    if (theEClass.eContainer() == modelPackage) {
      return doSwitch(theEClass.getClassifierID(), theEObject);
    }
    else {
      List eSuperTypes = theEClass.getESuperTypes();
      return
        eSuperTypes.isEmpty() ?
          defaultCase(theEObject) :
          doSwitch((EClass)eSuperTypes.get(0), theEObject);
    }
  }

  /**
   * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the first non-null result returned by a <code>caseXXX</code> call.
   * @generated
   */
  protected Object doSwitch(int classifierID, EObject theEObject) {
    switch (classifierID) {
      case JavaScopePackage.EJAVA_ANALYSIS_SCOPE: {
        EJavaAnalysisScope eJavaAnalysisScope = (EJavaAnalysisScope)theEObject;
        Object result = caseEJavaAnalysisScope(eJavaAnalysisScope);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaScopePackage.ECLASS_LOADER: {
        EClassLoader eClassLoader = (EClassLoader)theEObject;
        Object result = caseEClassLoader(eClassLoader);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaScopePackage.EMODULE: {
        EModule eModule = (EModule)theEObject;
        Object result = caseEModule(eModule);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaScopePackage.EBUILT_IN_MODULE: {
        EBuiltInModule eBuiltInModule = (EBuiltInModule)theEObject;
        Object result = caseEBuiltInModule(eBuiltInModule);
        if (result == null) result = caseEModule(eBuiltInModule);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaScopePackage.EJAR_FILE: {
        EJarFile eJarFile = (EJarFile)theEObject;
        Object result = caseEJarFile(eJarFile);
        if (result == null) result = caseEModule(eJarFile);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaScopePackage.EFILE: {
        EFile eFile = (EFile)theEObject;
        Object result = caseEFile(eFile);
        if (result == null) result = caseEModule(eFile);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaScopePackage.ECLASS_FILE: {
        EClassFile eClassFile = (EClassFile)theEObject;
        Object result = caseEClassFile(eClassFile);
        if (result == null) result = caseEFile(eClassFile);
        if (result == null) result = caseEModule(eClassFile);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaScopePackage.ESOURCE_FILE: {
        ESourceFile eSourceFile = (ESourceFile)theEObject;
        Object result = caseESourceFile(eSourceFile);
        if (result == null) result = caseEFile(eSourceFile);
        if (result == null) result = caseEModule(eSourceFile);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaScopePackage.ECLASSPATH: {
        EClasspath eClasspath = (EClasspath)theEObject;
        Object result = caseEClasspath(eClasspath);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      default: return defaultCase(theEObject);
    }
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EJava Analysis Scope</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EJava Analysis Scope</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEJavaAnalysisScope(EJavaAnalysisScope object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EClass Loader</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EClass Loader</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEClassLoader(EClassLoader object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EModule</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EModule</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEModule(EModule object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EBuilt In Module</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EBuilt In Module</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEBuiltInModule(EBuiltInModule object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EJar File</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EJar File</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEJarFile(EJarFile object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EFile</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EFile</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEFile(EFile object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EClass File</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EClass File</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEClassFile(EClassFile object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>ESource File</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>ESource File</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseESourceFile(ESourceFile object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EClasspath</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EClasspath</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEClasspath(EClasspath object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EObject</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch, but this is the last case anyway.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EObject</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject)
   * @generated
   */
  public Object defaultCase(EObject object) {
    return null;
  }

} //JavaScopeSwitch
