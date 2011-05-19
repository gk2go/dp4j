package com.dp4j.processors.core;

import com.dp4j.Reflect;
import com.dp4j.ast.ExpNode;
import com.dp4j.ast.Node;
import com.dp4j.ast.Resolver;
import com.dp4j.ast.StmtNode;
import com.dp4j.processors.DProcessor;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.*;
import java.util.ArrayList;
import javax.lang.model.type.*;
import javax.tools.Diagnostic.Kind.*;
import org.apache.commons.lang.*;
import com.sun.tools.javac.util.Name;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.util.ListBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.Diagnostic.Kind;

/**
 *
 * @author simpatico
 */
@SupportedAnnotationTypes(value = {"org.junit.Test", "org.testng.annotations.Test", "com.dp4j.Reflect"})
public class PrivateAccessProcessor extends DProcessor {

    private boolean fieldInjected = false;

    private static String rAll = "ll";

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> supportedOptions = new HashSet<String>(super.getSupportedOptions());
        supportedOptions.add(rAll);
        return supportedOptions;
    }
    boolean reflectAll = false;
    boolean catchExceptions;
    JCMethodDecl methTree;

    @Override
    protected void processElement(Element e, TypeElement ann, boolean warningsOnly) {

        if (ann.getQualifiedName().toString().equals(Reflect.class.getCanonicalName())) {
            final Reflect reflect = e.getAnnotation(Reflect.class);
            catchExceptions = reflect.catchExceptions();
        } else {
            catchExceptions = false;
        }

        if (options.containsKey("conservative")) {
            if (!ann.getQualifiedName().toString().equals(Reflect.class.getCanonicalName())) {
                return;
            }
        }
        if (options.containsKey(rAll)) {
            reflectAll = true;
        }

        encClass = (TypeElement) e.getEnclosingElement();
        PackageElement packageOf = elementUtils.getPackageOf(e);
        List<? extends Element> pkgClasses = packageOf.getEnclosedElements();

        rs = new Resolver(elementUtils, trees, tm, encClass, typeUtils, symTable, pkgClasses);

        methTree = (JCMethodDecl) elementUtils.getTree(e);

        thisExp = tm.This((Type) encClass.asType());

        final TreePath treePath = trees.getPath(e);
        final CompilationUnitTree cut = treePath.getCompilationUnit();
        boolean reflectOnlyThis = false;
        if (!reflectAll) {
            if (ann.getQualifiedName().toString().equals(Reflect.class.getCanonicalName())) {
                final Reflect refAnn = e.getAnnotation(Reflect.class);
                reflectOnlyThis = refAnn.all();
            }
        }
        if (reflectOnlyThis) {
            reflectAll = true;
        }
        methTree.body = processElement(methTree.body, cut, methTree);
        if (reflectOnlyThis) {
            reflectOnlyThis = false;
            reflectAll = false;
        }

        final List<JCExpression> exps = new LinkedList<JCExpression>();

        if (reflectionInjected || methodInjected || constructorInjected || fieldInjected) {
            exps.add(getId("java.lang.ClassNotFoundException"));

            if (fieldInjected) {
                exps.add(getId("java.lang.NoSuchFieldException"));
                fieldInjected = false;
            }

            exps.add(getId("java.lang.IllegalAccessException"));

            if (constructorInjected || methodInjected) {
                exps.add(getId("java.lang.NoSuchMethodException"));
                exps.add(getId("java.lang.reflect.InvocationTargetException"));
                if (constructorInjected) {
                    exps.add(getId("java.lang.InstantiationException"));
                    constructorInjected = false;
                }
            }
            if (methodInjected) {
                exps.add(getId("java.lang.IllegalArgumentException"));
                methodInjected = false;
            }
            reflectionInjected = false;

            if (catchExceptions) {
            } else {
                for (JCExpression exp : exps) {
                    methTree.thrown = methTree.thrown.append(exp);
                }
            }
        }
        printVerbose(cut, e);
    }
    boolean constructorInjected = false;

    protected JCBlock processElement(final JCBlock tree, final CompilationUnitTree cut, Scope validScope) {
        if (tree == null) {
            return null;
        }
        for (StatementTree stmt : tree.stats) {
            validScope = getScope(stmt, cut, validScope);
            tree.stats = (com.sun.tools.javac.util.List<JCStatement>) processStmt(stmt, cut, tree, validScope);
            if (tree.stats.indexOf(stmt) < tree.stats.size() - 1) {
                validScope = trees.getScope(trees.getPath(cut, stmt));
            }
        }
        return tree;
    }

    private Scope getScope(Tree stmt, final CompilationUnitTree cut, Scope validScope) {
        if (stmt instanceof JCVariableDecl) {
            JCExpression exp = ((JCVariableDecl) stmt).init;
            ((JCVariableDecl) stmt).init = null;
            TreePath path = TreePath.getPath(cut, stmt);
            validScope = trees.getScope(path);
            ((JCVariableDecl) stmt).init = exp;
        } else if (stmt instanceof JCEnhancedForLoop) {
            return getScope(((JCEnhancedForLoop) stmt).var, cut, validScope);
        } else if (stmt instanceof JCForLoop) {
            return getScope(((JCForLoop) stmt).init.last(), cut, validScope);
        }
        return validScope;
    }

    protected JCBlock processElement(BlockTree tree, final CompilationUnitTree cut, Tree scopeTree) {
        if (tree == null) {
            return null;
        }
        boolean dummyInjected = false;
        Scope scope = null;
        if (scopeTree instanceof MethodTree) {
            if (!((MethodTree) scopeTree).getParameters().isEmpty()) { //work-around
                final JCMethodInvocation dummyMi = getMethodInvoc("System.out.print", StringUtils.EMPTY);
                final JCStatement dummyStmt = tm.Exec(dummyMi);
                final JCMethodDecl mt = (JCMethodDecl) scopeTree;
                mt.body.stats = injectBefore(mt.body.stats.head, mt.body.stats, dummyStmt);
                scopeTree = mt.body.stats.head;
                dummyInjected = true;
                final TreePath path = trees.getPath(cut, scopeTree);
                scope = trees.getScope(path);
                mt.body.stats = rs.injectBefore(mt.body.stats.head, mt.body.stats, true);
            }
        }

        if (!dummyInjected) {
            final TreePath path = trees.getPath(cut, scopeTree);
            scope = trees.getScope(path);
        }
        return processElement((JCBlock) tree, cut, scope);
    }

    protected BlockTree blockify(StatementTree stmt) {
        if (stmt == null) {
            return null;
        }
        if (stmt instanceof BlockTree) {
            return (BlockTree) stmt;
        }
        return tm.Block(0l, com.sun.tools.javac.util.List.of((JCStatement) stmt));
    }

    protected com.sun.tools.javac.util.List<? extends StatementTree> processStmt(StatementTree stmt, final CompilationUnitTree cut, JCBlock encBlock, Scope validScope) {
        validScope = getScope(stmt, cut, validScope);
        final StmtNode n = new StmtNode(validScope, stmt, cut, this);
        encBlock.stats = (com.sun.tools.javac.util.List<JCStatement>) processStmt(n, cut, encBlock);
        return encBlock.stats;
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
    protected com.sun.tools.javac.util.List<? extends StatementTree> processStmt(StmtNode n, final CompilationUnitTree cut, JCBlock encBlock) {
        final StatementTree stmt = n.actual;
        if (stmt instanceof JCVariableDecl) {
            JCVariableDecl varDec = (JCVariableDecl) stmt;
            final boolean accessible = isAccessible(varDec, cut, n);
            if (!accessible) {
                ((JCVariableDecl) stmt).init = processCond(varDec.init, cut, new ExpNode(varDec.init, n), encBlock);
                Symbol s = rs.getSymbol(varDec.init, cut, n);
                final Type t = rs.getType(s);
                varDec.sym = (VarSymbol) rs.getSymbol(cut, n, null, varDec.name, null);
                varDec.type = varDec.sym.type;
                if (varDec.init.type == null) {
                    varDec.init.type = varDec.sym.type;
                }
                if (differentArg(t, varDec.sym.type)) {
                    varDec.init = tm.TypeCast(rs.getBoxedType(varDec.sym), varDec.init);
                }
            }
        } else if (stmt instanceof JCTry) {
            JCTry tryStmt = (JCTry) stmt;
            if (tryStmt.body != null && tryStmt.body.stats != null && !tryStmt.body.stats.isEmpty()) {
                tryStmt.body = processElement(tryStmt.body, cut, n.scope);
            }
            List<JCCatch> catchers = tryStmt.catchers;
            for (JCCatch jCCatch : catchers) {
                if (jCCatch.body != null && jCCatch.body.stats != null && !jCCatch.body.stats.isEmpty()) {
                    jCCatch.body = processElement(jCCatch.body, cut, jCCatch.param);
                }
            }
            if (tryStmt.finalizer != null && tryStmt.finalizer.stats != null && !tryStmt.finalizer.stats.isEmpty()) {
                tryStmt.finalizer = processElement(tryStmt.finalizer, cut, n.scope);
            }

        } else if (stmt instanceof JCIf) {
            JCIf ifStmt = (JCIf) stmt;
            ifStmt.cond = processCond(ifStmt.cond, cut, n, encBlock);
            ifStmt.thenpart = (JCStatement) blockify(ifStmt.thenpart);
            ifStmt.thenpart = processElement((JCBlock) ifStmt.thenpart, cut, ifStmt.cond);
            ifStmt.elsepart = (JCStatement) blockify(ifStmt.elsepart);
            ifStmt.elsepart = processElement((JCBlock) ifStmt.elsepart, cut, ifStmt.cond);
        } else if (stmt instanceof JCExpressionStatement) {
            JCExpressionStatement expStmt = (JCExpressionStatement) stmt;
            expStmt.expr = processCond(expStmt.expr, cut, n, encBlock);
            String[] exceptions = {"java.lang.IllegalAccessException", "java.lang.IllegalArgumentException"};


            if (catchExceptions) {
                encBlock.stats = replace((JCStatement) stmt, encBlock.stats, tm.Try(getBlock(expStmt), getCatches(cut, n, exceptions), null));

            } else {
                throwExceptions(exceptions);
            }


        } else if (stmt instanceof JCBlock) {
            n.actual = processElement((JCBlock) stmt, cut, n.scope);
        } else if (stmt instanceof JCWhileLoop) {
            JCWhileLoop loop = (JCWhileLoop) stmt;
            loop.cond = processCond(loop.cond, cut, n, encBlock);
            loop.body = (JCStatement) blockify(loop.body);
            loop.body = processElement((JCBlock) loop.body, cut, n.scope);
        } else if (stmt instanceof JCForLoop) {
            JCForLoop loop = (JCForLoop) stmt;
            loop.cond = processCond(loop.cond, cut, n, encBlock);
            loop.body = (JCStatement) blockify(loop.body);
            loop.body = processElement((JCBlock) loop.body, cut, loop.cond);
        } else if (stmt instanceof JCDoWhileLoop) {
            JCDoWhileLoop loop = (JCDoWhileLoop) stmt;
            loop.cond = processCond(loop.cond, cut, n, encBlock);
            loop.body = (JCStatement) blockify(loop.body);
            loop.body = processElement(((JCBlock) loop.body), cut, n.scope);
        } else if (stmt instanceof JCEnhancedForLoop) {
            JCEnhancedForLoop loop = (JCEnhancedForLoop) stmt;
            boolean accessible = isAccessible(loop.expr, cut, n);
            if (!accessible) {
                loop.expr = processCond(loop.expr, cut, n, encBlock);
                loop.var.sym = (VarSymbol) rs.getSymbol(cut, n, null, loop.var.name, null);
                final Symbol s = rs.getSymbol(loop.expr, cut, n);
                final Type t = rs.getType(s);
                ArrayType arrayType = typeUtils.getArrayType(loop.var.sym.type);
                if (differentArg(t, (Type) arrayType)) {
                    loop.expr = tm.TypeCast((Type) arrayType, loop.expr);
                }
            }
            loop.body = (JCStatement) blockify(loop.body);
            loop.body = processElement((JCBlock) loop.body, cut, loop.expr);
        }
        return encBlock.stats;
    }

    protected JCExpression processCond(JCFieldAccess fa, final CompilationUnitTree cut, Node n, JCBlock encBlock) {
        final boolean accessible = isAccessible(fa, cut, n);
        if (accessible) {
            return fa;
        }

        Symbol s = rs.getSymbol(fa, cut, n);
        reflect(s, cut, n, null, encBlock);
        final JCExpression accessor;
        if (s.isStatic()) {
            accessor = tm.Literal(StringUtils.EMPTY);
        } else {
            accessor = fa.selected;
        }
        fieldInjected = true;
        reflectionInjected = true;
        return getReflectedAccess(fa, cut, n, null, accessor);
    }

    protected JCExpression processCond(JCMethodInvocation mi, final CompilationUnitTree cut, Node n, JCBlock encBlock) {
        final MethodSymbol mSym = rs.getSymbol(mi, cut, n);
        if (!mi.args.isEmpty()) {
            for (JCExpression arg : mi.args) {
                JCExpression newArg = processCond(arg, cut, n, encBlock);
                if (!newArg.equals(arg)) {
                    mi.args = replace(arg, mi.args, newArg);
                }
            }
        }
        Symbol accSym = rs.getInvokationTarget(mi, cut, n);
        final boolean accessible = isAccessible(mSym, accSym, cut, n);
        if (!accessible) {
            mi.type = mSym.getReturnType();
            reflect(mSym, cut, n, mi.args, encBlock);
            JCExpression accessor = rs.getInvokationExp(mi, cut, n);
            mi = getReflectedAccess(mSym, cut, accessor, mi.args, n);
            //FIXME: here add invoke exceptions
            methodInjected = true;
        } else {
            mi.type = mSym.getReturnType();
        }
        return mi;
    }

    protected JCExpression processCond(JCNewClass init, final CompilationUnitTree cut, Node n, JCBlock encBlock) {
        Symbol initSym = rs.getSymbol(init, cut, n);
        if (!init.args.isEmpty()) {
            for (JCExpression arg : init.args) {
                JCExpression newArg = processCond(arg, cut, n, encBlock);
                if (!newArg.equals(arg)) {
                    init.args = rs.injectBefore(arg, init.args, true, newArg);
                }
            }
        }
        final boolean accessible = isAccessible(initSym, initSym.enclClass(), cut, n);
        init.type = rs.getType(initSym);
        if (accessible) {
            return init;
        }

        reflect(initSym, cut, n, init.args, encBlock);
        constructorInjected = true;
        //FIXME: constructor exception
        return getReflectedAccess(initSym, cut, null, init.args, n);
    }

    protected JCExpression processCond(JCIdent ifExp, final CompilationUnitTree cut, Node n, JCBlock encBlock) {
        Symbol symbol = rs.getSymbol(ifExp, cut, n);
        ifExp.type = symbol.type;
        return ifExp;
    }

    protected JCExpression processCond(JCTypeCast cast, final CompilationUnitTree cut, Node n, JCBlock encBlock) {
        cast.expr = processCond(cast.expr, cut, n, encBlock);
        return cast;
    }

    protected JCExpression processCond(JCLiteral ifExp, final CompilationUnitTree cut, Node n, JCBlock encBlock) {
            ifExp.type = rs.getType(ifExp);
        return ifExp;
    }

    protected JCExpression processCond(JCBinary ifB, final CompilationUnitTree cut, Node n, JCBlock encBlock) {
//            if (ifB.lhs instanceof JCFieldAccess) {
//                final JCFieldAccess fa = (JCFieldAccess) ifB.lhs;
            ifB.rhs = processCond(ifB.rhs, cut, n, encBlock);

            final boolean accessible = isAccessible(ifB.lhs, cut, n);
            if (!accessible) {
                ifB.lhs = processCond(ifB.lhs, cut, n, encBlock);
                Symbol s = rs.getSymbol(ifB.lhs, cut, n);
                final Type t = rs.getType(s);
                if (!typeUtils.getNullType().equals(ifB.rhs.type)) {
                    if (differentArg(t, ifB.rhs.type)) {
                        ifB.lhs = tm.Parens(tm.TypeCast(rs.getBoxedType(ifB.rhs.type), ifB.lhs));
                    }
                }
            }
//            }

//            if (ifB.rhs instanceof JCFieldAccess) {
//                final JCFieldAccess fa = (JCFieldAccess) ifB.rhs;
//                final boolean accessible = isAccessible(fa, cut, stmt);
//                if (!accessible) {
//                    Symbol s = rs.getSymbol(fa, cut, stmt);
//                    encBlock.stats = reflect(s, cut, encBlock.stats, stmt);
//                    ifB.rhs = cast(getReflectedAccess(fa, cut, stmt, null, varSyms, fa.selected), rs.getBoxedType(s));
//                    reflectionInjected = true;
//                }
//            }
            return ifB;
    }

    protected JCExpression processCond(JCAssign assignExp, final CompilationUnitTree cut, Node n, JCBlock encBlock) {
            if (!isAccessible(assignExp.rhs, cut, n)) {
                final Type rhsTypeBeforeReflection = rs.getType(assignExp.rhs, cut, n);
                final JCMethodInvocation reflectedAccess = (JCMethodInvocation) processCond(assignExp.rhs, cut, n, encBlock);
                assignExp.rhs = cast(reflectedAccess, rhsTypeBeforeReflection);
            }
            if (assignExp.lhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) assignExp.lhs;
                final boolean accessible = isAccessible(fa, cut, n);
                if (!accessible) {
                    Symbol s = rs.getSymbol(fa, cut, n);
                    reflect(s, cut, n, null, encBlock);

                    JCMethodInvocation reflectedFieldSetter = getReflectedFieldSetter(fa, assignExp.rhs, cut, s.type.isPrimitive(), n, s.isStatic());
                    if (s.getModifiers().contains(Modifier.FINAL)) {
                        final JCExpression fieldMods = getId(getFieldVar(fa.name) + ".modifiers");

                        final int varMods = (int) (s.flags_field) & ~java.lang.reflect.Modifier.FINAL;
                        final JCLiteral mod = tm.Literal(varMods);
                        final JCAssign constChange = tm.Assign(fieldMods, mod);
                        JCExpressionStatement constChangeStmt = tm.Exec(constChange);
                        encBlock.stats = injectBefore((JCStatement) n.actual, encBlock.stats, constChangeStmt);
                        Scope validScope = trees.getScope(trees.getPath(cut, constChangeStmt));
                        encBlock.stats = (com.sun.tools.javac.util.List<JCStatement>) processStmt(constChangeStmt, cut, encBlock, validScope);
                    }
                    return reflectedFieldSetter;
                }
            }
            return assignExp;
    }

protected JCExpression processCond(JCArrayAccess ifExp, final CompilationUnitTree cut, Node n, JCBlock encBlock) {
     //TODO: reflect array
    return ifExp;
}

    public JCExpression processCond(JCExpression ifExp, final CompilationUnitTree cut, Node n, JCBlock encBlock) {
        if (ifExp instanceof JCFieldAccess) {
            return processCond((JCFieldAccess) ifExp, cut, n, encBlock);
        } else if (ifExp instanceof JCMethodInvocation) {
            return processCond((JCMethodInvocation) ifExp, cut, n, encBlock);
        } else if (ifExp instanceof JCNewClass) {
            return processCond((JCNewClass) ifExp, cut, n, encBlock);
        } else if (ifExp instanceof JCTypeCast) {
        } else if (ifExp instanceof JCParens) {
            return processCond((JCTypeCast) ifExp, cut, n, encBlock);
        } else if (ifExp instanceof JCLiteral) {
            return processCond((JCLiteral) ifExp, cut, n, encBlock);
        } else if (ifExp instanceof JCIdent) {
            return processCond((JCIdent) ifExp, cut, n, encBlock);
        } else if (ifExp instanceof JCBinary) {
            return processCond((JCBinary) ifExp, cut, n, encBlock);
        } else if (ifExp instanceof JCAssign) {
            return processCond((JCAssign) ifExp, cut, n, encBlock);
        } else if (ifExp instanceof JCArrayAccess) {
            return processCond((JCArrayAccess) ifExp, cut, n, encBlock);
        }
        return ifExp;
    }

    public boolean isAccessible(JCMethodInvocation mi, CompilationUnitTree cut, Node n) {
        Symbol s = rs.getSymbol(mi, cut, n);
        Symbol accessor = rs.getInvokationTarget(mi, cut, n);
        return isAccessible(s, accessor, cut, n);
    }

    public boolean isAccessible(JCFieldAccess fa, CompilationUnitTree cut, Node n) {
        Symbol s = rs.getSymbol(fa, cut, n);
        Symbol accessor = rs.getAccessor(fa, cut, n);
        return isAccessible(s, accessor, cut, n);
    }

    public boolean isAccessible(JCVariableDecl varDecl, CompilationUnitTree cut, Node n) {
        if (varDecl.init != null) {
            return isAccessible(varDecl.init, cut, n);
        }
        return true;//just a declaration is always accessible
    }

    public boolean isAccessible(JCExpression exp, CompilationUnitTree cut, Node n) {
        Symbol s = rs.getSymbol(exp, cut, n);
        if (s == null) {
            msgr.printMessage(Kind.ERROR, "could not find the symbol for " + exp);
        }
        Symbol accessor = null;
        if (exp instanceof JCFieldAccess) {
            accessor = rs.getAccessor((JCFieldAccess) exp, cut, n);
        } else if (exp instanceof JCMethodInvocation) {
            accessor = rs.getInvokationTarget((JCMethodInvocation) exp, cut, n);
        } else if (exp instanceof JCPrimitiveTypeTree) {
            return true;
        } else if (exp instanceof JCNewArray) {
            JCNewArray arr = (JCNewArray) exp;
            boolean accessible = true;
            if (arr.elems != null) {
                for (JCExpression el : arr.elems) {
                    accessible &= isAccessible(el, cut, n);
                    if (!accessible) {
                        break;
                    }
                }
            }
            return accessible;
        } else if (exp instanceof JCLiteral) {
            return true;
        } else if (exp instanceof JCParens) {
            return isAccessible(((JCParens) exp).expr, cut, n);
        } else if (exp instanceof JCTypeCast) {
            return isAccessible(((JCTypeCast) exp).expr, cut, n);
        } else if (exp instanceof JCNewClass) {
            accessor = rs.getSymbol(((JCNewClass) exp).clazz, cut, n); //retrieve the class symbol, as it's considered to be the accessor of the constructor
        } else if (exp instanceof JCBinary) {
            JCBinary bin = (JCBinary) exp;
            return isAccessible(bin.lhs, cut, n) && isAccessible(bin.rhs, cut, n);
        } else if (exp instanceof JCIdent) {
            if (((VarSymbol) s).isLocal()) {
                return true;
            }
            accessor = (Symbol) encClass;
        } else if (exp instanceof JCAssign) {
            JCAssign assign = (JCAssign) exp;
            return isAccessible(assign.lhs, cut, n) && isAccessible(assign.rhs, cut, n);
        } else if (exp instanceof JCArrayAccess) {
            if (((JCArrayAccess) exp).indexed instanceof JCFieldAccess) {
                accessor = rs.getAccessor((JCFieldAccess) ((JCArrayAccess) exp).indexed, cut, n);
            } else {
                return isAccessible(((JCArrayAccess) exp).indexed, cut, n);
            }
        }
        if (accessor == null || s == null) {
            throw new RuntimeException("is this accessible " + exp);
        }
        return isAccessible(s, accessor, cut, n);
    }

    public boolean isAccessible(Symbol s, final Symbol accessor, CompilationUnitTree cut, Node n) {
        if (reflectAll) {
            return false;
        }

        final DeclaredType itd;
        if (accessor instanceof MethodSymbol) {
            itd = (DeclaredType) ((MethodSymbol) accessor).getReturnType();
        } else {
            if (accessor.type instanceof ArrayType) {
                return rs.getSymbol(s.name, accessor, cut, n, false) != null;//FIXME: what about args? But we already have the symbol!
            } else {
                itd = (DeclaredType) accessor.type;
            }
        }
        return trees.isAccessible(n.scope, s, itd);
    }

    protected void reflect(Symbol s, final CompilationUnitTree cut, Node n, com.sun.tools.javac.util.List<JCExpression> args, JCBlock encBlock) {
        final java.util.List<? extends Symbol> params;
        final Name accesseeVarName;

        if (s instanceof MethodSymbol) {
            if (s.isConstructor()) {
                accesseeVarName = getConstructorVar(s.owner.name, ((MethodSymbol) s).params);
            } else {
                accesseeVarName = getMethodVar(s.name, ((MethodSymbol) s).params);
            }
            final com.sun.tools.javac.util.List<TypeSymbol> formalTypeParams = ((MethodSymbol) s).getTypeParameters();
            if (formalTypeParams.isEmpty()) {
                params = ((MethodSymbol) s).params;
            } else {
                params = rs.getArgs(args, cut, n);
            }
        } else {
            accesseeVarName = getFieldVar(s.name);
            params = Collections.EMPTY_LIST;
        }
        reflect(s, cut, params, n, encBlock, accesseeVarName);
    }

    public void reflect(Symbol symbol, CompilationUnitTree cut, List<? extends Symbol> params, Node n, JCBlock encBlock, Name accesseeVarName) {
        ClassSymbol cs = (ClassSymbol) symbol.owner;
        JCIdent typeId = tm.Ident(cs.fullname); //"com.dp4j.samples.RPrivateArrayMethod"

        //getClass var
        MethodSymbol javaLangClassSym = (MethodSymbol) rs.getSymbol(elementUtils.getName(clazz), cs, cut, n, false);

        JCIdent javaLangClassId = tm.Ident(javaLangClassSym.getReturnType().tsym);
        //        Name classVarName = getClassVarName(className);
        JCExpression forNameAccessor = tm.Select(javaLangClassId, javaLangClassSym.name);
        JCExpression className = tm.Literal(typeId.toString());
        JCMethodInvocation classGetter = tm.Apply(com.sun.tools.javac.util.List.<JCExpression>nil(), forNameAccessor, com.sun.tools.javac.util.List.<JCExpression>of(className));
//        JCVariableDecl classDecl = tm.VarDef(tm.Modifiers(Flags.FINAL), classVarName, javaLangClassId, classGetter);
        final List<String> exceptions = new LinkedList<String>();
        exceptions.add("java.lang.ClassNotFoundException"); //for forName

        JCExpression[] types = getTypes(params);
        final com.sun.tools.javac.util.List<JCExpression> args;

        final JCExpression javaReflectMethField;
        final Name getterName;
        if (symbol instanceof MethodSymbol) {

            if (symbol.isConstructor()) {
                getterName = elementUtils.getName("getDeclaredConstructor");
                javaReflectMethField = getIdAfterImporting("java.lang.reflect.Constructor");
                args = toList(types);
                exceptions.add("java.lang.InstantiationException");
                exceptions.add("java.lang.IllegalAccessException");
            } else {
                getterName = elementUtils.getName("getDeclaredMethod");
                javaReflectMethField = getIdAfterImporting("java.lang.reflect.Method");
                JCExpression mName = tm.Literal(symbol.name.toString());
                args = merge(Collections.singleton(mName), toList(types));
                exceptions.add("java.lang.NoSuchMethodException");
            }
        } else {
            getterName = elementUtils.getName("getDeclaredField");
            javaReflectMethField = getIdAfterImporting("java.lang.reflect.Field");
            args = com.sun.tools.javac.util.List.<JCExpression>of(tm.Literal(symbol.name.toString()));
            exceptions.add("java.lang.NoSuchFieldException");
        }

        Symbol fieldMethSym = rs.getSymbol(cut, n, null, accesseeVarName, null);
        if (fieldMethSym == null) {
            final JCExpression getMethField = tm.Select(classGetter, getterName);

            final JCVariableDecl refDecl = tm.VarDef(tm.Modifiers(0l), accesseeVarName, javaReflectMethField, getNull());

            final JCMethodInvocation mi = tm.Apply(com.sun.tools.javac.util.List.<JCExpression>nil(), getMethField, args);
            final JCStatement refDeclInit = tm.Exec(tm.Assign(tm.Ident(refDecl.name), mi)); //separate initialization from declaration to make it easier to catch exceptions

            final JCMethodInvocation setAccInvoc = getMethodInvoc(accesseeVarName + ".setAccessible", true);
            final JCStatement setAccessibleExec = tm.Exec(setAccInvoc); //should there be a dereflect / or just setinaccessible just after access? Would be better to set true only at access/set time and set-false after it. So reflect injects only one stmt while access/set 3

            final LinkedList<JCStatement> refStmts = new LinkedList<JCStatement>();

            if (refDecl != null) {
                refStmts.add(refDecl);
                if (catchExceptions) {
                    com.sun.tools.javac.util.List<JCCatch> exceptionsList = getCatches(cut, n, exceptions);
                    JCBlock throwingStmts = tm.Block(0l, com.sun.tools.javac.util.List.of(refDeclInit, setAccessibleExec));
                    refStmts.add(tm.Try(throwingStmts, exceptionsList, null));
                } else {
                    refStmts.add(refDeclInit);
                    refStmts.add(setAccessibleExec);
                    throwExceptions(exceptions);
                }
                encBlock.stats = injectBefore((JCStatement) n.actual, encBlock.stats, refStmts.toArray(new JCStatement[0]));

                TreePath refPath = trees.getPath(cut, refStmts.getLast());
                n.scope = getScope(n.actual, cut, trees.getScope(refPath));
            }

            reflectionInjected = true;
        }
    }

    private void throwExceptions(final List<String> exceptions) {
        throwExceptions(exceptions.toArray(new String[0]));
    }

    private void throwExceptions(final String... exceptions) {
        for (String exception : exceptions) {
            methTree.thrown = methTree.thrown.append(getId(exception));
        }
    }

    JCMethodInvocation getReflectedAccess(JCFieldAccess fa, final CompilationUnitTree cut, Node n, com.sun.tools.javac.util.List<JCExpression> args, JCExpression accessor) {
        if (fa.name.contentEquals(clazz)) {
            Symbol sym = rs.getSymbol(cut, n, null, elementUtils.getName(fa.selected.toString()), null);
            return rs.forName(sym, cut, n);
        }
        final Symbol s = rs.getSymbol(fa, cut, n);
        return getReflectedAccess(s, cut, accessor, args, n);
    }

    /**
     *
     * @param s
     * @param cut
     * @param accessor assumed to be accessible. TODO: get rid of assumption!
     * @param args
     * @return
     */
    JCMethodInvocation getReflectedAccess(Symbol s, final CompilationUnitTree cut, JCExpression accessor, com.sun.tools.javac.util.List<JCExpression> args, Node n) {
        final Name getterName;
        final JCIdent fieldMethInitId;
        if (s instanceof MethodSymbol) {
            if (s.isConstructor()) {
                getterName = elementUtils.getName("newInstance");
                fieldMethInitId = tm.Ident(getConstructorVar(s.owner.name, ((MethodSymbol) s).params));
            } else {
                getterName = elementUtils.getName("invoke");

                fieldMethInitId = tm.Ident(getMethodVar(s.name, ((MethodSymbol) s).params));
            }

            if (((MethodSymbol) s).isVarArgs()) {
                int i = 0;
                VarSymbol last = ((MethodSymbol) s).params.last();
                Type varArgType = (Type) ((ArrayType) last.asType()).getComponentType();
                com.sun.tools.javac.util.List<JCExpression> reverse = args.reverse();
                for (JCExpression arg : reverse) {
                    Symbol argSym = rs.getSymbol(arg, cut, n);
                    Type type = rs.getType(argSym);
                    if (differentArg(type, varArgType)) {
                        break;
                    } else {
                        i++;
                    }
                }
                final int varArgEnd = args.size();
                final int varArgIndex = varArgEnd - i;
                java.util.List<JCExpression> varArgs = args.subList(varArgIndex, varArgEnd);
                JCNewArray varArgArray = getArray(varArgType, varArgs);
                List<JCExpression> otherArgs = args.subList(0, varArgIndex);
                List<JCExpression> arrayList = new ArrayList<JCExpression>();
                arrayList.add(varArgArray);
                args = merge(otherArgs, arrayList);
            }
            if (args.size() > 0) {
                Type t = elementUtils.getTypeElement("java.lang.Object").type;
                args = com.sun.tools.javac.util.List.<JCExpression>of(getArray(t, args));
            }
            if (!s.isConstructor()) {
                args = merge(Collections.singleton(accessor), args);
            }
        } else {
            fieldMethInitId = tm.Ident(getFieldVar(s.name));
            final String typeName;
            if (s.type.isPrimitive()) {
                final Symbol ts = rs.getTypeSymbol(s);
                typeName = StringUtils.capitalize(ts.name.toString());
            } else {
                typeName = StringUtils.EMPTY;
            }
            getterName = elementUtils.getName("get" + typeName); //TODO: for type safety replace with primitive concatenation
            args = com.sun.tools.javac.util.List.<JCExpression>of(accessor);
        }

        final JCExpression getMethField = tm.Select(fieldMethInitId, getterName);
        JCMethodInvocation mi = tm.Apply(com.sun.tools.javac.util.List.<JCExpression>nil(), getMethField, args);
        reflectionInjected = true;
        return mi;
    }

    /**
     * TODO: refactor with getReflectedFieldGetter
     * @param fa
     * @param value
     * @param cut
     * @param primitiveField
     * @param n
     * @param staticField
     * @return
     */
    JCMethodInvocation getReflectedFieldSetter(JCFieldAccess fa, final JCExpression value, final CompilationUnitTree cut, boolean primitiveField, Node n, final boolean staticField) {
        final Name field = getFieldVar(fa.name);
        Symbol s = rs.getSymbol(value, cut, n);
        s = rs.getTypeSymbol(s);

        final String typeName;
        if (primitiveField) {
            typeName = StringUtils.capitalize(s.name.toString());
        } else {
            typeName = StringUtils.EMPTY;
        }

        final JCExpression accessor;
        if (staticField) {
            accessor = tm.Literal(StringUtils.EMPTY);
        } else {
            accessor = fa.selected;
        }
        JCMethodInvocation set = getMethodInvoc(field + ".set" + typeName, accessor, value);
        fieldInjected = true;
        return set;
    }

    Name getFieldVar(final Name objName) {
        return elementUtils.getName(objName + "Field");
    }

    Name getMethodVar(Name objName, List<? extends Symbol> params) {
        return getVar(objName, params, "Method");
    }

    Name getVar(Name objName, List<? extends Symbol> params, final String varType) {
        String with = params.isEmpty() ? StringUtils.EMPTY : "With";

        with += StringUtils.join(getNames(params), "And");
        return elementUtils.getName(objName + with + varType);
    }

    Name getConstructorVar(Name initName, List<? extends Symbol> params) {
        initName = rs.getName(initName);
        initName = elementUtils.getName(StringUtils.uncapitalize(initName.toString()));
        return getVar(initName, params, "Constructor");
    }

    List<Name> getNames(List<? extends Symbol> params) {
        List<Name> names = new ArrayList<Name>();
        for (Symbol symbol : params) {
            final Symbol ts = rs.getTypeSymbol(symbol);
            final Name n;
            if (ts instanceof ArrayType) {
                String toString = ((ArrayType) ts).getComponentType().toString();
                n = elementUtils.getName(toString + ts.getSimpleName());
            } else {
                n = ts.getSimpleName();
            }
            names.add(n);
        }
        return names;
    }
    boolean reflectionInjected = false;
    boolean methodInjected = false;

    /**
     * Junit or someone else might want to handle it
     * @return
     */
    @Override
    protected boolean onlyHandler(Set<? extends TypeElement> annotations) {
        if (annotations.size() == 1) {
            TypeElement next = annotations.iterator().next();
            if (next.getQualifiedName().toString().equals(Reflect.class.getCanonicalName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * One thing you can't do under any circumstance is cast from an object to a primitive data type, or vice versa.
     * http://www.informit.com/articles/article.aspx?p=30871&seqNum=5
     * @param reflectedAccess
     * @param t
     * @return
     */
    JCParens cast(JCMethodInvocation reflectedAccess, Type t) {
        return tm.Parens(tm.TypeCast(rs.getBoxedType(t), reflectedAccess));
    }
}
