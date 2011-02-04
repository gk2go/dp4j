/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.comp.AttrContext;
import java.util.List;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic.Kind.*;

/**
 *
 * @author simpatico
 */
@SupportedAnnotationTypes("org.junit.Test")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PrivateAccessProcessor extends DProcessor {

    private int refIjected = 1; //FIXME: shouldn't be instance var

    protected com.sun.tools.javac.util.List<JCStatement> processElement(com.sun.tools.javac.util.List<JCStatement> stats, final TreePath treePath, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars) {
        for (JCStatement stmt : stats) {
            if (stmt instanceof JCVariableDecl) {
                JCVariableDecl varDec = (JCVariableDecl) stmt;
                if (varDec.init instanceof JCFieldAccess) {
                    JCFieldAccess fa = (JCFieldAccess) varDec.init;
                    JCExpression type = varDec.vartype;
                    if (varDec.vartype instanceof JCPrimitiveTypeTree) {
                        String typeName = varDec.vartype.toString();
                        if (typeName.equals("int")) {
                            type = getId("Integer");
                        } else if (typeName.equals("boolean")) {
                            type = getId("Boolean");
                        } else if (typeName.equals("double")) {
                            type = getId("Double");
                        } else if (typeName.equals("byte")) {
                            type = getId("Byte");
                        } else if (typeName.equals("short")) {
                            type = getId("Short");
                        } else if (typeName.equals("long")) {
                            type = getId("Long");
                        } else if (typeName.equals("float")) {
                            type = getId("Float");
                        } else if (typeName.equals("char")) {
                            type = getId("Character");
                        } else {
                            msgr.printMessage(javax.tools.Diagnostic.Kind.ERROR, "unrecognized primitive type " + typeName);
                        }
                    }
                    JavacScope scope = (JavacScope) trees.getScope(treePath);

                    JCExpression clas = null;
                    JCExpression exp = fa.getExpression();
                    Name name = null;
                    if (exp instanceof JCNewClass) {
                        JCNewClass nc = (JCNewClass) exp;
                        clas = nc.clazz;
                    } else if (exp instanceof JCIdent) {
                        JCIdent id = (JCIdent) exp;
                        name = id.name;
                    }

                    String className = (clas == null) ? name.toString() : clas.toString();
                    className = getQualifiedClassName(className, cut, packageName);
                    ClassSymbol typ = elementUtils.getTypeElement(className);
                    if (typ == null) { //wasn't static then look into the Declarations in the scope. Probably must re-do
                        JCExpression get = vars.get(className);
                        className = getQualifiedClassName(get.toString(), cut, packageName);
                        typ = elementUtils.getTypeElement(className);
                    }
                    List<Symbol> enclosedElements = typ.getEnclosedElements();
                    Name identifier = fa.getIdentifier();
                    String objName = fa.getIdentifier().toString();
                    Symbol s = null;
                    for (Symbol symbol : enclosedElements) { //
                        Name qualifiedName = symbol.getQualifiedName();
                        if (identifier.equals(qualifiedName)) { //TODO: will it confuse with method names too?
                            s = symbol;
                            break;
                        }
                    }
                    DeclaredType declaredType = typeUtils.getDeclaredType(typ);
                    boolean accessible = trees.isAccessible(scope, s, declaredType);
                    if (!accessible) {
                        final JCVariableDecl classDecl = addClassVarIfNew(vars, className);
                        final String clazz = getClassVar(className);
                        final JCVariableDecl fieldDecl = addFieldVarIfNeW(vars, objName, clazz);
                        final String field = getFieldVar(objName);
                        final JCMethodInvocation setAccInvoc = getMethodInvoc(field + ".setAccessible", true);
                        JCStatement setAccessibleExec = tm.Exec(setAccInvoc);
                        JCMethodInvocation get = getMethodInvoc(field + ".get", objName);
                        JCTypeCast refVal = tm.TypeCast(type, get);
                        varDec.init = refVal;
                        reflectionInjected = true;

                        if (classDecl != null || fieldDecl != null) {
                            int i = 0;
                            final ListBuffer<JCStatement> lb = ListBuffer.lb();
                            final int indexOfStmt = stats.indexOf(stmt);
                            for (; i < indexOfStmt; i++) {
                                lb.append(stats.get(i));
                            }
                            if (classDecl != null) {
                                lb.append(classDecl);
                            }
                            if (fieldDecl != null) {
                                //FIXME: should place in i position
                                lb.append(fieldDecl);
                                lb.append(setAccessibleExec);
                            }
                            for(i = indexOfStmt + 1; i < stats.size(); i++){
                               lb.append(stats.get(i));
                            }
                            stats = lb.toList();
                            refIjected++;
                        }
                    }
                }
                addVar(vars, varDec);
            } else if (stmt instanceof JCTry) {
                JCTry tryStmt = (JCTry) stmt;
                tryStmt.body.stats = processElement(tryStmt.body.stats, treePath, cut, packageName, vars);
            } else if (stmt instanceof JCIf) {
                JCIf ifStmt = (JCIf) stmt;

            }
        }
        return stats;
    }

    private JCVariableDecl addClassVarIfNew(Map<String, JCExpression> vars, String className) {
        JCVariableDecl classDecl = null;
        final String clazz = getClassVar(className);
        if (!vars.containsKey(clazz)) {
            classDecl = getVarDecl(clazz, "java.lang.Class", "java.lang.Class.forName", className);
            addVar(vars, classDecl);
        }
        return classDecl;
    }

    String getClassVar(final String className) {
        return className + "Class";
    }

    String getFieldVar(final String objName) {
        return objName + "Field";
    }

    private JCVariableDecl addFieldVarIfNeW(Map<String, JCExpression> vars, String objName, final String clazz) {
        JCVariableDecl fieldDecl = null;
        final String field = getFieldVar(objName);
        if (!vars.containsKey(field)) {
            fieldDecl = getVarDecl(field, "java.lang.reflect.Field", clazz + ".getDeclaredField", objName);
            addVar(vars, fieldDecl);
        }
        return fieldDecl;
    }

    private void addVar(Map<String, JCExpression> vars, JCVariableDecl varDec) {
        vars.put(varDec.name.toString(), varDec.vartype);
    }

    private String getQualifiedClassName(String className, final CompilationUnitTree cut, Object packageName) {
        if (!className.contains(".")) {
            List<? extends ImportTree> imports = cut.getImports();
            boolean imported = false;
            for (ImportTree importTree : imports) {
                if (importTree.toString().contains("." + className + ";")) {
                    Tree qualifiedIdentifier = importTree.getQualifiedIdentifier();
                    className = qualifiedIdentifier.toString();
                    imported = true;
                    break;
                }
            }
            if (packageName != null && !imported) {
                className = packageName.toString() + "." + className;
            }
        }
        return className;
    }
    boolean reflectionInjected = false;

    @Override
    protected void processElement(Element e) {
        final JCMethodDecl tree = (JCMethodDecl) elementUtils.getTree(e);
        final TreePath treePath = trees.getPath(e);
        Map<String, JCExpression> vars = new HashMap<String, JCExpression>();
        for (JCVariableDecl var : tree.params) {
            addVar(vars, var);
        }
        final CompilationUnitTree cut = treePath.getCompilationUnit();
        ExpressionTree packageName = cut.getPackageName();
        tree.body.stats = processElement(tree.body.stats, treePath, cut, packageName, vars);
        if (reflectionInjected) {
            tree.thrown = tree.thrown.append(getId("java.lang.ClassNotFoundException"));
            tree.thrown = tree.thrown.append(getId("java.lang.NoSuchFieldException"));
            tree.thrown = tree.thrown.append(getId("java.lang.IllegalAccessException"));
            reflectionInjected = false;
        }
        System.out.println(cut);
    }
}
