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
 * Copyright (c) 2009-2012,
 *
 * <p>Galois, Inc. (Aaron Tomb <atomb@galois.com>, Rogan Creswick <creswick@galois.com>, Adam
 * Foltzer <acfoltzer@galois.com>) Steve Suh <suhsteve@gmail.com>
 *
 * <p>All rights reserved.
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. The names of the contributors may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.scandroid.spec;

public interface ISpecs {
  /* A list of functions that are entry points. Arguments to these
   * functions that are considered sources should be in the
   * SourceSpec list. */
  public MethodNamePattern[] getEntrypointSpecs();

  /* Other methods that source data via their return values or
   * modification of their parameters. */
  public SourceSpec[] getSourceSpecs();

  /* Methods that sink data supplied by some of their parameters. */
  public SinkSpec[] getSinkSpecs();

  public static ISpecs EMPTY_SPECS =
      new ISpecs() {

        @Override
        public SourceSpec[] getSourceSpecs() {
          return new SourceSpec[] {};
        }

        @Override
        public SinkSpec[] getSinkSpecs() {
          return new SinkSpec[] {};
        }

        @Override
        public MethodNamePattern[] getEntrypointSpecs() {
          return new MethodNamePattern[] {};
        }
      };
}
