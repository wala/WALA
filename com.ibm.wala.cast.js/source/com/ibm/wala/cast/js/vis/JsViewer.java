package com.ibm.wala.cast.js.vis;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.viz.viewer.PaPanel;
import com.ibm.wala.viz.viewer.WalaViewer;

public class JsViewer extends WalaViewer{

	private static final long serialVersionUID = 1L;

	public JsViewer(CallGraph cg, PointerAnalysis pa) {
		super(cg, pa);
	}
	
	@Override
	protected PaPanel createPaPanel(CallGraph cg, PointerAnalysis pa) {
		return new JsPaPanel(cg, pa);
	}
}
