#!/bin/bash

if (uname | grep -i "cygwin"); then
	# This should be the default for most of cases;
	# adjust to your environment if necessary.
	MSVC="C:\Program Files\Microsoft Visual Studio 8\VC"
	ARCH=x86
	
	cmd.exe /c "call \"$MSVC\\vcvarsall.bat\" $ARCH && make"
else
	make
fi
