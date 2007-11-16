/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.util;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.ecore.EObject;

import com.ibm.wala.annotations.Internal;
import com.ibm.wala.ecore.common.EObjectWithContainerId;
import com.ibm.wala.ecore.graph.EGraph;
import com.ibm.wala.ecore.graph.ETree;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.JavaPackage;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.JavaPackage
 * @generated
 */
@Internal
public class JavaAdapterFactory extends AdapterFactoryImpl {
  /**
   * The cached model package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected static JavaPackage modelPackage;

  /**
   * Creates an instance of the adapter factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public JavaAdapterFactory() {
    if (modelPackage == null) {
      modelPackage = JavaPackage.eINSTANCE;
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
  protected JavaSwitch modelSwitch =
    new JavaSwitch() {
      public Object caseEJavaClass(EJavaClass object) {
        return createEJavaClassAdapter();
      }
      public Object caseEObjectWithContainerId(EObjectWithContainerId object) {
        return createEObjectWithContainerIdAdapter();
      }
      public Object caseEGraph(EGraph object) {
        return createEGraphAdapter();
      }
      public Object caseETree(ETree object) {
        return createETreeAdapter();
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
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.EJavaClass <em>EJava Class</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.EJavaClass
   * @generated
   */
  public Adapter createEJavaClassAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.EJavaMethod <em>EJava Method</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.EJavaMethod
   * @generated
   */
  public Adapter createEJavaMethodAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.ECallSite <em>ECall Site</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.ECallSite
   * @generated
   */
  public Adapter createECallSiteAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.EClassHierarchy <em>EClass Hierarchy</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.EClassHierarchy
   * @generated
   */
  public Adapter createEClassHierarchyAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.EInterfaceHierarchy <em>EInterface Hierarchy</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.EInterfaceHierarchy
   * @generated
   */
  public Adapter createEInterfaceHierarchyAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.java.ETypeHierarchy <em>EType Hierarchy</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.java.ETypeHierarchy
   * @generated
   */
  public Adapter createETypeHierarchyAdapter() {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.common.EObjectWithContainerId <em>EObject With Container Id</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.common.EObjectWithContainerId
   * @generated
   */
  public Adapter createEObjectWithContainerIdAdapter() {
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
   * Creates a new adapter for an object of class '{@link com.ibm.wala.ecore.graph.ETree <em>ETree</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see com.ibm.wala.ecore.graph.ETree
   * @generated
   */
  public Adapter createETreeAdapter() {
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

} //JavaAdapterFactory
