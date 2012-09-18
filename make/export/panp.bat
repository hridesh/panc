@echo off

set PANC_HOME=%~dp0\..
set LIB_HOME=%PANC_HOME%\lib\dist\bootstrap

rem TODO parse out the args and find -J opts, stick in here
set JAVA_OPTS=
rem TODO put only the non -J opts in here
set TOOL_OPTS=%*

rem if [ "$LANGTOOLS_USE_BOOTCLASSPATH" != "no" ]; then
rem    cp=`unzip -c "$mylib/javac.jar" META-INF/MANIFEST.MF |
rem        grep "Class-Path:" |
rem        sed -e 's|Class-Path: *||' -e 's|\([a-z]*\.jar\) *|'"$mylib"'/\1:|g'`
rem    bcp="$mylib/javac.jar:$cp"
rem fi
rem TODO need to set the bcp from the manifest in the javac.jar
set BCP=%LIB_HOME%\lib\javac.jar
if NOT "%BCP%"=="" set BCP=-Xbootclasspath/p:%BCP%


rem "java" "${bcp:+-Xbootclasspath/p:"$bcp"}" ${ea} ${javaOpts} -jar "${mylib}/javac.jar" ${toolOpts}
java %JAVA_OPTS% -cp "%PANC_HOME%\lib\panini_rt.jar";. -javaagent:"%PANC_HOME%\lib\profile.jar" -Dprofile.properties="%PANC_HOME%\lib\profile.properties" %TOOL_OPTS%
