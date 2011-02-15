/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.ast;

import com.sun.source.tree.Scope;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import com.dp4j.processors.DProcessor;
import com.dp4j.processors.ExpProcResult;
import com.dp4j.processors.ReflectedAccessResult;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.model.FilteredMemberList;
import com.sun.tools.javac.tree.JCTree;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.util.ListBuffer;
import java.util.*;
import java.util.ArrayList;
import javax.lang.model.type.*;
import javax.tools.Diagnostic.Kind.*;
import org.apache.commons.lang.*;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.*;
import java.util.Map.Entry;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import java.util.*;
import com.dp4j.templateMethod;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import java.util.Map;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.model.FilteredMemberList;
import javax.lang.model.type.ArrayType;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author simpatico
 */
public class Resolver {

    final JavacElements elementUtils;
    final Trees trees;

    public Resolver(JavacElements elementUtils,final Trees trees){
        this.elementUtils = elementUtils;
        this.trees = trees;
    }

    public Symbol getSymbol(Scope scope, Name varName){
        Symbol t = contains(scope, varName); //first lookup scope for all public identifiers
        TypeElement cl = scope.getEnclosingClass();
        while (t == null && cl != null) { //lookup hierarchy for inacessible identifiers too
            t = contains(elementUtils.getAllMembers(cl), varName);
            final TypeMirror superclass = cl.getSuperclass();
            if (superclass != null) {
                cl = (TypeElement) ((Type)superclass).asElement();
            }
        }
        if(t == null) throw new NoSuchElementException(varName.toString());
        return t;
    }

    public TypeElement getType(Scope scope, Name varName){
        TypeElement t = contains(scope, varName).enclClass(); //first lookup scope for all public identifiers
        TypeElement cl = scope.getEnclosingClass();
        while (t == null && cl != null) { //lookup hierarchy for inacessible identifiers too
            t = (TypeElement) contains(cl.getEnclosedElements(), varName).asType();
            final TypeMirror superclass = cl.getSuperclass();
            if (superclass != null) {
                cl = (TypeElement) superclass;
            }
        }
        if(t == null) throw new NoSuchElementException(varName.toString());
        return t;
    }

    public TypeElement getType(Scope scope, String varName) {
        return getType(scope, elementUtils.getName(varName));
    }

    private Type getType(Element e) {
        TreePath path = trees.getPath(e);
        if (path != null) {
            TypeMirror typeMirror = trees.getTypeMirror(path);
            return (Type) typeMirror;
        }
        return (Type) e.asType();
    }

    private Symbol contains(Scope scope, Name varName) {
        Symbol t = null;
        while (t == null && scope != null) {
            Iterable<? extends Element> localElements = scope.getLocalElements();
            t = contains(localElements, varName);
            scope = scope.getEnclosingScope();
        }
        return t;
    }

    private Symbol contains(Iterable<? extends Element> list, Name varName) {
        for (Element e : list) {
            final Name elName;
            if (e instanceof ClassSymbol) {
                ClassSymbol ct = (ClassSymbol) e;
                elName = ct.getQualifiedName();
            } else {
                elName = e.getSimpleName();
            }
            if (elName.equals(varName) || e.getSimpleName().equals(varName)) {
                return (Symbol) e;
            }
        }
        return null;
    }
}
