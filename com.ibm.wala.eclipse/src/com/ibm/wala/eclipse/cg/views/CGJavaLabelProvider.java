package com.ibm.wala.eclipse.cg.views;

import java.util.Map;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.warnings.WarningSet;

public class CGJavaLabelProvider extends LabelProvider  {
	
	private Map<Integer,IJavaElement> capaNodeIdToJavaElement;
	private JavaElementLabelProvider javaEltProvider;


	public CGJavaLabelProvider(Map<Integer,IJavaElement> capaNodeIdToJavaElement) {
	  super();
	  this.capaNodeIdToJavaElement = capaNodeIdToJavaElement;
	  javaEltProvider = 
	    new JavaElementLabelProvider(
	      JavaElementLabelProvider.SHOW_PARAMETERS |
	      JavaElementLabelProvider.SHOW_OVERLAY_ICONS |
	      JavaElementLabelProvider.SHOW_POST_QUALIFIED |
	      JavaElementLabelProvider.SHOW_RETURN_TYPE);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
	  if (element instanceof CGNode) {
	    CGNode capaNode = (CGNode)element;
	    IJavaElement jdtElt = capaNodeIdToJavaElement.get(capaNode.getGraphNodeId());
	    if( jdtElt != null && jdtElt.exists() ) {
	      return javaEltProvider.getText(jdtElt);
	    } else {
	      return getWalaText(capaNode);
	    }
	  } else {
	    return getWalaInvokeText((Pair)element);		
	  }
	}
	
	private String getWalaInvokeText(Pair nodeAndCallSite) {
	  CGNode node = (CGNode) nodeAndCallSite.fst;
	  CallSiteReference site = (CallSiteReference) nodeAndCallSite.snd;
	  IMethod method = node.getMethod();
	  if (method instanceof ShrikeBTMethod) {
	    return "call@" + ((ShrikeBTMethod)method).getLineNumber(site.getProgramCounter());
	  } else if (method instanceof AstMethod) {
	    return "call@"+((AstMethod)method).getSourcePosition(site.getProgramCounter());
	  } else {
	    return "<unknown call>";
	  }
	}
	
	private String getWalaText(CGNode capaNode) {    	  
	  String className = capaNode.getMethod().getDeclaringClass().getName().toString().substring(1);
	  
	  String methodName = capaNode.getMethod().getName().toString();
	  if( methodName.equals("<init>")) {
	    methodName = className.substring(className.lastIndexOf('/')+1, className.length());
	  }

	  String params = "";
	  for( int i=0; i < capaNode.getMethod().getNumberOfParameters(); i++) {
	    params +=capaNode.getMethod().getParameterType(i).getName() + ";";
	  }
	  
	  return methodName + "(" + params + ")" + " - " + className;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
	  if (element instanceof Pair) {
	    return JavaPlugin
	      .getImageDescriptorRegistry()
	      .get(new JavaElementImageDescriptor(
		     JavaPluginImages.DESC_MENU_SHIFT_RIGHT,
		     0,
		     JavaElementImageProvider.SMALL_SIZE));

	  }
		
	  // get the image depending on the element
	  CGNode capaNode = (CGNode)element;
	  IJavaElement jdtElt =
	    capaNodeIdToJavaElement.get(capaNode.getGraphNodeId());
	  Image image = null;
	  if( jdtElt == null ) {
	    image = getCapaImage();
	  }
	  else if( jdtElt.exists() ){
	    image = javaEltProvider.getImage(jdtElt);
	  }
	  else {
	    image = getWarningImage();
	  }
	  return image;
	}
	
	private Image getCapaImage() {
	  return JavaPlugin
	    .getImageDescriptorRegistry()
	    .get(new JavaElementImageDescriptor(
		   JavaPluginImages.DESC_OBJS_CFILE,
		   0,
		   JavaElementImageProvider.BIG_SIZE));
	}

	private Image getWarningImage() {
	  return JavaPlugin.getImageDescriptorRegistry()
	    .get(new JavaElementImageDescriptor(
		   JavaPluginImages.DESC_OBJS_REFACTORING_WARNING,
		   0,
		   JavaElementImageProvider.BIG_SIZE));
	}
}
