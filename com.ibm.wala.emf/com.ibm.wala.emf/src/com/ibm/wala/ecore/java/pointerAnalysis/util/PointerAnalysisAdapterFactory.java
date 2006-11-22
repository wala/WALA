/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis.util;

import com.ibm.wala.ecore.graph.EGraph;

import com.ibm.wala.ecore.java.pointerAnalysis.*;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage
 * @generated
 */
public class PointerAnalysisAdapterFactory extends AdapterFactoryImpl {
  /**
   * The cached model package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected static PointerAnalysisPackage modelPackage;

  /**
   * Creates an instance of the adapter factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public PointerAnalysisAdapterFactory() {
    if (modelPackage == null) {
      modelPackage = PointerAnalysisPackage.eINSTANCE;
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
  protected PointerAnalysisSwitch modelSwitch =
    new PointerAnalysisSwitch() {
      public Object caseEPointer(EPointer object) {
        return createEPointerAdapter();
      }
      public Object caseEInstanceField(EInstanceField object) {
        return createEInstanceFieldAdapter();
      }
      public Object caseEArrayContents(EArrayContents object) {
        return createEArrayContentsAdapter();
      }
      public Object caseEStaticField(EStaticField object) {
        return createEStaticFieldAdapter();
      }
      public Object caseELocalPointer(ELocalPointer object) {
        return createELocalPointerAdapter();
      }
      public Object caseEReturnValuePointer(EReturnValuePointer object) {
        return createEReturnValuePointerAdapter();
      }
      public Object caseEInstance(EInstance object) {
        return createEInstanceAdapter();
      }
      public Object caseEJavaClassInstance(EJavaClassInstance object) {
        return createEJavaClassInstanceAdapter();
      }
      public Object caseEHeapGraph(EHeapGraph object) {
        return createEHeapGraphAdapter();
      }
      public Object caseEGraph(EGraph object) {
        return createEGraphAdapter();
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
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EPointer <em>EPointer</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EPointer
   * @generated
   */
  public Adapter createEPointerAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EInstanceField <em>EInstance Field</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EInstanceField
   * @generated
   */
  public Adapter createEInstanceFieldAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EArrayContents <em>EArray Contents</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EArrayContents
   * @generated
   */
  public Adapter createEArrayContentsAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EStaticField <em>EStatic Field</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EStaticField
   * @generated
   */
  public Adapter createEStaticFieldAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer <em>ELocal Pointer</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer
   * @generated
   */
  public Adapter createELocalPointerAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer <em>EReturn Value Pointer</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer
   * @generated
   */
  public Adapter createEReturnValuePointerAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EInstance <em>EInstance</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EInstance
   * @generated
   */
  public Adapter createEInstanceAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EJavaClassInstance <em>EJava Class Instance</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EJavaClassInstance
   * @generated
   */
  public Adapter createEJavaClassInstanceAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.pointerAnalysis.EHeapGraph <em>EHeap Graph</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.pointerAnalysis.EHeapGraph
   * @generated
   */
  public Adapter createEHeapGraphAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.graph.EGraph <em>EGraph</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.graph.EGraph
   * @generated
   */
  public Adapter createEGraphAdapter() {
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

} //PointerAnalysisAdapterFactory
