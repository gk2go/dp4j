/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j;

import java.io.FilenameFilter;
import com.dp4j.processors.core.PrivateAccessProcessor;
import com.dp4j.processors.*;

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
public class PrivateAccessProcTest extends AbstractAnnotationProcessorTest {

    File getFile(final String... dirs) {
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

    final String getSrcFile(final Class clazz) {
        return new File(src, clazz.getCanonicalName().replace(".", File.separator) + ".java").getAbsolutePath();
    }

    static String getTestFileAbsolutePath(final String className) {
        return getFile(testResources, "com", "dp4j", "samples", className + ".java").getAbsolutePath();
    }

    static String getTestClassAbsolutePath(final String className) {
        return getFile(targetTestClasses, "com", "dp4j", "samples", className + ".class").getAbsolutePath();
    }

    final String getClassPath(final File dir, final Class clazz) {
        return new File(dir, clazz.getCanonicalName().replace(".", File.separator) + ".class").getAbsolutePath();
    }
    static File targetTestClasses = getFile(workingdir, "target", "test-classes");

    @org.junit.Test()
    public void mostComprehensiveTest() throws IOException {
        final Runtime runtime = Runtime.getRuntime();
        runtime.traceInstructions(true);
        runtime.traceMethodCalls(true);
        File targetClasses = getFile(workingdir, "target", "classes");

        final String junit = new File(testResources.getAbsolutePath(), "junit.jar").getAbsolutePath();
        final String commons = new File(testResources.getAbsolutePath(), "commons.jar").getAbsolutePath();
        String tools = getFile(System.getProperty("java.home")).getAbsolutePath();
        int lastIndexOf = StringUtils.lastIndexOf(tools, File.separator);
        tools = "\"" + getFile(tools.substring(0, lastIndexOf), "lib", "tools.jar").getAbsolutePath() + "\"";

        String cp = getCp(tools, commons);
        String javacCmd = "javac -Xlint -d " + targetClasses + " ";

        final String dp4jCompile = javacCmd + cp + getClassesToCompile(templateMethod.class, DProcessor.class, ExpProcResult.class, PrivateAccessProcessor.class);

        System.out.println(dp4jCompile);
        cp = getCp(targetClasses.getAbsolutePath(), tools, commons, junit);
        javacCmd = "javac -Xlint -d " + targetTestClasses;
        String testCmd = javacCmd + cp + " -processor " + PrivateAccessProcessor.class.getCanonicalName() + " " + getTestSources();
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

    private void cleanClasses(final String... testFiles) {
        for (String testFile : testFiles) {
            File f = new File(getTestClassAbsolutePath(testFile));
            if (f.exists()) {
                assertTrue(f.delete());
                File parentFile = f.getParentFile();
                if (parentFile != null && parentFile.isDirectory()) {
                    FilenameFilter filter = new FilenameFilter()      {

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
    final static String tests[] = {
//        "Test",
        "CallTest",
//        "IfTest",
        "MultipleCallsTest"
    };

    static String getTestSources() {
        String ret = "";
        for (String test : tests) {
            ret += getTestFileAbsolutePath(test) + " ";
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

    @Override
    protected Collection<Processor> getProcessors() {
        return Arrays.<Processor>asList(new PrivateAccessProcessor());
    }

    private void assertClassExists(final String[] testFiles) {
        for (String testFile : testFiles) {
            File f = new File(getTestClassAbsolutePath(testFile));
            System.out.println(f);
            assertTrue(f.exists());
        }
    }
}
