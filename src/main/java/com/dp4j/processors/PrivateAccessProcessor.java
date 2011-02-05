/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.model.FilteredMemberList;
import com.sun.tools.javac.tree.JCTree;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import java.util.*;
import javax.lang.model.type.*;
import javax.tools.Diagnostic.Kind.*;
import org.apache.commons.lang.*;

/**
 *
 * @author simpatico
 */
@SupportedAnnotationTypes("org.junit.Test")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PrivateAccessProcessor extends DProcessor {

    private int refIjected = 1; //FIXME: shouldn't be instance var

    protected com.sun.tools.javac.util.List<JCStatement> handleAssign(JCExpression expression, Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, JavacScope scope, com.sun.tools.javac.util.List<JCStatement> stats, JCStatement stmt) {
        JCAssign assignExp = (JCAssign) expression;
        if (assignExp.rhs instanceof JCFieldAccess) {
            final JCFieldAccess fa = (JCFieldAccess) assignExp.rhs;
            final boolean accessible = isAccessible(fa, vars, cut, packageName, scope);
            if (!accessible) {
                stats = reflectField(fa, scope, cut, packageName, vars, stats, stmt);
                assignExp.rhs = getReflectedFieldAccess(fa, cut, packageName, vars);
                reflectionInjected = true;
            }
        }
        if (assignExp.lhs instanceof JCFieldAccess) {
            final JCFieldAccess fa = (JCFieldAccess) assignExp.lhs;
            final boolean accessible = isAccessible(fa, vars, cut, packageName, scope);
            if (!accessible) {
                //TODO: set field field.set(), also handle like if statement
            }
        }
        return stats;
    }

    protected com.sun.tools.javac.util.List<JCStatement> processCond(JCExpression ifExp, Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, JavacScope scope, com.sun.tools.javac.util.List<JCStatement> stats, JCStatement stmt) {
        if (ifExp instanceof JCBinary) {
            JCBinary ifB = (JCBinary) ifExp;

            if (ifB.lhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) ifB.lhs;
                final boolean accessible = isAccessible(fa, vars, cut, packageName, scope);
                if (!accessible) {
                    stats = reflectField(fa, scope, cut, packageName, vars, stats, stmt);
                    ifB.lhs = getReflectedFieldAccess(fa, cut, packageName, vars);
                    reflectionInjected = true;
                }
            }
            if (ifB.rhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) ifB.rhs;
                final boolean accessible = isAccessible(fa, vars, cut, packageName, scope);
                if (!accessible) {
                    stats = reflectField(fa, scope, cut, packageName, vars, stats, stmt);
                    ifB.rhs = getReflectedFieldAccess(fa, cut, packageName, vars);
                    reflectionInjected = true;
                }
            }
        }
        return stats;
    }

    protected com.sun.tools.javac.util.List<JCStatement> processElement(com.sun.tools.javac.util.List<JCStatement> stats, final JCTree tree, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars) {
        JavacScope scope = getScope(cut, tree);
        for (JCStatement stmt : stats) {
            scope = getScope(cut, stmt);//there's also a public API way of getting it for elements only (I cannot convert any tree into an element, only methods, classes, ..
            stats = processStmt(stmt, vars, cut, packageName, scope, stats);
        }
        return stats;
    }

    protected com.sun.tools.javac.util.List<JCStatement> processStmt(JCStatement stmt, Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, JavacScope scope, com.sun.tools.javac.util.List<JCStatement> stats) {
        if (stmt instanceof JCVariableDecl) {
            JCVariableDecl varDec = (JCVariableDecl) stmt;
            if (varDec.init instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) varDec.init;
                final boolean accessible = isAccessible(fa, vars, cut, packageName, scope);
                if (!accessible) {
                    stats = reflectField(fa, scope, cut, packageName, vars, stats, stmt);
                    varDec.init = getReflectedFieldAccess(fa, cut, packageName, vars);
                    reflectionInjected = true;
                }
            }
            addVar(vars, varDec);
        } else if (stmt instanceof JCTry) {
            JCTry tryStmt = (JCTry) stmt;
            //make a copy of vars here, let him add what he wants but then we restore vars
            Map<String, JCExpression> tmpVars = new HashMap<String, JCExpression>(vars);
            if (tryStmt.finalizer != null && tryStmt.finalizer.stats != null && !tryStmt.finalizer.stats.isEmpty()) {
                tryStmt.finalizer.stats = processElement(tryStmt.finalizer.stats, tryStmt, cut, packageName, tmpVars);
            }
            List<JCCatch> catchers = tryStmt.catchers;
            for (JCCatch jCCatch : catchers) {
                if (jCCatch.body != null && jCCatch.body.stats != null && !jCCatch.body.stats.isEmpty()) {
                    jCCatch.body.stats = processElement(jCCatch.body.stats, jCCatch, cut, packageName, tmpVars);
                }
            }
            if (tryStmt.body != null && tryStmt.body.stats != null && !tryStmt.body.stats.isEmpty()) {
                tryStmt.body.stats = processElement(tryStmt.body.stats, tryStmt, cut, packageName, vars);
            }
        } else if (stmt instanceof JCIf) {
            JCIf ifStmt = (JCIf) stmt;
            stats = processCond(ifStmt.cond, vars, cut, packageName, scope, stats, stmt);
            if (ifStmt.thenpart instanceof JCBlock) {
                ((JCBlock) ifStmt.thenpart).stats = processElement(((JCBlock) ifStmt.thenpart).stats, ifStmt, cut, packageName, vars);
            }
        } else if (stmt instanceof JCExpressionStatement) {
            JCExpressionStatement expStmt = (JCExpressionStatement) stmt;
            JCExpression expression = expStmt.getExpression();
            if (expression instanceof JCAssign) {
                stats = handleAssign(expression, vars, cut, packageName, scope, stats, stmt);
            }
        } else if (stmt instanceof JCBlock) {
            ((JCBlock) stmt).stats = processElement(((JCBlock) stmt).stats, stmt, cut, packageName, vars);
        } else if (stmt instanceof JCWhileLoop) {
            JCWhileLoop loop = (JCWhileLoop) stmt;
            stats = processCond(loop.cond, vars, cut, packageName, scope, stats, stmt);
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars);
        } else if (stmt instanceof JCForLoop) {
            JCForLoop loop = (JCForLoop) stmt;
            stats = processCond(loop.cond, vars, cut, packageName, scope, stats, stmt);
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars);
        } else if (stmt instanceof JCDoWhileLoop) {
            JCDoWhileLoop loop = (JCDoWhileLoop) stmt;
            stats = processCond(loop.cond, vars, cut, packageName, scope, stats, stmt);
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars);
        } else if (stmt instanceof JCEnhancedForLoop) {
            JCEnhancedForLoop loop = (JCEnhancedForLoop) stmt;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars);
        }
        return stats;
    }

    private boolean isAccessible(final String className, final JavacScope scope, final String idName) {
        final Symbol s = getSymbol(className, idName);
        DeclaredType declaredType = getDeclaredType(className);
        return trees.isAccessible(scope, s, declaredType);
    }

    private boolean isAccessible(final JCFieldAccess fa, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, JavacScope scope) {
        String className = getClassNameOfAccessor(fa, vars, cut, packageName);
        final String idName = fa.getIdentifier().toString();
        return isAccessible(className, scope, idName);
    }

    private JavacScope getScope(final CompilationUnitTree cut, JCTree tree) {
        if (tree == null) {
            throw new IllegalArgumentException("tree is " + tree);
        }
        final TreePath treePath = TreePath.getPath(cut, tree);
        JavacScope scope = (JavacScope) trees.getScope(treePath);
        return scope;
    }

    JCTypeCast getReflectedFieldAccess(JCFieldAccess fa, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars) {
        final String fieldAccessedName = fa.name.toString();
        final String className = getClassNameOfAccessor(fa, vars, cut, packageName);
        final VarSymbol s = getSymbol(className, fieldAccessedName);
        Type type = s.type;
        if (s.type.isPrimitive()) {
            final TypeElement boxedClass = typeUtils.boxedClass(s.type);
            type = (Type) boxedClass.asType();
        }
        final String field = getFieldVar(fieldAccessedName);
        JCMethodInvocation get = getMethodInvoc(field + ".get", fa.selected);
        JCTypeCast refVal = tm.TypeCast(type, get);
        return refVal;
    }

    protected com.sun.tools.javac.util.List<JCStatement> reflectField(JCFieldAccess fa, final JavacScope scope, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, com.sun.tools.javac.util.List<JCStatement> stats, JCStatement stmt) {
        final String className = getClassNameOfAccessor(fa, vars, cut, packageName);
        final JCVariableDecl classDecl = addClassVarIfNew(vars, className);
        final String clazz = getClassVar(className);
        final String objName = fa.getIdentifier().toString();
        final JCVariableDecl fieldDecl = addFieldVarIfNeW(vars, objName, clazz);
        final String field = getFieldVar(objName);
        final JCMethodInvocation setAccInvoc = getMethodInvoc(field + ".setAccessible", true);
        JCStatement setAccessibleExec = tm.Exec(setAccInvoc);
        if (classDecl != null || fieldDecl != null) {
            JCStatement[] refStmts = new JCStatement[3];
            if (classDecl != null) {
                refStmts[0] = classDecl;
            }
            if (fieldDecl != null) {
                refStmts[1] = fieldDecl;
                refStmts[2] = setAccessibleExec;
            }
            stats = injectBefore(stmt, stats, refStmts);
            refIjected++;
        }
        return stats;
    }

    com.sun.tools.javac.util.List<JCStatement> injectBefore(JCStatement stmt, final com.sun.tools.javac.util.List<JCStatement> stats, JCStatement... newStmts) {
        final ListBuffer<JCStatement> lb = ListBuffer.lb();
        int i = 0;
        final int index = stats.indexOf(stmt);
        for (; i < index; i++) {
            lb.append(stats.get(i));
        }
        for (JCStatement newStmt : newStmts) {
            if (newStmt != null) {
                lb.append(newStmt);
            }
        }
        for (i = index; i < stats.size(); i++) {
            lb.append(stats.get(i));
        }
        return lb.toList();
    }

    private VarSymbol getSymbol(final String className, final String objName) {
        final ClassSymbol typ = elementUtils.getTypeElement(className);
        final List<Symbol> enclosedElements = typ.getEnclosedElements();
        for (Symbol symbol : enclosedElements) { //
            String qualifiedName = symbol.getQualifiedName().toString();
            if (objName.equals(qualifiedName)) { //TODO: will it confuse with method names too?
                return (VarSymbol) symbol;
            }
        }
        return null;
    }

    private DeclaredType getDeclaredType(final String className) {
        final ClassSymbol typ = elementUtils.getTypeElement(className);
        return typeUtils.getDeclaredType(typ);
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

    String getClassVar(String className) {
        className = className.substring(StringUtils.lastIndexOf(className, "."));
        if (className.startsWith(".")) {
            className = className.substring(1);
        }
        className = StringUtils.uncapitalize(className);

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
        TypeElement encClass = (TypeElement) e.getEnclosingElement();

        FilteredMemberList allMembers = elementUtils.getAllMembers(encClass);

        for (Symbol symbol : allMembers) {
            String varName = symbol.toString();
            if (symbol instanceof VarSymbol) {
                VarSymbol v = (VarSymbol) symbol;
                vars.put(varName, getId(v.type.toString()));
            }
        }
        final CompilationUnitTree cut = treePath.getCompilationUnit();
        ExpressionTree packageName = cut.getPackageName();

        tree.body.stats = processElement(tree.body.stats, tree, cut, packageName, vars);
        if (reflectionInjected) {
            tree.thrown = tree.thrown.append(getId("java.lang.ClassNotFoundException"));
            tree.thrown = tree.thrown.append(getId("java.lang.NoSuchFieldException"));
            tree.thrown = tree.thrown.append(getId("java.lang.IllegalAccessException"));
            reflectionInjected = false;
        }
        System.out.println(cut);
    }

    private String getClassNameOfAccessor(JCFieldAccess fa, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName) {
        String className = null;
        final JCExpression exp = fa.getExpression();
        if (exp instanceof JCNewClass) { //constructed instance on the fly
            final JCNewClass nc = (JCNewClass) exp;
            final JCExpression clas = nc.clazz;
            if (clas != null) {
                className = clas.toString();
            }
        } else if (exp instanceof JCIdent) { //is an instance or static
            final JCIdent id = (JCIdent) exp;
            className = id.name.toString();
            final JCExpression get = vars.get(className);
            if (get != null) { //is instance
                className = get.toString();
            }//else is static
        }
        return getQualifiedClassName(className, cut, packageName);
    }
}
