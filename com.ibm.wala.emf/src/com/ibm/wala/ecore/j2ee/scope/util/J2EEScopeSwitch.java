/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.j2ee.scope.util;

import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import com.ibm.wala.ecore.j2ee.scope.EEarFile;
import com.ibm.wala.ecore.j2ee.scope.EJ2EEAnalysisScope;
import com.ibm.wala.ecore.j2ee.scope.EWarFile;
import com.ibm.wala.ecore.j2ee.scope.J2EEScopePackage;
import com.ibm.wala.ecore.java.scope.EJarFile;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.ecore.java.scope.EModule;

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
 * @see com.ibm.wala.ecore.j2ee.scope.J2EEScopePackage
 * @generated
 */
public class J2EEScopeSwitch {
  /**
   * The cached model package
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected static J2EEScopePackage modelPackage;

  /**
   * Creates an instance of the switch.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public J2EEScopeSwitch() {
    if (modelPackage == null) {
      modelPackage = J2EEScopePackage.eINSTANCE;
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
      case J2EEScopePackage.EJ2EE_ANALYSIS_SCOPE: {
        EJ2EEAnalysisScope ej2EEAnalysisScope = (EJ2EEAnalysisScope)theEObject;
        Object result = caseEJ2EEAnalysisScope(ej2EEAnalysisScope);
        if (result == null) result = caseEJavaAnalysisScope(ej2EEAnalysisScope);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case J2EEScopePackage.EEAR_FILE: {
        EEarFile eEarFile = (EEarFile)theEObject;
        Object result = caseEEarFile(eEarFile);
        if (result == null) result = caseEJarFile(eEarFile);
        if (result == null) result = caseEModule(eEarFile);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case J2EEScopePackage.EWAR_FILE: {
        EWarFile eWarFile = (EWarFile)theEObject;
        Object result = caseEWarFile(eWarFile);
        if (result == null) result = caseEJarFile(eWarFile);
        if (result == null) result = caseEModule(eWarFile);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      default: return defaultCase(theEObject);
    }
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EJ2EE Analysis Scope</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EJ2EE Analysis Scope</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEJ2EEAnalysisScope(EJ2EEAnalysisScope object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EEar File</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EEar File</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEEarFile(EEarFile object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EWar File</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EWar File</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEWarFile(EWarFile object) {
    return null;
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

} //J2EEScopeSwitch
