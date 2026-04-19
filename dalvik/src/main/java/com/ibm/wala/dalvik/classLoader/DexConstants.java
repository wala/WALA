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

public interface DexConstants {
  int ACC_PUBLIC = AccessFlags.PUBLIC.getValue();
  int ACC_PRIVATE = AccessFlags.PRIVATE.getValue();
  int ACC_PROTECTED = AccessFlags.PROTECTED.getValue();
  int ACC_STATIC = AccessFlags.STATIC.getValue();
  int ACC_FINAL = AccessFlags.FINAL.getValue();
  int ACC_SYNCHRONIZED = AccessFlags.SYNCHRONIZED.getValue();
  int ACC_VOLATILE = AccessFlags.VOLATILE.getValue();
  int ACC_BRIDGE = AccessFlags.BRIDGE.getValue();
  int ACC_TRANSIENT = AccessFlags.TRANSIENT.getValue();
  int ACC_VARARGS = AccessFlags.VARARGS.getValue();
  int ACC_NATIVE = AccessFlags.NATIVE.getValue();
  int ACC_INTERFACE = AccessFlags.INTERFACE.getValue();
  int ACC_ABSTRACT = AccessFlags.ABSTRACT.getValue();
  int ACC_STRICT = AccessFlags.STRICTFP.getValue();
  int ACC_SYNTHETIC = AccessFlags.SYNTHETIC.getValue();
  int ACC_ANNOTATION = AccessFlags.ANNOTATION.getValue();
  int ACC_ENUM = AccessFlags.ENUM.getValue();
  int ACC_UNUSED = 0x8000;
  int ACC_CONSTRUCTOR = AccessFlags.CONSTRUCTOR.getValue();
  int ACC_DECLARED_SYNCHRONIZED = AccessFlags.DECLARED_SYNCHRONIZED.getValue();

  /**
   * @deprecated
   * @see #ACC_PUBLIC
   */
  @Deprecated(forRemoval = true, since = "1.7.1")
  int ACC_private = ACC_PUBLIC;

  int VALUE_BYTE = 0x00;
  int VALUE_SHORT = 0x02;
  int VALUE_CHAR = 0x03;
  int VALUE_INT = 0x04;
  int VALUE_LONG = 0x06;
  int VALUE_FLOAT = 0x10;
  int VALUE_DOUBLE = 0x11;
  int VALUE_STRING = 0x17;
  int VALUE_TYPE = 0x18;
  int VALUE_FIELD = 0x19;
  int VALUE_METHOD = 0x1a;
  int VALUE_ENUM = 0x1b;
  int VALUE_ARRAY = 0x1c;
  int VALUE_ANNOTATION = 0x1d;
  int VALUE_NULL = 0x1e;
  int VALUE_BOOLEAN = 0x1f;
}
