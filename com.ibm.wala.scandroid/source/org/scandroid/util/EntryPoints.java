/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/**
 *
 * Copyright (c) 2009-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, 
 *                Rogan Creswick <creswick@galois.com>, 
 *                Adam Foltzer <acfoltzer@galois.com>)
 *  Steve Suh    <suhsteve@gmail.com>
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.scandroid.spec.AndroidSpecs;
import org.scandroid.spec.MethodNamePattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.strings.StringStuff;


public class EntryPoints {
	
    private String pathToApkFile;
    private String pathToApkTool;
    private String pathToJava;
    private String tempFolder;
    private ArrayList<String[]> ActivityIntentList;
    private ArrayList<String[]> ReceiverIntentList;
    private ArrayList<String[]> ServiceIntentList;

    private LinkedList<Entrypoint> entries;

    public void listenerEntryPoints(ClassHierarchy cha) {
        ArrayList<MethodReference> entryPointMRs = new ArrayList<>();

        // onLocation
        entryPointMRs.add(StringStuff.makeMethodReference("android.location.LocationListener.onLocationChanged(Landroid/location/Location;)V"));
        for(MethodReference mr:entryPointMRs)
        for(IMethod im:cha.getPossibleTargets(mr))
        {
            

            // limit to functions defined within the application
            if(im.getReference().getDeclaringClass().getClassLoader().
                                equals(ClassLoaderReference.Application)) {
                
                entries.add(new DefaultEntrypoint(im, cha));
            }
        }
    }

    public static List<Entrypoint> defaultEntryPoints(ClassHierarchy cha) {
    	List<Entrypoint> entries = new ArrayList<>();
    	for (MethodNamePattern mnp:new AndroidSpecs().getEntrypointSpecs()) {
    		for (IMethod im: mnp.getPossibleTargets(cha)) {
    			
    			// limit to functions defined within the application
    			if(LoaderUtils.fromLoader(im, ClassLoaderReference.Application))
    			{
    				
    				entries.add(new DefaultEntrypoint(im, cha));
    			}
    		}
    	}
    	return entries;
    }
    
    public void activityModelEntry(ClassHierarchy cha) {
        String[] methodReferences = {
            "android.app.Activity.ActivityModel()V",
            // find all onActivityResult functions and add them as entry points
//            "android.app.Activity.onActivityResult(IILandroid/content/Intent;)V",
//
//            // SERVICE ENTRY POINTS
//            "android.app.Service.onCreate()V",
//            "android.app.Service.onStart(Landroid/content/Intent;I)V",
//            "android.app.Service.onBind(Landroid/content/Intent;)Landroid/os/IBinder;",
//            "android.app.Service.onTransact(ILandroid/os/Parcel;Landroid/os/Parcel;I)B"
         };

        for (String methodReference : methodReferences) {
            MethodReference mr =
                    StringStuff.makeMethodReference(methodReference);
            
            for (IMethod im : cha.getPossibleTargets(mr)) {
                

                // limit to functions defined within the application
                if (im.getReference().getDeclaringClass().getClassLoader()
                        .equals(ClassLoaderReference.Application)) {
                    
                    entries.add(new DefaultEntrypoint(im, cha));
                }
            }
        }
    }
    
	public void addTestEntry(ClassHierarchy cha) {
    	String[] methodReferences = {
//    			"Test.Apps.Outer$PrivateInnerClass.printNum()V",
    			//"Test.Apps.Outer$PublicInnerClass.printNum()V"
    			//"Test.Apps.Outer.<init>()V"
    			//"Test.Apps.Outer.getNum()I"
    			//"Test.Apps.FixpointSolver.someMethod(LTest/Apps/GenericSink;LTest/Apps/GenericSource;)V"
    			//"Test.Apps.Outer$PrivateInnerClass.testParameters(LTest/Apps/GenericSink;LTest/Apps/GenericSource;)V"
    			"android.view.View.setOnClickListener(Landroid/view/View$OnClickListener;)V",
    	};

    	for (String methodReference : methodReferences) {
    		MethodReference mr =
    				StringStuff.makeMethodReference(methodReference);

    		for (IMethod im : cha.getPossibleTargets(mr)) {
    			
    			entries.add(new DefaultEntrypoint(im, cha));
    		}
    	}
    }
    

    public void unpackApk(String classpath){
        StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);
        pathToApkFile = st.nextToken();
        //String pathToApkTool = new String(System.getProperty("user.dir").replace(" ", "\\ ") + File.separator + "apktool" +File.separator);
        pathToApkTool = System.getProperty("user.dir") + File.separator + "apktool" +File.separator;
        //String pathToJava = new String(System.getProperty("java.home").replace(" ", "\\ ") + File.separator + "bin" + File.separator);
        pathToJava = System.getProperty("java.home") + File.separator + "bin" + File.separator;
        String s = null;

        //String command = new String(pathToJava + "java -jar " + pathToApkTool + "apktool.jar d -f " + pathToApkFile + " " + pathToApkTool + tempFolder);

        //System.out.println("command: " + command);

        ProcessBuilder pb = new ProcessBuilder(pathToJava + "java", "-jar", pathToApkTool + "apktool.jar", "d", "-f", pathToApkFile, pathToApkTool+tempFolder);


        try {
            //Process p = Runtime.getRuntime().exec(command);
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

               BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            
            while ((s = stdInput.readLine()) != null) {
                
            }

            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                System.err.println(s);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //System.out.println( System.getProperty("user.dir") );
        //System.out.println("classpath: " + st.nextToken());
    }

    public void readXMLFile() {
        try {

            File fXmlFile = new File(pathToApkTool + tempFolder + File.separator + "AndroidManifest.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            //System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

            String basePackage = doc.getDocumentElement().getAttribute("package");
            NodeList iList = doc.getElementsByTagName("intent-filter");
            System.out.println("-----------------------");


            for (int i = 0; i < iList.getLength(); i++) {
                Node nNode = iList.item(i);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                      Element eElement = (Element) nNode;
//                    System.out.println(eElement.getNodeName());
                      populateIntentList(basePackage, eElement);
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
	private static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();

        Node nValue = nlList.item(0);

        return nValue.getNodeValue();
    }

    private void populateIntentList(String basePackage, Element eElement) {
        ArrayList<String[]> IntentList;
        NodeList actionList = eElement.getElementsByTagName("action");
        Node parent = eElement.getParentNode();
        IntentList = chooseIntentList(parent.getNodeName());

        String IntentClass = parent.getAttributes().getNamedItem("android:name").getTextContent();

        for (int i = 0; i < actionList.getLength(); i++)
        {
            Node nNode = actionList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                IntentList.add(new String[2]);
                IntentList.get(IntentList.size()-1)[0] = actionList.item(i).getAttributes().getNamedItem("android:name").getTextContent();

                if (IntentClass.startsWith(basePackage))
                    IntentList.get(IntentList.size()-1)[1] = IntentClass;
                else {
                    if (IntentClass.startsWith("."))
                        IntentList.get(IntentList.size()-1)[1] = basePackage + IntentClass;
                    else {
                        IntentList.get(IntentList.size()-1)[1] = basePackage + "." + IntentClass;
                        IntentList.add(new String[2]);
                        IntentList.get(IntentList.size()-1)[0] = actionList.item(i).getAttributes().getNamedItem("android:name").getTextContent();
                        IntentList.get(IntentList.size()-1)[1] = IntentClass;
                    }

                    //IntentList.get(IntentList.size()-1)[1] = basePackage + (IntentClass.startsWith(".") ? IntentClass : "." + IntentClass);
                }

                //System.out.println(IntentList.get(IntentList.size()-1)[0] + " ~> " + IntentList.get(IntentList.size()-1)[1]);
            }
        }
    }

    @SuppressWarnings("unused")
	private void populateEntryPoints(ClassHierarchy cha) {
        String method = null;
        IMethod im = null;
        for (String[] intent: ActivityIntentList) {
            //method = IntentToMethod(intent[0]);
            method = "onCreate(Landroid/os/Bundle;)V";

            im = cha.resolveMethod(StringStuff.makeMethodReference(intent[1]+"."+method));
            if (im!=null)
                entries.add(new DefaultEntrypoint(im,cha));

        }
        for (String[] intent: ReceiverIntentList) {
            //Seems that every broadcast receiver can be an entrypoints?
//          method = IntentToMethod(intent[0]);
            method = "onReceive(Landroid/content/Context;Landroid/content/Intent;)V";

            im = cha.resolveMethod(StringStuff.makeMethodReference(intent[1]+"."+method));
            if (im!=null)
                entries.add(new DefaultEntrypoint(im,cha));
        }
        //IMethod im = cha.resolveMethod(StringStuff.makeMethodReference("android.app.Activity.onCreate(Landroid/os/Bundle;)V"));
        //entries.add(new DefaultEntrypoint(im, cha));
    }

    @SuppressWarnings("unused")
	private static String IntentToMethod(String intent) {
        if (intent.contentEquals("android.intent.action.MAIN") ||
                intent.contentEquals("android.media.action.IMAGE_CAPTURE") ||
                intent.contentEquals("android.media.action.VIDEO_CAPTURE") ||
                intent.contentEquals("android.media.action.STILL_IMAGE_CAMERA") ||
                intent.contentEquals("android.intent.action.MUSIC_PLAYER") ||
                intent.contentEquals("android.media.action.VIDEO_CAMERA"))
            return "onCreate(Landroid/os/Bundle;)V";

//      else if (intent.contentEquals("android.intent.action.BOOT_COMPLETED") ||
//              intent.contentEquals("android.appwidget.action.APPWIDGET_UPDATE") ||
//              intent.contentEquals("android.provider.Telephony.SECRET_CODE") )
//          return "onReceive(Landroid/content/Context;Landroid/content/Intent;)V";


        else return null;
    }

    private ArrayList<String[]> chooseIntentList(String name) {
        if (name.equals("activity"))
            return ActivityIntentList;
        else if (name.equals("receiver"))
            return ReceiverIntentList;
        else if (name.equals("service"))
            return ServiceIntentList;
        else {
            return ActivityIntentList;
//          throw new UnimplementedError("EntryPoints intent category not yet covered: " + name);
        }
    }

     public LinkedList<Entrypoint> getEntries() {
        return entries;
    }

}
