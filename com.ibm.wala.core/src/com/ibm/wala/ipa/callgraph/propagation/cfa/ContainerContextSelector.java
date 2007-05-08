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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteKey;
import com.ibm.wala.ipa.callgraph.propagation.ContainerUtil;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.ReceiverInstanceContext;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * This context selector returns a context customized for the instancekey of the
 * receiver if
 * <ul>
 * <li>receiver is a container, or</li>
 * was allocated in a node whose context was a ReceiverInstanceContext, and the
 * type is interesting according to a delegate instance key selector
 * </ul>
 * 
 * @author sfink
 */
public class ContainerContextSelector implements ContextSelector {

  private final static boolean DEBUG = false;

  private final static TypeName SyntheticSystemName = TypeName.string2TypeName("Lcom/ibm/wala/model/java/lang/System");
  private final static TypeName JavaLangSystemName = TypeName.string2TypeName("Ljava/lang/System");

  public final static TypeReference SyntheticSystem = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
      SyntheticSystemName);
  public final static TypeReference JavaLangSystem = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
      JavaLangSystemName);

  public final static Atom arraycopyAtom = Atom.findOrCreateUnicodeAtom("arraycopy");

  private final static Descriptor arraycopyDesc = Descriptor.findOrCreateUTF8("(Ljava/lang/Object;Ljava/lang/Object;)V");

  public final static MethodReference synthArraycopy = MethodReference.findOrCreate(SyntheticSystem, arraycopyAtom, arraycopyDesc);

  /**
   * The governing class hierarchy.
   */
  private final ClassHierarchy cha;

  /**
   * An object that determines object naming policy
   */
  private final ZeroXInstanceKeys delegate;

  /**
   * @param cha
   * @param delegate
   */
  public ContainerContextSelector(ClassHierarchy cha, ZeroXInstanceKeys delegate) {
    this.cha = cha;
    this.delegate = delegate;
    if (Assertions.verifyAssertions) {
      Assertions._assert(delegate != null);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#getCalleeTarget(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {

    if (DEBUG) {
      Trace.println("ContainerContextSelector: getCalleeTarget " + callee);
    }
    if (mayUnderstand(caller, site, callee, receiver)) {
      if (DEBUG) {
        Trace.println("May Understand: " + callee + " recv " + receiver);
      }
      if (isArrayCopy(callee.getReference())) {
        return new CallerSiteContext(caller, site);
      } else {
        if (Assertions.verifyAssertions) {
          if (receiver == null) {
            Assertions.UNREACHABLE("null receiver for " + site);
          }
        }
        return new ReceiverInstanceContext(receiver);
      }
    } else {
      return null;
    }
  }

  public static boolean isArrayCopy(MethodReference m) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.getDeclaringClass().equals(JavaLangSystem)) {
      if (m.getName().equals(arraycopyAtom)) {
        return true;
      }
    }
    return m.equals(synthArraycopy);
  }
  
  /**
   * This method walks recursively up the definition of a context C, to see if
   * the chain of contexts that give rise to C a) includes the method M. or b)
   * includes the method in which the receiver was allocated
   * 
   * @return the matching context if found, null otherwise
   */
  public static Context findRecursiveMatchingContext(IMethod M, Context C, InstanceKey receiver) {
    if (DEBUG) {
      Trace.println("findRecursiveMatchingContext for " + M + " in context " + C + " receiver " + receiver);
    }
    Context result = findRecursiveMatchingContext(M, C);
    if (result != null) {
      return result;
    } else {
      if (receiver instanceof AllocationSiteKey) {
        AllocationSiteKey a = (AllocationSiteKey) receiver;
        IMethod m = a.getNode().getMethod();
        return findRecursiveMatchingContext(m, C);
      } else {
        return null;
      }
    }
  }

  /**
   * This method walks recursively up the definition of a context C, to see if
   * the chain of contexts that give rise to C includes the method M.
   * 
   * If C is a ReceiverInstanceContext, Let N be the node that allocated
   * C.instance. If N.method == M, return N. Else return
   * findRecursiveMatchingContext(M, N.context) Else return null
   */
  public static CGNode findNodeRecursiveMatchingContext(IMethod M, Context C) {
    if (DEBUG) {
      Trace.println("findNodeRecursiveMatchingContext " + M + " in context " + C);
    }
    if (C instanceof ReceiverInstanceContext) {
      ReceiverInstanceContext ric = (ReceiverInstanceContext) C;
      if (!(ric.getReceiver() instanceof AllocationSiteKey)) {
        return null;
      }
      AllocationSiteKey I = (AllocationSiteKey) ric.getReceiver();
      CGNode N = I.getNode();
      if (N.getMethod().equals(M)) {
        return N;
      } else {
        return findNodeRecursiveMatchingContext(M, N.getContext());
      }
    } else {
      return null;
    }
  }

  /**
   * This method walks recursively up the definition of a context C, to see if
   * the chain of contexts that give rise to C includes the method M.
   * 
   * If C is a ReceiverInstanceContext, Let N be the node that allocated
   * C.instance. If N.method == M, return N.context. Else return
   * findRecursiveMatchingContext(M, N.context) Else return null
   */
  public static Context findRecursiveMatchingContext(IMethod M, Context C) {
    CGNode n = findNodeRecursiveMatchingContext(M, C);
    return (n == null) ? null : n.getContext();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#getBoundOnNumberOfTargets(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.classLoader.IMethod)
   */
  public int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference site, IMethod targetMethod) {
    // if we understand this call, we don't know how many target contexts we may
    // create.
    return -1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#mayUnderstand(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.classLoader.IMethod)
   */
  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey receiver) {
    if (isArrayCopy(targetMethod.getReference())) {
      return true;
    } else {
      if (site.isStatic()) {
        return false;
      }
      if (targetMethod.getDeclaringClass().getReference().equals(TypeReference.JavaLangObject)) {
        // ramp down context: assuming methods on java.lang.Object don't cause pollution
        // important for containers that invoke reflection
        return false;
      }
      if (isContainer(targetMethod.getDeclaringClass())) {
        return true;
      }

      if (receiver == null) {
        // any possible receiver. However, we will only handle this call
        // if the concrete receiver type is interesting.
        IClass klass = targetMethod.getDeclaringClass();
        int n = cha.getNumberOfImmediateSubclasses(klass);
        if (n > 0) {
          // the receiver is not "effectively final".
          // give up and assume we might see an interesting subclass.
          return true;
        }
        // only one possible receiver class
        if (delegate.isInteresting(klass)) {
          // we may create a receiver instance context for this call
          return true;
        } else {
          // we will never create a receiver instance context for this call
          return false;
        }
      }
      if (!delegate.isInteresting(receiver.getConcreteType())) {
        return false;
      }
      if (receiver instanceof AllocationSiteKey) {
        AllocationSiteKey I = (AllocationSiteKey) receiver;
        CGNode N = I.getNode();
        if (N.getContext() instanceof ReceiverInstanceContext) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * @param C
   * @return true iff C is a container class
   */
  protected boolean isContainer(IClass C) {
    if (DEBUG) {
      Trace.println("isContainer? " + C + " " + ContainerUtil.isContainer(C, cha));
    }
    return ContainerUtil.isContainer(C, cha);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#setWarnings(com.ibm.wala.util.warnings.WarningSet)
   */
  public void setWarnings(WarningSet newWarnings) {
    // no-op, this object not bound to warnings

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#contextIsIrrelevant(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference)
   */
  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    return false;
  }

  /**
   * @return Returns the cha.
   */
  protected ClassHierarchy getClassHierarchy() {
    return cha;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#contextIsIrrelevant(com.ibm.wala.types.MethodReference)
   */
  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    return false;
  }
}
