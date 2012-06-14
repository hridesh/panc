#!/bin/bash

cd `dirname $0`

mkdir export

mkdir jar
mkdir jar/org
mkdir jar/org/paninij
mkdir jar/org/paninij/runtime

cd jar/org/paninij/runtime
cp ../../../../../build/bootstrap/classes/org/paninij/runtime/*.class ./
cd ../../..	
jar cvf panini_rt.jar *

cp panini_rt.jar ../export/
cd ..

rm -rf jar
