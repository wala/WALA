/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.AbstractEnumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>EClass Loader Name</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.JavaPackage#getEClassLoaderName()
 * @model
 * @generated
 */
public final class EClassLoaderName extends AbstractEnumerator {
  /**
   * The '<em><b>Application</b></em>' literal value.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of '<em><b>Application</b></em>' literal object isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @see #APPLICATION_LITERAL
   * @model name="Application"
   * @generated
   * @ordered
   */
  public static final int APPLICATION = 0;

  /**
   * The '<em><b>Primordial</b></em>' literal value.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of '<em><b>Primordial</b></em>' literal object isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @see #PRIMORDIAL_LITERAL
   * @model name="Primordial"
   * @generated
   * @ordered
   */
  public static final int PRIMORDIAL = 1;

  /**
   * The '<em><b>Extension</b></em>' literal value.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of '<em><b>Extension</b></em>' literal object isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @see #EXTENSION_LITERAL
   * @model name="Extension"
   * @generated
   * @ordered
   */
  public static final int EXTENSION = 2;

  /**
   * The '<em><b>Application</b></em>' literal object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #APPLICATION
   * @generated
   * @ordered
   */
  public static final EClassLoaderName APPLICATION_LITERAL = new EClassLoaderName(APPLICATION, "Application", "Application");

  /**
   * The '<em><b>Primordial</b></em>' literal object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #PRIMORDIAL
   * @generated
   * @ordered
   */
  public static final EClassLoaderName PRIMORDIAL_LITERAL = new EClassLoaderName(PRIMORDIAL, "Primordial", "Primordial");

  /**
   * The '<em><b>Extension</b></em>' literal object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #EXTENSION
   * @generated
   * @ordered
   */
  public static final EClassLoaderName EXTENSION_LITERAL = new EClassLoaderName(EXTENSION, "Extension", "Extension");

  /**
   * An array of all the '<em><b>EClass Loader Name</b></em>' enumerators.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private static final EClassLoaderName[] VALUES_ARRAY =
    new EClassLoaderName[] {
      APPLICATION_LITERAL,
      PRIMORDIAL_LITERAL,
      EXTENSION_LITERAL,
    };

  /**
   * A public read-only list of all the '<em><b>EClass Loader Name</b></em>' enumerators.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

  /**
   * Returns the '<em><b>EClass Loader Name</b></em>' literal with the specified literal value.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static EClassLoaderName get(String literal) {
    for (int i = 0; i < VALUES_ARRAY.length; ++i) {
      EClassLoaderName result = VALUES_ARRAY[i];
      if (result.toString().equals(literal)) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns the '<em><b>EClass Loader Name</b></em>' literal with the specified name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static EClassLoaderName getByName(String name) {
    for (int i = 0; i < VALUES_ARRAY.length; ++i) {
      EClassLoaderName result = VALUES_ARRAY[i];
      if (result.getName().equals(name)) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns the '<em><b>EClass Loader Name</b></em>' literal with the specified integer value.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static EClassLoaderName get(int value) {
    switch (value) {
      case APPLICATION: return APPLICATION_LITERAL;
      case PRIMORDIAL: return PRIMORDIAL_LITERAL;
      case EXTENSION: return EXTENSION_LITERAL;
    }
    return null;	
  }

  /**
   * Only this class can construct instances.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClassLoaderName(int value, String name, String literal) {
    super(value, name, literal);
  }

} //EClassLoaderName
