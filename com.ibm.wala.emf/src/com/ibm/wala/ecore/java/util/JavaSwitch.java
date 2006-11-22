/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.util;

import com.ibm.wala.ecore.common.EObjectWithContainerId;

import com.ibm.wala.ecore.graph.EGraph;
import com.ibm.wala.ecore.graph.ETree;

import com.ibm.wala.ecore.java.*;

import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

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
 * @see com.ibm.wala.ecore.java.JavaPackage
 * @generated
 */
public class JavaSwitch {
  /**
   * The cached model package
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected static JavaPackage modelPackage;

  /**
   * Creates an instance of the switch.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public JavaSwitch() {
    if (modelPackage == null) {
      modelPackage = JavaPackage.eINSTANCE;
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
      case JavaPackage.EJAVA_CLASS: {
        EJavaClass eJavaClass = (EJavaClass)theEObject;
        Object result = caseEJavaClass(eJavaClass);
        if (result == null) result = caseEObjectWithContainerId(eJavaClass);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaPackage.EJAVA_METHOD: {
        EJavaMethod eJavaMethod = (EJavaMethod)theEObject;
        Object result = caseEJavaMethod(eJavaMethod);
        if (result == null) result = caseEObjectWithContainerId(eJavaMethod);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaPackage.ECALL_SITE: {
        ECallSite eCallSite = (ECallSite)theEObject;
        Object result = caseECallSite(eCallSite);
        if (result == null) result = caseEObjectWithContainerId(eCallSite);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaPackage.ECLASS_HIERARCHY: {
        EClassHierarchy eClassHierarchy = (EClassHierarchy)theEObject;
        Object result = caseEClassHierarchy(eClassHierarchy);
        if (result == null) result = caseETree(eClassHierarchy);
        if (result == null) result = caseEGraph(eClassHierarchy);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaPackage.EINTERFACE_HIERARCHY: {
        EInterfaceHierarchy eInterfaceHierarchy = (EInterfaceHierarchy)theEObject;
        Object result = caseEInterfaceHierarchy(eInterfaceHierarchy);
        if (result == null) result = caseEGraph(eInterfaceHierarchy);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case JavaPackage.ETYPE_HIERARCHY: {
        ETypeHierarchy eTypeHierarchy = (ETypeHierarchy)theEObject;
        Object result = caseETypeHierarchy(eTypeHierarchy);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      default: return defaultCase(theEObject);
    }
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EJava Class</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EJava Class</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEJavaClass(EJavaClass object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EJava Method</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EJava Method</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEJavaMethod(EJavaMethod object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>ECall Site</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>ECall Site</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseECallSite(ECallSite object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EClass Hierarchy</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EClass Hierarchy</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEClassHierarchy(EClassHierarchy object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EInterface Hierarchy</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EInterface Hierarchy</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEInterfaceHierarchy(EInterfaceHierarchy object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EType Hierarchy</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EType Hierarchy</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseETypeHierarchy(ETypeHierarchy object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EObject With Container Id</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EObject With Container Id</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEObjectWithContainerId(EObjectWithContainerId object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EGraph</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EGraph</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEGraph(EGraph object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>ETree</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>ETree</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseETree(ETree object) {
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

} //JavaSwitch
