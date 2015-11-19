package compiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public class Driver
{

	private static final String CONFIG_FILE_NAME = "config.txt";
	private static final String NEWLINE = System.getProperty("line.separator");
	
    private static UnzipUtility unzipUtility;
    private static String root;

    private static String projectName;
    private static String packageName;
    private static String jdkPath;
    private static String mainClass;
    private static String[] commandLineArgs;
    
    private static ArrayList<File> libraries;
    private static ArrayList<File> extraSourceFiles;
    
    private static String compilationErrorReport = "";

    public static void main(String[] args) throws IOException
    {
    	System.out.println("CS 2114 Batch Compiler" + NEWLINE);
    	
        if (args.length == 0)
        {
            System.out.println("How to invoke:");
            System.out.println("BatchCompiler help");
            System.out.println("BatchCompiler compile [zipfile]");
            System.out.println("BatchCompiler run [pid]");
            System.out.println("For more information see the readme file");
        }
        else if(args[0].equals("compile"))
        {
        	System.out.println("PREPARING TO COMPILE " + args[1] + NEWLINE);
        	
        	System.out.println("Reading properties from config file...");
        	readConfigFile();
        	
        	System.out.println(NEWLINE + "Grabbing files...");
        	
        	libraries = readFilesFromDir("lib");
        	ClassPathHacker.addFiles(libraries);
        	System.out.println("Found libraries: " + libraries.toString());
        	
        	extraSourceFiles = readFilesFromDir("src");
        	System.out.println("Found extra source files: " + extraSourceFiles.toString());
        	
        	System.out.println(NEWLINE + "Press enter to compile using these settings");
        	System.in.read();
        	
        	System.out.println("EXTRACTING FILES FROM " + args[1]);
            extractFiles(args[1]);
            System.out.println("==============================");
            try
            {
                processFiles();
            }
            catch (IOException e)
            {

                e.printStackTrace();
                System.out.println("Could not access a class path file");
            }
            
            System.out.println("Batch compilation complete!" + NEWLINE);
            System.out.println("The following errors were reported during compilation."
            		+ " You should investigate the sources of these errors manually."
            		+ " The error report has been saved in the batch folder" + NEWLINE);
            System.out.println(compilationErrorReport);
            System.out.println("All other submissions successfully compiled, "
            		+ "use the run command to inspect individual submissions");
            
            FileWriter fileWriter = new FileWriter(root + File.separator + "errorReport.txt");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(compilationErrorReport);
            bufferedWriter.close();
        }
        else if(args[0].equals("run"))
        {
        	System.out.println("Reading properties from config file...");
        	readConfigFile();
        	
        	System.out.println(NEWLINE + "Grabbing files...");
        	
        	libraries = readFilesFromDir("lib");
        	ClassPathHacker.addFiles(libraries);
        	System.out.println("Found libraries: " + libraries.toString());
        	
        	String srcPath = args[1] + File.separator + 
        			"batchCompiled" + File.separator + 
        			args[2] + File.separator +
        			projectName + File.separator +
        			"src";
        	
        	runProgram(new File(srcPath));
        }
        else {
        	System.out.println("Please specify compile or run");
        }
    }
    
    private static void readConfigFile() {
    	
    	// Open config file and create a scanner for it
    	File configFile = new File(CONFIG_FILE_NAME);
    	Scanner scanner = null;
		try {
			scanner = new Scanner(configFile);
		} catch (FileNotFoundException e) {
			System.out.println("Could not open config file: " + CONFIG_FILE_NAME);
			System.exit(1);
		}
    	
		// Read properties out of config file
		String currentLine;
		String[] lineComponents;
    	while(scanner.hasNext()) {
    		currentLine = scanner.nextLine();
    		if(currentLine.length() != 0 && currentLine.charAt(0) != '#') {
    			lineComponents = currentLine.split(" = ");
    			
    			switch(lineComponents[0]) {
    			case "projectName":
    				projectName = lineComponents[1];
    				break;
    			case "packageName":
    				packageName = lineComponents[1];
    				break;
    			case "jdkPath":
    				jdkPath = lineComponents[1];
    				break;
    			case "mainClass":
    				mainClass = lineComponents[1];
    				break;
    			case "args":
    				if(lineComponents.length > 1) {
    					commandLineArgs = lineComponents[1].split(" ");
    				}
    				else {
    					commandLineArgs = new String[0];
    				}
    				break;
    			default:
    				System.out.println("Unrecognized config file property: " + lineComponents[0]);
    			}
    		}
    	}
    	scanner.close();
    	
    	// Confirm all necessary properties exist
    	if(projectName != null) {
    		System.out.println("Using project name: " + projectName);
    	} 
    	else {
    		System.out.println("Could not find projectName value in config file");
    		System.exit(1);
    	}
    	
    	if(packageName != null) {
    		System.out.println("Using package name: " + packageName);
    	} 
    	else {
    		System.out.println("Could not find packageName value in config file");
    		System.exit(1);
    	}
    	
    	if(jdkPath != null) {
    		System.out.println("Using jdk path: " + jdkPath);
    	} 
    	else {
    		System.out.println("Could not find jdkPath value in config file");
    		System.exit(1);
    	}
    	
    	if(mainClass != null) {
    		System.out.println("Using main class name: " + mainClass);
    	} 
    	else {
    		System.out.println("Could not find mainClass value in config file");
    		System.exit(1);
    	}
    	String argsReport = "Using command line arguments: ";
    	for(int i = 0; i < commandLineArgs.length; i++) {
    		if(i == 0) {
    			argsReport += commandLineArgs[i];
    		}
    		else {
    			argsReport += ", " + commandLineArgs[i];
    		}
    	}
    	System.out.println(argsReport);
    }
    
    private static ArrayList<File> readFilesFromDir(String dirName) {
    	ArrayList<File> dirFiles = new ArrayList<File>();
    	
    	File dir = new File(dirName);
    	
    	if(dir.exists() && dir.isDirectory()) {
    		File[] files = dir.listFiles();
    		for(int i = 0; i < files.length; i++) {
    			dirFiles.add(files[i]);
    		}
    	}
    	else {
    		System.out.println("Could not find directory " + dirName);
    		return null;
    	}
    	
    	return dirFiles;
    }

    private static void extractFiles(String zipName)
    {
        System.out.println("Attempting to extract " + zipName);

        unzipUtility = new UnzipUtility();
        long currentTime = new Date().getTime();
        String dest = "batch_" + currentTime;
        root = dest;

        try
        {
            unzipUtility.unzip(zipName, dest);
        }
        catch (IOException e)
        {
            System.out.println("Error opening zip, dumping stack trace:");
            e.printStackTrace();
            System.exit(1);
        }

        System.out
                .println("successfully extracted " + zipName + " into " + dest);
    }

    private static void processFiles() throws IOException
    {
        File rootFolder = new File(root);
        System.out.println("Navigating to: " + rootFolder.getPath());

        File crnFolder = new File(rootFolder.listFiles()[0].getPath());
        System.out.println("Navigating to: " + crnFolder.getPath());

        File projectFolder = new File(crnFolder.listFiles()[0].getPath());
        System.out.println("Navigating to: " + projectFolder.getPath());

        File[] listOfFiles = projectFolder.listFiles();

        new File(root + File.separator + "batchCompiled").mkdir();
        for (int i = 0; i < listOfFiles.length; i++)
        {
            // for(int i = 0; i < 1; i++) {
            processProject(listOfFiles[i]);
        }
    }

    private static void processProject(File individualRoot) throws IOException
    {
        String pid = individualRoot.getName();
        System.out.println("===== Processing: " + pid);

        File submission = new File(individualRoot.listFiles()[0].getPath());

        // FINALLY THE STUDENT'S JAR FILE
        File studentJar = new File(submission.listFiles()[0].toString());

        JarFile jar = null;
        try
        {
            jar = new JarFile(studentJar);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        System.out.println("Attempting to extract " + pid);
        String extractedSubmissionPath = processJarFile(jar, pid);

        if (extractedSubmissionPath == null)
        {
        	compilationErrorReport += "[" + pid + "] Error extracting jar file" + NEWLINE;
        }
        else
        {
            System.out.println("Extracted " + pid);

            // Folder containing all of the students' java files
            File extractedSubmission = new File(extractedSubmissionPath
                    + File.separator + projectName + File.separator + "src"
                    + File.separator + packageName);

            System.out.println("Attempting to compile " + pid);

            /*
             * This is where we manually copy in the QueueReader so we can
             * eventually run their submission
             */
            try
            {
            	for(File f : extraSourceFiles) {
            		Files.copy(f.toPath(),
                            new File(extractedSubmissionPath + File.separator
                                    + projectName + File.separator + "src"
                                    + File.separator + packageName + File.separator
                                    + f.getName()).toPath(),
                            StandardCopyOption.COPY_ATTRIBUTES);
            	}
            }
            catch (Exception e)
            {
                e.printStackTrace();
                compilationErrorReport += "[" + pid + "] Error copying extra source files" + NEWLINE;
            }
            
            compile(extractedSubmission, pid);
            
        }
    }

    private static String processJarFile(JarFile jar, String pid)
    {
        Enumeration<JarEntry> enumEntries = jar.entries();

        String destDir = root + File.separator + "batchCompiled"
                + File.separator + pid;

        new File(destDir).mkdir();

        while (enumEntries.hasMoreElements())
        {
            JarEntry file = (JarEntry) enumEntries.nextElement();
            File f = new File(
                    destDir + java.io.File.separator + file.getName());
            if (file.isDirectory())
            { // if its a directory, create it
                f.mkdir();
                continue;
            }

            InputStream is;
            try
            {
                is = jar.getInputStream(file);
                FileOutputStream fos = new java.io.FileOutputStream(f);
                while (is.available() > 0)
                { // write contents of 'is' to 'fos'
                    fos.write(is.read());
                }
                fos.close();
                is.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
        }

        return destDir;
    }

    private static boolean compile(File extractedSubmission, String pid) throws IOException
    {

        /*
         * Get all of the java files in the extracted submissions directory
         */
        List<File> javaFiles = Collections.emptyList();
        try
        {
            javaFiles = Arrays
                    .asList(extractedSubmission.listFiles((dir, name) ->
                    {
                        // llambda expression so we only get .java files
                        return name.contains(".java");
                    }));
        }
        catch (NullPointerException e)
        {
            System.out.println("Found no Files");
            return false;
        }

        /*
         * Initialize diagnostics collector to report compile errors
         */
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        
        /*
         * We need the jdk to get the compiler
         */
        System.setProperty("java.home", jdkPath);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        
        if(compiler == null) {
        	System.out.println("Unable to get system compiler, "
        			+ "check your jdkPath in the config file");
        	System.exit(1);
        }
        
        StandardJavaFileManager fileManager = compiler
                .getStandardFileManager(diagnostics, null, null);

        Iterable<? extends JavaFileObject> compilationUnits = fileManager
                .getJavaFileObjectsFromFiles(javaFiles);

        /**
         * Make sure the file manager gets the command line arguments
         * (libraries) as well
         */
        fileManager.setLocation(StandardLocation.CLASS_PATH, libraries);

        List<String> optionList = new ArrayList<String>();
        // set compiler's classpath to be same as the runtime's
        optionList.addAll(Arrays.asList("-classpath",
                System.getProperty("java.class.path")));

        // any other options you want would be added here

        // prepare to run the compiler with these arguments
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager,
                diagnostics, optionList, null, compilationUnits);

        // run the java compiler and check the success of it
        boolean success = task.call();

        if (!success)
        {
            compilationErrorReport += "[" + pid
            		+ "] Compilation error, compiler returned this diagnostic message:" + NEWLINE;
            for (Diagnostic<? extends JavaFileObject> d : diagnostics
                    .getDiagnostics())
            {
                compilationErrorReport += d.toString() + NEWLINE;
            }
            return false;
        }
        else
        {
            System.out.println("Successfully compiled " + pid);
            return true;
        }
    }

    private static void runProgram(File extractedSubmission)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                URLClassLoader loader = null;
                try
                {	
                    loader = new URLClassLoader(
                            new URL[] { extractedSubmission.toURI().toURL() },
                            ClassLoader.getSystemClassLoader());
                    loader.loadClass(packageName + "." + mainClass)
                            .getDeclaredMethod("main",
                                    new Class[] { String[].class })
                            .invoke(null, new Object[]
                    { (Object[]) commandLineArgs });
                    loader.close();
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
