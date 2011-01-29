/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.dp4j.getInstance;
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
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class GetInstanceProcessor extends DProcessor {

    @Override
    protected void processElement(Element e) {
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

        final Singleton ann = singleton.getAnnotation(Singleton.class);
        if (ann == null) {
            msgr.printMessage(Kind.ERROR, "enclosing class must be annotated with Singleton", e);
        }
    }
}
