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
 * Fetch and handle information on Android-Intents.
 *
 * Collects the new-Sites of Intents and tries to resolve the parameters to the
 * Intents constructor.
 *
 * When a startComponent-function is encountered this information is used to
 * redirect the call to an AndroidModel (optionally syntethized at this point).
 * For this to happen a wrapper is synthesized using AndroidModels getMethodAs-
 * Function.
 *
 * If the target could not be determined definitely all components of the application
 * matching the type of the startComponent-call are invoked.
 *
 * The specification on which startComponent-calls are known may be found in
 * IntentStarters.
 *
 * A context-free variant of the redirection of startComponent-calls may be found 
 * in the Overrides mentioned below.
 *
 * @see     com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.Overrides 
 * @see     com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel 
 * @see     com.ibm.wala.dalvik.ipa.callgraph.androidModel.MiniModel
 * @see     com.ibm.wala.dalvik.ipa.callgraph.androidModel.MicroModel 
 * @see     com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.UnknownTargetModel
 * @see     com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.ExternalModel
 * @author  Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
package com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa;
