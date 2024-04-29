/*
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices;

import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import java.util.Collection;
import java.util.Map;

/**
 * A vertex factory is associated with a flow graph. It manages its vertex set, making sure that
 * vertices aren't unnecessarily created twice.
 *
 * @author mschaefer
 */
public class VertexFactory {
  private final Map<Pair<FuncVertex, CallSiteReference>, CallVertex> callVertexCache =
      HashMapFactory.make();
  private final Map<Pair<FuncVertex, CallSiteReference>, ReflectiveCallVertex>
      reflectiveCallVertexCache = HashMapFactory.make();
  private final Map<IClass, FuncVertex> funcVertexCache = HashMapFactory.make();
  private final Map<Pair<FuncVertex, Integer>, ParamVertex> paramVertexCache =
      HashMapFactory.make();
  private final Map<String, PropVertex> propVertexCache = HashMapFactory.make();
  private final Map<FuncVertex, RetVertex> retVertexCache = HashMapFactory.make();
  private final Map<FuncVertex, ArgVertex> argVertexCache = HashMapFactory.make();
  private final Map<Pair<FuncVertex, Integer>, VarVertex> varVertexCache = HashMapFactory.make();
  private final Map<Pair<String, String>, LexicalVarVertex> lexicalAccessVertexCache =
      HashMapFactory.make();
  private final Map<Pair<IMethod, Integer>, CreationSiteVertex> creationSites =
      HashMapFactory.make();

  public CallVertex makeCallVertex(FuncVertex func, JavaScriptInvoke invk) {
    CallSiteReference site = invk.getCallSite();
    Pair<FuncVertex, CallSiteReference> key = Pair.make(func, site);
    CallVertex value = callVertexCache.get(key);
    if (value == null) callVertexCache.put(key, value = new CallVertex(func, site, invk));
    return value;
  }

  public ReflectiveCallVertex makeReflectiveCallVertex(FuncVertex func, JavaScriptInvoke invk) {
    CallSiteReference site = invk.getCallSite();
    Pair<FuncVertex, CallSiteReference> key = Pair.make(func, site);
    ReflectiveCallVertex value = reflectiveCallVertexCache.get(key);
    if (value == null) {
      reflectiveCallVertexCache.put(key, value = new ReflectiveCallVertex(func, site, invk));
    }
    return value;
  }

  public Iterable<CallVertex> getCallVertices() {
    return callVertexCache.values();
  }

  public CreationSiteVertex makeCreationSiteVertex(
      IMethod method, int instruction, TypeReference createdType) {
    Pair<IMethod, Integer> key = Pair.make(method, instruction);
    CreationSiteVertex value = creationSites.get(key);
    if (value == null) {
      creationSites.put(key, value = new CreationSiteVertex(method, instruction, createdType));
    }
    return value;
  }

  public Collection<CreationSiteVertex> creationSites() {
    return creationSites.values();
  }

  public FuncVertex makeFuncVertex(IClass klass) {
    FuncVertex value = funcVertexCache.get(klass);
    if (value == null) funcVertexCache.put(klass, value = new FuncVertex(klass));
    return value;
  }

  public Collection<FuncVertex> getFuncVertices() {
    return funcVertexCache.values();
  }

  public ParamVertex makeParamVertex(FuncVertex func, int index) {
    Pair<FuncVertex, Integer> key = Pair.make(func, index);
    ParamVertex value = paramVertexCache.get(key);
    if (value == null) paramVertexCache.put(key, value = new ParamVertex(func, index));
    return value;
  }

  public PropVertex makePropVertex(String name) {
    PropVertex value = propVertexCache.get(name);
    if (value == null) propVertexCache.put(name, value = new PropVertex(name));
    return value;
  }

  public Iterable<PropVertex> getPropVertices() {
    return propVertexCache.values();
  }

  public RetVertex makeRetVertex(FuncVertex func) {
    RetVertex value = retVertexCache.get(func);
    if (value == null) retVertexCache.put(func, value = new RetVertex(func));
    return value;
  }

  public Iterable<RetVertex> getRetVertices() {
    return retVertexCache.values();
  }

  public ArgVertex makeArgVertex(FuncVertex func) {
    ArgVertex value = argVertexCache.get(func);
    if (value == null) argVertexCache.put(func, value = new ArgVertex(func));
    return value;
  }

  public Iterable<ArgVertex> getArgVertices() {
    return argVertexCache.values();
  }

  public UnknownVertex makeUnknownVertex() {
    return UnknownVertex.INSTANCE;
  }

  public VarVertex makeVarVertex(FuncVertex func, int valueNumber) {
    Pair<FuncVertex, Integer> key = Pair.make(func, valueNumber);
    VarVertex value = varVertexCache.get(key);
    if (value == null) varVertexCache.put(key, value = new VarVertex(func, valueNumber));
    return value;
  }

  public Iterable<VarVertex> getVarVertices() {
    return varVertexCache.values();
  }

  public LexicalVarVertex makeLexicalAccessVertex(String definer, String name) {
    Pair<String, String> key = Pair.make(definer, name);
    LexicalVarVertex value = lexicalAccessVertexCache.get(key);
    if (value == null)
      lexicalAccessVertexCache.put(key, value = new LexicalVarVertex(definer, name));
    return value;
  }

  private final GlobalVertex global = GlobalVertex.instance();

  public GlobalVertex global() {
    return global;
  }
}
