#!/bin/bash -eu

if (uname | grep -i "cygwin"); then
	# This should be the default for most of cases;
	# adjust to your environment if necessary.
	MSVC="C:\Program Files\Microsoft Visual Studio 8\VC"
	ARCH=x86
	
	cmd.exe /c "call \"$MSVC\\vcvarsall.bat\" $ARCH && make"
else
	make
fi

# Local variables:
# eval: (smie-config-local '((8 :after "else" 2) (8 :elem basic 2)))
# End:
