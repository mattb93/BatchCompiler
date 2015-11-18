# BatchCompiler
Compiler for 2114 java projects


Used to automate the process of compiling student submissions for TA evaluations

Usage is in src/compiler/Driver.java (include final usage in the README once finalized) 



I was able to run it with this command

<Change JDK java dir to whatever you need>   <this program's jar file>

C:\Program Files\Java\jdk1.8.0_60\bin\java.exe -jar batch.jar Project-4.zip RollerCoaster rollercoaster student.jar GraphWindow.jar junit.jar CarranoDataStructures.jar


In addition some things are required for now.
QueueReader.java must be in the directory that we invoke the command, as it needs to be compiled with every submission.
