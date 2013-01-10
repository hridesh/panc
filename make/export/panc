#!/bin/bash

mydir=`dirname $0`
case `uname -s` in
    CYGWIN*)
      mydir=`cygpath -m $mydir`
      ;;
esac
export PANC_HOME=$mydir/../

$PANC_HOME/lib/dist/bootstrap/bin/javac -source 1.6 -target 1.6 -cp $PANC_HOME/lib/panini_rt.jar:. -Xlint:-options ${@}
