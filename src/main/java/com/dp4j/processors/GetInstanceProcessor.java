/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.dp4j.Singleton;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

/**
 *
 * Processes @instance annotation verifying that it's:
 * - static
 * - returns instance
 */
@SupportedAnnotationTypes("com.mysimpatico.se.dp4java.annotations.singleton.getInstance") //singleton instance
public class GetInstanceProcessor extends DProcessor {

    @Override
    protected void processElement(Element e, TypeElement ann, final boolean warningsOnly) {
        Set<Modifier> modifiers = e.getModifiers();
        if (!modifiers.contains(Modifier.STATIC)) {
            msgr.printMessage(Kind.ERROR, "instance must be static", e);
        }
        TypeMirror asType = e.asType();
        String returnClass = asType.toString();

        final TypeElement singleton = (TypeElement) e.getEnclosingElement();
        final String enclosingClass = singleton.toString();
        if (!returnClass.contains(enclosingClass)) { //skip ()
            msgr.printMessage(Kind.ERROR, "the return type must be of type " + enclosingClass, e);
        }

        final Singleton singletonAnn = singleton.getAnnotation(Singleton.class);
        if (singletonAnn == null) {
            new SingletonProcessor().processElement(singleton, ann, true);
            //TODO: figure out if successful processing and if so report that it's indeed a Singleton and should be so annotated.
//            msgr.printMessage(Kind.WARNING, "enclosing class should be annotated with Singleton", e);


        }

    }
}
