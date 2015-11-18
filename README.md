# BatchCompiler
Compiler for 2114 java projects


Used to automate the process of compiling student submissions for TA evaluations

Usage is in src/compiler/Driver.java (include final usage in the README once finalized) 



I was able to run it with this command

<Change JDK java dir to whatever you need>   <this program's jar file>

C:\Program Files\Java\jdk1.8.0_60\bin\java.exe -jar batch.jar Project-4.zip RollerCoaster rollercoaster student.jar GraphWindow.jar junit.jar CarranoDataStructures.jar


In addition some things are required for now.
QueueReader.java must be in the directory that we invoke the command, as it needs to be compiled with every submission.

# Readme.txt contents:

CS 2114 Batch Compiler
Created by Matthew Bock and Eric Williamson

----------------
Package Contents
----------------
- lib: Fill this directory with necesary jar library files for compiling and running
- src: Fill this directory with extra java source files to be compiled with the submission
- batchCompiler: The executable jar file for the batch compiler
- config: The config file for the batch compiler
- README: This wonderful readme file!

-----------------------------
How to use the batch compiler
-----------------------------
Compile mode will extract and compile a zip file full of web-cat submissions.
The compiler expects the archive to have the following structure:

project.zip
|--|crn
|  |--project name
|  |  |--pid1
|  |  |  |--submission number
|  |  |  |  |--pid1.jar
|  |  |--pid2
|  |  |  |--submission number
|  |  |  |  |--pid2.jar
|  |  |--pid3
|  |  ...

This is the structure that web-cat's batch downloader will give you by default.
To invoke the compiler, use java to run "batchCompiler compile [zip file]".
On windows, the command is "java -jar batchCompiler compile [zip file]".

The compiler will extract and compile all of the valid files it is able to.
ny errors which occured during compilation will be reported at the end.
Occasionally exceptions will show up during compilation, they should be
reflected in the report at the end but are left there so you can investigate
in more detail.

If the program needs any files to run (ie: input files with data), put them
in the same directory as the batchCompiler jar.

----------------------------
How to run compiled programs
----------------------------
Run mode will copy the files from res into the specified PID's directory, and
then run the program using the main class and command line arguments specified
in the config file. To invoke the runner, use java to run
"batchCompiler run [Batch Folder] [PID]". On windows, this command is 
"java -jar batchCompiler run [Batch Folder] [PID]". Batch folder is the name of
the folder that the compiler created.

-----------
Config file
-----------
The config file allows you to specify several things, all of them are required
to be defined in the file, even if they don't do anything:

projectName
- The name of the project as in projectName/src/packageName in eclipse

packageName
- The name of the project as in projectName/src/packageName in eclipse

jdkPath
- In order for java to invoke its compiler, it needs access to the jdk. This is
generally in the same folder as your jre, for example C:\Program Files\Java\jdk1.8.0_65

mainClass
- The name of the class containing the main method

args
- Command line arguments to use when running the compiled programs

------------
Known issues
------------
- Some issues with copying extra source files, may or may not be our fault
- Some issues extracting jar files, also may or may not be our fault
