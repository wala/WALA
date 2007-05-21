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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.resource.impl.URIConverterImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.ibm.wala.ecore.common.CommonFactory;
import com.ibm.wala.ecore.common.EPair;
import com.ibm.wala.ecore.common.ERelation;
import com.ibm.wala.ecore.common.EStringHolder;
import com.ibm.wala.ecore.regex.impl.RegexPackageImpl;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;
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

  /**
   * Serialize an EObject to a file.
   * 
   * @param o
   * @param fileName
   * @throws WalaException
   */
  public static void saveToFile(EObject o, String fileName) throws WalaException {
    saveToFile(Collections.singleton(o), fileName);
  }

  /**
   * Serialize a set of EObjects.
   * 
   * Each EObject in the set will be moved to the exporting container.  So, you need
   * to explicitly populate the Collection 'set' includes everything reachable
   * that is not referenced by a containment relationship.  Otherwise, you will get
   * "Dangling HRef" exceptions from the emf save() operation.
   * 
   * @param set
   *          set of EObjects
   * @param fileName
   * @throws WalaException
   * @throws IllegalArgumentException  if set is null
   */
  public static void saveToFile(Collection<EObject> set, String fileName) throws WalaException {
    if (set == null) {
      throw new IllegalArgumentException("set is null");
    }
    // save the xmi file
    ResourceSet resSet = new ResourceSetImpl();
    Resource r = resSet.createResource(URI.createFileURI(fileName));
    for (Iterator<EObject> it = set.iterator(); it.hasNext();) {
      EObject o = it.next();
      r.getContents().add(o);
    }
    java.util.Map<String, String> options = new HashMap<String, String>();
    options.put(org.eclipse.emf.ecore.xmi.XMLResource.OPTION_ENCODING, "UTF-8");
    try {
      r.save(options);
    } catch (IOException e) {
      e.printStackTrace();
      throw new WalaException("failure to save to file: " + fileName, e);
    }
  }

  /**
   * Save a serialized version of an EObject in a byte array
   * 
   * @param o
   *          an object to serialize
   * @return the serialized form of o as an array
   * @throws IllegalArgumentException  if o is null
   */
  public static byte[] saveToArray(EObject o) {

    if (o == null) {
      throw new IllegalArgumentException("o is null");
    }
    o = EcoreUtil.copy(o);
    // save the xmi file
    ResourceSet resSet = new ResourceSetImpl();
    Resource r = resSet.createResource(URI.createURI("junk"));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    r.getContents().add(o);
    java.util.Map<String, String> options = new HashMap<String, String>();
    options.put(org.eclipse.emf.ecore.xmi.XMLResource.OPTION_ENCODING, "UTF-8");
    try {
      r.save(out, options);
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    byte[] data = out.toByteArray();
    return data;
  }

  /**
   * @param R
   *          a releation
   * @return a java.util.Map that holds the contents of the relation
   * @throws IllegalArgumentException  if R is null
   */
  public static Map createMap(ERelation R) {
    if (R == null) {
      throw new IllegalArgumentException("R is null");
    }
    HashMap result = new HashMap();
    for (Iterator it = R.getContents().iterator(); it.hasNext();) {
      EPair p = (EPair) it.next();
      Set<Object> s = MapUtil.findOrCreateSet(result, p.getX());
      s.add(p.getY());
    }
    return result;
  }

  /**
   * Populate an ERelation with the EObject contents of a Map
   * 
   * @param R
   * @param M
   *          Map <EObject -> EObject>
   * @throws IllegalArgumentException  if M is null
   */
  public static void populateRelation(ERelation R, Map M) {
    if (M == null) {
      throw new IllegalArgumentException("M is null");
    }
    for (Iterator it = M.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      Object k = e.getKey();
      EObject key = null;
      if (k instanceof String) {
        key = CommonFactory.eINSTANCE.createEStringHolder();
        ((EStringHolder) key).setValue((String) k);
      } else {
        key = (EObject) k;
      }
      Set s = (Set) e.getValue();
      for (Iterator it2 = s.iterator(); it2.hasNext();) {
        Object val = it2.next();
        if (val instanceof EObject) {
          EObject value = (EObject) val;
          EPair p = CommonFactory.eINSTANCE.createEPair();
          p.setX(key);
          p.setY(value);
          R.getContents().add(p);
        }
      }
    }
  }

  /**
   * Get a resolved EPackage object for a particular EClass
   * 
   * @param klass
   *          an EClass
   * @return a resolved instance of the EPackage for the class
   * @throws WalaException
   *           if the resolution fails for some reason
   * @throws IllegalArgumentException  if klass is null
   */
  public static EPackage resolveEPackage(EClass klass) throws WalaException, IllegalArgumentException {
    // the data in the EComponent instance may not be fully resolved due to
    // weirdness in how ecore.xmi reads xmi files. We have to first consult
    // the registry to get the desired ePackage instance for klass.
    
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    if (klass.getEPackage() == null) {
      throw new IllegalArgumentException("klass with no EPackage " + klass);
    }
    
    EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(klass.getEPackage().getNsURI());

    // check that the package lookup succeeded
    if (ePackage == null) {
      StringBuffer msg = new StringBuffer("could not resolve package with nsURI " + klass.getEPackage().getNsURI());
      msg.append("\n");
      msg.append("The client program likely forgot to call init() on the package implementation");
      throw new WalaException(msg.toString());
    }
    return ePackage;
  }
  
  /**
   * Get a resolved EPackage object for a particular EPackage
   * 
   * @param p
   * @return a resolved instance of the EPackage for the class
   * @throws WalaException
   *           if the resolution fails for some reason
   * @throws IllegalArgumentException  if p is null
   */
  public static EPackage resolveEPackage(EPackage p) throws WalaException {
    if (p == null) {
      throw new IllegalArgumentException("p is null");
    }
    // the data in the EComponent instance may not be fully resolved due to
    // weirdness in how ecore.xmi reads xmi files. We have to first consult
    // the registry to get the desired ePackage instance for klass.
    EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(p.getNsURI());

    // check that the package lookup succeeded
    if (ePackage == null) {
      StringBuffer msg = new StringBuffer("could not resolve package with nsURI " + p.getNsURI());
      msg.append("\n");
      msg.append("The client program likely forgot to call init() on the package implementation");
      throw new WalaException(msg.toString());
    }
    return ePackage;
  }

  /**
   * Get a resolved representative for a particular EClass
   * 
   * @param klass1
   *          an EClass, potentially represented as a proxy
   * @return a resolved (not proxy) version of the EClass
   * @throws WalaException
   *           if the resolution fails for some reason
   * @throws IllegalArgumentException  if klass1 is null
   */
  public static EClass resolveEClass(EClass klass1) throws WalaException {
    if (klass1 == null) {
      throw new IllegalArgumentException("klass1 is null");
    }
    // check that we can resolve the klass metadata
    if (klass1.getEPackage() == null) {
      throw new WalaException("ill-formed EClass " + klass1);
    }

    EPackage ePackage = resolveEPackage(klass1);

    // now that we have the "true" ePackage, we can get the "true"
    // EClass for klass
    EClass klass = (EClass) ePackage.getEClassifier(klass1.getName());

    if (klass == null) {
      throw new WalaException("no classifier found for " + klass1 + ". We can't create an instance. This error is unexpected.");
    }
    // check that klass is now OK.
    if (klass.getClassifierID() == -1) {
      throw new WalaException(klass + " has a classifier id of -1.  We can't create an instance. This error is unexpected.");
    }
    return klass;
  }

  /**
   * @param R
   * @param key
   * @return the subset r \in R s.t. r.X.equals(key)
   * @throws IllegalArgumentException  if R is null
   */
  public static ERelation pruneRelationForKey(ERelation R, EObject key) {
    if (R == null) {
      throw new IllegalArgumentException("R is null");
    }
    Map m = createMap(R);
    Set s = (Set) m.get(key);
    m = new HashMap(1);
    if (s != null) {
      m.put(key,s);
    }
    ERelation result = CommonFactory.eINSTANCE.createERelation();
    result.setName(R.getName());
    populateRelation(result,m);
    return result;
  }
  /**
   * @param R
   * @param f
   * @return the subset r \in R s.t. r.X.equals(some y s.t. f.accepts(y))
   * @throws IllegalArgumentException  if R is null
   */
  public static ERelation pruneRelationForKey(ERelation R, Filter f) {
    if (R == null) {
      throw new IllegalArgumentException("R is null");
    }
    Map m = createMap(R);
    Map newM = new HashMap();
    for (FilterIterator it = new FilterIterator(m.keySet().iterator(),f); it.hasNext(); ) {
      Object key = it.next();
      newM.put(key,m.get(key));
    }

    ERelation result = CommonFactory.eINSTANCE.createERelation();
    result.setName(R.getName());
    populateRelation(result,newM);
    return result;
  }

}