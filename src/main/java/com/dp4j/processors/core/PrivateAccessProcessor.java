package com.dp4j.processors.core;

import com.dp4j.ast.Resolver;
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
import com.sun.source.tree.Scope;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.code.Type.WildcardType;
/**
 *
 * @author simpatico
 */
@SupportedAnnotationTypes(value = {"org.junit.Test", "com.dp4j.InjectReflection"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PrivateAccessProcessor extends DProcessor {

    public void addAll(Map<String, JCExpression> vars, Set<Symbol> varSyms, final TypeElement encClass) {
        FilteredMemberList allMembers = elementUtils.getAllMembers(encClass);
        for (Symbol symbol : allMembers) {
            addVar(symbol, vars, varSyms);
        }
    }

    public void addVar(Symbol v, Map<String, JCExpression> vars, Collection<Symbol> varSyms) {
        JCExpression id = getId(v.type.toString());
        id = id.setType(v.type);
        vars.put(v.toString(), id);
        varSyms.add(v);
    }

    private void addVar(Map<String, JCExpression> vars, JCVariableDecl varDec, Collection<Symbol> varSyms) {
        final JCExpression ex;
        if (varDec.type == null) {
            ex = varDec.vartype;
        } else {
            ex = tm.Type(varDec.type);
        }
        vars.put(varDec.name.toString(), ex);
        if (varDec.sym != null) {
            varSyms.add(varDec.sym);
        }
    }

    @Override
    protected void processElement(Element e, TypeElement ann, boolean warningsOnly) {
        encClass = (TypeElement) e.getEnclosingElement();
        PackageElement packageOf = elementUtils.getPackageOf(e);
        List<? extends Element> pkgClasses = packageOf.getEnclosedElements();
        rs = new Resolver(elementUtils, trees, tm, encClass, typeUtils, symTable, pkgClasses);

        final JCMethodDecl tree = (JCMethodDecl) elementUtils.getTree(e);
        final TreePath treePath = trees.getPath(e);

        Map<String, JCExpression> vars = new HashMap<String, JCExpression>();

        Set<Symbol> varSyms = new HashSet<Symbol>(); //TODO: eventually replace vars
        for (JCVariableDecl var : tree.params) {
            addVar(vars, var, varSyms);
            varSyms.add(var.sym);
        }

        thisExp = tm.This((Type) encClass.asType());

        addAll(vars, varSyms, encClass);

        TypeElement superclass = getTypeElement(encClass.getSuperclass().toString());

        while (superclass != null) {
            addAll(vars, varSyms, superclass);
            TypeMirror superclass1 = superclass.getSuperclass();
            if (superclass1 == null) {
                break;
            }
            superclass = getTypeElement(superclass1.toString());
        }

        com.sun.source.tree.Scope scope = trees.getScope(treePath);

        final CompilationUnitTree cut = treePath.getCompilationUnit();

        List<? extends ImportTree> imports = cut.getImports();
        for (ImportTree importTree : imports) {
            if (importTree.isStatic()) {
                Tree qualifiedIdentifier = importTree.getQualifiedIdentifier();
                String imported = qualifiedIdentifier.toString();
                String importedClassName;
                if (imported.contains("*")) {
                    importedClassName = imported.replace(".*", StringUtils.EMPTY);
                } else {
                    importedClassName = imported.substring(0, imported.lastIndexOf("."));
                }
                ClassSymbol cs = getTypeElement(importedClassName);
                List<Symbol> enclosedElements = cs.getEnclosedElements();
                for (Element element1 : enclosedElements) {
                    addVar((Symbol) element1, vars, varSyms);
                }
            }
        }
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

    protected com.sun.tools.javac.util.List<JCStatement> processElement(com.sun.tools.javac.util.List<JCStatement> stats, final JCTree tree, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, Collection<Symbol> varSyms) {
        Scope scope = getScope(cut, tree);
        return processElement(stats, scope, cut, packageName, vars, varSyms);
    }

    protected com.sun.tools.javac.util.List<JCStatement> processElement(com.sun.tools.javac.util.List<JCStatement> stats, Scope scope, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, Collection<Symbol> varSyms) {
        for (JCStatement stmt : stats) {
            scope = getScope(cut, stmt);
            stats = processStmt(stmt, vars, cut, packageName, scope, stats, varSyms);
        }
        return stats;
    }

    /**
     * @param stmt
     * @param vars
     * @param cut
     * @param packageName
     * @param scope
     * @param stats required for injecting reflection statements globally, and also when not possible locally, eg. if-expr
     * @param varSyms
     * @return
     */
    protected com.sun.tools.javac.util.List<JCStatement> processStmt(JCStatement stmt, Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, Scope scope, com.sun.tools.javac.util.List<JCStatement> stats, Collection<Symbol> varSyms) {
        if (stmt instanceof JCVariableDecl) {
            JCVariableDecl varDec = (JCVariableDecl) stmt;
            varDec.sym = (VarSymbol) rs.getSymbol(scope, null, varDec.name, null);
            boolean accessible = isAccessible(varDec.init, scope);
            if (!accessible) {
                ExpProcResult processCond = processCond(varDec.init, vars, cut, packageName, scope, stats, stmt, null, varSyms);
                addVar(vars, varDec, varSyms);
                Symbol s = rs.getSymbol(varDec.init, scope);
                if (differentArg(s.type, varDec.sym.type)) {
                    varDec.init = tm.TypeCast(getBoxedType(varDec.type.tsym), processCond.exp);
                } else {
                    varDec.init = processCond.exp;
                }
                stats = processCond.stats;
            }

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
            //FIXME: loop.cond might declare i
            stats = processCond(loop.cond, vars, cut, packageName, scope, stats, stmt, null, varSyms).stats;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars, varSyms);
        } else if (stmt instanceof JCDoWhileLoop) {
            JCDoWhileLoop loop = (JCDoWhileLoop) stmt;
            stats = processCond(loop.cond, vars, cut, packageName, scope, stats, stmt, null, varSyms).stats;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, vars, varSyms);
        } else if (stmt instanceof JCEnhancedForLoop) {
            JCEnhancedForLoop loop = (JCEnhancedForLoop) stmt;
            Map<String, JCExpression> tmpVars = new HashMap<String, JCExpression>(vars);
            Collection<Symbol> tmpSyms = new HashSet<Symbol>(varSyms);
            addVar(tmpVars, loop.var, tmpSyms);

            final ExpProcResult result = processCond(loop.expr, vars, cut, packageName, scope, stats, stmt, null, varSyms);
            loop.expr = result.exp;
            stats = result.stats;
            ((JCBlock) loop.body).stats = processElement(((JCBlock) loop.body).stats, stmt, cut, packageName, tmpVars, tmpSyms);
        }
        return stats;
    }

    protected ExpProcResult processCond(JCExpression ifExp, Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, com.sun.tools.javac.util.List<JCStatement> stats, JCStatement stmt, List<Type> args, Collection<Symbol> varSyms) {
        if (ifExp instanceof JCFieldAccess) {
            final JCFieldAccess fa = (JCFieldAccess) ifExp;
            final boolean accessible = isAccessible(fa, scope);
            if (!accessible) {
                final ReflectedAccessResult reflectedAccess;
                stats = reflect(fa, scope, cut, packageName, vars, stats, stmt, args, varSyms);
                reflectedAccess = getReflectedAccess(fa, cut, packageName, vars, scope, stmt, args, varSyms);
                ifExp = cast(reflectedAccess);
                ifExp.type = reflectedAccess.expType;
                reflectionInjected = true;
            }
        } else if (ifExp instanceof JCMethodInvocation) {
            JCMethodInvocation mi = (JCMethodInvocation) ifExp;
            final ListBuffer<JCExpression> lb = ListBuffer.lb();
            if (!mi.args.isEmpty()) {
                for (JCExpression arg : mi.args) {
                    ExpProcResult result = processCond(arg, vars, cut, packageName, scope, stats, null, null, varSyms);
                    stats = result.stats;
                    lb.append(result.exp);
                }
                mi.args = lb.toList();
            }
            final boolean accessible = isAccessible(mi, args, vars, cut, packageName, scope, stmt, varSyms);
            final MethodSymbol mSym = rs.getSymbol(mi, scope);
            if (!accessible) {
                ifExp.type = mSym.getReturnType();
                stats = reflect(mSym, vars, cut, packageName, scope, stmt, stats, varSyms);
                Symbol invokationTarget = rs.getInvokationTarget(mi, scope);
                ReflectedAccessResult result = getReflectedAccess(mSym, null, cut, packageName, vars);
                mi.meth = result.exp;
            } else {
                ifExp.type = mSym.getReturnType();
            }
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
            ifExp.type = rs.getType((JCLiteral) ifExp);
        } else if (ifExp instanceof JCIdent) {
            Symbol symbol = rs.getSymbol(ifExp, scope);
            ifExp.type = symbol.type;
        } else if (ifExp instanceof JCBinary) {
            JCBinary ifB = (JCBinary) ifExp;
            if (ifB.lhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) ifB.lhs;
                final boolean accessible = isAccessible(fa, scope);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt, args, varSyms);
                    ifB.lhs = cast(getReflectedAccess(fa, cut, packageName, vars, scope, stmt, args, varSyms));
                    reflectionInjected = true;
                }
            }
            if (ifB.rhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) ifB.rhs;
                final boolean accessible = isAccessible(fa, scope);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt, args, varSyms);
                    ifB.rhs = cast(getReflectedAccess(fa, cut, packageName, vars, scope, stmt, args, varSyms));
                    reflectionInjected = true;
                }
            }
        } else if (ifExp instanceof JCAssign) {
            JCAssign assignExp = (JCAssign) ifExp;
            if (assignExp.rhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) assignExp.rhs;
                final boolean accessible = isAccessible(fa, scope);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt, args, varSyms);
                    assignExp.rhs = cast(getReflectedAccess(fa, cut, packageName, vars, scope, stmt, args, varSyms));
                    reflectionInjected = true;
                }
            }
            if (assignExp.lhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) assignExp.lhs;
                final boolean accessible = isAccessible(fa, scope);
                if (!accessible) {
                    stats = reflect(fa, scope, cut, packageName, vars, stats, stmt, args, varSyms);
                    JCMethodInvocation reflectedFieldSetter = getReflectedFieldSetter(fa, assignExp.rhs, cut, packageName, vars);
                    ifExp = reflectedFieldSetter;
                }
            }
        } else if (ifExp.type == null) {
//            ifExp.type = getType(ifExp, vars, cut, packageName, scope, stmt, args, varSyms);
        }
        return new ExpProcResult(stats, ifExp, ifExp.type);
    }

    public boolean isAccessible(JCMethodInvocation mi, final List<Type> args, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
        Symbol s = rs.getSymbol(mi, scope);
        Symbol accessor = rs.getInvokationTarget(mi, scope);
        return isAccessible(s, scope, accessor);
    }

    public boolean isAccessible(JCFieldAccess fa, final Scope scope) {
        Symbol s = rs.getSymbol(fa, scope);
        Symbol accessor = rs.getAccessor(fa, scope);
        return isAccessible(s, scope, accessor);
    }

    public boolean isAccessible(JCExpression exp, final Scope scope) {
        Symbol s = rs.getSymbol(exp, scope);
        Symbol accessor = null;
        if (exp instanceof JCFieldAccess) {
            accessor = rs.getAccessor((JCFieldAccess) exp, scope);
        } else if (exp instanceof JCMethodInvocation) {
            accessor = rs.getInvokationTarget((JCMethodInvocation) exp, scope);
        } else if (exp instanceof JCPrimitiveTypeTree) {
            return true;
        } else if (exp instanceof JCNewArray) {
            JCNewArray arr = (JCNewArray) exp;
            boolean accessible = true;
            for (JCExpression el : arr.elems) {
                accessible &= isAccessible(el, scope);
                if (!accessible) {
                    break;
                }
            }
            return accessible;
        } else if (exp instanceof JCLiteral) {
            return true;
        } else if (exp instanceof JCParens) {
            return isAccessible(((JCParens) exp).expr, scope);
        } else if (exp instanceof JCTypeCast) {
            return isAccessible(((JCTypeCast) exp).expr, scope);
        } else if (exp instanceof JCNewClass) {
            accessor = rs.getSymbol(((JCNewClass)exp).clazz, scope); //retrieve the class symbol, as it's considered to be the accessor of the constructor
        } else if (exp instanceof JCBinary){
            JCBinary bin = (JCBinary) exp;
            return isAccessible(bin.lhs, scope) && isAccessible(bin.rhs, scope);
        } else if (exp instanceof JCIdent){
            accessor = (Symbol) encClass;

        }
        if (accessor == null || s == null) {
            throw new RuntimeException("is this accessible " + exp);
        }
        return isAccessible(s, scope, accessor);
    }

    public boolean isAccessible(Symbol s, final Scope scope, final Symbol accessor) {
        TypeElement it = (TypeElement) accessor.type.asElement();
        DeclaredType itd = typeUtils.getDeclaredType(it);
        return trees.isAccessible(scope, s, itd);
    }

    private Scope getScope(final CompilationUnitTree cut, JCTree tree) {
        if (tree == null) {
            throw new IllegalArgumentException("tree is " + tree);
        }
        final TreePath treePath = TreePath.getPath(cut, tree);

        com.sun.source.tree.Scope scope = trees.getScope(treePath);
        return scope;
    }

    protected com.sun.tools.javac.util.List<JCStatement> reflect(JCFieldAccess fa, final Scope scope, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, com.sun.tools.javac.util.List<JCStatement> stats, JCStatement stmt, List<Type> args, Collection<Symbol> varSyms) {
        JCMethodInvocation mi;
        Symbol symbol;
        try {
            mi = (JCMethodInvocation) ((JCExpressionStatement) stmt).expr;
            symbol = rs.getSymbol(mi, scope);
        } catch (ClassCastException ce) {
            symbol = rs.getSymbol(fa, scope);
        }
        return reflect(symbol, vars, cut, packageName, scope, stmt, stats, varSyms);
    }

    public com.sun.tools.javac.util.List<JCStatement> reflect(Symbol symbol, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, Scope scope, JCStatement stmt, com.sun.tools.javac.util.List<JCStatement> stats, Collection<Symbol> varSyms) {
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
            //FIXME: method here
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
        }
        return stats;
    }

    ReflectedAccessResult getReflectedAccess(JCFieldAccess fa, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars, Scope scope, JCStatement stmt, List<Type> args, Collection<Symbol> varSyms) {
        final Symbol s = rs.getSymbol(fa, scope);
        return getReflectedAccess(s, fa.selected, cut, packageName, vars);
    }

    ReflectedAccessResult getReflectedMethod(MethodSymbol s, JCMethodInvocation mi, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars) {
        if (s.isStatic()) {
            Object param = "";
        } else {
            Object param = mi.meth;
        }
        return null;
    }

    ReflectedAccessResult getReflectedAccess(Symbol s, JCExpression faSelected, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars) {
        final String fieldAccessedName = s.name.toString();
        final String field;
        final boolean isMethod = s instanceof MethodSymbol;
        final Object param;
        Type type;
        if (s.isStatic()) {
            param = "";
        } else {
            param = getId(s.owner.getQualifiedName());
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

//        final JCMethodInvocation resMi;
//        if (result.exp instanceof JCParens) {
//            resMi = (JCMethodInvocation) ((JCTypeCast) ((JCParens) result.exp).expr).expr;
//        } else {
//            resMi = (JCMethodInvocation) result.exp;
//        }
//        if (mi.args.size() == 1) {
//            mi.args = com.sun.tools.javac.util.List.<JCExpression>of(getArray("java.lang.Object", mi.args, cut));
//        }
//        com.sun.tools.javac.util.List<VarSymbol> params = mSym.params();
//        if (mSym.isVarArgs()) {
//            int i = 0;
//            VarSymbol last = params.last();
//            Type varArgType = (Type) ((ArrayType) last.asType()).getComponentType();
//            com.sun.tools.javac.util.List<JCExpression> reverse = mi.args.reverse();
//            for (JCExpression arg : reverse) {
//                Type type = getType(arg, vars, cut, packageName, scope, stmt, args, varSyms);
//                if (differentArg(type, varArgType)) {
//                    break;
//                } else {
//                    i++;
//                }
//            }
//            final int varArgEnd = mi.args.size();
//            final int varArgIndex = varArgEnd - i;
//            List<JCExpression> varArgs = mi.args.subList(varArgIndex, varArgEnd);
//            JCNewArray varArgArray = getArray(varArgType, varArgs);
//            List<JCExpression> otherArgs = mi.args.subList(0, varArgIndex);
//            List<JCExpression> arrayList = new ArrayList<JCExpression>();
//            arrayList.add(varArgArray);
//            mi.args = merge(otherArgs, arrayList);
//        }
//        resMi.args = merge(resMi.args, mi.args);
//        ifExp = result.exp;

        final JCExpression get = getRefMethodInvoc(methodCall, param);
        return new ReflectedAccessResult(get, type);
    }

    JCMethodInvocation getReflectedFieldSetter(JCFieldAccess fa, final JCExpression value, final CompilationUnitTree cut, Object packageName, Map<String, JCExpression> vars) {
        final String fieldAccessedName = fa.name.toString();
        final String field = getFieldVar(fieldAccessedName);
        JCMethodInvocation set = getMethodInvoc(field + ".set", fa.selected, value); //TODO: would be better to use setInt, setDouble, etc.. based on type to compile-time check more
        return set;
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

    private JCVariableDecl addFieldVarIfNeW(Map<String, JCExpression> vars, String objName, final String clazz, Collection<Symbol> varSyms) {
        JCVariableDecl fieldDecl = null;
        final String field = getFieldVar(objName);
        if (!vars.containsKey(field)) {
            fieldDecl = getVarDecl(field, "java.lang.reflect.Field", clazz + ".getDeclaredField", objName);
            addVar(vars, fieldDecl, varSyms);
        }
        return fieldDecl;
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

    String getMethodVar(String objName) {
        return objName + "Method";
    }

    private JCVariableDecl addMethodVarIfNeW(Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, String objName, java.util.List<VarSymbol> params, final String clazz, Collection<Symbol> varSyms) {
        JCVariableDecl meDecl = null;
        final String methodVar = getMethodVar(objName.toString());
        if (!vars.containsKey(methodVar)) {
            meDecl = getVarDecl(methodVar, "java.lang.reflect.Method", clazz + ".getDeclaredMethod", objName, getTypes(params), vars, cut, packageName, scope, stmt, varSyms);
            addVar(vars, meDecl, varSyms);
        }
        return meDecl;
    }
    boolean reflectionInjected = false;
    boolean methodInjected = false;

    /**
     * Junit or someone else might want to handle it
     * @return
     */
    @Override
    protected boolean onlyHandler(Set<? extends TypeElement> annotations) {
        return false;
    }

    JCParens cast(ReflectedAccessResult reflectedAccess) {
        return tm.Parens(tm.TypeCast(reflectedAccess.expType, reflectedAccess.exp));
    }
}
