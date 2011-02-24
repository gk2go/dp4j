/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j;

import java.io.FilenameFilter;
import com.dp4j.processors.core.PrivateAccessProcessor;
import com.dp4j.processors.*;
import com.sun.tools.javac.comp.Resolve;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.Processor;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author simpatico
 */
public class PrivateAccessProcTest {

    static File getFile(final String... dirs) {
        File ret = null;
        for (String dir : dirs) {
            if (ret == null) {
                ret = new File(dir);
            } else {
                ret = new File(ret.getPath(), dir);
            }
        }
        return ret;
    }

    static File getFile(final File dir, final String... dirs) {
        File ret = dir;
        for (String dir1 : dirs) {
            if (ret == null) {
                ret = new File(dir1);
            } else {
                ret = new File(ret.getPath(), dir1);
            }
        }
        return ret;
    }
    File src = getFile(System.getProperty("user.dir"), "src", "main", "java");
    final String procSrc = getFile(src.getAbsolutePath(), "com", "dp4j", "processors", "core").getAbsolutePath();
    static final File workingdir = new File(System.getProperty("user.dir"));
    static final File testResources = getFile(workingdir, "src", "test", "resources");
    static final File testSrc = getFile(workingdir, "src", "test", "java");

    final String getSrcFile(final Class clazz) {
        return new File(src, clazz.getCanonicalName().replace(".", File.separator) + ".java").getAbsolutePath();
    }

    static String getTestFileAbsolutePath(final String dir, final String[] className, final String fileFormat) {
        return getFile(dir, className[1], className[2] + fileFormat).getAbsolutePath();
    }

    final String getClassPath(final File dir, final Class clazz) {
        return new File(dir, clazz.getCanonicalName().replace(".", File.separator) + ".class").getAbsolutePath();
    }
    static File targetTestClasses = getFile(workingdir, "target", "test-classes");

    @Test()
    public void mostComprehensiveTest() throws IOException {
        if (tests.length == 0) {
            return;
        }
        final Runtime runtime = Runtime.getRuntime();
        runtime.traceInstructions(true);
        runtime.traceMethodCalls(true);
        File targetClasses = getFile(workingdir, "target", "classes");
        final String junit = getFile(System.getProperty("user.home"), ".m2", "repository", "junit", "junit", "4.8.2", "junit-4.8.2.jar").getAbsolutePath();
        final String commons = getFile(System.getProperty("user.home"), ".m2", "repository", "commons-lang", "commons-lang", "2.6", "commons-lang-2.6.jar").getAbsolutePath();
        String tools = getFile(System.getProperty("java.home")).getAbsolutePath();
        int lastIndexOf = StringUtils.lastIndexOf(tools, File.separator);
        tools = "\"" + getFile(tools.substring(0, lastIndexOf), "lib", "tools.jar").getAbsolutePath() + "\"";

        String cp = getCp(tools, commons);
        String javacCmd = "javac -Xlint -d " + targetClasses + " ";

        final String dp4jCompile = javacCmd + cp + getClassesToCompile(templateMethod.class, InjectReflection.class, Resolve.class, DProcessor.class, PrivateAccessProcessor.class);

        System.out.println(dp4jCompile);
        cp = getCp(targetClasses.getAbsolutePath(), tools, commons, junit);
        javacCmd = "javac -Xlint -d " + targetTestClasses;
        final String[] testSources = getTestSources();
        String testCmd = javacCmd + cp + " -processor " + PrivateAccessProcessor.class.getCanonicalName() + " " + StringUtils.join(testSources);
        System.out.println(testCmd);
        cleanClasses(tests);
        runtime.exec(dp4jCompile, null, src);
        Process proc = runtime.exec(testCmd, null, workingdir);

        // any error message?
        StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");

        // any output?
        StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT");

        // kick them off
        errorGobbler.start();
        outputGobbler.start();

        // any error???
        try {
            int exitVal = proc.waitFor();
            assertEquals(0, exitVal);
        } catch (InterruptedException ex) {
            Logger.getLogger(PrivateAccessProcTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
        assertClassExists(tests);
    }

    private void cleanClasses(final String[][] testFiles) {
        for (String[] testFile : testFiles) {
            File f = new File(getTestFileAbsolutePath(targetTestClasses.getAbsolutePath(), testFile, ".class"));
            if (f.exists()) {
                assertTrue(f.delete());
                File parentFile = f.getParentFile();
                if (parentFile != null && parentFile.isDirectory()) {
                    FilenameFilter filter = new FilenameFilter() {

                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".class");
                        }
                    };
                    cleanClasses(parentFile.list(filter));
                }
            }
        }
    }

    private void cleanClasses(final String[] testFiles) {
        for (String testFile : testFiles) {
            File f = new File(testFile);
            if (f.exists()) {
                assertTrue(f.delete());
                File parentFile = f.getParentFile();
                if (parentFile != null && parentFile.isDirectory()) {
                    FilenameFilter filter = new FilenameFilter() {

                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".class");
                        }
                    };
                    cleanClasses(parentFile.list(filter));
                }
            }
        }
    }
    final static String comDp4jSamples = "com" + File.separator + "dp4j" + File.separator + "samples";
    final static String tests[][] = {
        {testSrc.getAbsolutePath(), comDp4jSamples, "PrivateData"},
        {testSrc.getAbsolutePath(), comDp4jSamples, "PrivateMethods"},
        {testSrc.getAbsolutePath(), comDp4jSamples, "PrivateVarArgs"},
        {testSrc.getAbsolutePath(), comDp4jSamples, "WithAccessibilePrivateDataInstance"},
        {testSrc.getAbsolutePath(), comDp4jSamples, "WithAccessibleVarArgsInstance"},
        {testSrc.getAbsolutePath(), comDp4jSamples, "ASingleton"},
        {testSrc.getAbsolutePath(), comDp4jSamples, "Genericer"},

        {testResources.getAbsolutePath(), comDp4jSamples,"Test"},
        {testResources.getAbsolutePath(), comDp4jSamples,"VarArgsCallTest"},
        {testResources.getAbsolutePath(), comDp4jSamples,"CallTest"},
        {testResources.getAbsolutePath(), comDp4jSamples,"IfTest"},
        {testResources.getAbsolutePath(), comDp4jSamples,"MultipleCallsTest"},
        {testResources.getAbsolutePath(), comDp4jSamples,"ForEachTest"},
        {testResources.getAbsolutePath(), comDp4jSamples,"ArrayCallTest"},
        {testResources.getAbsolutePath(), comDp4jSamples,"PrivateConstructorTest"},
        {testResources.getAbsolutePath(), comDp4jSamples,"GenericsTest"},
        {testSrc.getAbsolutePath(), StringUtils.EMPTY,"Test10"}
    };

    static String[] getTestSources() {
        String[] ret = new String[tests.length];
        int i = 0;
        for (String[] test : tests) {
            ret[i++] = getTestFileAbsolutePath(test[0], test, ".java") + " ";
        }
        return ret;
    }

    private String getCp(final String... cmds) {
        String ret = " -cp ";
        for (String string : cmds) {
            ret += string + File.pathSeparator;
        }
        return ret.substring(0, ret.length() - 1) + " ";
    }

    private String getClassesToCompile(final Class... cmds) {
        String ret = " ";
        for (Class string : cmds) {
            ret += getSrcFile(string) + " ";
        }
        return ret;
    }
//
//    @Test
//    public void testCallingPrivateMethod() {
//        asssertCompilationSuccessful(compileTestCase("CallTest"));
//    }
//
//    @Test
//    public void testSettingValues() {
//        asssertCompilationSuccessful(compileTestCase("IfTest"));
//    }

//    @Override
//    protected Collection<Processor> getProcessors() {
//        return Arrays.<Processor>asList(new PrivateAccessProcessor());
//    }
    private void assertClassExists(final String[][] testFiles) {
        for (String[] testFile : testFiles) {
            File f = new File(getTestFileAbsolutePath(targetTestClasses.getAbsolutePath(), testFile, ".class"));
            System.out.println(f);
            assertTrue(f.exists());
        }
    }
}
