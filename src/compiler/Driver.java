package compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

//import javax.tools.JavaCompiler;
//import javax.tools.ToolProvider;

import java.util.Enumeration;
import java.util.List;
//import java.util.Scanner;
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

    private static UnzipUtility unzipUtility;
    private static String root;

    private static String projectName;
    private static String packageName;
    private static ArrayList<File> libraries;

    public static void main(String[] args) throws IOException
    {
        if (args.length == 0)
        {
            System.out.println("How to invoke:");
            System.out.println(
                    "BatchCompiler [zipFile] [projectName] [packageName] [libraryFiles]...");
            System.out.println("projectName and packageName are from eclipse"
                    + " file structure: projectName/src/packageName");
            System.out.println(
                    "libraryFiles are any number of jar files the project needs to compile");
            System.out.println(
                    "For more information use command BatchCompiler help");
        }
        else if (args[0].equals("help"))
        {
            System.out.println("CS 2114 Batch Compiler\n");

            System.out.println("How to invoke:");
            System.out.println(
                    "BatchCompiler [zipFile] [projectName] [packageName] [libraryFiles]...");

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
        else
        {
            if (args[1] == null)
            {
                System.out.println("Missing project name argument, see help");
                System.exit(1);
            }
            if (args[2] == null)
            {
                System.out.println("Missing package name argument, see help");
                System.exit(1);
            }

            setUpEnvironment(args);
            System.out.println("==============================");
            extractFiles(args[0]);
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
        }
    }

    private static void setUpEnvironment(String[] args) throws IOException
    {
        System.out.println("CS 2114 batch compiler\n");

        System.out.println("Compiling archive: " + args[0]);

        projectName = args[1];
        System.out.println("Using project name: " + args[1]);

        packageName = args[2];
        System.out.println("Using package name: " + args[2]);

        String inputLibraries = "Using Libraries: ";
        libraries = new ArrayList<File>();
        for (int i = 3; i < args.length; i++)
        {
            inputLibraries += args[i];

            libraries.add(new File(args[i]));
            ClassPathHacker.addFile(new File(args[i]));
            if (i < args.length - 1)
            {
                inputLibraries += ", ";
            }
        }
        System.out.println(inputLibraries);
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
                .println("Succesfully extracted " + zipName + " into " + dest);
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
            System.out.println("Error extracting " + pid
                    + ", you should investigate this submission manually.");
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

                Files.copy(new File("QueueReader.java").toPath(),
                        new File(extractedSubmissionPath + File.separator
                                + projectName + File.separator + "src"
                                + File.separator + packageName + File.separator
                                + "QueueReader.java").toPath(),
                        StandardCopyOption.COPY_ATTRIBUTES);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.out.println(
                        "We did not have a correctly setup project :(");
            }
            if (compile(extractedSubmission))
            {
                runProgram(new File(extractedSubmissionPath + File.separator
                        + projectName + File.separator + "src"));
            }
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

    private static boolean compile(File extractedSubmission) throws IOException
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

        /**
         * Initialize diagnostics collector to report compile errors
         */
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

        /*
         * We need the jdk to get the compiler
         */
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
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
            System.out.println(
                    "Failed to Compile, would reccomend looking manualy");
            for (Diagnostic<? extends JavaFileObject> d : diagnostics
                    .getDiagnostics())
            {
                System.out.println(d.toString());
            }
            return false;
        }
        else
        {
            System.out.println("Successfully compiled the program");
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
                String[] params = new String[0];
                URLClassLoader loader = null;
                try
                {
                    loader = new URLClassLoader(
                            new URL[] { extractedSubmission.toURI().toURL() },
                            ClassLoader.getSystemClassLoader());
                    loader.loadClass("rollercoaster.QueueReader")
                            .getDeclaredMethod("main",
                                    new Class[] { String[].class })
                            .invoke(null, new Object[]
                    { (Object[]) params });
                    loader.close();
                }
                catch (MalformedURLException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (IllegalAccessException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (IllegalArgumentException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (InvocationTargetException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (NoSuchMethodException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (SecurityException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (ClassNotFoundException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
