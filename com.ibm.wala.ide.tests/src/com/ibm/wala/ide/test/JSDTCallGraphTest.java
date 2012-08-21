package com.ibm.wala.ide.test;

import java.io.IOException;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.jsdt.core.IMember;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ibm.wala.cast.js.test.JavaScriptTestPlugin;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ide.tests.util.EclipseTestUtil;
import com.ibm.wala.ide.util.JsdtUtil;
import com.ibm.wala.ide.util.JsdtUtil.CGInfo;

public abstract class JSDTCallGraphTest {

  private final String testProjectName;
  private final String testProjectZip;
    
  protected JSDTCallGraphTest(String testProjectName, String testProjectZip) {
    this.testProjectName = testProjectName;
    this.testProjectZip = testProjectZip;
  }

  @Before
  public void beforeClass() {
    EclipseTestUtil.importZippedProject(JavaScriptTestPlugin.getDefault(), testProjectName, testProjectZip, new NullProgressMonitor());
    System.err.println("finish importing project");
  }

  @After
  public void afterClass() {
    EclipseTestUtil.destroyProject(testProjectName);
  }

  @Test
  public void test() throws IOException, CoreException {
    Set<ModuleEntry> mes = JsdtUtil.getJavaScriptCodeFromProject(testProjectName);
    CGInfo info = JsdtUtil.buildJSDTCallGraph(mes);
    System.err.println(info.calls.size());
    System.err.println("call graph:\n" + info.cg);
    
    int edges = 0;
    for(IMember node : info.cg) {
      edges += info.cg.getSuccNodeCount(node);
    }
    System.err.println("call graph edges: " + edges);
    
    Assert.assertTrue("cannot find any function calls", info.calls.size()>0);
  }
  
  public static class JSDT_3dmodel_Test extends JSDTCallGraphTest {
    public JSDT_3dmodel_Test() {
      super("3dmodel", "3dmodel.zip");
    }
  }

  public static class JSDT_beslimed_Test extends JSDTCallGraphTest {
    public JSDT_beslimed_Test() {
      super("beslimed", "beslimed.zip");
    }
  }
  
  public static class JSDT_flotr_Test extends JSDTCallGraphTest {
    public JSDT_flotr_Test() {
      super("flotr", "flotr.zip");
    }
  }

  public static class JSDT_fullcalendar_1d5d3_Test extends JSDTCallGraphTest {
    public JSDT_fullcalendar_1d5d3_Test() {
      super("fullcalendar-1.5.3", "fullcalendar-1.5.3.zip");
    }
  }

  public static class JSDT_google_pacman_Test extends JSDTCallGraphTest {
    public JSDT_google_pacman_Test() {
      super("google_pacman", "google_pacman.zip");
    }
  }

  public static class JSDT_htmledit_Test extends JSDTCallGraphTest {
    public JSDT_htmledit_Test() {
      super("htmledit", "htmledit.zip");
    }
  }

  public static class JSDT_markitup_Test extends JSDTCallGraphTest {
    public JSDT_markitup_Test() {
      super("markitup", "markitup.zip");
    }
  }

  public static class JSDT_pdfjs_Test extends JSDTCallGraphTest {
    public JSDT_pdfjs_Test() {
      super("pdfjs", "pdfjs.zip");
    }
  }

  public static class JSDT_pong_Test extends JSDTCallGraphTest {
    public JSDT_pong_Test() {
      super("pong", "pong.zip");
    }
  }

  public static class JSDT_sunmap_Test extends JSDTCallGraphTest {
    public JSDT_sunmap_Test() {
      super("sunmap", "sunmap.zip");
    }
  }
}