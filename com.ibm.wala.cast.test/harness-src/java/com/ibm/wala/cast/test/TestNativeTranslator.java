package com.ibm.wala.cast.test;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import com.ibm.wala.cast.ir.translator.NativeTranslatorToCAst;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.CopyKey;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.RewriteContext;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.util.io.TemporaryFile;

public class TestNativeTranslator {
  
  static {
    System.loadLibrary("xlator_test");
  }

  private static native CAstNode inventAst(SmokeXlator ast);

  private static class SmokeXlator extends NativeTranslatorToCAst {

    private SmokeXlator(CAst Ast, URL sourceURL) throws IOException {
      super(Ast, sourceURL, TemporaryFile.urlToFile("temp", sourceURL).getAbsolutePath());
    }

    @Override
    public <C extends RewriteContext<K>, K extends CopyKey<K>> void addRewriter(CAstRewriterFactory<C, K> factory,
        boolean prepend) {
      assert false;
    }

    @Override
    public CAstEntity translateToCAst() {
      return new CAstEntity() {

        @Override
        public int getKind() {
          return CAstEntity.FUNCTION_ENTITY;
        }

        @Override
        public String getName() {
          return sourceURL.getFile();
        }

        @Override
        public String getSignature() {
          return "()";
        }

        @Override
        public String[] getArgumentNames() {
           return new String[0];
        }

        @Override
        public CAstNode[] getArgumentDefaults() {
          return new CAstNode[0];
        }

        @Override
        public int getArgumentCount() {
          return 0;
        }

        @Override
        public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
          return Collections.emptyMap();
        }

        @Override
        public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
          return Collections.emptyIterator();
        }

        private CAstNode ast;
        
        @Override
        public CAstNode getAST() {
          if (ast == null) {
            ast = inventAst(SmokeXlator.this);
          }
          return ast;
        }

        @Override
        public CAstControlFlowMap getControlFlow() {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public CAstSourcePositionMap getSourceMap() {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Position getPosition() {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public CAstNodeTypeMap getNodeTypeMap() {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Collection<CAstQualifier> getQualifiers() {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public CAstType getType() {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Collection<CAstAnnotation> getAnnotations() {
          // TODO Auto-generated method stub
          return null;
        }
      };
    }
  }
    
  @Test
  public void testNativeCAst() throws IOException {
    CAst Ast = new CAstImpl();
    
    URL junk = TestNativeTranslator.class.getClassLoader().getResource("smoke_main");
     
    SmokeXlator xlator = new SmokeXlator(Ast, junk);
    
    CAstNode ast = xlator.translateToCAst().getAST();    
  
    System.err.println(ast);
    
    assert ast.getChildCount() == 3;
  }
}
