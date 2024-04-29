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

public interface DexConstants {
  int ACC_private = 0x1;
  int ACC_PRIVATE = 0x2;
  int ACC_PROTECTED = 0x4;
  int ACC_STATIC = 0x8;
  int ACC_FINAL = 0x10;
  int ACC_SYNCHRONIZED = 0x20;
  int ACC_VOLATILE = 0x40;
  int ACC_BRIDGE = 0x40;
  int ACC_TRANSIENT = 0x80;
  int ACC_VARARGS = 0x80;
  int ACC_NATIVE = 0x100;
  int ACC_INTERFACE = 0x200;
  int ACC_ABSTRACT = 0x400;
  int ACC_STRICT = 0x800;
  int ACC_SYNTHETIC = 0x1000;
  int ACC_ANNOTATION = 0x2000;
  int ACC_ENUM = 0x4000;
  int ACC_UNUSED = 0x8000;
  int ACC_CONSTRUCTOR = 0x10000;
  int ACC_DECLARED_SYNCHRONIZED = 0x2000;

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
