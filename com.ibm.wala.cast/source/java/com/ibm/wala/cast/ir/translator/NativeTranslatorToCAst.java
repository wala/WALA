package com.ibm.wala.cast.ir.translator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;

public abstract class NativeTranslatorToCAst 
  extends NativeBridge 
  implements TranslatorToCAst
{

  protected final URL sourceURL;

  protected final String sourceFileName;

  protected NativeTranslatorToCAst(CAst Ast, URL sourceURL, String sourceFileName) {
    super(Ast);
    this.sourceURL = sourceURL;
    this.sourceFileName = sourceFileName;
  }

  @SuppressWarnings("unused")
  private String getLocalFile() {
    return sourceFileName;
  }

  @SuppressWarnings("unused")
  protected String getFile() {
    return sourceURL.getFile();
  }

  @SuppressWarnings("unused")
  private Position makeLocation(final int fl, final int fc, final int ll, final int lc) {
    return new AbstractSourcePosition() {
      public int getFirstLine() {
        return fl;
      }

      public int getLastLine() {
        return ll;
      }

      public int getFirstCol() {
        return fc;
      }

      public int getLastCol() {
        return lc;
      }

      public int getFirstOffset() {
        return -1;
      }

      public int getLastOffset() {
        return -1;
      }
      
      public URL getURL() {
        return sourceURL;
      }

      public InputStream getInputStream() throws IOException {
        return new FileInputStream(sourceFileName);
      }

      public String toString() {
        String urlString = sourceURL.toString();
        if (urlString.lastIndexOf(File.separator) == -1)
          return "[" + fl + ":" + fc + "]->[" + ll + ":" + lc + "]";
        else
          return urlString.substring(urlString.lastIndexOf(File.separator) + 1) + "@[" + fl + ":" + fc + "]->[" + ll + ":" + lc
              + "]";
      }
    };
  }

  public abstract CAstEntity translateToCAst();

}
