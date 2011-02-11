/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors.core;

import com.dp4j.processors.DProcessor;
import com.dp4j.processors.ExpProcResult;
import com.dp4j.processors.ReflectedAccessResult;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.model.FilteredMemberList;
import com.sun.tools.javac.tree.JCTree;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.ListBuffer;
import java.util.*;
import javax.lang.model.type.*;
import javax.tools.Diagnostic.Kind.*;
import org.apache.commons.lang.*;

/**
 *
 * @author simpatico
 */
@SupportedAnnotationTypes(value = {"org.junit.Test", "com.dp4j.InjectReflection"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PrivateAccessProcessor extends DProcessor {

    private int refIjected = 1; //FIXME: shouldn't be instance var

    public Type getBoxedType(final Symbol s) {
        Type type = s.type;
        if (s.type.isPrimitive()) {
            final TypeElement boxedClass = typeUtils.boxedClass(s.type);
            type = (Type) boxedClass.asType();
        }
        return type;
    }

    public Symbol getSymbol(final ClassSymbol typ, final String objName) {
        FilteredMemberList allMembers = elementUtils.getAllMembers(typ);
        List<Symbol> subList = allMembers.subList(0, allMembers.size());
        final List<Symbol> allElements = merge(typ.getEnclosedElements(), subList);

        for (Symbol symbol : allElements) { //
            String qualifiedName = symbol.getQualifiedName().toString();
            if (objName.equals(qualifiedName)) { //TODO: will it confuse with method names too?
                return symbol;
            }
        }
        return null;
    }

    protected com.sun.tools.javac.util.List<JCStatement> processElement(com.sun.tools.javac.util.List<JCStatement> stats, final JCTree tree, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars) {
        com.sun.source.tree.Scope scope = getScope(cut, tree);
        return processElement(stats, scope, cut, packageName, vars);
    }

    protected com.sun.tools.javac.util.List<JCStatement> processStmt(JCStatement stmt, Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, com.sun.tools.javac.util.List<JCStatement> stats) {
        if (stmt instanceof JCVariableDecl) {
            JCVariableDecl varDec = (JCVariableDecl) stmt;
            ExpProcResult processCond = processCond(varDec.init, vars, cut, packageName, scope, stats, stmt);
            stats = processCond.stats;
            varDec.init = processCond.exp;
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
            stats = processCond(ifStmt.cond, vars, cut, packageName, scope, stats, stmt).stats;
            if (ifStmt.thenpart instanceof JCBlock) {
                ((JCBlock) ifStmt.thenpart).stats = processElement(((JCBlock) ifStmt.thenpart).stats, ifStmt, cut, packageName, vars);
            }
        } else if (stmt instanceof JCExpressionStatement) {
            JCExpressionStatement expStmt = (JCExpressionStatement) stmt;
            ExpProcResult result = processCond(expStmt.expr, vars, cut, packageName, scope, stats, stmt);
            stats = result.stats;
            expStmt.expr = result.exp;
        } else if (stmt instanceof JCBlock) {
            ((JCBlock) stmt).stats = processElement(((JCBlock) stmt).stats, stmt, cut, packageName, vars);
        } else if (stmt instanceof JCWhileLoop) {
            JCWhileLoop loop = (JCWhileLoop) stmt;
            stats = processCond(loop.cond, vars, cut, packageName, scope, stats, stmt).stats;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars);
        } else if (stmt instanceof JCForLoop) {
            JCForLoop loop = (JCForLoop) stmt;
            stats = processCond(loop.cond, vars, cut, packageName, scope, stats, stmt).stats;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars);
        } else if (stmt instanceof JCDoWhileLoop) {
            JCDoWhileLoop loop = (JCDoWhileLoop) stmt;
            stats = processCond(loop.cond, vars, cut, packageName, scope, stats, stmt).stats;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars);
        } else if (stmt instanceof JCEnhancedForLoop) {
            JCEnhancedForLoop loop = (JCEnhancedForLoop) stmt;
            stats = processCond(loop.expr, vars, cut, packageName, scope, stats, stmt).stats;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars);
        }
        return stats;
    }

    JCParens cast(ReflectedAccessResult reflectedAccess) {
        return tm.Parens(tm.TypeCast(reflectedAccess.expType, reflectedAccess.exp));
    }

    protected ExpProcResult processCond(JCExpression ifExp, Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, com.sun.tools.javac.util.List<JCStatement> stats, JCStatement stmt) {
        if (ifExp instanceof JCBinary) {
            JCBinary ifB = (JCBinary) ifExp;
            if (ifB.lhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) ifB.lhs;
                final boolean accessible = isAccessible(fa, vars, cut, packageName, scope);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt);
                    ifB.lhs = cast(getReflectedAccess(fa, cut, packageName, vars, stmt));
                    reflectionInjected = true;
                }
            }
            if (ifB.rhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) ifB.rhs;
                final boolean accessible = isAccessible(fa, vars, cut, packageName, scope);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt);
                    ifB.rhs = cast(getReflectedAccess(fa, cut, packageName, vars, stmt));
                    reflectionInjected = true;
                }
            }
        } else if (ifExp instanceof JCFieldAccess) {
            final JCFieldAccess fa = (JCFieldAccess) ifExp;
            //            FIXME: recursively process fa.getExpression()
            final boolean accessible;
            if (fa.selected instanceof JCMethodInvocation || !fa.selected.toString().contains(".class")) {
                accessible = isAccessible(fa, vars, cut, packageName, scope);
                //FIXME: for accessibility must also check method parameters
            } else {
                accessible = isAccessible(fa.selected.toString(), scope, cut, packageName);
            }
            if (!accessible) {
                stats = reflect(fa, scope, cut, packageName, vars, stats, stmt); //when ifExp
                final ReflectedAccessResult reflectedAccess = getReflectedAccess(fa, cut, packageName, vars, stmt);

                if (stmt instanceof JCExpressionStatement && ((JCExpressionStatement) stmt).expr instanceof JCMethodInvocation) { //just method call
                    ifExp = reflectedAccess.exp;
                } else {
                    ifExp = cast(reflectedAccess);
                }
                if (stmt instanceof JCEnhancedForLoop) {
                    ((JCEnhancedForLoop) stmt).expr = ifExp;
                }
                reflectionInjected = true;
            }
        } else if (ifExp instanceof JCAssign) {
            JCAssign assignExp = (JCAssign) ifExp;
            if (assignExp.rhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) assignExp.rhs;
                final boolean accessible = isAccessible(fa, vars, cut, packageName, scope);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt);
                    assignExp.rhs = cast(getReflectedAccess(fa, cut, packageName, vars, stmt));
                    reflectionInjected = true;
                }
            }
            if (assignExp.lhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) assignExp.lhs;
                final boolean accessible = isAccessible(fa, vars, cut, packageName, scope);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt);
                    JCMethodInvocation reflectedFieldSetter = getReflectedFieldSetter(fa, assignExp.rhs, cut, packageName, vars);
                    ifExp = reflectedFieldSetter;
                }
            }
        } else if (ifExp instanceof JCMethodInvocation) {
            JCMethodInvocation mi = (JCMethodInvocation) ifExp;
            final ListBuffer<JCExpression> lb = ListBuffer.lb();
            if (!mi.args.isEmpty()) {
                for (JCExpression arg : mi.args) {
                    ExpProcResult result = processCond(arg, vars, cut, packageName, scope, stats, stmt);
                    stats = result.stats;
                    lb.append(result.exp);
                }
                mi.args = lb.toList();
            }
            final ExpProcResult result = processCond(mi.meth, vars, cut, packageName, scope, stats, stmt);
            if (result.exp instanceof JCParens || result.exp instanceof JCMethodInvocation) {
                final JCMethodInvocation resMi;
                if (result.exp instanceof JCParens) {
                   resMi = (JCMethodInvocation)((JCTypeCast)((JCParens) result.exp).expr).expr;
                } else {
                    resMi = (JCMethodInvocation) result.exp;
                }
                resMi.args = merge(resMi.args, mi.args);
                ifExp = result.exp;
            } else {
                mi.meth = result.exp;
            }
            //FIXME: merge args here
            stats = result.stats;
        } else if (ifExp instanceof JCNewClass) {
            //TODO: it's a method too! handle similarly
        } else if (ifExp instanceof JCTypeCast) {
            return processCond(((JCTypeCast) ifExp).expr, vars, cut, packageName, scope, stats, stmt);
        }
        return new ExpProcResult(stats, ifExp);
    }

    private boolean isAccessible(String className, final com.sun.source.tree.Scope scope, final CompilationUnitTree cut, final Object packageName) {
        className = getQualifiedClassName(className, cut, packageName);
        DeclaredType declaredType = getDeclaredType(className);
        return trees.isAccessible(scope, (TypeElement) declaredType.asElement());
    }

    private boolean isAccessible(final JCFieldAccess fa, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope) {
        final String idName = fa.getIdentifier().toString();
        String className;
        try {
            className = fa.getExpression().toString();
            return isAccessible(className, scope, idName);
        } catch (Exception e) {
            final JCExpression exp = fa.getExpression();
            className = exp.toString();
            if (exp instanceof JCFieldAccess && !fa.toString().endsWith(".class")) {
                className = getType((JCFieldAccess) exp, vars, cut, packageName, scope);
                return isAccessible(className, scope, idName) && isAccessible((JCFieldAccess) exp, vars, cut, packageName, scope);
            } else {
                className = getClassNameOfAccessor(fa, vars, cut, packageName);
            }
        }
        return isAccessible(getQualifiedClassName(className, cut, packageName), scope, idName);
    }

    public String getType(JCFieldAccess fa, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope) {
        final String className = getClassNameOfAccessor(fa, vars, cut, packageName);
        final String fieldAccessedName = fa.name.toString();
        final Symbol s = getSymbol(className, fieldAccessedName);
        return s.type.toString();
    }

    private boolean isAccessible(final String className, final com.sun.source.tree.Scope scope, final String idName) {
        final Symbol s = getSymbol(className, idName);
        DeclaredType declaredType = getDeclaredType(className); //ordr matters to logic!
        if (idName.equals("class") && declaredType != null) {
            return true;
        }
        return trees.isAccessible(scope, s, declaredType);
    }

    private String getClassNameOfAccessor(JCFieldAccess fa, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName) {
        final JCExpression exp = fa.getExpression();
        String className = exp.toString();
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
        } else if (exp instanceof JCMethodInvocation) {
            JCMethodInvocation mi = (JCMethodInvocation) exp;
            className = getReturnType(mi, cut, packageName);
            //FIXME: what about chains and args of the method?
        }
        return getQualifiedClassName(className, cut, packageName);
    }

    private com.sun.source.tree.Scope getScope(final CompilationUnitTree cut, JCTree tree) {
        if (tree == null) {
            throw new IllegalArgumentException("tree is " + tree);
        }
        final TreePath treePath = TreePath.getPath(cut, tree);
        com.sun.source.tree.Scope scope = trees.getScope(treePath);
        return scope;
    }

    ReflectedAccessResult getReflectedAccess(JCFieldAccess fa, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, JCStatement stmt) {
        final String fieldAccessedName = fa.name.toString();
        final String className = getClassNameOfAccessor(fa, vars, cut, packageName);
        final Symbol s = getSymbol(className, fieldAccessedName);
        final String field;
        final boolean isMethod = s instanceof MethodSymbol;
        final Object param;
        Type type;
        if (s.isStatic()) {
            param = "";
        } else {
            param = fa.selected;
        }

        final String methodCall;
        if (isMethod) {
            field = getMethodVar(fieldAccessedName);
            methodCall = field + ".invoke";
            methodInjected = true; //could be a source of bugs here. Not injected yet!
            type = ((MethodSymbol) s).getReturnType();
        } else {
            type = getBoxedType(s);
            field = getFieldVar(fieldAccessedName);
            methodCall = field + ".get";
        }
        final JCExpression get = getRefMethodInvoc(methodCall, param);
        return new ReflectedAccessResult(get, type);
    }

    JCMethodInvocation getReflectedFieldSetter(JCFieldAccess fa, final JCExpression value, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars) {
        final String fieldAccessedName = fa.name.toString();
        final String field = getFieldVar(fieldAccessedName);
        JCMethodInvocation set = getMethodInvoc(field + ".set", fa.selected, value); //TODO: would be better to use setInt, setDouble, etc.. based on type to compile-time check more
        return set;
    }

    protected com.sun.tools.javac.util.List<JCStatement> reflect(JCFieldAccess fa, final com.sun.source.tree.Scope scope, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, com.sun.tools.javac.util.List<JCStatement> stats, JCStatement stmt) {
        final String className = getClassNameOfAccessor(fa, vars, cut, packageName);
        final JCVariableDecl classDecl = addClassVarIfNew(vars, className);
        final String clazz = getClassVar(className);
        final String objName = fa.getIdentifier().toString();
        Symbol symbol = getSymbol(className, objName);
        final JCVariableDecl refDecl;
        final String refVarName;
        if (symbol instanceof MethodSymbol) {
            refDecl = addMethodVarIfNeW(vars, objName, ((MethodSymbol) symbol).params, clazz);
            refVarName = getMethodVar(objName);
        } else {
            refDecl = addFieldVarIfNeW(vars, objName, clazz);
            refVarName = getFieldVar(objName);
        }

        final JCMethodInvocation setAccInvoc = getMethodInvoc(refVarName + ".setAccessible", true);
        JCStatement setAccessibleExec = tm.Exec(setAccInvoc);
        if (classDecl != null || refDecl != null) {
            JCStatement[] refStmts = new JCStatement[3];
            if (classDecl != null) {
                refStmts[0] = classDecl;
            }
            if (refDecl != null) {
                refStmts[1] = refDecl;
                refStmts[2] = setAccessibleExec;
            }
            stats = injectBefore(stmt, stats, refStmts);
            refIjected++;
        }
        return stats;
    }

    private Symbol getSymbol(final String className, final String objName) {
        final ClassSymbol typ = elementUtils.getTypeElement(className);
        return getSymbol(typ, objName);
    }

    private DeclaredType getDeclaredType(final String className) {
        final ClassSymbol typ = elementUtils.getTypeElement(className);
        return typeUtils.getDeclaredType(typ);
    }

    private JCVariableDecl addClassVarIfNew(Map<String, JCExpression> vars, String className) {
        JCVariableDecl classDecl = null;
        final String clazz = getClassVar(className);
        if (!vars.containsKey(clazz)) {
            classDecl = getVarDecl(clazz, javalangClass, javalangClass + ".forName", className);
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

    private JCVariableDecl addMethodVarIfNeW(Map<String, JCExpression> vars, String objName, List<VarSymbol> params, final String clazz) {
        JCVariableDecl meDecl = null;
        final String methodVar = getMethodVar(objName.toString());
        if (!vars.containsKey(methodVar)) {
            meDecl = getVarDecl(methodVar, "java.lang.reflect.Method", clazz + ".getDeclaredMethod", objName, getTypes(params));
            addVar(vars, meDecl);
        }
        return meDecl;
    }

    private void addVar(Map<String, JCExpression> vars, JCVariableDecl varDec) {
        vars.put(varDec.name.toString(), varDec.vartype);
    }
    final static String javalangClass = "java.lang.Class";

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
                String tmp = className;
                className = packageName.toString() + "." + className;
                ClassSymbol te = elementUtils.getTypeElement(className);
                if (te == null) { //must be java.lang,
                    //FIXME: or some .* .. this is a good moment to try-catch
                    return getQualifiedClassName("java.lang." + tmp, cut, packageName);
                }
            }
        } else if (className.endsWith(".class")) {
            className = javalangClass;
        }
        return className;
    }
    boolean reflectionInjected = false;
    TypeElement encClass;

    @Override
    protected void processElement(Element e, TypeElement ann, boolean warningsOnly) {
        final JCMethodDecl tree = (JCMethodDecl) elementUtils.getTree(e);
        final TreePath treePath = trees.getPath(e);

        Map<String, JCExpression> vars = new HashMap<String, JCExpression>();

        for (JCVariableDecl var : tree.params) {
            addVar(vars, var);
        }
        encClass = (TypeElement) e.getEnclosingElement();

        FilteredMemberList allMembers = elementUtils.getAllMembers(encClass);

        for (Symbol symbol : allMembers) {
            String varName = symbol.toString();
            if (symbol instanceof VarSymbol) {
                VarSymbol v = (VarSymbol) symbol;
                vars.put(varName, getId(v.type.toString()));
            }
        }
        com.sun.source.tree.Scope scope = trees.getScope(treePath);
        final CompilationUnitTree cut = treePath.getCompilationUnit();
        ExpressionTree packageName = cut.getPackageName();
        tree.body.stats = processElement(tree.body.stats, scope, cut, packageName, vars);
        if (reflectionInjected) {
            tree.thrown = tree.thrown.append(getId("java.lang.ClassNotFoundException"));
            tree.thrown = tree.thrown.append(getId("java.lang.NoSuchFieldException"));
            tree.thrown = tree.thrown.append(getId("java.lang.IllegalAccessException"));
            if (methodInjected) {
                tree.thrown = tree.thrown.append(getId("java.lang.reflect.InvocationTargetException"));
                tree.thrown = tree.thrown.append(getId("java.lang.IllegalArgumentException"));
                tree.thrown = tree.thrown.append(getId("java.lang.NoSuchMethodException"));
                methodInjected = false;
            }
            reflectionInjected = false;
        }
        System.out.println(cut);
    }
    boolean methodInjected = false;

    protected com.sun.tools.javac.util.List<JCStatement> processElement(com.sun.tools.javac.util.List<JCStatement> stats, final com.sun.source.tree.Scope scope, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars) {
        for (JCStatement stmt : stats) {
            stats = processStmt(stmt, vars, cut, packageName, scope, stats);
        }
        return stats;
    }

    private String getReturnType(JCMethodInvocation mi, CompilationUnitTree cut, Object packageName) {

        String className = null, mName = mi.meth.toString();

        if (mName.contains(".")) {
            final int methodAccessDot = StringUtils.lastIndexOf(mName, ".");
            mName = mName.substring(methodAccessDot + 1);
            className = mi.meth.toString().substring(0, methodAccessDot);
            className = getQualifiedClassName(className, cut, packageName);
        }
        if (className == null) {
            className = encClass.getQualifiedName().toString();
        }
        MethodSymbol symbol = (MethodSymbol) getSymbol(className, mName);
        Type.MethodType mt = (MethodType) symbol.type;
        return mt.restype.toString();
    }

    private String getMethodVar(String objName) {
        return objName + "Method";
    }

    /**
     * Junit or someone else might want to handle it
     * @return
     */
    @Override
    protected boolean onlyHandler(Set<? extends TypeElement> annotations) {
        return false;
    }

    public JCExpression[] getTypes(List<VarSymbol> params) {
        int length = 0;
        if (params != null) {
            length = params.size();
        }

        JCExpression[] exps = new JCExpression[length];
        int i = 0;
        for (VarSymbol param : params) {
            final String clazz;
            final String typeName = getBoxedType(param).toString();
            if (param.type.isPrimitive()) {
                Symbol symbol = getSymbol(typeName, "TYPE");
                JCExpression primitiveClass = tm.QualIdent(symbol);
                exps[i] = primitiveClass;
            } else {
                clazz = typeName + ".class";
                exps[i] = getIdAfterImporting(clazz);
            }
            i++;
        }
        return exps;
    }
}
