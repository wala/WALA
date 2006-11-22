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

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.ImmutableByteArray;
import com.ibm.wala.util.UTF8Convert;

/**
 *
 * Constants defined in the EJB SPEC.
 * 
 * @author sfink
 */
public interface EJBConstants {
  /*
   * Some method names defined in the EJB spec 
   */

  public final static Atom ejbRemoveAtom = Atom.findOrCreateAsciiAtom("ejbRemove");
  public final static byte[] ejbRemoveSig = UTF8Convert.toUTF8("()V");

  public final static Atom ejbActivateAtom = Atom.findOrCreateAsciiAtom("ejbActivate");
  public final static byte[] ejbActivateSig = UTF8Convert.toUTF8("()V");

  public final static Atom ejbPassivateAtom = Atom.findOrCreateAsciiAtom("ejbPassivate");
  public final static byte[] ejbPassivateSig = UTF8Convert.toUTF8("()V");

  public final static Atom ejbLoadAtom = Atom.findOrCreateAsciiAtom("ejbLoad");
  public final static byte[] ejbLoadSig = UTF8Convert.toUTF8("()V");

  public final static Atom ejbStoreAtom = Atom.findOrCreateAsciiAtom("ejbStore");
  public final static byte[] ejbStoreSig = UTF8Convert.toUTF8("()V");

  public final static Atom setSessionContextAtom = Atom.findOrCreateAsciiAtom("setSessionContext");
  public final static byte[] setSessionContextSig = UTF8Convert.toUTF8("(Ljavax/ejb/SessionContext;)V");
  public final static Descriptor setSessionContextDescriptor =
    Descriptor.findOrCreate(new ImmutableByteArray(setSessionContextSig));

  public final static Atom setEntityContextAtom = Atom.findOrCreateAsciiAtom("setEntityContext");
  public final static byte[] setEntityContextSig = UTF8Convert.toUTF8("(Ljavax/ejb/EntityContext;)V");

  public final static Atom unsetEntityContextAtom = Atom.findOrCreateAsciiAtom("unsetEntityContext");
  public final static byte[] unsetEntityContextSig = UTF8Convert.toUTF8("()V");

  public final static Atom onMessageAtom = Atom.findOrCreateAsciiAtom("onMessage");
  public final static byte[] onMessageBytes = UTF8Convert.toUTF8("(Ljavax/jms/Message;)V");
  public final Descriptor onMessageDesc = Descriptor.findOrCreate(new ImmutableByteArray(onMessageBytes));

  // some special methods defined by the contract for entity bean interfaces
  public static final Atom CREATE = Atom.findOrCreateAsciiAtom("create");
  public static final Atom EJB_CREATE = Atom.findOrCreateAsciiAtom("ejbCreate");
  public static final Atom EJB_POST_CREATE = Atom.findOrCreateAsciiAtom("ejbPostCreate");
  public static final Atom REMOVE = Atom.findOrCreateAsciiAtom("remove");
  public static final Atom EJB_REMOVE = Atom.findOrCreateAsciiAtom("ejbRemove");
  public static final Atom GET_PRIMARY_KEY = Atom.findOrCreateAsciiAtom("getPrimaryKey");
  public static final Atom GET_EJB_META_DATA = Atom.findOrCreateAsciiAtom("getEJBMetaData");
  public static final Atom GET_EJB_HOME = Atom.findOrCreateAsciiAtom("getEJBHome");
  public static final Atom GET_HANDLE = Atom.findOrCreateAsciiAtom("getHandle");
  public static final Atom IS_IDENTICAL = Atom.findOrCreateAsciiAtom("isIdentical");

  // special-case support for some exceptions
  public final static TypeReference CreateExceptionClass =
    TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/ejb", "CreateException");

  public final static TypeReference EJBExceptionClass =
    TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/ejb", "EJBException");

  public final static TypeReference FinderExceptionClass =
    TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/ejb", "FinderException");

  public final static TypeReference RemoveExceptionClass =
    TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/ejb", "RemoveException");

  public final static TypeReference RemoteExceptionClass =
    TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "java/rmi", "RemoteException");

  public final static TypeReference ObjectNotFoundExceptionClass =
    TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/ejb", "ObjectNotFoundException");

  public final static TypeReference MessageClass =
    TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/jms", "Message");
  public final static TypeReference BytesMessageClass =
    TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/jms", "BytesMessage");
  public final static TypeReference ObjectMessageClass =
    TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/jms", "ObjectMessage");
  public final static TypeReference StreamMessageClass =
    TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/jms", "StreamMessage");
  public final static TypeReference TextMessageClass =
    TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/jms", "TextMessage");
  public final static TypeReference[] KnownMessages =
    new TypeReference[] { MessageClass, BytesMessageClass, ObjectMessageClass, StreamMessageClass, TextMessageClass };
}
