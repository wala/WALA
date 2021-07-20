# Setting up WALACodeNetDev Environment

## Setting up GPU Support in Docker for WSL2 in Windows
This is mostly based on instructions from Nvidia's website (https://docs.nvidia.com/cuda/wsl-user-guide/index.html) under the assumption that this is a fresh setup (i.e. nothing related is currently installed). This is a setup that allows for Docker to utilize GPU resources that are available and is only applicable to computers with a fairly recent dedicated NVIDIA GPU. If your computer does not have a dedicated NVIDIA GPU, skip to the next section.

1. Install the latest build from the Microsoft Windows Insider Program. For GPU support, you would need to install the latest development version of windows from the Windows Insider Program. This is done through registration of from Microsoft's website from here https://insider.windows.com/en-us/getting-started#register. After that, you need to install the latest version from the dev program, instructions here https://blogs.windows.com/windows-insider/2020/06/15/introducing-windows-insider-channels/. Afterwards, you should be able to go into your windows updates from the start menu and there should be an option to download a development build.
2. Install NVIDIA's CUDA drivers for WSL depending on the type of GPU you have here (https://developer.nvidia.com/cuda/wsl). You may be prompted to sign up for the developer's program, which is free, before you are allowed to download the executable that installs the driver.
3. Install WSL2 by following the instructions here (https://docs.microsoft.com/en-us/windows/wsl/install-win10). Make sure that the latest kernel is installed by checking for updates in the windows updates section of the settings app in Windows 10. Next, download your preference of Linux on the Windows store.
4. Setup CUDA Toolkit by running the following command (Note that commands may differ depending on Linux distros. Here, I am using Ubuntu-18.04):
```
$ apt-key adv --fetch-keys http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1804/x86_64/7fa2af80.pub
$ sh -c 'echo "deb http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1804/x86_64 /" > /etc/apt/sources.list.d/cuda.list'
$ apt-get update
$ apt-get install -y cuda-toolkit-11-0
(Note that cuda-toolkit may be updated in the future and that this command may change depending on version)
```
5. Setting up to run containers by installing Docker. Since the current version of NVIDIA Container Toolkit as of writing does not support Docker Desktop, you need to install Docker through the command line with the following:
```
curl https://get.docker.com | sh
```
6. Install the NVIDIA Container Toolkit and runtime packages by running the following commands:
```
$ distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
$ curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | sudo apt-key add -
$ curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | sudo tee /etc/apt/sources.list.d/nvidia-docker.list
$ curl -s -L https://nvidia.github.io/libnvidia-container/experimental/$distribution/libnvidia-container-experimental.list | sudo tee /etc/apt/sources.list.d/libnvidia container-experimental.list
$ sudo apt-get update
$ sudo apt-get install -y nvidia-docker2
```
7.Complete the installation by stopping and starting the Docker daemon using the following commands:
```
$ sudo service docker stop
$ sudo service docker start
```
8. Test the functionality by running the following:
```
$ docker run --gpus all nvcr.io/nvidia/k8s/cuda-sample:nbody nbody -gpu -benchmark
```
Here is an example of sample output:
```
Run "nbody -benchmark [-numbodies=<numBodies>]" to measure performance.
        -fullscreen       (run n-body simulation in fullscreen mode)
        -fp64             (use double precision floating point values for simulation)
        -hostmem          (stores simulation data in host memory)
        -benchmark        (run benchmark to measure performance)
        -numbodies=<N>    (number of bodies (>= 1) to run in simulation)
        -device=<d>       (where d=0,1,2.... for the CUDA device to use)
        -numdevices=<i>   (where i=(number of CUDA devices > 0) to use for simulation)
        -compare          (compares simulation results running once on the default GPU and once on the CPU)
        -cpu              (run n-body simulation on the CPU)
        -tipsy=<file.bin> (load a tipsy model file for simulation)

NOTE: The CUDA Samples are not meant for performance measurements. Results may vary when GPU Boost is enabled.

> Windowed mode
> Simulation data stored in video memory
> Single precision floating point simulation
> 1 Devices used for simulation
GPU Device 0: "Turing" with compute capability 7.5

> Compute 7.5 CUDA device: [NVIDIA GeForce RTX 2080 with Max-Q Design]
47104 bodies, total time for 10 iterations: 138.109 ms
= 160.654 billion interactions per second
= 3213.086 single-precision GFLOP/s at 20 flops per interaction

```


## Setting Up The Environment Using Docker
In order to set up your coding environment, first make sure that [Docker](https://www.docker.com/products/docker-desktop) and [VNC Viewer](https://www.realvnc.com/en/connect/download/viewer/) are installed on your computer. 

Once those are properly set up, open up a command terminal of your choice (Git Bash, PowerShell, etc.) and start the WALACodeNetDev Docker image with this command: 
```sh
docker run -v <some dir with the test files>:/input -v <where to write output>:/output -p 5901:5901 -t julianwindows/wala:walacodenetdev
```
After doing this, connect to the image through VNC Viewer by connecting VNC to display 1, port 5901 of your machine (i.e. localhost:1).
This is what it should look like:
![dev_env1](https://github.com/VQTran123/WALA/blob/dev_env_setup/GettingStarted-Assets/dev-env1.png)

Once this is accomplished, open a terminal within the VNC Viewer environment. The terminal can be found through the Start menu in the bottom left corner and going to System Tools > LXTerminal. 
![LXTerminal](https://github.com/VQTran123/WALA/blob/dev_env_setup/GettingStarted-Assets/LXTerminal.png)
Once the terminal has been located, enter in the following command to run an analysis in order to make sure that everything works so far:
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
![eclipsebuild1](https://github.com/VQTran123/WALA/blob/dev_env_setup/GettingStarted-Assets/eclipsebuild1.png)
From here, click Browse from the root directory and open Home > AnalysisCodeGenerator and select Finish. Eclipse will build the Maven project for you, and you will be ready to code afterwards!

## Building Project in Intellij

First install Intellij by downloading the latest version in the virtual environment. Unpack and run it using this command:
```
tar -zxf ideaIU-2021.1.3.tar.gz
(Your version may differ)
cd idea-IC-2021.1.3/bin
./idea.sh
```

Go through the installation steps until you reach this screen:
![intellij1](https://github.com/VQTran123/WALA/blob/dev_env_setup/GettingStarted-Assets/intellij1.png)

Click open home > wala > AnalysisCodeGenerator then select Ok. 
Select Open as: Maven project and trust the project when prompted.
When opened, go to src > main > java [AnalysisGraphGenerator] > WalaToGNNFiles
![intellij2](https://github.com/VQTran123/WALA/blob/dev_env_setup/GettingStarted-Assets/intellij2.png)

Click the Setup SDK prompt at the top. Then, click the option underneath Detected SDKs
![intellij3](https://github.com/VQTran123/WALA/blob/dev_env_setup/GettingStarted-Assets/intellij3.png)

To ensure everything is working correctly, attempt to run the WalaToGNNFiles file. Intellij should prompt you with a NullPointerException.

## Setting Up The Environment on Linux
The dependencies for Linux are simpler than that of windows with no docker environment being required at all. The dependencies list are as follows
```
wget
curl
openjdk-11
git
g++
mercurial
make
maven
ant
sudo
```
Most modern Linux distributions will include most of these. If they are not default included, they are simple to install using whichever package manager fits your distribution.

From here, WALA can be built from commandline. To begin with, the maven dependencies for the project must be installed. The easiest way to do this from any install (fresh or not) is to run 
```
mvn clean install
```

After that completes, you can run a basic gradle command from the WALA root directory to build and lightly test the build
```
./gradlew build publishToMavenLocal -x test -x javadoc
```

To build WALA for use in other codebases such as Project CodeNet, the build must be shared to maven:
```
 ./gradlew clean build publishToMavenLocal
```

One of the importance considerations to have is the branch of WALA you have checked out and make sure that your operating system is running a compatible version of java. Your java version can be checked by running
```
java -version
```

System specific issue notwithstanding, at this point WALA should be able to be fully built. 
