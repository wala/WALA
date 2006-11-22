package com.ibm.wala.j2ee;

import org.eclipse.jst.j2ee.ejb.EnterpriseBean;
import org.eclipse.jst.j2ee.ejb.MethodElement;

import com.ibm.wala.types.MethodReference;

/**
 * @author sfink
 */
public interface IDeclaredTransaction extends Comparable<IDeclaredTransaction> {

  public abstract MethodReference getMethodReference();

  public abstract boolean isRequired();

  public abstract boolean isRequiresNew();

  public abstract boolean isNotSupported();

  public abstract boolean isNever();

  public abstract boolean isMandatory();

  public abstract boolean isSupports();

  public abstract EnterpriseBean getBean();

  public abstract MethodElement getMethodElement();
}