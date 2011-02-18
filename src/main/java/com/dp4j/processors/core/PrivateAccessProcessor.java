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
@SupportedAnnotationTypes(value = {"org.junit.Test", "com.dp4j.InjectReflection", "org.testng.annotations.Test"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PrivateAccessProcessor extends DProcessor {

    public Type getType(Symbol s) {
        Type t;
        if (s instanceof MethodSymbol) {
            t = ((MethodSymbol) s).getReturnType();
        } else {
            t = s.type;
        }
        return t;
    }

    @Override
    protected void processElement(Element e, TypeElement ann, boolean warningsOnly) {
        encClass = (TypeElement) e.getEnclosingElement();
        PackageElement packageOf = elementUtils.getPackageOf(e);
        List<? extends Element> pkgClasses = packageOf.getEnclosedElements();
        rs = new Resolver(elementUtils, trees, tm, encClass, typeUtils, symTable, pkgClasses);

        final JCMethodDecl tree = (JCMethodDecl) elementUtils.getTree(e);
        final TreePath treePath = trees.getPath(e);


        thisExp = tm.This((Type) encClass.asType());

        TypeElement superclass = getTypeElement(encClass.getSuperclass().toString());

        while (superclass != null) {
            TypeMirror superclass1 = superclass.getSuperclass();
            if (superclass1 == null) {
                break;
            }
            superclass = getTypeElement(superclass1.toString());
        }
        final CompilationUnitTree cut = treePath.getCompilationUnit();

        tree.body = processElement(tree.body, cut);
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

    protected JCBlock processElement(final JCBlock tree, final CompilationUnitTree cut) {
        for (JCStatement stmt : tree.stats) {
            tree.stats = processStmt(stmt, cut, tree);
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
    protected com.sun.tools.javac.util.List<JCStatement> processStmt(JCStatement stmt, final CompilationUnitTree cut, JCBlock encBlock) {
        if (stmt instanceof JCVariableDecl) {
            JCVariableDecl varDec = (JCVariableDecl) stmt;
            varDec.sym = (VarSymbol) rs.getSymbol(cut, stmt, null, varDec.name, null);
            boolean accessible = isAccessible(varDec.init, cut, stmt);
            if (!accessible) {
                ((JCVariableDecl) stmt).init = processCond(varDec.init, cut, stmt, encBlock);
                Symbol s = rs.getSymbol(varDec.init, cut, stmt);
                final Type t = getType(s);
                if (differentArg(t, varDec.sym.type)) {
                    varDec.init = tm.TypeCast(rs.getBoxedType(varDec.sym), varDec.init);
                }
            }
        } else if (stmt instanceof JCTry) {
            JCTry tryStmt = (JCTry) stmt;
            //make a copy of vars here, let him add what he wants but then we restore vars
            if (tryStmt.finalizer != null && tryStmt.finalizer.stats != null && !tryStmt.finalizer.stats.isEmpty()) {
                tryStmt.finalizer = processElement(tryStmt.finalizer, cut);
            }
            List<JCCatch> catchers = tryStmt.catchers;
            for (JCCatch jCCatch : catchers) {
                if (jCCatch.body != null && jCCatch.body.stats != null && !jCCatch.body.stats.isEmpty()) {
                    jCCatch.body = processElement(jCCatch.body, cut);
                }
            }
            if (tryStmt.body != null && tryStmt.body.stats != null && !tryStmt.body.stats.isEmpty()) {
                tryStmt.body = processElement(tryStmt.body, cut);
            }
        } else if (stmt instanceof JCIf) {
            JCIf ifStmt = (JCIf) stmt;
            ifStmt.cond = processCond(ifStmt.cond, cut, stmt, encBlock);
            if (ifStmt.thenpart instanceof JCBlock) {
                ifStmt.thenpart = processElement(((JCBlock) ifStmt.thenpart), cut);
            }
        } else if (stmt instanceof JCExpressionStatement) {
            JCExpressionStatement expStmt = (JCExpressionStatement) stmt;
            expStmt.expr = processCond(expStmt.expr, cut, stmt, encBlock);
        } else if (stmt instanceof JCBlock) {
            stmt = processElement((JCBlock) stmt, cut);
        } else if (stmt instanceof JCWhileLoop) {
            JCWhileLoop loop = (JCWhileLoop) stmt;
            loop.cond = processCond(loop.cond, cut, stmt, encBlock);
            loop.body = processElement((JCBlock) loop.body, cut);
        } else if (stmt instanceof JCForLoop) {
            JCForLoop loop = (JCForLoop) stmt;
            //FIXME: loop.cond might declare i
            loop.cond = processCond(loop.cond, cut, stmt, encBlock);
            loop.body = processElement((JCBlock) loop.body, cut);
        } else if (stmt instanceof JCDoWhileLoop) {
            JCDoWhileLoop loop = (JCDoWhileLoop) stmt;
            loop.cond = processCond(loop.cond, cut, stmt, encBlock);
            loop.body = processElement(((JCBlock) loop.body), cut);
        } else if (stmt instanceof JCEnhancedForLoop) {
            JCEnhancedForLoop loop = (JCEnhancedForLoop) stmt;
            boolean accessible = isAccessible(loop.expr, cut, stmt);
            if (!accessible) {
                loop.expr = processCond(loop.expr, cut, stmt, encBlock);
                loop.var.sym = (VarSymbol) rs.getSymbol(cut, loop.body, null, loop.var.name, null);
                final Symbol s = rs.getSymbol(loop.expr, cut, stmt);
                final Type t = getType(s);
                ArrayType arrayType = typeUtils.getArrayType(loop.var.sym.type);
                if (differentArg(t, (Type) arrayType)) {
                    loop.expr = tm.TypeCast((Type) arrayType, loop.expr);
                }
            }
            loop.body = processElement((JCBlock) loop.body, cut);
        }
        return encBlock.stats;
    }

    protected JCExpression processCond(JCExpression ifExp, final CompilationUnitTree cut, JCStatement stmt, JCBlock encBlock) {
        if (ifExp instanceof JCFieldAccess) {
            final JCFieldAccess fa = (JCFieldAccess) ifExp;
            Symbol s = rs.getSymbol(ifExp, cut, stmt);
            final boolean accessible = isAccessible(fa, cut, stmt);
            if (!accessible) {
                encBlock.stats = reflect(s, cut, encBlock.stats, stmt);
                final JCExpression accessor;
                if (s.isStatic()) {
                    accessor = tm.Literal("");
                } else {
                    accessor = fa.selected;
                }
                ifExp = getReflectedAccess(fa, cut, stmt, null, accessor);
                reflectionInjected = true;
            }
        } else if (ifExp instanceof JCMethodInvocation) {
            JCMethodInvocation mi = (JCMethodInvocation) ifExp;
            final MethodSymbol mSym = rs.getSymbol(mi, cut, stmt);
            if (!mi.args.isEmpty()) {
                for (JCExpression arg : mi.args) {
                    JCExpression newArg = processCond(arg, cut, stmt, encBlock);
                    if (!newArg.equals(arg)) {
                        mi.args = rs.injectBefore(arg, mi.args, true, newArg);
                    }
                }
            }
            final boolean accessible = isAccessible(mi, cut, stmt);
            if (!accessible) {
                ifExp.type = mSym.getReturnType();
                encBlock.stats = reflect(mSym, cut, encBlock.stats, stmt);
                JCExpression accessor = rs.getInvokationExp(mi, cut, stmt);
                ifExp = getReflectedAccess(mSym, cut, accessor, mi.args, stmt);
                methodInjected = true; //could be a source of bugs here. Not injected yet!
            } else {
                ifExp.type = mSym.getReturnType();
            }
        } else if (ifExp instanceof JCNewClass) {
            //TODO: it's a method too! handle similarly
            ifExp.type = ((JCNewClass) ifExp).type;
        } else if (ifExp instanceof JCTypeCast) {
            JCTypeCast cast = (JCTypeCast) ifExp;
            cast.expr = processCond(cast.expr, cut, stmt, encBlock);

        } else if (ifExp instanceof JCParens) {
            JCParens parensExp = (JCParens) ifExp;
            parensExp.expr = processCond(parensExp.expr, cut, stmt, encBlock);
            ifExp.type = parensExp.expr.type;
        } else if (ifExp instanceof JCLiteral) {
            ifExp.type = rs.getType((JCLiteral) ifExp);
        } else if (ifExp instanceof JCIdent) {
            Symbol symbol = rs.getSymbol(ifExp, cut, stmt);
            ifExp.type = symbol.type;
        } else if (ifExp instanceof JCBinary) {
            JCBinary ifB = (JCBinary) ifExp;
//            if (ifB.lhs instanceof JCFieldAccess) {
//                final JCFieldAccess fa = (JCFieldAccess) ifB.lhs;
            ifB.rhs = processCond(ifB.rhs, cut, stmt, encBlock);

            final boolean accessible = isAccessible(ifB.lhs, cut, stmt);
            if (!accessible) {
                ifB.lhs = processCond(ifB.lhs, cut, stmt, encBlock);
                Symbol s = rs.getSymbol(ifB.lhs, cut, stmt);
                final Type t = getType(s);
                if (differentArg(t, ifB.rhs.type)) {
                    ifB.lhs = tm.Parens(tm.TypeCast(rs.getBoxedType(ifB.rhs.type), ifB.lhs));
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
        } else if (ifExp instanceof JCAssign) {
            JCAssign assignExp = (JCAssign) ifExp;
            if (assignExp.rhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) assignExp.rhs;
                Symbol accessor = rs.getAccessor(fa, cut, stmt);
                Symbol s = rs.getSymbol(fa, cut, stmt);
                final boolean accessible = isAccessible(s, accessor, cut, stmt);
                if (!accessible) {
                    encBlock.stats = reflect(s, cut, encBlock.stats, stmt);
                    assignExp.rhs = cast(getReflectedAccess(fa, cut, stmt, null, fa.selected), rs.getBoxedType(s));
                    reflectionInjected = true;
                }
            }
            if (assignExp.lhs instanceof JCFieldAccess) {
                final JCFieldAccess fa = (JCFieldAccess) assignExp.lhs;
                final boolean accessible = isAccessible(fa, cut, stmt);
                if (!accessible) {
                    Symbol s = rs.getSymbol(fa, cut, stmt);
                    encBlock.stats = reflect(s, cut, encBlock.stats, stmt);
                    JCMethodInvocation reflectedFieldSetter = getReflectedFieldSetter(fa, assignExp.rhs, cut, stmt);
                    ifExp = reflectedFieldSetter;
                }
            }
        } else if (ifExp.type == null) {
//            ifExp.type = getType(ifExp, vars, cut, packageName, scope, stmt, args, varSyms);
        }
        return ifExp;
    }

    public boolean isAccessible(JCMethodInvocation mi, CompilationUnitTree cut, JCStatement stmt) {
        Symbol s = rs.getSymbol(mi, cut, stmt);
        Symbol accessor = rs.getInvokationTarget(mi, cut, stmt);
        return isAccessible(s, accessor, cut, stmt);
    }

    public boolean isAccessible(JCFieldAccess fa, CompilationUnitTree cut, JCStatement stmt) {
        Symbol s = rs.getSymbol(fa, cut, stmt);
        Symbol accessor = rs.getAccessor(fa, cut, stmt);
        return isAccessible(s, accessor, cut, stmt);
    }

    public boolean isAccessible(JCExpression exp, CompilationUnitTree cut, JCStatement stmt) {
        Symbol s = rs.getSymbol(exp, cut, stmt);
        Symbol accessor = null;
        if (exp instanceof JCFieldAccess) {
            accessor = rs.getAccessor((JCFieldAccess) exp, cut, stmt);
        } else if (exp instanceof JCMethodInvocation) {
            accessor = rs.getInvokationTarget((JCMethodInvocation) exp, cut, stmt);
        } else if (exp instanceof JCPrimitiveTypeTree) {
            return true;
        } else if (exp instanceof JCNewArray) {
            JCNewArray arr = (JCNewArray) exp;
            boolean accessible = true;
            if (arr.elems != null) {
                for (JCExpression el : arr.elems) {
                    accessible &= isAccessible(el, cut, stmt);
                    if (!accessible) {
                        break;
                    }
                }
            }
            return accessible;
        } else if (exp instanceof JCLiteral) {
            return true;
        } else if (exp instanceof JCParens) {
            return isAccessible(((JCParens) exp).expr, cut, stmt);
        } else if (exp instanceof JCTypeCast) {
            return isAccessible(((JCTypeCast) exp).expr, cut, stmt);
        } else if (exp instanceof JCNewClass) {
            accessor = rs.getSymbol(((JCNewClass) exp).clazz, cut, stmt); //retrieve the class symbol, as it's considered to be the accessor of the constructor
        } else if (exp instanceof JCBinary) {
            JCBinary bin = (JCBinary) exp;
            return isAccessible(bin.lhs, cut, stmt) && isAccessible(bin.rhs, cut, stmt);
        } else if (exp instanceof JCIdent) {
            if (((VarSymbol) s).isLocal()) {
                return true;
            }
            accessor = (Symbol) encClass;
        } else if (exp instanceof JCAssign) {
            JCAssign assign = (JCAssign) exp;
            return isAccessible(assign.lhs, cut, stmt) && isAccessible(assign.rhs, cut, stmt);
        }
        if (accessor == null || s == null) {
            throw new RuntimeException("is this accessible " + exp);
        }
        return isAccessible(s, accessor, cut, stmt);
    }

    public boolean isAccessible(Symbol s, final Symbol accessor, CompilationUnitTree cut, JCStatement stmt) {
        final DeclaredType itd;
        if (accessor instanceof MethodSymbol) {
            itd = (DeclaredType) ((MethodSymbol) accessor).getReturnType();
        } else {
            itd = (DeclaredType) accessor.type;
        }
        return trees.isAccessible(rs.getScope(cut, stmt), s, itd);
    }

    protected com.sun.tools.javac.util.List<JCStatement> reflect(Symbol s, final CompilationUnitTree cut, com.sun.tools.javac.util.List<JCStatement> stats, JCStatement stmt) {
        final java.util.List<? extends Symbol> params;
        if (s instanceof MethodSymbol) {
            params = ((MethodSymbol) s).params;
        } else {
            params = Collections.EMPTY_LIST;
        }
        return reflect(s, cut, stats, params, stmt);
    }

    public com.sun.tools.javac.util.List<JCStatement> reflect(Symbol symbol, CompilationUnitTree cut, com.sun.tools.javac.util.List<JCStatement> stats, List<? extends Symbol> params, JCStatement stmt) {
        ClassSymbol cs = (ClassSymbol) symbol.owner;
        JCIdent typeId = tm.Ident(cs.fullname); //"com.dp4j.samples.RPrivateArrayMethod"

        //getClass var
        MethodSymbol javaLangClassSym = (MethodSymbol) rs.getSymbol(elementUtils.getName(clazz), cs, cut, stmt);

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
            JCExpression mName = tm.Literal(symbol.name.toString());
            args = merge(Collections.singleton(mName), toList(types));
        } else {
            accesseeVarName = getFieldVar(symbol.name);
            getterName = elementUtils.getName("getDeclaredField");
            javaReflectMethField = getIdAfterImporting("java.lang.reflect.Field");
            args = com.sun.tools.javac.util.List.<JCExpression>of(tm.Literal(symbol.name.toString()));
        }

        Symbol fieldMethSym = rs.getSymbol(cut, stmt, null, accesseeVarName, null);
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

    JCMethodInvocation getReflectedAccess(JCFieldAccess fa, final CompilationUnitTree cut, JCStatement stmt, com.sun.tools.javac.util.List<JCExpression> args, JCExpression accessor) {
        final Symbol s = rs.getSymbol(fa, cut, stmt);
        return getReflectedAccess(s, cut, accessor, args, stmt);
    }

    /**
     *
     * @param s
     * @param cut
     * @param accessor assumed to be accessible. TODO: get rid of assumption!
     * @param args
     * @return
     */
    JCMethodInvocation getReflectedAccess(Symbol s, final CompilationUnitTree cut, JCExpression accessor, com.sun.tools.javac.util.List<JCExpression> args, JCStatement stmt) {
        final Name getterName;
        final JCIdent fieldMethId;
        if (s instanceof MethodSymbol) {
            fieldMethId = tm.Ident(getMethodVar(s.name));
            getterName = elementUtils.getName("invoke");
            if (((MethodSymbol) s).isVarArgs()) {
                int i = 0;
                VarSymbol last = ((MethodSymbol) s).params.last();
                Type varArgType = (Type) ((ArrayType) last.asType()).getComponentType();
                com.sun.tools.javac.util.List<JCExpression> reverse = args.reverse();
                for (JCExpression arg : reverse) {
                    Symbol argSym = rs.getSymbol(arg, cut, stmt);
                    Type type = getType(argSym);
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
            args = merge(Collections.singleton(accessor), args);
        } else {
            fieldMethId = tm.Ident(getFieldVar(s.name));
            getterName = elementUtils.getName("get"); //TODO: for type safety replace with primitive concatenation
            args = com.sun.tools.javac.util.List.<JCExpression>of(accessor);
        }

        final JCExpression getMethField = tm.Select(fieldMethId, getterName);
        JCMethodInvocation mi = tm.Apply(com.sun.tools.javac.util.List.<JCExpression>nil(), getMethField, args);
        reflectionInjected = true; //call this method to actually use it!
        return mi;
    }

    JCMethodInvocation getReflectedFieldSetter(JCFieldAccess fa, final JCExpression value, final CompilationUnitTree cut, JCStatement stmt) {
        final Name field = getFieldVar(fa.name);
        Symbol s = rs.getSymbol(value, cut, stmt);
        s = rs.getTypeSymbol(s);
        String typeName = s.name.toString();
        typeName = StringUtils.capitalize(typeName);
        JCMethodInvocation set = getMethodInvoc(field + ".set" + typeName, fa.selected, value);
        return set;
    }

    Name getClassVarName(Name className) {
        className = rs.getName(className);
        className = elementUtils.getName(StringUtils.uncapitalize(className.toString()));

        return elementUtils.getName(className + "Class");
    }

    Name getFieldVar(final Name objName) {
        return elementUtils.getName(objName + "Field");
    }

    Name getMethodVar(Name objName) {
        return elementUtils.getName(objName + "Method");
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

    JCParens cast(JCMethodInvocation reflectedAccess, Type t) {
        return tm.Parens(tm.TypeCast(t, reflectedAccess));
    }
}
