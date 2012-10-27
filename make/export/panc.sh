#!/bin/bash

mydir=`dirname $0`
case `uname -s` in
    CYGWIN*)
      mydir=`cygpath -m $mydir`
      ;;
esac
export PANC_HOME=$mydir/../

# pass classpath argument through to inner javac call
CP=.
NON_CP=""
while [ $# -gt 0 ]
do
    ARG="$1"
    case $ARG in
        -cp)
            CP=$CP:$2
            shift 2
            ;;
        *)
            NON_CP="$NON_CP $ARG"
            shift
            ;;
    esac
done

$PANC_HOME/lib/dist/bootstrap/bin/javac -source 1.6 -target 1.6 -cp $CP:$PANC_HOME/lib/panini_rt.jar -Xlint:-options $NON_CP
