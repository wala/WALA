#!/bin/bash
#
# Fetch source files or builds needed for wala.core regression tests
#
#

fetch_jlex() {
   echo "Fetching jlex ...";
   makeTempFolder;
   cd tmp;
   mkdir JLex;
   cd JLex;
   wget http://www.cs.princeton.edu/~appel/modern/java/JLex/current/Main.java; 
   javac Main.java;
   cd ..;
   jar cvf JLex.jar JLex; 
   mv JLex.jar ../../bin;
   cd ..;
   removeTempFolder;
}

fetch_javacup() {
   echo "Fetching java-cup...";
   makeTempFolder;
   cd tmp;
   wget http://www2.cs.tum.edu/projects/cup/java-cup-11a.jar;
   mv java-cup-11a.jar ../../bin;
   cd ..;
   removeTempFolder;
}

fetch_bcel() {
   echo "Fetching bcel ...";
   makeTempFolder;
   cd tmp;
   wget http://www.apache.org/dist/jakarta/bcel/binaries/bcel-5.2.tar.gz;
   gunzip -c bcel-5.2.tar.gz | tar xvf - ;
   mv bcel-5.2/bcel-5.2.jar ../../bin;
   cd ..;
   removeTempFolder;
}

makeTempFolder() {
   echo "Creating tmp/";
   mkdir tmp;
}

removeTempFolder() {
   echo "Removing tmp/";
   rm -rf tmp;
}

fetch_bcel;
fetch_jlex;
fetch_javacup;
