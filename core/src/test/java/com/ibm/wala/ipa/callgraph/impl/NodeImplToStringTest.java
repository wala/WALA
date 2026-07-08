/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph.impl;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerContext;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.collections.EmptyIterator;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

/**
 * Regression test for <a href="https://github.com/wala/WALA/issues/1992">issue #1992</a>: {@link
 * BasicCallGraph.NodeImpl#toString()} must terminate on a cyclic context rather than recurse
 * without bound and exhaust the heap.
 */
public class NodeImplToStringTest {

  /** A minimal concrete {@link BasicCallGraph.NodeImpl} that carries a caller-defined context. */
  private static final class TestNode extends BasicCallGraph.NodeImpl {
    TestNode(IMethod method, Context context) {
      super(method, context);
    }

    @Override
    public boolean addTarget(CallSiteReference site, CGNode target) {
      return false;
    }

    @Override
    public IR getIR() {
      return null;
    }

    @Override
    public DefUse getDU() {
      return null;
    }

    @Override
    public Iterator<NewSiteReference> iterateNewSites() {
      return EmptyIterator.instance();
    }

    @Override
    public Iterator<CallSiteReference> iterateCallSites() {
      return EmptyIterator.instance();
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }
  }

  /**
   * Builds a node whose caller context transitively renders that same node, which arises naturally
   * from mutually-recursive closures. Before the fix, rendering it recursed until the JVM ran out
   * of memory; it must now terminate with a bounded string.
   */
  @Test
  public void cyclicContextToStringTerminates() {
    IMethod method = fakeMethod();

    // A caller node whose toString() loops back to the node under test, closing the cycle. The node
    // under test is not available when its (final) context is constructed, so the back-edge is
    // resolved lazily through this one-element holder.
    CGNode[] holder = new CGNode[1];
    CGNode loopingCaller = fakeCaller(holder);
    Context cyclicContext = new CallerContext(loopingCaller);

    TestNode node = new TestNode(method, cyclicContext);
    holder[0] = node;

    String rendered =
        assertTimeoutPreemptively(
            Duration.ofSeconds(10),
            node::toString,
            "NodeImpl.toString() did not terminate on a cyclic context (issue #1992)");

    assertTrue(rendered.startsWith("Node: "), rendered);
    assertTrue(
        rendered.length() < 10_000,
        "render should be bounded, was " + rendered.length() + " chars");
  }

  /**
   * A stub {@link IMethod} that renders as {@code fakeMethod()} and returns default values for
   * every other call, enough to stand in for the method of a {@link BasicCallGraph.NodeImpl}.
   */
  private static IMethod fakeMethod() {
    return (IMethod)
        Proxy.newProxyInstance(
            NodeImplToStringTest.class.getClassLoader(),
            new Class<?>[] {IMethod.class},
            (proxy, m, args) -> {
              if (m.getName().equals("toString")) return "fakeMethod()";
              return defaultValue(m.getReturnType());
            });
  }

  /**
   * A stub {@link CGNode} whose {@code toString()} delegates to {@code holder[0]}, the node under
   * test. Used as the caller of the node's {@link CallerContext} so that rendering the node's
   * context loops back to the node itself, closing the cycle. The back-edge is resolved through
   * {@code holder} because the node does not yet exist when its (final) context is constructed.
   */
  private static CGNode fakeCaller(CGNode[] holder) {
    return (CGNode)
        Proxy.newProxyInstance(
            NodeImplToStringTest.class.getClassLoader(),
            new Class<?>[] {CGNode.class},
            (proxy, m, args) ->
                switch (m.getName()) {
                  case "toString" -> holder[0].toString();
                  case "getMethod" -> fakeMethod();
                  case "getGraphNodeId" -> 0;
                  case "equals" -> proxy == args[0];
                  case "hashCode" -> 0;
                  default -> defaultValue(m.getReturnType());
                });
  }

  /**
   * Returns a type-appropriate default for an unstubbed proxy method: {@code false}, 0, or null.
   */
  private static Object defaultValue(Class<?> returnType) {
    if (returnType == boolean.class) return false;
    if (returnType == int.class) return 0;
    return null;
  }
}
