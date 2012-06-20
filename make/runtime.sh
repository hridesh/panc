#!/bin/bash

cd `dirname $0`

mkdir export

mkdir jar
mkdir jar/org
mkdir jar/org/paninij

cd jar/org/paninij
cp -r ../../../../build/bootstrap/classes/org/paninij/runtime .
cd ../..	
jar cvf panini_rt.jar *

cp panini_rt.jar ../export/
cd ..

rm -rf jar
