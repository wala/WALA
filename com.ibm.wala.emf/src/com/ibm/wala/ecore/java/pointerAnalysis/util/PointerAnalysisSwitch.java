/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis.util;

import com.ibm.wala.ecore.graph.EGraph;

import com.ibm.wala.ecore.java.pointerAnalysis.*;

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
 * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage
 * @generated
 */
public class PointerAnalysisSwitch {
  /**
   * The cached model package
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected static PointerAnalysisPackage modelPackage;

  /**
   * Creates an instance of the switch.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public PointerAnalysisSwitch() {
    if (modelPackage == null) {
      modelPackage = PointerAnalysisPackage.eINSTANCE;
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
      case PointerAnalysisPackage.EPOINTER: {
        EPointer ePointer = (EPointer)theEObject;
        Object result = caseEPointer(ePointer);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case PointerAnalysisPackage.EINSTANCE_FIELD: {
        EInstanceField eInstanceField = (EInstanceField)theEObject;
        Object result = caseEInstanceField(eInstanceField);
        if (result == null) result = caseEPointer(eInstanceField);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case PointerAnalysisPackage.EARRAY_CONTENTS: {
        EArrayContents eArrayContents = (EArrayContents)theEObject;
        Object result = caseEArrayContents(eArrayContents);
        if (result == null) result = caseEPointer(eArrayContents);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case PointerAnalysisPackage.ESTATIC_FIELD: {
        EStaticField eStaticField = (EStaticField)theEObject;
        Object result = caseEStaticField(eStaticField);
        if (result == null) result = caseEPointer(eStaticField);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case PointerAnalysisPackage.ELOCAL_POINTER: {
        ELocalPointer eLocalPointer = (ELocalPointer)theEObject;
        Object result = caseELocalPointer(eLocalPointer);
        if (result == null) result = caseEPointer(eLocalPointer);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case PointerAnalysisPackage.ERETURN_VALUE_POINTER: {
        EReturnValuePointer eReturnValuePointer = (EReturnValuePointer)theEObject;
        Object result = caseEReturnValuePointer(eReturnValuePointer);
        if (result == null) result = caseEPointer(eReturnValuePointer);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case PointerAnalysisPackage.EINSTANCE: {
        EInstance eInstance = (EInstance)theEObject;
        Object result = caseEInstance(eInstance);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case PointerAnalysisPackage.EJAVA_CLASS_INSTANCE: {
        EJavaClassInstance eJavaClassInstance = (EJavaClassInstance)theEObject;
        Object result = caseEJavaClassInstance(eJavaClassInstance);
        if (result == null) result = caseEInstance(eJavaClassInstance);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case PointerAnalysisPackage.EHEAP_GRAPH: {
        EHeapGraph eHeapGraph = (EHeapGraph)theEObject;
        Object result = caseEHeapGraph(eHeapGraph);
        if (result == null) result = caseEGraph(eHeapGraph);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      default: return defaultCase(theEObject);
    }
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EPointer</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EPointer</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEPointer(EPointer object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EInstance Field</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EInstance Field</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEInstanceField(EInstanceField object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EArray Contents</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EArray Contents</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEArrayContents(EArrayContents object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EStatic Field</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EStatic Field</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEStaticField(EStaticField object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>ELocal Pointer</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>ELocal Pointer</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseELocalPointer(ELocalPointer object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EReturn Value Pointer</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EReturn Value Pointer</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEReturnValuePointer(EReturnValuePointer object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EInstance</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EInstance</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEInstance(EInstance object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EJava Class Instance</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EJava Class Instance</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEJavaClassInstance(EJavaClassInstance object) {
    return null;
  }

  /**
   * Returns the result of interpretting the object as an instance of '<em>EHeap Graph</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpretting the object as an instance of '<em>EHeap Graph</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public Object caseEHeapGraph(EHeapGraph object) {
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

} //PointerAnalysisSwitch
