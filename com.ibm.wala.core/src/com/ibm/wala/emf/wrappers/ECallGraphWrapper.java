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
package com.ibm.wala.emf.wrappers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.XMLResource.XMLMap;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLMapImpl;

import com.ibm.wala.ecore.common.CommonFactory;
import com.ibm.wala.ecore.common.EContainer;
import com.ibm.wala.ecore.common.EPair;
import com.ibm.wala.ecore.common.ERelation;
import com.ibm.wala.ecore.java.ECallSite;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.EJavaMethod;
import com.ibm.wala.ecore.java.callGraph.CallGraphFactory;
import com.ibm.wala.ecore.java.callGraph.CallGraphPackage;
import com.ibm.wala.ecore.java.callGraph.ECallGraph;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * This is a convenience class: it provides a view of an ECallGraph that should
 * be more convenient for most client-side use.
 * 
 * @author sfink
 */
public class ECallGraphWrapper extends EObjectGraphImpl {

  private static boolean DEBUG = false;

  /**
   * @return an ECallGraph representing the contents of this object
   * @see com.ibm.wala.emf.wrappers.EObjectGraphImpl#export()
   */
  public EObject export() {
    ECallGraph cg = CallGraphFactory.eINSTANCE.createECallGraph();
    EObjectDictionary nodes = makeNodes(cg);
    makeEdges(cg, nodes);
    return cg;
  }

  /**
   * @param cg
   * @param nodes
   */
  @SuppressWarnings("unchecked")
  private void makeEdges(ECallGraph cg, EObjectDictionary nodes) {
    ERelation r = CommonFactory.eINSTANCE.createERelation();
    for (Iterator it = iterator(); it.hasNext();) {
      EObject src = (EObject) it.next();
      for (Iterator it2 = getSuccNodes(src); it2.hasNext();) {
        EObject dst = (EObject) it2.next();
        EPair p = CommonFactory.eINSTANCE.createEPair();
        p.setX(nodes.findOrAdd(src));
        p.setY(nodes.findOrAdd(dst));
        r.getContents().add(p);
      }
    }
    cg.setEdges(r);
  }

  private EObjectDictionary makeNodes(ECallGraph cg) {
    EContainer debugContainer = null;

    EObjectDictionary d = new EObjectDictionary();
    for (Iterator it = iterator(); it.hasNext();) {
      EObject o = (EObject) it.next();
      d.findOrAdd(o);
      if (o instanceof ECallSite) {
        ECallSite site = (ECallSite) o;
        site.setJavaMethod((EJavaMethod) d.findOrAdd(site.getJavaMethod()));
      }
      if (DEBUG) {
        if (o instanceof EJavaMethod) {
          debugContainer = debugClassContainers(debugContainer, ((EJavaMethod) o).getJavaClass());
        } else if (o instanceof ECallSite) {
          EJavaMethod m = ((ECallSite) o).getJavaMethod();
          debugContainer = debugClassContainers(debugContainer, m.getJavaClass());
        } else {
          Assertions.UNREACHABLE();
        }
      }
    }
    EContainer c = (EContainer) d.export(true);
    cg.setNodes(c);
    return d;
  }

  private EContainer debugClassContainers(EContainer debugContainer, EJavaClass klass) {
    EObject c = klass.eContainer();
    Assertions._assert(c != null, "null container found");
    Assertions._assert(c instanceof EContainer, "expected an EContainer, got " + c.getClass());
    if (debugContainer == null) {
      debugContainer = (EContainer) c;
    }
    Assertions._assert(debugContainer == c);
    return debugContainer;
  }

  @SuppressWarnings("unchecked")
  public static ECallGraphWrapper load(String fileName, ClassLoader loader) {
    ECallGraph G = loadFromFile(fileName, loader);
    Assertions.productionAssertion(G != null);
    ECallGraphWrapper result = new ECallGraphWrapper();

    for (Iterator<EPair> it = G.getNodes().getContents().iterator(); it.hasNext();) {
      result.addNode(it.next());
    }
    for (Iterator<EPair> it = G.getEdges().getContents().iterator(); it.hasNext();) {
      EPair p = it.next();
      result.addEdge(p.getX(), p.getY());
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static ECallGraph loadFromFile(String fileName, ClassLoader loader) {

    Resource r = new XMIResourceImpl(URI.createURI(fileName));
    XMLResource.XMLMap xmlMap = new XMLMapImpl();
    xmlMap.setNoNamespacePackage(CallGraphPackage.eINSTANCE);
    Map<String, XMLMap> options = new HashMap<String, XMLMap>();
    options.put(XMLResource.OPTION_XML_MAP, xmlMap);
    try {
      r.load(options);
    } catch (IOException e) {
      // try again a different way
      // This is offensive. TODO: fix this.
      InputStream s = loader.getResourceAsStream(fileName);
      Assertions.productionAssertion(s != null, "null resource for " + fileName);
      ResourceSet resSet = new ResourceSetImpl();
      r = resSet.createResource(URI.createURI("junk"));
      options = new HashMap<String, XMLMap>();
      try {
        r.load(s, options);
      } catch (IOException e2) {
        e2.printStackTrace();
        Assertions.UNREACHABLE("failure loading call graph from file " + fileName);
      }
    }
    EList contents = r.getContents();
    for (Iterator<EObject> it = contents.iterator(); it.hasNext();) {
      Object o = it.next();
      if (o instanceof ECallGraph) {
        return (ECallGraph) o;
      }
    }
    return null;
  }
}