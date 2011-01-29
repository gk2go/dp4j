/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.processors;

import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic.Kind;

/**
 *
 * @author simpatico
 */
@SupportedAnnotationTypes(value={"com.dp4j.templateMethod", "org.jpatterns.gof.TemplateMethodPattern"}) //singleton instance
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TemplateMethodProcessor extends DProcessor{

    @Override
    protected void processElement(Element e) {
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
}
