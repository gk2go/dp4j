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
import com.sun.tools.javac.model.FilteredMemberList;
import com.sun.tools.javac.tree.JCTree;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.ListBuffer;
import java.util.*;
import java.util.ArrayList;
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

    public void addVar(Symbol v, Map<String, JCExpression> vars, Collection<Symbol> varSyms) {
        JCExpression id = getId(v.type.toString());
        id = id.setType(v.type);
        vars.put(v.toString(), id);
        varSyms.add(v);
    }

    public com.sun.tools.javac.util.List<JCStatement> reflect(Symbol symbol, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, com.sun.tools.javac.util.List<JCStatement> stats, Collection<Symbol> varSyms) {
        ClassSymbol cs = (ClassSymbol) symbol.owner;
        String objName = symbol.name.toString();
        String className = cs.fullname.toString();
        final JCVariableDecl classDecl = addClassVarIfNew(vars, className, varSyms);
        final String clazz = getClassVar(className);
        final JCVariableDecl refDecl;
        final String refVarName;
        if (symbol instanceof MethodSymbol) {
            MethodSymbol ms = (MethodSymbol) symbol;
            com.sun.tools.javac.util.List<VarSymbol> params = ms.params();
            refDecl = addMethodVarIfNeW(vars, cut, packageName, scope, stmt, objName, params, clazz, varSyms);
            refVarName = getMethodVar(objName);
        } else {
            refDecl = addFieldVarIfNeW(vars, objName, clazz, varSyms);
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

    protected com.sun.tools.javac.util.List<JCStatement> processElement(com.sun.tools.javac.util.List<JCStatement> stats, final JCTree tree, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, Collection<Symbol> varSyms) {
        com.sun.source.tree.Scope scope = getScope(cut, tree);
        return processElement(stats, scope, cut, packageName, vars, varSyms);
    }

    protected com.sun.tools.javac.util.List<JCStatement> processStmt(JCStatement stmt, Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, com.sun.tools.javac.util.List<JCStatement> stats, Collection<Symbol> varSyms) {
        if (stmt instanceof JCVariableDecl) {
            JCVariableDecl varDec = (JCVariableDecl) stmt;
            ExpProcResult processCond = processCond(varDec.init, vars, cut, packageName, scope, stats, stmt, null, varSyms);
            stats = processCond.stats;
            varDec.init = processCond.exp;
            addVar(vars, varDec, varSyms);
        } else if (stmt instanceof JCTry) {
            JCTry tryStmt = (JCTry) stmt;
            //make a copy of vars here, let him add what he wants but then we restore vars
            Map<String, JCExpression> tmpVars = new HashMap<String, JCExpression>(vars);
            Collection<Symbol> tmpSyms = new HashSet<Symbol>(varSyms);
            if (tryStmt.finalizer != null && tryStmt.finalizer.stats != null && !tryStmt.finalizer.stats.isEmpty()) {
                tryStmt.finalizer.stats = processElement(tryStmt.finalizer.stats, tryStmt, cut, packageName, tmpVars, tmpSyms);
            }
            List<JCCatch> catchers = tryStmt.catchers;
            for (JCCatch jCCatch : catchers) {
                if (jCCatch.body != null && jCCatch.body.stats != null && !jCCatch.body.stats.isEmpty()) {
                    addVar(tmpVars, jCCatch.param, tmpSyms);
                    jCCatch.body.stats = processElement(jCCatch.body.stats, jCCatch, cut, packageName, tmpVars, tmpSyms);
                }
            }
            if (tryStmt.body != null && tryStmt.body.stats != null && !tryStmt.body.stats.isEmpty()) {
                tryStmt.body.stats = processElement(tryStmt.body.stats, tryStmt, cut, packageName, vars, tmpSyms);
            }
        } else if (stmt instanceof JCIf) {
            JCIf ifStmt = (JCIf) stmt;
            stats = processCond(ifStmt.cond, vars, cut, packageName, scope, stats, stmt, null, varSyms).stats;
            if (ifStmt.thenpart instanceof JCBlock) {
                ((JCBlock) ifStmt.thenpart).stats = processElement(((JCBlock) ifStmt.thenpart).stats, ifStmt, cut, packageName, vars, varSyms);
            }
        } else if (stmt instanceof JCExpressionStatement) {
            JCExpressionStatement expStmt = (JCExpressionStatement) stmt;
            ExpProcResult result = processCond(expStmt.expr, vars, cut, packageName, scope, stats, stmt, null, varSyms);
            stats = result.stats;
            expStmt.expr = result.exp;
        } else if (stmt instanceof JCBlock) {
            ((JCBlock) stmt).stats = processElement(((JCBlock) stmt).stats, stmt, cut, packageName, vars, varSyms);
        } else if (stmt instanceof JCWhileLoop) {
            JCWhileLoop loop = (JCWhileLoop) stmt;
            stats = processCond(loop.cond, vars, cut, packageName, scope, stats, stmt, null, varSyms).stats;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars, varSyms);
        } else if (stmt instanceof JCForLoop) {
            JCForLoop loop = (JCForLoop) stmt;
            stats = processCond(loop.cond, vars, cut, packageName, scope, stats, stmt, null, varSyms).stats;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars, varSyms);
        } else if (stmt instanceof JCDoWhileLoop) {
            JCDoWhileLoop loop = (JCDoWhileLoop) stmt;
            stats = processCond(loop.cond, vars, cut, packageName, scope, stats, stmt, null, varSyms).stats;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars, varSyms);
        } else if (stmt instanceof JCEnhancedForLoop) {
            JCEnhancedForLoop loop = (JCEnhancedForLoop) stmt;
            stats = processCond(loop.expr, vars, cut, packageName, scope, stats, stmt, null, varSyms).stats;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars, varSyms);
        }
        return stats;
    }

    JCParens cast(ReflectedAccessResult reflectedAccess) {
        return tm.Parens(tm.TypeCast(reflectedAccess.expType, reflectedAccess.exp));
    }

    protected ExpProcResult processCond(JCExpression ifExp, Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, com.sun.tools.javac.util.List<JCStatement> stats, JCStatement stmt, List<Type> args, Collection<Symbol> varSyms) {
        if (ifExp instanceof JCBinary) {
            JCBinary ifB = (JCBinary) ifExp;
            if (ifB.lhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) ifB.lhs;
                final boolean accessible = isAccessible(fa, vars, cut, packageName, scope, stmt, args, varSyms);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt, args, varSyms);
                    ifB.lhs = cast(getReflectedAccess(fa, cut, packageName, vars, scope, stmt, args));
                    reflectionInjected = true;
                }
            }
            if (ifB.rhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) ifB.rhs;
                final boolean accessible = isAccessible(fa, vars, cut, packageName, scope, stmt, args, varSyms);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt, args, varSyms);
                    ifB.rhs = cast(getReflectedAccess(fa, cut, packageName, vars, scope, stmt, args));
                    reflectionInjected = true;
                }
            }
        } else if (ifExp instanceof JCFieldAccess) {
            final JCFieldAccess fa = (JCFieldAccess) ifExp;
            final boolean method = stmt instanceof JCExpressionStatement && ((JCExpressionStatement) stmt).expr instanceof JCMethodInvocation;
            final MethodSymbol meSym;
            if (method) {
                meSym = (MethodSymbol) getSymbol((JCMethodInvocation) ((JCExpressionStatement) stmt).expr, args, vars, cut, packageName, scope, stmt, varSyms);
                ifExp.type = meSym.getReturnType();
            } else {
                meSym = null;
                ifExp.type = getType(fa, vars, cut, packageName, scope, stmt, args, varSyms);
            }
            final boolean accessible;
            if (fa.selected instanceof JCMethodInvocation || !fa.selected.toString().contains(".class")) {
                accessible = isAccessible(fa, vars, cut, packageName, scope, stmt, args, varSyms);
                //FIXME: for accessibility must also check method parameters
            } else {
                accessible = isAccessible(fa.selected.toString(), scope, cut, packageName);
            }
            if (!accessible) {
                final ReflectedAccessResult reflectedAccess;
                if (method) {
                    stats = reflect(meSym, vars, cut, packageName, scope, stmt, stats, varSyms);
                    reflectedAccess = getReflectedAccess(meSym, fa.selected, cut, packageName, vars, stmt);
                    ifExp = reflectedAccess.exp;
                } else {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt, args, varSyms); //when ifExp
                    reflectedAccess = getReflectedAccess(fa, cut, packageName, vars, scope, stmt, args);
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
                final boolean accessible = isAccessible(fa, vars, cut, packageName, scope, stmt, args, varSyms);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt, args, varSyms);
                    assignExp.rhs = cast(getReflectedAccess(fa, cut, packageName, vars, scope, stmt, args));
                    reflectionInjected = true;
                }
            }
            if (assignExp.lhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) assignExp.lhs;
                final boolean accessible = isAccessible(fa, vars, cut, packageName, scope, stmt, args, varSyms);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt, args, varSyms);
                    JCMethodInvocation reflectedFieldSetter = getReflectedFieldSetter(fa, assignExp.rhs, cut, packageName, vars);
                    ifExp = reflectedFieldSetter;
                }
            }
        } else if (ifExp instanceof JCMethodInvocation) {
            JCMethodInvocation mi = (JCMethodInvocation) ifExp;
            final ListBuffer<JCExpression> lb = ListBuffer.lb();
            List<Type> tmpArgs = new ArrayList<Type>();
            if (!mi.args.isEmpty()) {
                for (JCExpression arg : mi.args) {
                    ExpProcResult result = processCond(arg, vars, cut, packageName, scope, stats, stmt, null, varSyms);
                    stats = result.stats;
                    lb.append(result.exp);
                    if (result.type != null) {
                        tmpArgs.add(result.type);
                    }
                }
                mi.args = lb.toList();
            }
            args = getArgs(mi, vars, cut, packageName, scope, stmt, varSyms);
            boolean accessible = isAccessible(mi, args, vars, cut, packageName, scope, stmt, varSyms);
            if (!accessible) {
                final ExpProcResult result = processCond(mi.meth, vars, cut, packageName, scope, stats, stmt, args, varSyms);
                if (result.exp instanceof JCParens || result.exp instanceof JCMethodInvocation) {
                    final JCMethodInvocation resMi;
                    if (result.exp instanceof JCParens) {
                        resMi = (JCMethodInvocation) ((JCTypeCast) ((JCParens) result.exp).expr).expr;
                    } else {
                        resMi = (JCMethodInvocation) result.exp;
                    }
                    resMi.args = merge(resMi.args, mi.args);
                    ifExp = result.exp;
                } else {
                    mi.meth = result.exp;
                }
                stats = result.stats;
            }
            ifExp.type = ((MethodSymbol) getSymbol(mi, args, vars, cut, packageName, scope, stmt, varSyms)).getReturnType();
        } else if (ifExp instanceof JCNewClass) {
            //TODO: it's a method too! handle similarly
            ifExp.type = ((JCNewClass) ifExp).type;
        } else if (ifExp instanceof JCTypeCast) {
            JCTypeCast cast = (JCTypeCast) ifExp;
            ExpProcResult result = processCond(cast.expr, vars, cut, packageName, scope, stats, stmt, args, varSyms);
            cast.expr = result.exp;
            stats = result.stats;
            ifExp.type = cast.type;
        } else if (ifExp instanceof JCParens) {
            JCParens parensExp = (JCParens) ifExp;
            ExpProcResult result = processCond(parensExp.expr, vars, cut, packageName, scope, stats, stmt, args, varSyms);
            parensExp.expr = result.exp;
            stats = result.stats;
            ifExp.type = parensExp.expr.type;
        } else if (ifExp instanceof JCLiteral) {
            ifExp.type = getType((JCLiteral) ifExp);
        } else if (ifExp instanceof JCIdent) {
            final String exp = ifExp.toString();
            final Symbol sym = contains(varSyms, exp, args, cut, packageName);
            if (sym == null) {
                JCExpression get = vars.get(exp);
                ifExp.type = get.type;
            } else {
                ifExp.type = sym.type;
            }
        }
        if(ifExp.type == null){
            ifExp.type = getType(ifExp, vars, cut, packageName, scope, stmt, args, varSyms);
        }
        return new ExpProcResult(stats, ifExp, ifExp.type);
    }

    private boolean isAccessible(String className, final com.sun.source.tree.Scope scope, final CompilationUnitTree cut, final Object packageName) {
        className = getQualifiedClassName(className, cut, packageName);
        DeclaredType declaredType = getDeclaredType(className);
        return trees.isAccessible(scope, (TypeElement) declaredType.asElement());
    }

    Symbol getSymbol(JCFieldAccess fa, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, com.sun.source.tree.Scope scope, JCStatement stmt, List<Type> args) {
        final String objName = fa.getIdentifier().toString();
        final String className = getClassNameOfAccessor(fa, vars, cut, packageName, scope, stmt, args);
        return getSymbol(className, objName);
    }

    public boolean isAccessible(JCMethodInvocation mi, final List<Type> args, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
        Symbol s = getSymbol(mi, args, vars, cut, packageName, scope, stmt, varSyms);
        String className = s.owner.getQualifiedName().toString();
        return isAccessible(scope, s, className);
    }

    private boolean isAccessible(final JCFieldAccess fa, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, List<Type> args, Collection<Symbol> varSyms) {
        final String idName = fa.getIdentifier().toString();
        String className;
        try {
            className = fa.getExpression().toString().replace("[]", "");
            return isAccessible(className, scope, idName);
        } catch (Exception e) {
            final JCExpression exp = fa.getExpression();
            className = exp.toString();
            if (exp instanceof JCFieldAccess && !fa.toString().endsWith(".class")) {
                className = getType((JCFieldAccess) exp, vars, cut, packageName, scope, stmt, args, varSyms).toString();
                return isAccessible(className, scope, idName) && isAccessible((JCFieldAccess) exp, vars, cut, packageName, scope, stmt, args, varSyms);
            } else {
                className = getClassNameOfAccessor(fa, vars, cut, packageName, scope, stmt, args);
            }
        }
        return isAccessible(getQualifiedClassName(className, cut, packageName), scope, idName);
    }

    private boolean isAccessible(final String className, final com.sun.source.tree.Scope scope, final String idName) {
        DeclaredType declaredType = getDeclaredType(className); //ordr matters to logic!
        if (idName.equals(clazz) && declaredType != null) {
            return true;
        }
        final Symbol s = getSymbol(className, idName);
        return isAccessible(scope, s, className);
    }

    public boolean isAccessible(final com.sun.source.tree.Scope scope, final Symbol s, final String className) {
        DeclaredType declaredType = getDeclaredType(className);
        return trees.isAccessible(scope, s, declaredType);
    }

    private com.sun.source.tree.Scope getScope(final CompilationUnitTree cut, JCTree tree) {
        if (tree == null) {
            throw new IllegalArgumentException("tree is " + tree);
        }
        final TreePath treePath = TreePath.getPath(cut, tree);

        com.sun.source.tree.Scope scope = trees.getScope(treePath);
        return scope;
    }

    ReflectedAccessResult getReflectedAccess(JCFieldAccess fa, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, com.sun.source.tree.Scope scope, JCStatement stmt, List<Type> args) {
        final String fieldAccessedName = fa.name.toString();
        final String className = getClassNameOfAccessor(fa, vars, cut, packageName, scope, stmt, args);

        final Symbol s = getSymbol(className, fieldAccessedName);
        return getReflectedAccess(s, fa.selected, cut, packageName, vars, stmt);
    }

    ReflectedAccessResult getReflectedAccess(Symbol s, JCExpression faSelected, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, JCStatement stmt) {
        final String fieldAccessedName = s.name.toString();
        final String field;
        final boolean isMethod = s instanceof MethodSymbol;
        final Object param;
        Type type;
        if (s.isStatic()) {
            param = "";
        } else {
            param = faSelected;
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

    protected com.sun.tools.javac.util.List<JCStatement> reflect(JCFieldAccess fa, final com.sun.source.tree.Scope scope, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, com.sun.tools.javac.util.List<JCStatement> stats, JCStatement stmt, List<Type> args, Collection<Symbol> varSyms) {
        JCMethodInvocation mi;
        Symbol symbol;
        try {
            mi = (JCMethodInvocation) ((JCExpressionStatement) stmt).expr;
            symbol = getSymbol(mi, args, vars, cut, packageName, scope, stmt, varSyms);
        } catch (ClassCastException ce) {
            symbol = getSymbol(fa, cut, packageName, vars, scope, stmt, args);
        }
        return reflect(symbol, vars, cut, packageName, scope, stmt, stats, varSyms);
    }

    private DeclaredType getDeclaredType(final String className) {
        final ClassSymbol typ = elementUtils.getTypeElement(className);
        return typeUtils.getDeclaredType(typ);
    }

    private JCVariableDecl addClassVarIfNew(Map<String, JCExpression> vars, String className, Collection<Symbol> varSyms) {
        JCVariableDecl classDecl = null;
        final String clazz = getClassVar(className);
        if (!vars.containsKey(clazz)) {
            classDecl = getVarDecl(clazz, javaLangClass, javaLangClass + ".forName", className);

            addVar(vars, classDecl, varSyms);
        }
        return classDecl;
    }

    String getClassVar(String className) {
        className = className.substring(className.lastIndexOf("."));
        if (className.startsWith(".")) {
            className = className.substring(1);
        }
        className = StringUtils.uncapitalize(className);

        return className + "Class";
    }

    String getFieldVar(final String objName) {
        return objName + "Field";
    }

    private JCVariableDecl addFieldVarIfNeW(Map<String, JCExpression> vars, String objName, final String clazz, Collection<Symbol> varSyms) {
        JCVariableDecl fieldDecl = null;
        final String field = getFieldVar(objName);
        if (!vars.containsKey(field)) {
            fieldDecl = getVarDecl(field, "java.lang.reflect.Field", clazz + ".getDeclaredField", objName);
            addVar(vars, fieldDecl, varSyms);
        }
        return fieldDecl;
    }

    private JCVariableDecl addMethodVarIfNeW(Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, String objName, List<VarSymbol> params, final String clazz, Collection<Symbol> varSyms) {
        JCVariableDecl meDecl = null;
        final String methodVar = getMethodVar(objName.toString());
        if (!vars.containsKey(methodVar)) {
            meDecl = getVarDecl(methodVar, "java.lang.reflect.Method", clazz + ".getDeclaredMethod", objName, getTypes(params), vars, cut, packageName, scope, stmt, varSyms);
            addVar(vars, meDecl, varSyms);
        }
        return meDecl;
    }

    private void addVar(Map<String, JCExpression> vars, JCVariableDecl varDec, Collection<Symbol> varSyms) {
        final JCExpression ex;
        if(varDec.type == null){
            ex = varDec.vartype;
        }else{
            ex = tm.Type(varDec.type);
        }
        vars.put(varDec.name.toString(), ex);
        if (varDec.sym != null) {
            varSyms.add(varDec.sym);
        }
    }
    boolean reflectionInjected = false;

    @Override
    protected void processElement(Element e, TypeElement ann, boolean warningsOnly) {
        final JCMethodDecl tree = (JCMethodDecl) elementUtils.getTree(e);
        final TreePath treePath = trees.getPath(e);

        Map<String, JCExpression> vars = new HashMap<String, JCExpression>();

        Set<Symbol> varSyms = new HashSet<Symbol>(); //TODO: eventually replace vars
        for (JCVariableDecl var : tree.params) {
            addVar(vars, var, varSyms);
            varSyms.add(var.sym);
        }
        encClass = (TypeElement) e.getEnclosingElement();

        FilteredMemberList allMembers = elementUtils.getAllMembers(encClass);

        for (Symbol symbol : allMembers) {
//            if (symbol instanceof VarSymbol) {
//                VarSymbol v = (VarSymbol) symbol;
            addVar(symbol, vars, varSyms);
//            }
        }
        com.sun.source.tree.Scope scope = trees.getScope(treePath);
        final CompilationUnitTree cut = treePath.getCompilationUnit();
        ExpressionTree packageName = cut.getPackageName();
        tree.body.stats = processElement(tree.body.stats, scope, cut, packageName, vars, varSyms);
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

    protected com.sun.tools.javac.util.List<JCStatement> processElement(com.sun.tools.javac.util.List<JCStatement> stats, final com.sun.source.tree.Scope scope, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, Collection<Symbol> varSyms) {
        for (JCStatement stmt : stats) {
            stats = processStmt(stmt, vars, cut, packageName, scope, stats, varSyms);
        }
        return stats;
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
//    private final static Set<ElementKind> variableKinds = Collections.unmodifiableSet(EnumSet.of(ElementKind.FIELD, ElementKind.ENUM_CONSTANT, ElementKind.PARAMETER, ElementKind.LOCAL_VARIABLE));
//
//    public Type getTypeOfVariable(com.sun.source.tree.Scope scope, String varName) {
//        for (Element e : scope.getLocalElements()) {
//            if (variableKinds.contains(e.getKind()) && e.getSimpleName().toString().equals(varName)) {
//
//            }
//        }
//        throw new NoSuchElementException("No variable " + varName + " in " + scope);
//    }
}
