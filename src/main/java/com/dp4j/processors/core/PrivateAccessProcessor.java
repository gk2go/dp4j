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
import com.sun.tools.javac.util.Name;

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

    private void addVar(JCVariableDecl varDec, Collection<Symbol> varSyms) {
        final JCExpression ex;
        if (varDec.type == null) {
            ex = varDec.vartype;
        } else {
            ex = tm.Type(varDec.type);
        }
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
            addVar(var, varSyms);
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
        tree.body = processElement(tree.body, cut, varSyms);
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

    protected JCBlock processElement(final JCBlock tree, final CompilationUnitTree cut, Collection<Symbol> varSyms) {
        for (JCStatement stmt : tree.stats) {
            final Scope scope = getScope(cut, stmt);
            tree.stats = processStmt(stmt, cut, scope, varSyms, tree);
        }
        return tree;
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
    protected com.sun.tools.javac.util.List<JCStatement> processStmt(JCStatement stmt, final CompilationUnitTree cut, Scope scope, Collection<Symbol> varSyms, JCBlock encBlock) {
        if (stmt instanceof JCVariableDecl) {
            JCVariableDecl varDec = (JCVariableDecl) stmt;
            varDec.sym = (VarSymbol) rs.getSymbol(scope, null, varDec.name, null);
            boolean accessible = isAccessible(varDec.init, scope);
            if (!accessible) {
                ExpProcResult processCond = processCond(varDec.init, cut, scope, stmt, null, varSyms, encBlock);
                Symbol s = rs.getSymbol(varDec.init, scope);
                if (differentArg(s.type, varDec.sym.type)) {
                    varDec.init = tm.TypeCast(getBoxedType(varDec.type.tsym), processCond.exp);
                } else {
                    varDec.init = processCond.exp;
                }
                encBlock.stats = processCond.stats;
            }

        } else if (stmt instanceof JCTry) {
            JCTry tryStmt = (JCTry) stmt;
            //make a copy of vars here, let him add what he wants but then we restore vars
            Collection<Symbol> tmpSyms = new HashSet<Symbol>(varSyms);
            if (tryStmt.finalizer != null && tryStmt.finalizer.stats != null && !tryStmt.finalizer.stats.isEmpty()) {
                tryStmt.finalizer = processElement(tryStmt.finalizer, cut, tmpSyms);
            }
            List<JCCatch> catchers = tryStmt.catchers;
            for (JCCatch jCCatch : catchers) {
                if (jCCatch.body != null && jCCatch.body.stats != null && !jCCatch.body.stats.isEmpty()) {
                    addVar(jCCatch.param, tmpSyms);
                    jCCatch.body = processElement(jCCatch.body, cut, tmpSyms);
                }
            }
            if (tryStmt.body != null && tryStmt.body.stats != null && !tryStmt.body.stats.isEmpty()) {
                tryStmt.body = processElement(tryStmt.body, cut, tmpSyms);
            }
        } else if (stmt instanceof JCIf) {
            JCIf ifStmt = (JCIf) stmt;
            encBlock.stats = processCond(ifStmt.cond, cut, scope, stmt, null, varSyms, encBlock).stats;
            if (ifStmt.thenpart instanceof JCBlock) {
                ifStmt.thenpart = processElement(((JCBlock) ifStmt.thenpart), cut, varSyms);
            }
        } else if (stmt instanceof JCExpressionStatement) {
            JCExpressionStatement expStmt = (JCExpressionStatement) stmt;
            ExpProcResult result = processCond(expStmt.expr, cut, scope, stmt, null, varSyms, encBlock);
            encBlock.stats = result.stats;
            expStmt.expr = result.exp;
        } else if (stmt instanceof JCBlock) {
            stmt = processElement((JCBlock) stmt, cut, varSyms);
        } else if (stmt instanceof JCWhileLoop) {
            JCWhileLoop loop = (JCWhileLoop) stmt;
            encBlock.stats = processCond(loop.cond, cut, scope, stmt, null, varSyms, encBlock).stats;
            loop.body = processElement((JCBlock) loop.body, cut, varSyms);
        } else if (stmt instanceof JCForLoop) {
            JCForLoop loop = (JCForLoop) stmt;
            //FIXME: loop.cond might declare i
            encBlock.stats = processCond(loop.cond, cut, scope, stmt, null, varSyms, encBlock).stats;
            loop.body = processElement((JCBlock) loop.body, cut, varSyms);
        } else if (stmt instanceof JCDoWhileLoop) {
            JCDoWhileLoop loop = (JCDoWhileLoop) stmt;
            encBlock.stats = processCond(loop.cond, cut, scope, stmt, null, varSyms, encBlock).stats;
            loop.body = processElement(((JCBlock) loop.body), cut, varSyms);
        } else if (stmt instanceof JCEnhancedForLoop) {
            JCEnhancedForLoop loop = (JCEnhancedForLoop) stmt;
            Collection<Symbol> tmpSyms = new HashSet<Symbol>(varSyms);
            addVar(loop.var, tmpSyms);

            final ExpProcResult result = processCond(loop.expr, cut, scope, stmt, null, varSyms, encBlock);
            loop.expr = result.exp;
            encBlock.stats = result.stats;
            loop.body= processElement((JCBlock) loop.body, cut, tmpSyms);
        }
        return encBlock.stats;
    }

    protected ExpProcResult processCond(JCExpression ifExp, final CompilationUnitTree cut, com.sun.source.tree.Scope scope, JCStatement stmt, List<Type> args, Collection<Symbol> varSyms, JCBlock encBlock) {

        if (ifExp instanceof JCFieldAccess) {
            final JCFieldAccess fa = (JCFieldAccess) ifExp;
            Symbol s = rs.getSymbol(ifExp, scope);
            final boolean accessible = isAccessible(fa, scope);
            if (!accessible) {
                final ReflectedAccessResult reflectedAccess;
                encBlock.stats = reflect(s, scope, cut, encBlock.stats, stmt);
                Symbol accessor = rs.getAccessor(fa, scope);
                reflectedAccess = getReflectedAccess(fa, cut, scope, stmt, null, varSyms, fa.selected);
                ifExp = cast(reflectedAccess);
                ifExp.type = reflectedAccess.expType;
                reflectionInjected = true;
            }
        } else if (ifExp instanceof JCMethodInvocation) {
            JCMethodInvocation mi = (JCMethodInvocation) ifExp;
            final ListBuffer<JCExpression> lb = ListBuffer.lb();
            if (!mi.args.isEmpty()) {
                for (JCExpression arg : mi.args) {
                    ExpProcResult result = processCond(arg, cut, scope, null, null, varSyms, encBlock);
                    encBlock.stats = result.stats;
                    lb.append(result.exp);
                }
                mi.args = lb.toList();
            }
            final boolean accessible = isAccessible(mi, args, cut, scope, stmt, varSyms);
            final MethodSymbol mSym = rs.getSymbol(mi, scope);
            if (!accessible) {
                ifExp.type = mSym.getReturnType();
                encBlock.stats = reflect(mSym, scope, cut, encBlock.stats, stmt);
                Symbol accessor = rs.getInvokationTarget(mi, scope);
                //TODO transform accessor into JCExpression
                ReflectedAccessResult result = getReflectedAccess(mSym, cut, null, mi.args);
                methodInjected = true; //could be a source of bugs here. Not injected yet!
                mi.meth = result.exp;
            } else {
                ifExp.type = mSym.getReturnType();
            }
        } else if (ifExp instanceof JCNewClass) {
            //TODO: it's a method too! handle similarly
            ifExp.type = ((JCNewClass) ifExp).type;
        } else if (ifExp instanceof JCTypeCast) {
            JCTypeCast cast = (JCTypeCast) ifExp;
            ExpProcResult result = processCond(cast.expr, cut, scope, stmt, args, varSyms, encBlock);
            cast.expr = result.exp;
            encBlock.stats = result.stats;
            ifExp.type = cast.type;
        } else if (ifExp instanceof JCParens) {
            JCParens parensExp = (JCParens) ifExp;
            ExpProcResult result = processCond(parensExp.expr, cut, scope, stmt, args, varSyms, encBlock);
            parensExp.expr = result.exp;
            encBlock.stats = result.stats;
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
                    Symbol s = rs.getSymbol(fa, scope);
                    encBlock.stats = reflect(s, scope, cut, encBlock.stats, stmt);
                    ifB.lhs = cast(getReflectedAccess(fa, cut, scope, stmt, null, varSyms, fa.selected));
                }
            }
            if (ifB.rhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) ifB.rhs;
                final boolean accessible = isAccessible(fa, scope);
                if (!accessible) {
                    Symbol s = rs.getSymbol(fa, scope);
                    encBlock.stats = reflect(s, scope, cut, encBlock.stats, stmt);
                    Symbol accessor = rs.getAccessor(fa, scope);
                    ifB.rhs = cast(getReflectedAccess(fa, cut, scope, stmt, null, varSyms, fa.selected));
                    reflectionInjected = true;
                }
            }
        } else if (ifExp instanceof JCAssign) {
            JCAssign assignExp = (JCAssign) ifExp;
            if (assignExp.rhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) assignExp.rhs;
                Symbol accessor = rs.getAccessor(fa, scope);
                Symbol s = rs.getSymbol(fa, scope);
                final boolean accessible = isAccessible(s, scope, accessor);
                if (!accessible) {
                    encBlock.stats = reflect(s, scope, cut, encBlock.stats, stmt);
                    assignExp.rhs = cast(getReflectedAccess(fa, cut, scope, stmt, null, varSyms, fa.selected));
                    reflectionInjected = true;
                }
            }
            if (assignExp.lhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) assignExp.lhs;
                final boolean accessible = isAccessible(fa, scope);
                if (!accessible) {
                    Symbol s = rs.getSymbol(fa, scope);
                    encBlock.stats = reflect(s, scope, cut, encBlock.stats, stmt);
                    JCMethodInvocation reflectedFieldSetter = getReflectedFieldSetter(fa, assignExp.rhs, cut);
                    ifExp = reflectedFieldSetter;
                }
            }
        } else if (ifExp.type == null) {
//            ifExp.type = getType(ifExp, vars, cut, packageName, scope, stmt, args, varSyms);
        }
        return new ExpProcResult(encBlock.stats, ifExp, ifExp.type);
    }

    public boolean isAccessible(JCMethodInvocation mi, final List<Type> args, CompilationUnitTree cut, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
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
            accessor = rs.getSymbol(((JCNewClass) exp).clazz, scope); //retrieve the class symbol, as it's considered to be the accessor of the constructor
        } else if (exp instanceof JCBinary) {
            JCBinary bin = (JCBinary) exp;
            return isAccessible(bin.lhs, scope) && isAccessible(bin.rhs, scope);
        } else if (exp instanceof JCIdent) {
            accessor = (Symbol) encClass;
        } else if (exp instanceof JCAssign) {
            JCAssign assign = (JCAssign) exp;
            return isAccessible(assign.lhs, scope) && isAccessible(assign.rhs, scope);
        }
        if (accessor == null || s == null) {
            throw new RuntimeException("is this accessible " + exp);
        }
        return isAccessible(s, scope, accessor);
    }

    public boolean isAccessible(Symbol s, final Scope scope, final Symbol accessor) {
        final DeclaredType itd;
        if (accessor instanceof MethodSymbol) {
            itd = (DeclaredType) ((MethodSymbol) accessor).getReturnType();
        } else {
            itd = (DeclaredType) accessor.type;
        }
        return trees.isAccessible(scope, s, itd);
    }

    private Scope getScope(final CompilationUnitTree cut, JCTree tree) {
        if (tree == null) {
            throw new IllegalArgumentException("tree is " + tree);
        }
        final TreePath treePath = TreePath.getPath(cut, tree);

        try {
            com.sun.source.tree.Scope scope = trees.getScope(treePath);
            return scope;
        } catch (java.lang.Throwable ne) { //this occurs when the symbol is invalid (inaccessible)
            ne.printStackTrace();
        }
        throw new RuntimeException();
    }

    protected com.sun.tools.javac.util.List<JCStatement> reflect(Symbol s, final Scope scope, final CompilationUnitTree cut, com.sun.tools.javac.util.List<JCStatement> stats, JCStatement stmt) {
        final java.util.List<? extends Symbol> params;
        if (s instanceof MethodSymbol) {
            params = ((MethodSymbol) s).params;
        } else {
            params = Collections.EMPTY_LIST;
        }
        return reflect(s, cut, scope, stats, params, stmt);
    }

    public com.sun.tools.javac.util.List<JCStatement> reflect(Symbol symbol, CompilationUnitTree cut, Scope scope, com.sun.tools.javac.util.List<JCStatement> stats, List<? extends Symbol> params, JCStatement stmt) {
        ClassSymbol cs = (ClassSymbol) symbol.owner;
        JCIdent typeId = tm.Ident(cs.fullname); //"com.dp4j.samples.RPrivateArrayMethod"

        //getClass var
        MethodSymbol javaLangClassSym = (MethodSymbol) rs.getSymbol(elementUtils.getName(clazz), cs, scope);

        JCIdent javaLangClassId = tm.Ident(javaLangClassSym.getReturnType().tsym);
        //        Name classVarName = getClassVarName(className);
        JCExpression forNameAccessor = tm.Select(javaLangClassId, javaLangClassSym.name);
        JCExpression className = tm.Literal(typeId.toString());
        JCMethodInvocation classGetter = tm.Apply(com.sun.tools.javac.util.List.<JCExpression>nil(), forNameAccessor, com.sun.tools.javac.util.List.<JCExpression>of(className));
//        JCVariableDecl classDecl = tm.VarDef(tm.Modifiers(Flags.FINAL), classVarName, javaLangClassId, classGetter);

        JCExpression[] types = getTypes(params);
        final com.sun.tools.javac.util.List<JCExpression> args;

        final JCExpression javaReflectMethField;
        final Name getterName;
        final Name accesseeVarName;
        if (symbol instanceof MethodSymbol) {
            accesseeVarName = getMethodVar(symbol.name);
            getterName = elementUtils.getName("getDeclaredMethod");
            javaReflectMethField = getIdAfterImporting("java.lang.reflect.Method");
            args = merge(Collections.singleton(className), toList(types));
        } else {
            accesseeVarName = getFieldVar(symbol.name);
            getterName = elementUtils.getName("getDeclaredField");
            javaReflectMethField = getIdAfterImporting("java.lang.reflect.Field");
            args = com.sun.tools.javac.util.List.<JCExpression>of(tm.Literal(symbol.name.toString()));
        }

        Symbol fieldMethSym = rs.getSymbol(scope, null, accesseeVarName, null);
        if (fieldMethSym == null) {
            final JCExpression getMethField = tm.Select(classGetter, getterName);
            JCMethodInvocation mi = tm.Apply(com.sun.tools.javac.util.List.<JCExpression>nil(), getMethField, args);
            final JCVariableDecl refDecl = tm.VarDef(tm.Modifiers(Flags.FINAL), accesseeVarName, javaReflectMethField, mi);
            final JCMethodInvocation setAccInvoc = getMethodInvoc(accesseeVarName + ".setAccessible", true);
            JCStatement setAccessibleExec = tm.Exec(setAccInvoc); //should there be a dereflect / or just setinaccessible just after access? Would be better to set true only at access/set time and set-false after it. So reflect injects only one stmt while access/set 3
            JCStatement[] refStmts = new JCStatement[2];

            if (refDecl != null) {
                refStmts[0] = refDecl;
                refStmts[1] = setAccessibleExec;
            }
            stats = injectBefore(stmt, stats, refStmts);
            reflectionInjected = true;
        }
        return stats;
    }

    ReflectedAccessResult getReflectedAccess(JCFieldAccess fa, final CompilationUnitTree cut, Scope scope, JCStatement stmt, com.sun.tools.javac.util.List<JCExpression> args, Collection<Symbol> varSyms, JCExpression accessor) {
        final Symbol s = rs.getSymbol(fa, scope);
        return getReflectedAccess(s, cut, accessor, args);
    }

    ReflectedAccessResult getReflectedMethod(MethodSymbol s, JCMethodInvocation mi, final CompilationUnitTree cut) {
        if (s.isStatic()) {
            Object param = "";
        } else {
            Object param = mi.meth;
        }
        return null;
    }

    /**
     *
     * @param s
     * @param cut
     * @param accessor assumed to be accessible. TODO: get rid of assumption!
     * @param args
     * @return
     */
    ReflectedAccessResult getReflectedAccess(Symbol s, final CompilationUnitTree cut, JCExpression accessor, com.sun.tools.javac.util.List<JCExpression> args) {
        final Name getterName;
        final JCIdent fieldMethId;
        if (s instanceof MethodSymbol) {
            fieldMethId = tm.Ident(getMethodVar(s.name));
            getterName = elementUtils.getName("invoke");
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
            args = merge(Collections.singleton(accessor), args);
        } else {
            fieldMethId = tm.Ident(getFieldVar(s.name));
            getterName = elementUtils.getName("get"); //TODO: for type safety replace with primitive concatenation
            args = com.sun.tools.javac.util.List.<JCExpression>of(accessor);
        }

        final JCExpression getMethField = tm.Select(fieldMethId, getterName);
        JCMethodInvocation mi = tm.Apply(com.sun.tools.javac.util.List.<JCExpression>nil(), getMethField, args);
        reflectionInjected = true; //call this method to actually use it!
        return new ReflectedAccessResult(mi, getBoxedType(s)); //FIXME: null
    }

    JCMethodInvocation getReflectedFieldSetter(JCFieldAccess fa, final JCExpression value, final CompilationUnitTree cut) {
        final Name field = getFieldVar(fa.name);
        JCMethodInvocation set = getMethodInvoc(field + ".set", fa.selected, value); //TODO: would be better to use setInt, setDouble, etc.. based on type to compile-time check more
        return set;
    }

    private JCVariableDecl addClassVarIfNew(Map<String, JCExpression> vars, String className, Collection<Symbol> varSyms, Symbol s) {
        JCVariableDecl classDecl = null;
//        final Name clName = getClassVarName(className);
//        if (!vars.containsKey(clName)) {
//            VarSymbol classVar = new VarSymbol(Flags.FINAL, clName, , null);
//            classDecl = getVarDecl(clName, javaLangClass, javaLangClass + ".forName", clName);
//
//            addVar(vars, classDecl, varSyms);
//        }
        return classDecl;
    }

    private JCVariableDecl addFieldVarIfNeW(Map<String, JCExpression> vars, Name objName, final String clazz, Collection<Symbol> varSyms) {
        JCVariableDecl fieldDecl = null;
        final Name field = getFieldVar(objName);
        if (!vars.containsKey(field)) {
//            fieldDecl = getVarDecl(field, "java.lang.reflect.Field", clazz + ".getDeclaredField", objName);
            addVar(fieldDecl, varSyms);
        }
        return fieldDecl;
    }

    Name getClassVarName(Name className) {
        final int lastIndexOf = className.toString().lastIndexOf(".");
        if (lastIndexOf > -1) {
            className = elementUtils.getName(className.toString().substring(lastIndexOf));
            if (className.toString().startsWith(".")) {
                className = elementUtils.getName(className.toString().substring(1));
            }
        }
        className = elementUtils.getName(StringUtils.uncapitalize(className.toString()));

        return elementUtils.getName(className + "Class");
    }

    Name getFieldVar(final Name objName) {
        return elementUtils.getName(objName + "Field");
    }

    Name getMethodVar(Name objName) {
        return elementUtils.getName(objName + "Method");
    }

    private JCVariableDecl addMethodVarIfNeW(Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Symbol accessor, java.util.List<VarSymbol> params, final Symbol classSymbol, Collection<Symbol> varSyms) {
        JCVariableDecl meDecl = null;
//        final String methodVar = getMethodVar();
//        if (!vars.containsKey(methodVar)) {
//
//
//            JCIdent classId = tm.Ident(classSymbol);
//            JCExpression acccessor = tm.Select(classId, elementUtils.getName("getDeclaredMethod"));
//
//            JCExpression[] types = getTypes(params);
//            com.sun.tools.javac.util.List<JCExpression> args = merge(Collections.singleton(acccessor), toList(types));
//            JCMethodInvocation mi = tm.Apply(com.sun.tools.javac.util.List.<JCExpression>nil(), acccessor, args);
//            tm.VarDef(null, null, acccessor, acccessor);
//            meDecl = getVarDecl(methodVar, "java.lang.reflect.Method", clazz + ".getDeclaredMethod", objName, getTypes(params), vars, cut, packageName, scope, stmt, varSyms);
//            addVar(vars, meDecl, varSyms);
//        }
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
