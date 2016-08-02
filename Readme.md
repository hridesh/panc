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


