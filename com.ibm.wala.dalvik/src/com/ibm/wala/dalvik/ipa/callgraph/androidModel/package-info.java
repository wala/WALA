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
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
/**
 *  Inserts synthetic code that resembles Androids lifecycle.
 *
 *  It generates a new synthetic class (AndroidModelClass) containing all methods necessary to do so.
 *
 *  To model add a lifecycle-model one has to do the following steps:
 *
 *  1.  Scan for the Entrypoints of the application
 *      <code>
 *      AndroidEntryPointLocator epl = new AndroidEntryPointLocator(options);
 *      List&lt;AndroidEntryPoint&gt; entrypoints = epl.getEntryPoints(cha);
 *      AndroidEntryPointManager.ENTRIES = entrypoints;
 *      </code>
 *  2.  Optionally read in the AndroidManifest.xml
 *      <code>
 *      final AndroidManifestXMLReader reader = new AndroidManifestXMLReader(manifestFile);
 *      </code>
 *  3.  Optionally change the order of entrypoints and change the instantiation behaviour
 *  4.  Create the model and use it as the new entrypoint of the analysis 
 *      <code>
 *      IMethod model = new AndroidModel(cha, p.options, p.scfg.cache).getMethod();
 *      </code>
 *
 *  The model generated that way will "start" all components of the App. The various start-calls
 *  occurring in these components will not yet call anything useful. To change this there are two
 *  possibilities
 *
 *  * Insert a MethodTargetSelector:
 *      This works context-insensitive so if a call of "startActivity" is encountered a new model
 *      starting _all_ the Activities is generated.
 *
 *      TODO: This is about to change!
 *      <code>
 *      AnalysisOptions options;
 *      ActivityMiniModel activities = new ActivityMiniModel(cha, p.options, p.scfg.cache);
 *      options.setSelector(new DelegatingMethodTargetSelector(activities.overrideAll(), options.getMethodTargetSelector()));
 *      </code>
 *
 * * Resolve the calls context-sensitive:
 *      In Android all calls to different components use an Intent. The IntentContextSelector 
 *      remembers all Intents generated in the course of the analysis and attaches them to the
 *      start-calls as Context.
 *
 *      The IntentContextInterpreter then replaces the IR of the start-calls to start only the
 *      resolved component (or a placeholder like startExternalACTIVITY)
 *      <code>
 *          final ContextSelector contextSelector = new IntentContextSelector(new DefaultContextSelector(options, cha))
 *          final SSAContextInterpreter contextInterpreter = new FallbackContextInterpreter(new DelegatingSSAContextInterpreter(
 *              new IntentContextInterpreter(cha, options, cache), new DefaultSSAInterpreter(options, cache)));
 *      </code>
 *
 *  For the context-sensitive stuff to be able to resolve the targets either the AndroidManifest.xml
 *  should have been read or overrides been placed manually (or both).
 *
 *  @since  2013-10-25
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
package com.ibm.wala.dalvik.ipa.callgraph.androidModel;
