/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.util.List;

/**
 *
 * @author simpatico
 */
@SupportedAnnotationTypes("org.junit.Test")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PrivateAccessProcessor extends DProcessor {

    @Override
    protected void processElement(Element e) {
        JCMethodDecl tree = (JCMethodDecl) elementUtils.getTree(e);
        CompilationUnitTree cut = trees.getPath(e).getCompilationUnit();
        ExpressionTree packageName = cut.getPackageName();
        for (JCStatement stmt : tree.body.stats) {
            if (stmt instanceof JCVariableDecl) {
                JCVariableDecl varDec = (JCVariableDecl) stmt;
                System.out.println(varDec.init);
                if (varDec.init instanceof JCFieldAccess) {
                    JCFieldAccess fa = (JCFieldAccess) varDec.init;
                    System.out.println(fa.sym);
                    System.out.println(fa.getExpression());
                    JCExpression type = fa.getExpression();
                    com.sun.tools.javac.util.Name name = elementUtils.getName(fa.getExpression().toString());
                    Class<?> clazz = null;
                    String className = name.toString();
                    if (!className.contains(".")) {
                        className = packageName.toString() + "." + className;
                    }
                    JCVariableDecl classDecl = getVarDecl("clazz", "java.lang.Class", "java.lang.Class.forName", className);
                    final String field = "field";
                    String objName = fa.getIdentifier().toString();
                    JCVariableDecl fieldDecl = getVarDecl(field, "java.lang.reflect.Field", "clazz.getDeclaredField", objName);
                    final JCMethodInvocation setAccInvoc = getMethodInvoc(field + ".setAccessible", true);
                    JCStatement setAccessibleExec = tm.Exec(setAccInvoc);
                    JCMethodInvocation get = getMethodInvoc(field + ".get", objName);
                    JCTypeCast refVal = tm.TypeCast(type, get);
                    varDec.init = refVal;
                    tree.body.stats = tree.body.stats.prepend(setAccessibleExec);
                    tree.body.stats = tree.body.stats.prepend(fieldDecl); //no problem with for-each loop!
                    tree.body.stats = tree.body.stats.prepend(classDecl);
                    //FIXME: need to add this. Surround the whole
                    JCVariableDecl exDecl = getVarDecl("ex", "java.lang.ClassNotFoundException");
                     List<JCStatement> emptyList = emptyList();
                    JCTree.JCCatch catchEx = tm.Catch(exDecl, tm.Block(0, emptyList));
                    List<JCCatch> catchStm = List.of(catchEx);
                    JCTry Try = tm.Try(tree.body, catchStm, tm.Block(0, emptyList));
                }
            }
        }
        System.out.println(cut);
   }
}
