/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.processors;

import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import com.dp4j.*;

/**
 *
 * @author simpatico
 */
@SupportedAnnotationTypes("com.dp4j.templateMethod") //singleton instance
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TemplateMethodProcessor extends AbstractProcessor{

     @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Messager msgr = processingEnv.getMessager();
        for (Element e : roundEnv.getElementsAnnotatedWith(templateMethod.class)) {
            Set<Modifier> modifiers = e.getModifiers();
            if(modifiers.contains(Modifier.STATIC)){
               msgr.printMessage(Kind.ERROR, "template method must not be static", e);
            }
            if(modifiers.contains(Modifier.ABSTRACT)){
                msgr.printMessage(Kind.ERROR, "template method must not be abstract.", e);
            }
            
            if(!modifiers.contains(Modifier.FINAL)){
                msgr.printMessage(Kind.WARNING, "It's recommended to make the template method final", e);
            }
            //TODO: analyze the code in the method body and make sure:
            // at least one instance method is called, and it's not declared final in the enclosing class (or superclass)
            
            //TODO: issue warning whenever the template method is overriden.
        }
        return true;
    }
}
