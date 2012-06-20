#!/bin/bash

cd `dirname $0`

rm -rf panc
rm -rf panc.zip

./runtime.sh

mkdir panc
mkdir panc/lib
mkdir panc/bin
mkdir panc/editors

cp -r ../dist panc/lib/dist
cp -r ../licenses panc/licenses
cp export/panc.sh panc/bin/panc
cp export/panc.bat panc/bin/
cp export/panini_rt.jar panc/lib/
cp export/javac panc/lib/dist/bootstrap/bin/
cp export/panini-mode.el panc/editors/
cp export/panini.vim panc/editors/
cp -r ../examples panc/
cp ../README panc/

find panc -name '.svn' -exec rm -Rf '{}' ';' 2>/dev/null
zip -r panc.zip panc
