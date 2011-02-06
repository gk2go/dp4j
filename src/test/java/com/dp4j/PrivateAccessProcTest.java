/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j;

import com.dp4j.processors.*;
import com.qrmedia.commons.test.annotation.processing.*;
import java.io.File;
import java.util.*;
import java.util.Collection;
import javax.annotation.processing.*;
import org.junit.*;
import static org.junit.Assert.*;

import java.io.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.processing.*;
import javax.tools.*;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaCompiler.CompilationTask;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author simpatico
 */
public class PrivateAccessProcTest {

    private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();

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

    File getFile(final File dir, final String... dirs) {
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
    final String procSrc = getFile(src.getAbsolutePath(), "com", "dp4j", "processors").getAbsolutePath();
    final File workingdir = new File(System.getProperty("user.dir"));
    final File testResources = getFile(workingdir, "src", "test", "resources");

    final String getSrcFile(final Class clazz) {
        return new File(src, clazz.getCanonicalName().replace(".", File.separator) + ".java").getAbsolutePath();
    }

    final String getTestFile(final String className) {
        return getFile(testResources, "com", "dp4j", className + ".java").getAbsolutePath();
    }

    final String getClassPath(final File dir, final Class clazz) {
        return new File(dir, clazz.getCanonicalName().replace(".", File.separator) + ".class").getAbsolutePath();
    }

    @org.junit.Test
    public void mostComprehensiveTest() throws IOException {
        final Runtime runtime = Runtime.getRuntime();
        runtime.traceInstructions(true);
        runtime.traceMethodCalls(true);
        File targetClasses = getFile(workingdir, "target", "classes");
        File targetTestClasses = getFile(workingdir, "target", "test-classes");
        final String junit = new File(testResources.getAbsolutePath(), "junit.jar").getAbsolutePath();
        final String commons = new File(testResources.getAbsolutePath(), "commons.jar").getAbsolutePath();
        String tools = getFile(System.getProperty("java.home")).getAbsolutePath();
        int lastIndexOf = StringUtils.lastIndexOf(tools, File.separator);
        tools = "\"" + getFile(tools.substring(0, lastIndexOf), "lib", "tools.jar").getAbsolutePath() + "\"";

        String cp = getCp(tools, commons);
        String javacCmd = "javac -d " + targetClasses + " ";

        final String dp4jCompile = javacCmd + cp + getClassesToCompile(templateMethod.class,DProcessor.class,ExpProcResult.class ,PrivateAccessProcessor.class);

        System.out.println(dp4jCompile);
        cp = getCp(targetClasses.getAbsolutePath(), tools, commons, junit);
        javacCmd = "javac -d " + targetTestClasses;
        String testCmd = javacCmd + cp + " -processor " + PrivateAccessProcessor.class.getCanonicalName() + " " + getTestFile("Test");
        System.out.println(testCmd);
        Process exec = runtime.exec(dp4jCompile, null, workingdir);

        runtime.exec(testCmd, null, workingdir);
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

}
