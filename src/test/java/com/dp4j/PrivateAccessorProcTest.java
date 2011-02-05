package com.dp4j;

import com.dp4j.processors.*;
import java.net.*;
import java.util.*;
import java.util.Collection;
import javax.annotation.processing.*;
import javax.tools.JavaCompiler;
import javax.tools.*;

import org.junit.Test;

public class PrivateAccessorProcTest {

     private static final String SOURCE_FILE_SUFFIX = ".java";
     protected String getPathForTestCase(final String className) throws ClassNotFoundException{
        URL systemResource = ClassLoader.getSystemResource("com/dp4j/" + className + SOURCE_FILE_SUFFIX);
        return systemResource.getPath();
    }




    @Test
    public void ifTest() throws ClassNotFoundException {
        String pathForTestCase = getPathForTestCase("IfTest");
    }
}
