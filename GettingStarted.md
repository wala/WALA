# Setting up WALACodeNetDev Environment

Summary of WALACodeNetDev

## Setting Up The Environment on Windows
In order to set up your coding environment, first make sure that [Docker Desktop](https://www.docker.com/products/docker-desktop) and [VNC Viewer](https://www.realvnc.com/en/connect/download/viewer/) are installed on your computer. 

Once those are properly set up, open up a command terminal of your choice (Git Bash, PowerShell, etc.) and start the WALACodeNetDev Docker image with this command: 
```sh
docker run -v <some dir with the test files>:/input -v <where to write output>:/output -p 5901:5901 -t julianwindows/wala:walacodenetdev
```
After doing this, connect to the image through VNC Viewer by connecting VNC to display 1, port 5901 of your machine (i.e. localhost:1). 

Once this is accomplished, open a terminal within the VNC Viewer environment. The terminal can be found through the Start menu in the bottom left corner and going to System Tools > LXTerminal. Once the terminal has been located, enter in the following command to run an analysis in order to make sure that everything works so far:
```sh
java -cp /home/wala/AnalysisCodeGenerator/target/AnalysisGraphGenerator-0.0.1-SNAPSHOT.jar -DoutputDir=/output -DSDG=true com.ibm.wala.codeNet.WalaToGNNFiles /input/<one of the test files>
```
Some of the test files that can be run are: counter.py, counter.java, and counter.js.

After running the analysis, download an IDE of your choice (Eclipse, IntelliJ, etc.) onto the VNC Viewer machine by going to the Web Browser icon on the taskbar and searching for your IDE. 

Important note: Everytime before logging out of your environment, make sure to ALWAYS type in the two following commands in the LXTerminal:
```sh
rm /tmp/.X1-lock
rm /tmp/.X11-unix/X1
```
After doing this, open up a new command terminal on your computer (not the developer environment) and perform a docker commit:
```sh
docker commit <container id>
```
You can find your container id by inputting the following command:
```sh
docker ps
```
After following these steps is it then safe to log out of the WALA environment.
## Building Project in Eclipse

If you have decided to install Eclipse onto the environment, launch Eclipse and from the top right corner, go to File > Import > Maven > Existing Maven Projects and click Next. 

From here, click Browse from the root directory and open Home > AnalysisCodeGenerator and select Finish. Eclipse will build the Maven project for you, and you will be ready to code afterwards!


