package com.ibm.wala.cast.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import com.ibm.wala.cast.ir.translator.NativeTranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.cast.util.TemporaryFile;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

public abstract class CAstAbstractNativeLoader extends CAstAbstractLoader {
    
  public CAstAbstractNativeLoader(IClassHierarchy cha, IClassLoader parent) {
    super(cha, parent);
  }

  public CAstAbstractNativeLoader(IClassHierarchy cha) {
    this(cha, null);
  }

  protected abstract NativeTranslatorToCAst
    getTranslatorToCAst(CAst ast, URL sourceURL, String localFileName);

  protected abstract TranslatorToIR initTranslator();

  protected void finishTranslation() {

  }

  public void init(final Set modules) {
    final CAst ast = new CAstImpl();

    final Set topLevelEntities = new LinkedHashSet();

    final TranslatorToIR xlatorToIR = initTranslator();

    class TranslatorNestingHack {

      private void init(ModuleEntry moduleEntry) {
        if (moduleEntry.isModuleFile()) {
          init(moduleEntry.asModule());
        } else if (moduleEntry instanceof SourceFileModule) {
          File f = ((SourceFileModule) moduleEntry).getFile();
          String fn = f.toString();

          try {
            NativeTranslatorToCAst xlatorToCAst = 
	      getTranslatorToCAst(ast, new URL("file://" + f), fn);

            CAstEntity fileEntity = xlatorToCAst.translateToCAst();

	    if (fileEntity != null) {
	      Trace.println(CAstPrinter.print(fileEntity));

	      topLevelEntities.add(Pair.make(fileEntity, fn));
	    }
          } catch (MalformedURLException e) {
            Trace.println("unpected problems with " + f);
	    e.printStackTrace( Trace.getTraceStream() );
            Assertions.UNREACHABLE();
          } catch (RuntimeException e) {
            Trace.println("unpected problems with " + f);
	    e.printStackTrace( Trace.getTraceStream() );
	  }

        } else if (moduleEntry instanceof SourceURLModule) {
          java.net.URL url = ((SourceURLModule) moduleEntry).getURL();
          String fileName = ((SourceURLModule) moduleEntry).getName();
          String localFileName = fileName.replace('/', '_');

          try {
            File F = TemporaryFile.streamToFile(localFileName,
                ((SourceURLModule) moduleEntry).getInputStream());

            final NativeTranslatorToCAst xlatorToCAst =
		getTranslatorToCAst(ast, url, localFileName);

            CAstEntity fileEntity = xlatorToCAst.translateToCAst();

	    if (fileEntity != null) {
	      Trace.println(CAstPrinter.print(fileEntity));

	      topLevelEntities.add(Pair.make(fileEntity, fileName));
	    }

            F.delete();
          } catch (IOException e) {
            Trace.println("unexpected problems with " + fileName);
	    e.printStackTrace( Trace.getTraceStream() );
            Assertions.UNREACHABLE();
          } catch (RuntimeException e) {
            Trace.println("unexpected problems with " + fileName);
	    e.printStackTrace( Trace.getTraceStream() );
	  }
        }
      }

      private void init(Module module) {
        for (Iterator mes = module.getEntries(); mes.hasNext();) {
          init((ModuleEntry) mes.next());
        }
      }

      private void init() {
        for (Iterator mes = modules.iterator(); mes.hasNext();) {
          init((Module) mes.next());
        }

	for(Iterator tles = topLevelEntities.iterator(); tles.hasNext(); ) {
	  Pair p = (Pair)tles.next();
	  xlatorToIR.translate((CAstEntity)p.fst, (String)p.snd);
	}
      }
    }

    (new TranslatorNestingHack()).init();

    for (Iterator ts = types.keySet().iterator(); ts.hasNext();) {
      TypeName tn = (TypeName) ts.next();
      try {
        Trace.println("found type " + tn + " : " + types.get(tn) + " < "
            + ((IClass) types.get(tn)).getSuperclass());
      } catch (Exception e) {
        System.err.println(e);
      }
    }

    finishTranslation();
  }

}