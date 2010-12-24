/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mysimpatico.se.dp4java.annotations;

import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.api.java.source.JavaSource;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author simpatico
 */
public class NewEmptyJUnitTest {

    @Test
    public void hello() {
        final String srcDir = System.getProperty("user.dir");
        File srcFile = new File(srcDir, "test.txt");
        if (!srcFile.exists()) {
            throw new RuntimeException();
        }
        FileObject fileObj = FileUtil.toFileObject(srcFile);
        assertNotNull(fileObj);
        if (fileObj == null) {
            throw new RuntimeException();
        }
        final JavaSource classSource = JavaSource.forFileObject(fileObj);

    }
}