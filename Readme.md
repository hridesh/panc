Getting Started with the Development of the Panini Compiler
===========================================================
By Hridesh Rajan, Rex Fernando, Eric Lin, Adriano Pais Rodrigues, Sean L. Mooney


### Purpose
The main purpose of this document is to help a new developer 
quickly get up-to-speed on the architecture, design, process, 
and conventions of the Panini compiler and to decrease their 
learning curve. The document is aimed at beginning developers.

### Preparatory Steps

Before you can start reading about and contributing to the Panini compiler, 
you will need access to its source code. 
This code is maintained in a git repository at GitHub. If you are reading this
on GitHub, you probably know that already. 
In order to commit to the repository:

1. Create a GitHub account and send Hridesh Rajan your username with a 
request to add you to the GitHub project.

2. Next, clone the repository.

```
$> git clone git@github.com:hridesh/panc.git
```

This folder contains the all the code for the Panini compiler.  The master branch will be checked-out by default. Other branches may exist, depending on the current development activities.

Use 
```
$> git branch -a
to list all of the branches in the repository.
```

### Building with Ant

[Apache Ant](http://ant.apache.org) is a command-line tool widely-used 
for large projects, using scripts it can compile, assemble, test and 
run a project automatically. 
Panini comes with a script for ant on make folder. 
If you don’t have Ant installed, check (http://ant.apache.org/).

Before running, it’s necessary to set the project’s ant file up 
so it knows where your jdk installation is. 
For that, go to where you downloaded the Panini project source 
and find, under the make folder, the file build.properties.template. 
Rename it to build.properties and open it up in some text editor, 
such as wordpad (windows) and gedit (linux).

Find the line that contains “boot.java.home = /usr/lib/jvm/java-7-openjdk-i386”(Line 33) and change the part after the equals sign to your jdk7 home. It should be a folder under /usr/lib/jvm/ on linux or %JAVA_HOME% on windows. (Using “%JAVA_HOME%” there won’t work, though)

Once you have it, using the terminal, navigate to make folder and use “ant” command to build Panini.

### Conventions for Changes and Additions

We follow two conventions for changing existing code of the Panini compiler and for adding new code to it.

1. We make minimum changes to existing code to make periodic merge with main javac compiler (made available by Oracle) easier. For example, we do not change indentation of existing code (because that is an unnecessary change). Another example, we do not change names of existing classes, fields, methods unless that change is essential to implementing the new functionality.

2. All changes in existing java files begin with a single-line comment “// Panini code” without the quotes, one space after “//” and end with “// end Panini code”, also without the quotes and one space after the “//”.

IMPORTANT: Note a single space between '//' and 'Panini' and '//' and 'end'. It is essential to follow this convention to be able to find code changes.

New files added purely for the purpose of implementing Panini's functionality do not have any such restriction, but they MUST include Panini’s license at the top. See the following file for an example.

https://github.com/hridesh/panc/blob/master/src/share/classes/org/paninij/runtime/PaniniCapsule.java

### Regression Tests

Regression tests are located in the directory `test/tools/panc`. The regression tests suite uses the standard jtreg test harness. When a new feature is added, or a bug is fixed, a regression test or collection of regression tests should be added. 

## Writing a jtreg test

A jtreg test  is usually a Java programs with a main method. The test harness runs the test by compiling each test and executing it.  Any class which represents a test for jtreg to execute needs to be marked with an @test annotation. An @summary annotation should also be added. The summary is included in the output and makes the reports easier to read. If the test is addressing a specific bug the @bug annotation can be added for traceability.  All of these annotations go in a comment block before the class declaration and are not annotation on the class itself.

```
Example (from the jtreg FAQ):
/* @test 1.1 97/10/12
   @bug 1234567
   @summary Make sure that 1 != 0
*/


public class OneZero {
    public static void main(String[] args) throws Exception {
        if (1 == 0) {
            throw new Exception("1 == 0");
        }
   }
}
```
	
More information about writing tests can be found on the openJDK website.

## Panini compilation tests

Any panini program which acts as a compiler regression test can be created by adding the ‘@test’ and ‘@compile’ annotations to the top source code. The @compile annotation takes the name of the files, relative to the file annotation is in, to compile as arguments. This behavior makes creating a self contained, ‘this should compile’ test very easy to write. Just write the test and add the annotations. For example:

```
File RegTest.java
/*@test
 *@compile RegTest.java
 */
system RegTest(){ … }
```

## Negative tests

Tests for error messages from the compiler are written in a similar style. A ‘golden file’ needs to be created with the expected output. The convention for the expected output file name is to use the same name as the test file, but replace the ‘.java’ extension with ‘.out’. For example if a test file, TErr1.java is expected to fail, a file TErr1.out should contain the expected error message.

The @compile tag needs to be slightly modified. Add /fail/ref=TName.out to the compile annotation and the ‘-XDrawDiagnostics’ argument. The ‘-XDrawDiagonstics’ produces a simpler output that easier to check. The output when the flag is enabled is effectively the same arguments used when ‘Log.error(key, args)’ is called in the source code.

For example:

```
/*@test
 *@summary “Example expected failure”
 *@compile/fail/ref=TFail.out -XDrawDiagnostics TFail.java
 */
system S() {...}
```

## Running jtreg

1. Install jtreg
	- Download from (http://download.java.net/openjdk/jtreg/) or
	- Linux users may be able to install jtreg with their package manager(s). For example:  apt-get install jtreg or
	- [Build from source](http://openjdk.java.net/jtreg/build.html)
2. Ant Target
	- The jtreg-panc target in make/build.xml can also be used to run the jtreg tests with ant. 
	- Modify build.properties
		- Set jtreg.home to the location of the unzipped jtreg distributribution from step 1a.
		- Set target.java.home to the path to your JDK install (e.g. /usr/lib/jvm/java-1.7.0-openjdk-amd64)
		- Add the property `panc.tests=tools/panc/`. This property can be copied in from make/build.properties.template.
	- Run the tests with:	`ant -f make/build.xml jtreg-panc`

3. Running tests from command line
	- Create a copy of panc.jar to test. This can be accomplished by executing the ant ‘build-panc’ target.
	- From the command line, navigate to tests.
	- Run the tests with 
	```
	jtreg -jdk:/path/to/installed/jdk7 -Xbootclasspath/p:path/to/dist/lib/panc.jar tools/panc/ 
	```
	In the argument list for jtreg, the -jdk path may be omitted if your system is setup correctly with Java 7. Note, the /p part of  -Xbootclasspth flag specifies to prefix the path on the boot class path.

