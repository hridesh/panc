#!/bin/bash

if test -z "$JAVA_HOME";
then
	echo "Error : JAVA_HOME not set"
else
	cp $JAVA_HOME/jre/lib/rt.jar ../lib/rt.jar
	cd ../lib
	java -cp .:./org/paninij/OwnershipAdapter/:./org/paninij/OwnershipAdapter/asm-4.0.jar  org.paninij.OwnershipAdapter.OwnershipAdapter ./rt.jar 
	rm ./rt.jar
fi
