/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.j2ee;

import org.eclipse.jst.j2ee.ejb.EnterpriseBean;
import org.eclipse.jst.j2ee.ejb.MethodElement;
import org.eclipse.jst.j2ee.ejb.MethodElementKind;
import org.eclipse.jst.j2ee.ejb.TransactionAttributeType;

import com.ibm.wala.types.MemberReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * Represents a declarative transaction attribute, either from a deployment descriptor or synthetic
 */
public abstract class AbstractDeclaredTransaction implements Comparable<IDeclaredTransaction>, IDeclaredTransaction {

  /**
   * The governing entity bean.
   */
  private final EnterpriseBean bean;

  /**
   * A constant from MethodElementKind
   */
  private int kind;

  /**
   * A constant from TransactionAttributeType
   */
  private int transactionType;

  public AbstractDeclaredTransaction(EnterpriseBean B, int kind, int transactionType) {
    this.bean = B;
    this.kind = kind;
    this.transactionType = transactionType;
  }

  public String toString() {
    StringBuffer result = new StringBuffer(bean.getName() + ":" + kindString() + getMethodReference());
    result.append("\n   ");
    result.append(transactionTypeString());
    result.append("\n   ");

    return result.toString();
  }

  private String transactionTypeString() {
    switch (transactionType) {
    case TransactionAttributeType.MANDATORY:
      return "MANDATORY";
    case TransactionAttributeType.NEVER:
      return "NEVER         ";
    case TransactionAttributeType.NOT_SUPPORTED:
      return "NOT SUPPORTED ";
    case TransactionAttributeType.REQUIRED:
      return "REQUIRED      ";
    case TransactionAttributeType.REQUIRES_NEW:
      return "REQUIRES NEW  ";
    case TransactionAttributeType.SUPPORTS:
      return "SUPPORTS      ";
    default:
      Assertions.UNREACHABLE();
      return null;
    }
  }

  private String kindString() {
    switch (kind) {
    case MethodElementKind.HOME:
      return "Home Interface:      ";
    case MethodElementKind.REMOTE:
      return "Remote Interface:    ";
    case MethodElementKind.LOCAL:
      return "Local Interface:     ";
    case MethodElementKind.LOCAL_HOME:
      return "LocalHome Interface: ";
    case MethodElementKind.UNSPECIFIED:
      return "Unspecified Interface: ";
    default:
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass())
      return false;
    AbstractDeclaredTransaction other = (AbstractDeclaredTransaction) obj;
    return (bean.equals(other.bean) && getMethodReference().equals(other.getMethodReference()) && kind == other.kind);
  }

  public int hashCode() {
    return bean.hashCode() * 93 + kind * 5 + getMethodReference().hashCode();
  }

  public int compareTo(IDeclaredTransaction o) {
    assert this.getClass().equals(o.getClass());
    String A = bean.toString() + kindString() + getMethodReference().toString();
    AbstractDeclaredTransaction other = (AbstractDeclaredTransaction) o;
    String B = other.bean.toString() + other.kindString() + other.getMethodReference().toString();
    return A.compareTo(B);
  }

  /**
   * TODO: cache this?
   */
  public abstract MemberReference getMethodReference();

  public boolean isRequired() {
    return transactionType == TransactionAttributeType.REQUIRED;
  }

  public boolean isRequiresNew() {
    return transactionType == TransactionAttributeType.REQUIRES_NEW;
  }

  public boolean isNotSupported() {
    return transactionType == TransactionAttributeType.NOT_SUPPORTED;
  }

  public boolean isNever() {
    return transactionType == TransactionAttributeType.NEVER;
  }

  public boolean isMandatory() {
    return transactionType == TransactionAttributeType.MANDATORY;
  }

  public boolean isSupports() {
    return transactionType == TransactionAttributeType.SUPPORTS;
  }

  public EnterpriseBean getBean() {
    return bean;
  }

  public abstract MethodElement getMethodElement();
}
