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
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.ContainerUtil;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.ReceiverInstanceContext;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.strings.Atom;

/**
 * This context selector returns a context customized for the {@link InstanceKey} of the receiver if
 * <ul>
 * <li>receiver is a container, or</li>
 * <li>was allocated in a node whose context was a {@link ReceiverInstanceContext}, and the type is interesting according to a delegate
 * {@link ZeroXInstanceKeys}</li>
 * </ul>
 * 
 * Additionally, we add one level of call string context to a few well-known static factory methods from the standard libraries.
 */
public class ContainerContextSelector implements ContextSelector {

  private final static boolean DEBUG = false;

  private final static TypeName SyntheticSystemName = TypeName.string2TypeName("Lcom/ibm/wala/model/java/lang/System");

  public final static TypeReference SyntheticSystem = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
      SyntheticSystemName);

  public final static TypeReference JavaUtilHashtable = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
      "Ljava/util/Hashtable");

  public final static Atom arraycopyAtom = Atom.findOrCreateUnicodeAtom("arraycopy");

  private final static Descriptor arraycopyDesc = Descriptor.findOrCreateUTF8("(Ljava/lang/Object;Ljava/lang/Object;)V");

  public final static MethodReference synthArraycopy = MethodReference.findOrCreate(SyntheticSystem, arraycopyAtom, arraycopyDesc);

  private final static TypeReference Arrays = TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/util/Arrays");

  private final static Atom asList = Atom.findOrCreateUnicodeAtom("asList");

  private final static Atom copyOf = Atom.findOrCreateUnicodeAtom("copyOf");

  private final static Atom copyOfRange = Atom.findOrCreateUnicodeAtom("copyOfRange");

  private final static Atom toString = Atom.findOrCreateUnicodeAtom("toString");

  private final static MethodReference StringValueOf = MethodReference.findOrCreate(TypeReference.JavaLangString, "valueOf",
      "(Ljava/lang/Object;)Ljava/lang/String;");

  private final static MethodReference HashtableNewEntry = MethodReference.findOrCreate(JavaUtilHashtable, "newEntry",
      "(Ljava/lang/Object;Ljava/lang/Object;I)Ljava/util/Hashtable$Entry;");

  /**
   * The governing class hierarchy.
   */
  private final IClassHierarchy cha;

  /**
   * An object that determines object naming policy
   */
  private final ZeroXInstanceKeys delegate;

  /**
   * @param cha governing class hierarchy
   * @param delegate object which determines which classes are "interesting"
   */
  public ContainerContextSelector(IClassHierarchy cha, ZeroXInstanceKeys delegate) {
    this.cha = cha;
    this.delegate = delegate;
    if (delegate == null) {
      throw new IllegalArgumentException("null delegate");
    }
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#getCalleeTarget(com.ibm.wala.ipa.callgraph.CGNode,
   * com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod,
   * com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
   */
  @Override
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] keys) {
    if (DEBUG) {
      System.err.println("ContainerContextSelector: getCalleeTarget " + callee);
    }
    InstanceKey receiver = null;
    if (keys != null && keys.length > 0 && keys[0] != null) {
      receiver = keys[0];
    }
    if (mayUnderstand(site, callee, receiver)) {
      if (DEBUG) {
        System.err.println("May Understand: " + callee + " recv " + receiver);
      }
      if (isWellKnownStaticFactory(callee.getReference())) {
        return new CallerSiteContext(caller, site);
      } else {
        if (receiver == null) {
          Assertions.UNREACHABLE("null receiver for " + site);
        }
        return new ReceiverInstanceContext(receiver);
      }
    } else {
      return null;
    }
  }

  /**
   * Does m represent a static factory method we know about from the standard libraries, that we usually wish to model with one
   * level of call-string context?
   */
  public static boolean isWellKnownStaticFactory(MethodReference m) {
    if (isArrayCopyingMethod(m)) {
      return true;
    }
    if (isArrayToStringMethod(m)) {
      return true;
    }
    if (m.equals(StringValueOf)) {
      return true;
    }
    if (m.equals(HashtableNewEntry)) {
      return true;
    }
    return false;
  }

  /**
   * Does m represent a library method that copies arrays?
   */
  public static boolean isArrayCopyingMethod(MethodReference m) {
    if (m == null) {
      throw new IllegalArgumentException("null m");
    }
    if (m.getDeclaringClass().equals(TypeReference.JavaLangSystem)) {
      if (m.getName().toString().equals("arraycopy")) {
        return true;
      }
    }
    if (m.equals(synthArraycopy)) {
      return true;
    }
    if (isArrayCopyMethod(m)) {
      return true;
    }
    return false;
  }

  /**
   * return true iff m represents one of the well-known methods in
   * java.lang.reflect.Arrays that do some sort of arraycopy
   */
  private static boolean isArrayCopyMethod(MethodReference m) {
    if (m.getDeclaringClass().equals(Arrays)) {
      if (m.getName().equals(asList) || m.getName().equals(copyOf) || m.getName().equals(copyOfRange)) {
        return true;
      }
    }
    return false;
  }

  /**
   * return true iff m represents one of the well-known methods in java.lang.reflect.Arrays that do toString() on an array
   */
  private static boolean isArrayToStringMethod(MethodReference m) {
    if (m.getDeclaringClass().equals(Arrays)) {
      if (m.getName().equals(toString)) {
        return true;
      }
    }
    return false;
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
      System.err.println("findRecursiveMatchingContext for " + M + " in context " + C + " receiver " + receiver);
    }
    Context result = findRecursiveMatchingContext(M, C);
    if (result != null) {
      return result;
    } else {
      if (receiver instanceof AllocationSiteInNode) {
        AllocationSiteInNode a = (AllocationSiteInNode) receiver;
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
  public static CGNode findNodeRecursiveMatchingContext(IMethod m, Context c) {
    if (DEBUG) {
      System.err.println("findNodeRecursiveMatchingContext " + m + " in context " + c);
    }
    if (c instanceof ReceiverInstanceContext) {
      ReceiverInstanceContext ric = (ReceiverInstanceContext) c;
      if (!(ric.getReceiver() instanceof AllocationSiteInNode)) {
        return null;
      }
      AllocationSiteInNode i = (AllocationSiteInNode) ric.getReceiver();
      CGNode n = i.getNode();
      if (n.getMethod().equals(m)) {
        return n;
      } else {
        return findNodeRecursiveMatchingContext(m, n.getContext());
      }
    } else if (c instanceof CallerContext) {
      CallerContext cc = (CallerContext) c;
      CGNode n = cc.getCaller();
      if (n.getMethod().equals(m)) {
        return n;
      } else {
        return findNodeRecursiveMatchingContext(m, n.getContext());
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

  public boolean mayUnderstand(CallSiteReference site, IMethod targetMethod, InstanceKey receiver) {
    if (targetMethod == null) {
      throw new IllegalArgumentException("targetMethod is null");
    }
    if (isWellKnownStaticFactory(targetMethod.getReference())) {
      return true;
    } else {
      if (site.isStatic()) {
        return false;
      }
      if (receiver == null) {
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

      // TODO MS disabling logic below; it has been disabled anyway
      // for a while since we were avoiding calling this method with
      // receiver == null.  Should we delete it? 
//      if (receiver == null) {
//        // any possible receiver. However, we will only handle this call
//        // if the concrete receiver type is interesting.
//        IClass klass = targetMethod.getDeclaringClass();
//        int n = cha.getNumberOfImmediateSubclasses(klass);
//        if (n > 0) {
//          // the receiver is not "effectively final".
//          // give up and assume we might see an interesting subclass.
//          return true;
//        }
//        // only one possible receiver class
//        if (delegate.isInteresting(klass)) {
//          // we may create a receiver instance context for this call
//          return true;
//        } else {
//          // we will never create a receiver instance context for this call
//          return false;
//        }
//      }
      if (!delegate.isInteresting(receiver.getConcreteType())) {
        return false;
      }
      if (receiver instanceof AllocationSiteInNode) {
        AllocationSiteInNode I = (AllocationSiteInNode) receiver;
        CGNode N = I.getNode();
        if (N.getContext() instanceof ReceiverInstanceContext) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * @return true iff C is a container class
   */
  protected boolean isContainer(IClass C) {
    if (DEBUG) {
      System.err.println("isContainer? " + C + " " + ContainerUtil.isContainer(C));
    }
    return ContainerUtil.isContainer(C);
  }

  protected IClassHierarchy getClassHierarchy() {
    return cha;
  }

  private static final IntSet thisParameter = IntSetUtil.make(new int[]{0});

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (site.isDispatch() || site.getDeclaredTarget().getNumberOfParameters() > 0) {
      return thisParameter;
    } else {
      return EmptyIntSet.instance;
    }
  }

}
