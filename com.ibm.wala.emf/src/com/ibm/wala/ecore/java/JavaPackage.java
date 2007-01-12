/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import com.ibm.wala.ecore.common.CommonPackage;
import com.ibm.wala.ecore.graph.GraphPackage;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.JavaFactory
 * @model kind="package"
 * @generated
 */
public interface JavaPackage extends EPackage {
  /**
   * The package name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNAME = "java";

  /**
   * The package namespace URI.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_URI = "http:///com/ibm/wala/wala.ecore.java";

  /**
   * The package namespace name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_PREFIX = "com.ibm.wala.java";

  /**
   * The singleton instance of the package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  JavaPackage eINSTANCE = com.ibm.wala.ecore.java.impl.JavaPackageImpl.init();

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.impl.EJavaClassImpl <em>EJava Class</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.impl.EJavaClassImpl
   * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getEJavaClass()
   * @generated
   */
  int EJAVA_CLASS = 0;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_CLASS__ID = CommonPackage.EOBJECT_WITH_CONTAINER_ID__ID;

  /**
   * The feature id for the '<em><b>Class Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_CLASS__CLASS_NAME = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 0;

  /**
   * The feature id for the '<em><b>Loader</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_CLASS__LOADER = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 1;

  /**
   * The number of structural features of the '<em>EJava Class</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_CLASS_FEATURE_COUNT = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 2;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.impl.EJavaMethodImpl <em>EJava Method</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.impl.EJavaMethodImpl
   * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getEJavaMethod()
   * @generated
   */
  int EJAVA_METHOD = 1;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_METHOD__ID = CommonPackage.EOBJECT_WITH_CONTAINER_ID__ID;

  /**
   * The feature id for the '<em><b>Method Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_METHOD__METHOD_NAME = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 0;

  /**
   * The feature id for the '<em><b>Descriptor</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_METHOD__DESCRIPTOR = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 1;

  /**
   * The feature id for the '<em><b>Java Class</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_METHOD__JAVA_CLASS = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 2;

  /**
   * The feature id for the '<em><b>Signature</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_METHOD__SIGNATURE = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 3;

  /**
   * The number of structural features of the '<em>EJava Method</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EJAVA_METHOD_FEATURE_COUNT = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 4;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.impl.ECallSiteImpl <em>ECall Site</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.impl.ECallSiteImpl
   * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getECallSite()
   * @generated
   */
  int ECALL_SITE = 2;

  /**
   * The feature id for the '<em><b>Id</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECALL_SITE__ID = CommonPackage.EOBJECT_WITH_CONTAINER_ID__ID;

  /**
   * The feature id for the '<em><b>Bytecode Index</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECALL_SITE__BYTECODE_INDEX = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 0;

  /**
   * The feature id for the '<em><b>Java Method</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECALL_SITE__JAVA_METHOD = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 1;

  /**
   * The feature id for the '<em><b>Declared Target</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECALL_SITE__DECLARED_TARGET = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 2;

  /**
   * The number of structural features of the '<em>ECall Site</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECALL_SITE_FEATURE_COUNT = CommonPackage.EOBJECT_WITH_CONTAINER_ID_FEATURE_COUNT + 3;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.impl.EClassHierarchyImpl <em>EClass Hierarchy</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.impl.EClassHierarchyImpl
   * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getEClassHierarchy()
   * @generated
   */
  int ECLASS_HIERARCHY = 3;

  /**
   * The feature id for the '<em><b>Nodes</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECLASS_HIERARCHY__NODES = GraphPackage.ETREE__NODES;

  /**
   * The feature id for the '<em><b>Edges</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECLASS_HIERARCHY__EDGES = GraphPackage.ETREE__EDGES;

  /**
   * The number of structural features of the '<em>EClass Hierarchy</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECLASS_HIERARCHY_FEATURE_COUNT = GraphPackage.ETREE_FEATURE_COUNT + 0;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.impl.EInterfaceHierarchyImpl <em>EInterface Hierarchy</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.impl.EInterfaceHierarchyImpl
   * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getEInterfaceHierarchy()
   * @generated
   */
  int EINTERFACE_HIERARCHY = 4;

  /**
   * The feature id for the '<em><b>Nodes</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EINTERFACE_HIERARCHY__NODES = GraphPackage.EGRAPH__NODES;

  /**
   * The feature id for the '<em><b>Edges</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EINTERFACE_HIERARCHY__EDGES = GraphPackage.EGRAPH__EDGES;

  /**
   * The number of structural features of the '<em>EInterface Hierarchy</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EINTERFACE_HIERARCHY_FEATURE_COUNT = GraphPackage.EGRAPH_FEATURE_COUNT + 0;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.impl.ETypeHierarchyImpl <em>EType Hierarchy</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.impl.ETypeHierarchyImpl
   * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getETypeHierarchy()
   * @generated
   */
  int ETYPE_HIERARCHY = 5;

  /**
   * The feature id for the '<em><b>Classes</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ETYPE_HIERARCHY__CLASSES = 0;

  /**
   * The feature id for the '<em><b>Interfaces</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ETYPE_HIERARCHY__INTERFACES = 1;

  /**
   * The feature id for the '<em><b>Implements</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ETYPE_HIERARCHY__IMPLEMENTS = 2;

  /**
   * The number of structural features of the '<em>EType Hierarchy</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ETYPE_HIERARCHY_FEATURE_COUNT = 3;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.EClassLoaderName <em>EClass Loader Name</em>}' enum.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.EClassLoaderName
   * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getEClassLoaderName()
   * @generated
   */
  int ECLASS_LOADER_NAME = 6;


  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.EJavaClass <em>EJava Class</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EJava Class</em>'.
   * @see com.ibm.wala.ecore.java.EJavaClass
   * @generated
   */
  EClass getEJavaClass();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.EJavaClass#getClassName <em>Class Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Class Name</em>'.
   * @see com.ibm.wala.ecore.java.EJavaClass#getClassName()
   * @see #getEJavaClass()
   * @generated
   */
  EAttribute getEJavaClass_ClassName();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.EJavaClass#getLoader <em>Loader</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Loader</em>'.
   * @see com.ibm.wala.ecore.java.EJavaClass#getLoader()
   * @see #getEJavaClass()
   * @generated
   */
  EAttribute getEJavaClass_Loader();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.EJavaMethod <em>EJava Method</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EJava Method</em>'.
   * @see com.ibm.wala.ecore.java.EJavaMethod
   * @generated
   */
  EClass getEJavaMethod();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.EJavaMethod#getMethodName <em>Method Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Method Name</em>'.
   * @see com.ibm.wala.ecore.java.EJavaMethod#getMethodName()
   * @see #getEJavaMethod()
   * @generated
   */
  EAttribute getEJavaMethod_MethodName();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.EJavaMethod#getDescriptor <em>Descriptor</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Descriptor</em>'.
   * @see com.ibm.wala.ecore.java.EJavaMethod#getDescriptor()
   * @see #getEJavaMethod()
   * @generated
   */
  EAttribute getEJavaMethod_Descriptor();

  /**
   * Returns the meta object for the reference '{@link com.ibm.wala.ecore.java.EJavaMethod#getJavaClass <em>Java Class</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference '<em>Java Class</em>'.
   * @see com.ibm.wala.ecore.java.EJavaMethod#getJavaClass()
   * @see #getEJavaMethod()
   * @generated
   */
  EReference getEJavaMethod_JavaClass();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.EJavaMethod#getSignature <em>Signature</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Signature</em>'.
   * @see com.ibm.wala.ecore.java.EJavaMethod#getSignature()
   * @see #getEJavaMethod()
   * @generated
   */
  EAttribute getEJavaMethod_Signature();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.ECallSite <em>ECall Site</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>ECall Site</em>'.
   * @see com.ibm.wala.ecore.java.ECallSite
   * @generated
   */
  EClass getECallSite();

  /**
   * Returns the meta object for the attribute '{@link com.ibm.wala.ecore.java.ECallSite#getBytecodeIndex <em>Bytecode Index</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Bytecode Index</em>'.
   * @see com.ibm.wala.ecore.java.ECallSite#getBytecodeIndex()
   * @see #getECallSite()
   * @generated
   */
  EAttribute getECallSite_BytecodeIndex();

  /**
   * Returns the meta object for the reference '{@link com.ibm.wala.ecore.java.ECallSite#getJavaMethod <em>Java Method</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference '<em>Java Method</em>'.
   * @see com.ibm.wala.ecore.java.ECallSite#getJavaMethod()
   * @see #getECallSite()
   * @generated
   */
  EReference getECallSite_JavaMethod();

  /**
   * Returns the meta object for the reference '{@link com.ibm.wala.ecore.java.ECallSite#getDeclaredTarget <em>Declared Target</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference '<em>Declared Target</em>'.
   * @see com.ibm.wala.ecore.java.ECallSite#getDeclaredTarget()
   * @see #getECallSite()
   * @generated
   */
  EReference getECallSite_DeclaredTarget();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.EClassHierarchy <em>EClass Hierarchy</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EClass Hierarchy</em>'.
   * @see com.ibm.wala.ecore.java.EClassHierarchy
   * @generated
   */
  EClass getEClassHierarchy();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.EInterfaceHierarchy <em>EInterface Hierarchy</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EInterface Hierarchy</em>'.
   * @see com.ibm.wala.ecore.java.EInterfaceHierarchy
   * @generated
   */
  EClass getEInterfaceHierarchy();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.ETypeHierarchy <em>EType Hierarchy</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EType Hierarchy</em>'.
   * @see com.ibm.wala.ecore.java.ETypeHierarchy
   * @generated
   */
  EClass getETypeHierarchy();

  /**
   * Returns the meta object for the containment reference '{@link com.ibm.wala.ecore.java.ETypeHierarchy#getClasses <em>Classes</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Classes</em>'.
   * @see com.ibm.wala.ecore.java.ETypeHierarchy#getClasses()
   * @see #getETypeHierarchy()
   * @generated
   */
  EReference getETypeHierarchy_Classes();

  /**
   * Returns the meta object for the containment reference '{@link com.ibm.wala.ecore.java.ETypeHierarchy#getInterfaces <em>Interfaces</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Interfaces</em>'.
   * @see com.ibm.wala.ecore.java.ETypeHierarchy#getInterfaces()
   * @see #getETypeHierarchy()
   * @generated
   */
  EReference getETypeHierarchy_Interfaces();

  /**
   * Returns the meta object for the containment reference '{@link com.ibm.wala.ecore.java.ETypeHierarchy#getImplements <em>Implements</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Implements</em>'.
   * @see com.ibm.wala.ecore.java.ETypeHierarchy#getImplements()
   * @see #getETypeHierarchy()
   * @generated
   */
  EReference getETypeHierarchy_Implements();

  /**
   * Returns the meta object for enum '{@link com.ibm.wala.ecore.java.EClassLoaderName <em>EClass Loader Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for enum '<em>EClass Loader Name</em>'.
   * @see com.ibm.wala.ecore.java.EClassLoaderName
   * @generated
   */
  EEnum getEClassLoaderName();

  /**
   * Returns the factory that creates the instances of the model.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the factory that creates the instances of the model.
   * @generated
   */
  JavaFactory getJavaFactory();

  /**
   * <!-- begin-user-doc -->
   * Defines literals for the meta objects that represent
   * <ul>
   *   <li>each class,</li>
   *   <li>each feature of each class,</li>
   *   <li>each enum,</li>
   *   <li>and each data type</li>
   * </ul>
   * <!-- end-user-doc -->
   * @generated
   */
  interface Literals {
    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.impl.EJavaClassImpl <em>EJava Class</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.impl.EJavaClassImpl
     * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getEJavaClass()
     * @generated
     */
    EClass EJAVA_CLASS = eINSTANCE.getEJavaClass();

    /**
     * The meta object literal for the '<em><b>Class Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EJAVA_CLASS__CLASS_NAME = eINSTANCE.getEJavaClass_ClassName();

    /**
     * The meta object literal for the '<em><b>Loader</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EJAVA_CLASS__LOADER = eINSTANCE.getEJavaClass_Loader();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.impl.EJavaMethodImpl <em>EJava Method</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.impl.EJavaMethodImpl
     * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getEJavaMethod()
     * @generated
     */
    EClass EJAVA_METHOD = eINSTANCE.getEJavaMethod();

    /**
     * The meta object literal for the '<em><b>Method Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EJAVA_METHOD__METHOD_NAME = eINSTANCE.getEJavaMethod_MethodName();

    /**
     * The meta object literal for the '<em><b>Descriptor</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EJAVA_METHOD__DESCRIPTOR = eINSTANCE.getEJavaMethod_Descriptor();

    /**
     * The meta object literal for the '<em><b>Java Class</b></em>' reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference EJAVA_METHOD__JAVA_CLASS = eINSTANCE.getEJavaMethod_JavaClass();

    /**
     * The meta object literal for the '<em><b>Signature</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute EJAVA_METHOD__SIGNATURE = eINSTANCE.getEJavaMethod_Signature();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.impl.ECallSiteImpl <em>ECall Site</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.impl.ECallSiteImpl
     * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getECallSite()
     * @generated
     */
    EClass ECALL_SITE = eINSTANCE.getECallSite();

    /**
     * The meta object literal for the '<em><b>Bytecode Index</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ECALL_SITE__BYTECODE_INDEX = eINSTANCE.getECallSite_BytecodeIndex();

    /**
     * The meta object literal for the '<em><b>Java Method</b></em>' reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ECALL_SITE__JAVA_METHOD = eINSTANCE.getECallSite_JavaMethod();

    /**
     * The meta object literal for the '<em><b>Declared Target</b></em>' reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ECALL_SITE__DECLARED_TARGET = eINSTANCE.getECallSite_DeclaredTarget();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.impl.EClassHierarchyImpl <em>EClass Hierarchy</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.impl.EClassHierarchyImpl
     * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getEClassHierarchy()
     * @generated
     */
    EClass ECLASS_HIERARCHY = eINSTANCE.getEClassHierarchy();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.impl.EInterfaceHierarchyImpl <em>EInterface Hierarchy</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.impl.EInterfaceHierarchyImpl
     * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getEInterfaceHierarchy()
     * @generated
     */
    EClass EINTERFACE_HIERARCHY = eINSTANCE.getEInterfaceHierarchy();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.impl.ETypeHierarchyImpl <em>EType Hierarchy</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.impl.ETypeHierarchyImpl
     * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getETypeHierarchy()
     * @generated
     */
    EClass ETYPE_HIERARCHY = eINSTANCE.getETypeHierarchy();

    /**
     * The meta object literal for the '<em><b>Classes</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ETYPE_HIERARCHY__CLASSES = eINSTANCE.getETypeHierarchy_Classes();

    /**
     * The meta object literal for the '<em><b>Interfaces</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ETYPE_HIERARCHY__INTERFACES = eINSTANCE.getETypeHierarchy_Interfaces();

    /**
     * The meta object literal for the '<em><b>Implements</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ETYPE_HIERARCHY__IMPLEMENTS = eINSTANCE.getETypeHierarchy_Implements();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.EClassLoaderName <em>EClass Loader Name</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.EClassLoaderName
     * @see com.ibm.wala.ecore.java.impl.JavaPackageImpl#getEClassLoaderName()
     * @generated
     */
    EEnum ECLASS_LOADER_NAME = eINSTANCE.getEClassLoaderName();

  }

} //JavaPackage
