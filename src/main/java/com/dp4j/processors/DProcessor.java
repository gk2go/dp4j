/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.sun.source.util.Trees;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.*;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import java.util.HashSet;
import com.dp4j.templateMethod;

/**
 *
 * @author simpatico
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public abstract class DProcessor extends AbstractProcessor {

    protected Trees trees;
    protected TreeMaker tm;
    protected static JavacElements elementUtils;
    protected Messager msgr;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        final Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        trees = Trees.instance(processingEnv);
        elementUtils = JavacElements.instance(context);
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        msgr = processingEnv.getMessager();
        tm = TreeMaker.instance(((JavacProcessingEnvironment) processingEnv).getContext());
    }

    protected Set<? extends Element> getElementsAnnotated(final RoundEnvironment roundEnv, Set<? extends TypeElement> annotations) {
        final Set<Element> annotatatedElements = new HashSet<Element>();
        for (TypeElement ann : annotations) {
            final Set<? extends Element> annElements = roundEnv.getElementsAnnotatedWith(ann);
            annotatatedElements.addAll(annElements);
        }
        return annotatatedElements;
    }

    @templateMethod
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (final Element e : getElementsAnnotated(roundEnv, annotations)) {
            processElement(e);
        }
        return true;
    }

    protected abstract void processElement(final Element e);
}
