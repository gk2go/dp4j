/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mysimpatico.se.dp4java.annotations.processors;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import com.mysimpatico.se.dp4java.annotations.singleton.*;
import java.io.File;
import org.codehaus.plexus.util.DirectoryScanner;
/**
 *
 * Processes @instance annotation verifying that it's:
 * - static
 * - returns instance
 */

@SupportedAnnotationTypes("com.mysimpatico.se.dp4java.annotations.singleton.getInstance") //singleton instance
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class GetInstanceProcessor extends AbstractProcessor{

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Messager msgr = processingEnv.getMessager();
        String anyDir = "**\\";
        for (Element e : roundEnv.getElementsAnnotatedWith(getInstance.class)) {
            Set<Modifier> modifiers = e.getModifiers();
            if(!modifiers.contains(Modifier.STATIC)){
               msgr.printMessage(Kind.ERROR, "instance must be static", e);
            }
            TypeMirror asType = e.asType();
            String returnClass = asType.toString();

            final TypeElement singleton = (TypeElement) e.getEnclosingElement();
            final String enclosingClass = singleton.toString();
            if(!returnClass.contains(enclosingClass)){ //skip ()
                msgr.printMessage(Kind.ERROR,"the return type must be of type " + enclosingClass ,e);
            }

            final Singleton ann = singleton.getAnnotation(Singleton.class);
            if(ann == null){
                msgr.printMessage(Kind.ERROR,"enclosing class must be annotated with Singleton", e);
            }

//            final DirectoryScanner ds = new DirectoryScanner();
//            String file = enclosingClass.replaceAll("\\.", "\\" + File.separator);
//            final File classFile = new File(file);
//            System.out.println(classFile.getAbsolutePath());
//            String srcDir = System.getProperty("user.dir");
//            ds.setBasedir(srcDir);
//            final String classs = anyDir + classFile.getName() + ".java";
//            ds.setIncludes(new String[]{classs});
//            ds.scan();
//            String[] includedFiles = ds.getIncludedFiles();
//            if(includedFiles.length != 1){
//                msgr.printMessage(Kind.ERROR,"not declared in separate file?" + e);
//            }
//            File srcFile = new File(srcDir, includedFiles[0]);
//            try {
//                Scanner sc = new Scanner(srcFile);
//            } catch (FileNotFoundException ex) {
//                Logger.getLogger(GetInstanceProcessor.class.getName()).log(Level.SEVERE, null, ex);
//            }
        }
        return true;
    }
}
