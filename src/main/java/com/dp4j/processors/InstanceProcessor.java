/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.dp4j.Singleton;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
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
 * - field of a @Singleton annotated class
 * - of the same type as the enclosing class
 *
 *
 * TODO:
 * Warning for:
 * - not being private
 *
 * @author simpatico
 */
@SupportedAnnotationTypes("com.mysimpatico.se.dp4java.annotations.singleton.instance") //singleton instance
public class InstanceProcessor extends DProcessor {

    @Override
    protected void processElement(Element e, TypeElement ann, boolean warningsOnly) {
        Set<Modifier> mods = e.getModifiers();
        if (!mods.contains(Modifier.STATIC)) {
            msgr.printMessage(Kind.ERROR, "instance must be static", e);
        }
        if (!mods.contains(Modifier.PRIVATE) && !mods.contains(Modifier.FINAL)) {
            msgr.printMessage(Kind.ERROR, e + ": it's possible for external objects to change the singleton since the instance is neither declared private nor final.");
        }
        TypeMirror asType = e.asType();
        String returnClass = asType.toString();

        final TypeElement singleton = (TypeElement) e.getEnclosingElement();
        final String enclosingClass = singleton.toString();
        if (!returnClass.equals(enclosingClass)) {
            msgr.printMessage(Kind.ERROR, "the instance field must be of type " + enclosingClass, e);
        }

        final Singleton singletonAnn = singleton.getAnnotation(Singleton.class);
        if (singletonAnn == null) {
            if (singletonAnn.lazy()) {
                VariableTree tree = (VariableTree) trees.getTree(e);
                ExpressionTree initializer = tree.getInitializer();
                if (initializer != null) {
                    msgr.printMessage(Kind.ERROR, "For lazy Singleton initialization you must not inline initialize " + e.getSimpleName(), e);
                }
            }
            new SingletonProcessor().processElement(singleton, ann, true);
            //TODO: figure out if successful processing and if so report that it's indeed a Singleton and should be so annotated.
//            msgr.printMessage(Kind.WARNING, "enclosing class should be annotated with Singleton", e);
        }
    }
}
