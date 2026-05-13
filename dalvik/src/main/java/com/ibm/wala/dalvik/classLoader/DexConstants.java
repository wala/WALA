/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released under the terms listed below.
 *
 */
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package com.ibm.wala.dalvik.classLoader;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.ValueType;

/**
 * @deprecated replace {@code ACC_XXX} with {@link AccessFlags}{@code .XXX.getValue()} and replace
 *     {@code VALUE_XXX} with {@link ValueType}{@code .XXX}
 */
@Deprecated(forRemoval = true, since = "1.7.2")
public interface DexConstants {

  /**
   * @deprecated
   * @see AccessFlags#PUBLIC
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_PUBLIC = AccessFlags.PUBLIC.getValue();

  /**
   * @deprecated
   * @see AccessFlags#PRIVATE
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_PRIVATE = AccessFlags.PRIVATE.getValue();

  /**
   * @deprecated
   * @see AccessFlags#PROTECTED
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_PROTECTED = AccessFlags.PROTECTED.getValue();

  /**
   * @deprecated
   * @see AccessFlags#STATIC
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_STATIC = AccessFlags.STATIC.getValue();

  /**
   * @deprecated
   * @see AccessFlags#FINAL
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_FINAL = AccessFlags.FINAL.getValue();

  /**
   * @deprecated
   * @see AccessFlags#SYNCHRONIZED
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_SYNCHRONIZED = AccessFlags.SYNCHRONIZED.getValue();

  /**
   * @deprecated
   * @see AccessFlags#VOLATILE
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_VOLATILE = AccessFlags.VOLATILE.getValue();

  /**
   * @deprecated
   * @see AccessFlags#BRIDGE
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_BRIDGE = AccessFlags.BRIDGE.getValue();

  /**
   * @deprecated
   * @see AccessFlags#TRANSIENT
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_TRANSIENT = AccessFlags.TRANSIENT.getValue();

  /**
   * @deprecated
   * @see AccessFlags#VARARGS
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_VARARGS = AccessFlags.VARARGS.getValue();

  /**
   * @deprecated
   * @see AccessFlags#NATIVE
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_NATIVE = AccessFlags.NATIVE.getValue();

  /**
   * @deprecated
   * @see AccessFlags#INTERFACE
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_INTERFACE = AccessFlags.INTERFACE.getValue();

  /**
   * @deprecated
   * @see AccessFlags#ABSTRACT
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_ABSTRACT = AccessFlags.ABSTRACT.getValue();

  /**
   * @deprecated
   * @see AccessFlags#STRICTFP
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_STRICT = AccessFlags.STRICTFP.getValue();

  /**
   * @deprecated
   * @see AccessFlags#SYNTHETIC
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_SYNTHETIC = AccessFlags.SYNTHETIC.getValue();

  /**
   * @deprecated
   * @see AccessFlags#ANNOTATION
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_ANNOTATION = AccessFlags.ANNOTATION.getValue();

  /**
   * @deprecated
   * @see AccessFlags#ENUM
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_ENUM = AccessFlags.ENUM.getValue();

  int ACC_UNUSED = 0x8000;

  /**
   * @deprecated
   * @see AccessFlags#CONSTRUCTOR
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_CONSTRUCTOR = AccessFlags.CONSTRUCTOR.getValue();

  /**
   * @deprecated
   * @see AccessFlags#DECLARED_SYNCHRONIZED
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_DECLARED_SYNCHRONIZED = AccessFlags.DECLARED_SYNCHRONIZED.getValue();

  /**
   * @deprecated
   * @see #ACC_PUBLIC
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int ACC_private = ACC_PUBLIC;

  /**
   * @deprecated
   * @see ValueType#BYTE
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_BYTE = ValueType.BYTE;

  /**
   * @deprecated
   * @see ValueType#SHORT
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_SHORT = ValueType.SHORT;

  /**
   * @deprecated
   * @see ValueType#CHAR
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_CHAR = ValueType.CHAR;

  /**
   * @deprecated
   * @see ValueType#INT
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_INT = ValueType.INT;

  /**
   * @deprecated
   * @see ValueType#LONG
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_LONG = ValueType.LONG;

  /**
   * @deprecated
   * @see ValueType#FLOAT
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_FLOAT = ValueType.FLOAT;

  /**
   * @deprecated
   * @see ValueType#DOUBLE
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_DOUBLE = ValueType.DOUBLE;

  /**
   * @deprecated
   * @see ValueType#STRING
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_STRING = ValueType.STRING;

  /**
   * @deprecated
   * @see ValueType#TYPE
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_TYPE = ValueType.TYPE;

  /**
   * @deprecated
   * @see ValueType#FIELD
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_FIELD = ValueType.FIELD;

  /**
   * @deprecated
   * @see ValueType#METHOD
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_METHOD = ValueType.METHOD;

  /**
   * @deprecated
   * @see ValueType#ENUM
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_ENUM = ValueType.ENUM;

  /**
   * @deprecated
   * @see ValueType#ARRAY
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_ARRAY = ValueType.ARRAY;

  /**
   * @deprecated
   * @see ValueType#ANNOTATION
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_ANNOTATION = ValueType.ANNOTATION;

  /**
   * @deprecated
   * @see ValueType#NULL
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_NULL = ValueType.NULL;

  /**
   * @deprecated
   * @see ValueType#BOOLEAN
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int VALUE_BOOLEAN = ValueType.BOOLEAN;
}
