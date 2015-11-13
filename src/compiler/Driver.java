package compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

//import javax.tools.JavaCompiler;
//import javax.tools.ToolProvider;

import java.util.Enumeration;
//import java.util.Scanner;

// ewmson

public class Driver {
	
	private static UnzipUtility unzipUtility;
	private static String root;
	
	private static String projectName;
	private static String packageName;
	private static ArrayList<File> libraries;
	
	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("How to invoke:");
			System.out.println("BatchCompiler [zipFile] [projectName] [packageName] [libraryFiles]...");
			System.out.println("projectName and packageName are from eclipse"
					+ " file structure: projectName/src/packageName");
			System.out.println("libraryFiles are any number of jar files the project needs to compile");
			System.out.println("For more information use command BatchCompiler help");
		}
		else if(args[0].equals("help")){
			System.out.println("CS 2114 Batch Compiler\n");
			
			System.out.println("How to invoke:");
			System.out.println("BatchCompiler [zipFile] [projectName] [packageName] [libraryFiles]...");
			
			System.out.println("Extracts a zip file full of jars formatted "
					+ "the way Web-Cat will give them to you.\n");
			
			
			System.out.println("File format structure:");
			System.out.println("project.zip");
			System.out.println("|--crn");
			System.out.println("|  |--projectName");
			System.out.println("|  |  |--pid1");
			System.out.println("|  |  |  |--submissionNumber");
			System.out.println("|  |  |  |  |--pid.jar");
			System.out.println("|  |  |--pid2");
			System.out.println("|  |  |--pid3");
			System.out.println("|  |  ...\n");
		}
		else {
			if(args[1] == null) {
				System.out.println("Missing project name argument, see help");
				System.exit(1);
			}
			if(args[2] == null) {
				System.out.println("Missing package name argument, see help");
				System.exit(1);
			}
			
			setUpEnvironment(args);
			System.out.println("==============================");
			extractFiles(args[0]);
			System.out.println("==============================");
			processFiles();
		}
	}
	
	private static void setUpEnvironment(String[] args) {
		System.out.println("CS 2114 batch compiler\n");
		
		System.out.println("Compiling archive: " + args[0]);
		
		projectName = args[1];
		System.out.println("Using project name: " + args[1]);
		
		packageName = args[2];
		System.out.println("Using package name: " + args[2]);
		
		String inputLibraries = "Using Libraries: ";
		for(int i = 3; i < args.length; i++) {
			inputLibraries += args[i];
			
			libraries.add(new File(args[i]));
			
			if(i < args.length - 1) {
				inputLibraries += ", ";
			}
		}
		System.out.println(inputLibraries);
	}
	
	private static void extractFiles(String zipName) {
		System.out.println("Attempting to extract " + zipName);
		
		unzipUtility = new UnzipUtility();
		long currentTime = new Date().getTime();
		String dest = "batch_" + currentTime;
		root = dest;
		
		try {
			unzipUtility.unzip(zipName, dest);
		} catch (IOException e) {
			System.out.println("Error opening zip, dumping stack trace:");
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println("Succesfully extracted " + zipName + " into " + dest);
	}
	
	private static void processFiles() {
		File rootFolder = new File(root);
		System.out.println("Navigating to: " + rootFolder.getPath());
		
		File crnFolder = new File(rootFolder.listFiles()[0].getPath());
		System.out.println("Navigating to: " + crnFolder.getPath());
		
		File projectFolder = new File(crnFolder.listFiles()[0].getPath());
		System.out.println("Navigating to: " + projectFolder.getPath());
		
		File[] listOfFiles = projectFolder.listFiles();
		
		new File(root + File.separator + "batchCompiled").mkdir();
		for(int i = 0; i < listOfFiles.length; i++) {
		//for(int i = 0; i < 1; i++) {
			processProject(listOfFiles[i]);
		}
	}
	
	private static void processProject(File individualRoot) {
		String pid =  individualRoot.getName();
		System.out.println("===== Processing: " + pid);
		
		File submission = new File(individualRoot.listFiles()[0].getPath());
		
		// FINALLY THE STUDENT'S JAR FILE
		File studentJar = new File(submission.listFiles()[0].toString());
		
		JarFile jar = null;
		try {
			jar = new JarFile(studentJar);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Attempting to extract " + pid);
		String extractedSubmissionPath = processJarFile(jar, pid);
		
		if(extractedSubmissionPath == null) {
			System.out.println("Error extracting " + pid + ", you should investigate this submission manually.");
		}
		else {
			System.out.println("Extracted " + pid);
			
			// Folder containing all of the students' java files
			File extractedSubmission = new File(extractedSubmissionPath + File.separator + projectName + 
					File.separator + "src" + File.separator + packageName);
			
			System.out.println("Attempting to compile " + pid);
			compile(extractedSubmission);
		}
	}
	
	private static String processJarFile(JarFile jar, String pid) {
		Enumeration<JarEntry> enumEntries = jar.entries();
		
		String destDir = root + File.separator + "batchCompiled" + File.separator + pid;
		
		new File(destDir).mkdir();
		
		while(enumEntries.hasMoreElements()) {
			JarEntry file = (JarEntry) enumEntries.nextElement();
			File f = new File(destDir + java.io.File.separator + file.getName());
		    if (file.isDirectory()) { // if its a directory, create it
		        f.mkdir();
		        continue;
		    }
		    
		    InputStream is;
			try {
				is = jar.getInputStream(file);
			    FileOutputStream fos = new java.io.FileOutputStream(f);
			    while (is.available() > 0) {  // write contents of 'is' to 'fos'
			        fos.write(is.read());
			    }
			    fos.close();
			    is.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		return destDir;
	}
	
	private static void compile(File extractedSubmission) {
		System.out.println("lol this shit doesn't work yet");
		//JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		//String compilerArgs = "";
				
		//compiler.run(null, null, null, compilerArgs);
	}
}
