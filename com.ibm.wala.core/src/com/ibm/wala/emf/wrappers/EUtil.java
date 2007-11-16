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
import java.net.URL;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.resource.impl.URIConverterImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.ibm.wala.ecore.regex.impl.RegexPackageImpl;
import com.ibm.wala.util.warnings.WalaException;

/**
 * 
 * miscellaneous utilities for manipulating EMF objects
 * 
 * @author sfink
 */
@SuppressWarnings("unchecked")
public class EUtil {

  private static final String CORE_ECORE = "wala.ecore";


  static {
    RegexPackageImpl.init();
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }

  /**
   * @return a URIConverter set up to find ecore models used by WALA
   */
  public static URIConverter getURIConverter() {

    ClassLoader loader = EUtil.class.getClassLoader();
    if (loader == null) {
      // a hack for when we're sticking stuff in the bootstrap classpath
      loader = ClassLoader.getSystemClassLoader();
    }
    URL url = loader.getResource(CORE_ECORE);
    String s = url.toString();
    int index = s.indexOf(CORE_ECORE);
    s = s.substring(0, index);

//    URI uri1 = URI.createURI(CORE_CONTROLLER_URI);
//    URI uri2 = URI.createURI(s);
    URIConverter converter = new URIConverterImpl();
//    converter.getURIMap().put(uri1, uri2);

    return converter;
  }

  /**
   * @param xmlFile
   *          an xml file
   * @return List<EObject> serialized in said file.
   * @throws IllegalArgumentException  if loader is null
   */
  public static List<EObject> readEObjects(String xmlFile, ClassLoader loader) throws WalaException {

    if (loader == null) {
      throw new IllegalArgumentException("loader is null");
    }
    URL url = loader.getResource(xmlFile);
    URI fileURI = null;
    if (url != null) {
      fileURI = URI.createURI(url.toString());
    } else {
      fileURI = URI.createFileURI(xmlFile);
    }

    URIConverter converter = getURIConverter();
    ResourceSet resourceSet = new ResourceSetImpl();
    resourceSet.setURIConverter(converter);

    Resource resource = resourceSet.createResource(fileURI);

    try {
      resource.load(null);
    } catch (IOException e) {
      throw new WalaException("failure to load " + xmlFile + ".", e);
    }

    return resource.getContents();
  }

//  /**
//   * @param R
//   * @param key
//   * @return the subset r \in R s.t. r.X.equals(key)
//   * @throws IllegalArgumentException  if R is null
//   */
//  public static ERelation pruneRelationForKey(ERelation R, EObject key) {
//    if (R == null) {
//      throw new IllegalArgumentException("R is null");
//    }
//    Map m = createMap(R);
//    Set s = (Set) m.get(key);
//    m = new HashMap(1);
//    if (s != null) {
//      m.put(key,s);
//    }
//    ERelation result = CommonFactory.eINSTANCE.createERelation();
//    result.setName(R.getName());
//    populateRelation(result,m);
//    return result;
//  }
//  /**
//   * @param R
//   * @param f
//   * @return the subset r \in R s.t. r.X.equals(some y s.t. f.accepts(y))
//   * @throws IllegalArgumentException  if R is null
//   */
//  public static ERelation pruneRelationForKey(ERelation R, Filter f) {
//    if (R == null) {
//      throw new IllegalArgumentException("R is null");
//    }
//    Map m = createMap(R);
//    Map newM = new HashMap();
//    for (FilterIterator it = new FilterIterator(m.keySet().iterator(),f); it.hasNext(); ) {
//      Object key = it.next();
//      newM.put(key,m.get(key));
//    }
//
//    ERelation result = CommonFactory.eINSTANCE.createERelation();
//    result.setName(R.getName());
//    populateRelation(result,newM);
//    return result;
//  }

}