/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import com.sun.tools.javac.code.Type;
/**
 *
 * @author simpatico
 */
public abstract class MethProcessor extends DProcessor {

    @Override
    protected void perElementInit(Element e) {
        super.perElementInit(e);
        encClass = (TypeElement) e.getEnclosingElement();
        thisExp = tm.This((Type) encClass.asType());
        methTree = (JCMethodDecl) elementUtils.getTree(e);
    }
}
