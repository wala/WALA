/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Steve Suh           <suhsteve@gmail.com>
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

package org.scandroid.util;

import java.io.File;
import java.util.Properties;

import org.scandroid.domain.DomainElement;

import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ide.ui.IFDSExplorer;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;


public class GraphUtil {
	private static String folderPath = "callgraphs";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <E extends ISSABasicBlock> void makeCG(
			CGAnalysisContext<E> analysisContext) {
		make(analysisContext.getOptions().getFilename(), (Graph) analysisContext.cg,
				"FullCallGraph");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <E extends ISSABasicBlock> void makePCG(
			CGAnalysisContext<E> analysisContext) {
		make(analysisContext.getOptions().getFilename(),
				(Graph) analysisContext.partialGraph, "PartialCallGraph");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <E extends ISSABasicBlock> void makeOneLCG(
			CGAnalysisContext<E> analysisContext) {
		make(analysisContext.getOptions().getFilename(),
				(Graph) analysisContext.oneLevelGraph, "OneLevelCallGraph");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <E extends ISSABasicBlock> void makeSystemToAPKCG(
			CGAnalysisContext<E> analysisContext) {
		make(analysisContext.getOptions().getFilename(),
				(Graph) analysisContext.systemToApkGraph, "SystemToApkGraph");
	}

	public static <E extends ISSABasicBlock> void exploreIFDS(
			TabulationResult<BasicBlockInContext<E>, CGNode, DomainElement> flowResult) {
		Properties p = null;
		try {
			p = WalaProperties.loadProperties();
		} catch (WalaException e) {
			e.printStackTrace();
			Assertions.UNREACHABLE();
		}
		IFDSExplorer.setDotExe(p.getProperty(WalaExamplesProperties.DOT_EXE));
		IFDSExplorer
				.setGvExe(p.getProperty(WalaExamplesProperties.PDFVIEW_EXE));
		try {
			IFDSExplorer.viewIFDS(flowResult);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void make(String filename, Graph<Object> cg, String suffix) {
		Properties p = null;
		try {
			p = WalaProperties.loadProperties();
		} catch (WalaException e) {
			e.printStackTrace();
			Assertions.UNREACHABLE();
		}

		File theDir = new File(folderPath);
		if (!theDir.exists())
			theDir.mkdir();

		String pdfFile, dotFile, dotExe;
		pdfFile = folderPath + File.separatorChar + filename + "." + suffix
				+ ".pdf";
		dotFile = folderPath + File.separatorChar + filename
				+ PDFTypeHierarchy.DOT_FILE;
		dotExe = p.getProperty(WalaExamplesProperties.DOT_EXE);
		try {
			DexDotUtil.dotify(cg, null, dotFile, pdfFile, dotExe);
		} catch (WalaException e) {
			e.printStackTrace();
			Assertions.UNREACHABLE();
		}
	}

}
