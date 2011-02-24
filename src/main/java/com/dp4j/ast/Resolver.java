/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.ast;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Scope;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import javax.lang.model.type.*;
import javax.tools.Diagnostic.Kind.*;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree.*;
import javax.lang.model.element.*;
import com.sun.tools.javac.tree.TreeMaker;
import java.util.*;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.ListBuffer;
import java.lang.Iterable;
import java.util.ArrayList;
import javax.lang.model.util.Types;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author simpatico
 */
public class Resolver {

    final JavacElements elementUtils;
    final Trees trees;
    private final TreeMaker tm;
    private final TypeElement encClass;
    protected final Types typeUtils;
    protected final Symtab symTable;
    private final java.util.List<? extends Element> pkgClasses;
    public static final String init = "<init>";

    public Resolver(JavacElements elementUtils, final Trees trees, final TreeMaker tm, TypeElement encClass, final Types typeUtils, final Symtab symTable, final java.util.List<? extends Element> pkgClasses) {
        this.elementUtils = elementUtils;
        this.trees = trees;
        this.tm = tm;
        this.encClass = encClass;
        this.typeUtils = typeUtils;
        this.symTable = symTable;
        this.pkgClasses = pkgClasses;
    }

    public java.util.List<Symbol> getEnclosedElements(Symbol accessor, CompilationUnitTree cut, JCStatement stmt) {
        java.util.List<Symbol> enclosedElements = new ArrayList(accessor.getEnclosedElements());
        if (accessor.type instanceof ArrayType) {
            Symbol object = getSymbol(cut, stmt, null, elementUtils.getName("java.lang.Object"), null);
            enclosedElements.addAll(object.getEnclosedElements());
        }
        return enclosedElements;
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
        throw new RuntimeException(tree.toString());
    }

    public Scope getScope(final Element e) {
        final TreePath treePath = trees.getPath(e);
        Scope scope = trees.getScope(treePath);
        if(scope == null){
            throw new RuntimeException("null scope for " + e);
        }
        return scope;
    }

    public Symbol getSymbol(CompilationUnitTree cut, JCStatement stmt, List<JCExpression> typeParams, Name varName, List<JCExpression> args, final Element e) {
        final Scope scope = getScope(e);
        return getSymbol(cut, stmt, typeParams, varName, args, scope);
    }

    private Symbol getSymbol(CompilationUnitTree cut, JCStatement stmt, List<JCExpression> typeParams, Name varName, List<JCExpression> args, Scope scope) {
        java.util.List<Type> typeSyms = getArgTypes(typeParams, cut, stmt);
        java.util.List<Type> argsSyms = getArgTypes(args, cut, stmt);
        Symbol t = contains(scope, typeSyms, varName, argsSyms); //first lookup scope for all public identifiers
        TypeElement cl = scope.getEnclosingClass();
        while (t == null && cl != null) { //lookup hierarchy for inacessible identifiers too
            t = contains(elementUtils.getAllMembers(cl), typeSyms, varName, argsSyms);
            final TypeMirror superclass = cl.getSuperclass();
            if (superclass != null) {
                cl = (TypeElement) ((Type) superclass).asElement();
            }
        }
        return t;
    }

    public Symbol getSymbol(CompilationUnitTree cut, JCStatement stmt, List<JCExpression> typeParams, Name varName, List<JCExpression> args) {
        final Scope scope = getScope(cut, stmt);
        return getSymbol(cut, stmt, typeParams, varName, args, scope);
    }

    /**
     * scope is not needed
     * @param varName
     * @param accessor
     * @param cut
     * @param stmt
     * @return
     */
    public Symbol getSymbol(Name varName, Symbol accessor, CompilationUnitTree cut, JCStatement stmt) {
        if (varName.contentEquals("class")) {
            Symbol javaLangClassSym = getSymbol(cut, stmt, null, elementUtils.getName("java.lang.Class"), null);
            JCIdent id = tm.Ident(javaLangClassSym);
            JCExpression mName = tm.Select(id, elementUtils.getName("forName"));
            JCLiteral idLiteral = tm.Literal(accessor.toString());
            JCMethodInvocation mi = tm.Apply(List.<JCExpression>nil(), mName, List.<JCExpression>of(idLiteral));
            Symbol s = getSymbol(mi, cut, stmt);
            return s;
        }
        accessor = getTypeSymbol(accessor);
        java.util.List<Symbol> enclosedElements = getEnclosedElements(accessor, cut, stmt);
        Symbol s = contains(enclosedElements, null, varName, null);
        return s;
    }

    /**
     * cannot handle just like a fieldAccess? No, need to strip args and params
     * @param mi
     * @param scope
     * @return
     */
    public MethodSymbol getSymbol(final JCMethodInvocation mi, CompilationUnitTree cut, JCStatement stmt) {
        Symbol invTarget = getInvokationTarget(mi, cut, stmt);
        Name mName = getName(mi);
        java.util.List<Type> args = getArgTypes(mi.args, cut, stmt);
        java.util.List<Type> typeParams = getArgTypes(mi.typeargs, cut, stmt);
        if (invTarget instanceof VarSymbol) {//this, super,
            invTarget = invTarget.type.tsym;
        } else if (invTarget instanceof MethodSymbol) {
            invTarget = ((MethodSymbol) invTarget).getReturnType().tsym; //cannot invoke on void
        }
        java.util.List<Symbol> enclosedElements = getEnclosedElements(invTarget, cut, stmt);
        Symbol s = contains(enclosedElements, typeParams, mName, args);
        if (s != null) {
            return (MethodSymbol) s;
        }
        MethodSymbol ms = (MethodSymbol) contains(elementUtils.getAllMembers((TypeElement) invTarget), typeParams, mName, args);
        if (ms == null) {
            throw new NoSuchElementException(mi.toString());
        }
        return ms;
    }

    /**
     * cannot handle just like a fieldAccess? No, need to strip args and params
     * @param mi
     * @param scope
     * @return
     */
    public MethodSymbol getSymbol(final JCMethodInvocation mi, CompilationUnitTree cut, JCStatement stmt, Element e) {
        Symbol invTarget = getInvokationTarget(mi, cut, stmt, e);
        Name mName = getName(mi);
        java.util.List<Type> args = getArgTypes(mi.args, cut, stmt);
        java.util.List<Type> typeParams = getArgTypes(mi.typeargs, cut, stmt);
        if (invTarget instanceof VarSymbol) {//this, super,
            invTarget = invTarget.type.tsym;
        } else if (invTarget instanceof MethodSymbol) {
            invTarget = ((MethodSymbol) invTarget).getReturnType().tsym; //cannot invoke on void
        }
        java.util.List<Symbol> enclosedElements = getEnclosedElements(invTarget, cut, stmt);
        Symbol s = contains(enclosedElements, typeParams, mName, args);
        if (s != null) {
            return (MethodSymbol) s;
        }
        MethodSymbol ms = (MethodSymbol) contains(elementUtils.getAllMembers((TypeElement) invTarget), typeParams, mName, args);
        if (ms == null) {
            throw new NoSuchElementException(mi.toString());
        }
        return ms;
    }

    public Symbol getSymbol(JCExpression exp, CompilationUnitTree cut, JCStatement stmt, Element e) {
        if (exp instanceof JCIdent) {
            return getSymbol(cut, stmt, null, ((JCIdent) exp).name, null, e);
        } else if (exp instanceof JCFieldAccess) {
            Symbol symbol = getSymbol(cut, stmt, null, getName(exp.toString()), null, e);
            if (symbol != null) {
                return symbol;
            }
            Symbol acc = getAccessor((JCFieldAccess) exp, cut, stmt,e);
            return getSymbol(((JCFieldAccess) exp).name, acc, cut, stmt);
        } else if (exp instanceof JCNewClass) {
            final JCNewClass nc = (JCNewClass) exp;
            final Name name = getName(nc.clazz);
            TypeElement cl = (TypeElement) getSymbol(cut, stmt, null, name, null);//FIXME: null
            java.util.List<Type> args = getArgTypes(nc.args, cut, stmt);
            java.util.List<Type> typeParams = getArgTypes(nc.typeargs, cut, stmt);
            if(cl == null){
                System.out.println(name);
                System.out.println(exp);
                System.out.println(cut);
            }
            Symbol s = contains(cl.getEnclosedElements(), typeParams, elementUtils.getName(init), args);
            return s;
        } else if (exp instanceof JCMethodInvocation) {
            return getSymbol((JCMethodInvocation) exp, cut, stmt);
        } else if (exp instanceof JCLiteral) {
            return getType((JCLiteral) exp).tsym;
        } else if (exp instanceof JCNewArray) {
            JCNewArray arr = getTypedArray((JCNewArray) exp, cut, stmt);
            Symbol symbol = getSymbol(arr.elemtype, cut, stmt);
            ArrayType arrayType = typeUtils.getArrayType(symbol.type);
            ((Type) arrayType).tsym.type = (Type) arrayType;
            return ((Type) arrayType).tsym;
        } else if (exp instanceof JCArrayTypeTree) {
            JCArrayTypeTree arr = (JCArrayTypeTree) exp;
            Symbol s = getSymbol(arr.elemtype, cut, stmt);
            ArrayType arrayType = typeUtils.getArrayType(s.type);
            return ((Type) arrayType).tsym;
        } else if (exp instanceof JCParens) {
            return getSymbol(((JCParens) exp).expr, cut, stmt);
        } else if (exp instanceof JCTypeCast) {
            return getSymbol(((JCTypeCast) exp).expr, cut, stmt);
        } else if (exp instanceof JCBinary) {
            JCBinary bin = (JCBinary) exp;
            String op = bin.toString();
            op = StringUtils.remove(op, bin.lhs.toString());
            op = StringUtils.remove(op, bin.rhs.toString());
            if (op.contains("==") || op.contains("!=") || op.contains("&&") || op.contains("||") || op.contains(">") || op.contains("<") || op.contains(">=") || op.contains("<=")) {
                return symTable.booleanType.tsym;
            }
            Type type = getType(bin.lhs, cut, stmt);
            if (!typeUtils.getNullType().equals(type)) {
                return getSymbol(bin.lhs, cut, stmt);
            }
            return getSymbol(bin.rhs, cut, stmt);
        } else if (exp instanceof JCPrimitiveTypeTree) {
            if (exp.type != null) {
                return exp.type.tsym;
            }
            PrimitiveType primitiveType = typeUtils.getPrimitiveType(((JCPrimitiveTypeTree) exp).getPrimitiveTypeKind());
            exp.type = (Type) primitiveType;
            return getSymbol(exp, cut, stmt);
        } else if (exp instanceof JCArrayAccess) {
            Symbol s = getSymbol((((JCArrayAccess) exp).indexed), cut, stmt);
            return s;
        } else if (exp instanceof JCTypeApply) {
            return getSymbol(((JCTypeApply) exp).clazz, cut, stmt);
        }
        throw new RuntimeException(exp.toString());
    }

    public Symbol getSymbol(JCExpression exp, CompilationUnitTree cut, JCStatement stmt) {
        if (exp instanceof JCIdent) {
            return getSymbol(cut, stmt, null, ((JCIdent) exp).name, null);
        } else if (exp instanceof JCFieldAccess) {
            Symbol symbol = getSymbol(cut, stmt, null, getName(exp.toString()), null);
            if (symbol != null) {
                return symbol;
            }
            Symbol acc = getAccessor((JCFieldAccess) exp, cut, stmt, null);
            return getSymbol(((JCFieldAccess) exp).name, acc, cut, stmt);
        } else if (exp instanceof JCNewClass) {
            final JCNewClass nc = (JCNewClass) exp;
            final Name name = getName(nc.clazz);
            TypeElement cl = (TypeElement) getSymbol(cut, stmt, null, name, null);
            java.util.List<Type> args = getArgTypes(nc.args, cut, stmt);
            java.util.List<Type> typeParams = getArgTypes(nc.typeargs, cut, stmt);
            if(cl == null){
                System.out.println(name);
                System.out.println(exp);
                System.out.println(cut);
            }
            Symbol s = contains(cl.getEnclosedElements(), typeParams, elementUtils.getName(init), args);
            return s;
        } else if (exp instanceof JCMethodInvocation) {
            return getSymbol((JCMethodInvocation) exp, cut, stmt);
        } else if (exp instanceof JCLiteral) {
            return getType((JCLiteral) exp).tsym;
        } else if (exp instanceof JCNewArray) {
            JCNewArray arr = getTypedArray((JCNewArray) exp, cut, stmt);
            Symbol symbol = getSymbol(arr.elemtype, cut, stmt);
            ArrayType arrayType = typeUtils.getArrayType(symbol.type);
            ((Type) arrayType).tsym.type = (Type) arrayType;
            return ((Type) arrayType).tsym;
        } else if (exp instanceof JCArrayTypeTree) {
            JCArrayTypeTree arr = (JCArrayTypeTree) exp;
            Symbol s = getSymbol(arr.elemtype, cut, stmt);
            ArrayType arrayType = typeUtils.getArrayType(s.type);
            return ((Type) arrayType).tsym;
        } else if (exp instanceof JCParens) {
            return getSymbol(((JCParens) exp).expr, cut, stmt);
        } else if (exp instanceof JCTypeCast) {
            return getSymbol(((JCTypeCast) exp).expr, cut, stmt);
        } else if (exp instanceof JCBinary) {
            JCBinary bin = (JCBinary) exp;
            String op = bin.toString();
            op = StringUtils.remove(op, bin.lhs.toString());
            op = StringUtils.remove(op, bin.rhs.toString());
            if (op.contains("==") || op.contains("!=") || op.contains("&&") || op.contains("||") || op.contains(">") || op.contains("<") || op.contains(">=") || op.contains("<=")) {
                return symTable.booleanType.tsym;
            }
            Type type = getType(bin.lhs, cut, stmt);
            if (!typeUtils.getNullType().equals(type)) {
                return getSymbol(bin.lhs, cut, stmt);
            }
            return getSymbol(bin.rhs, cut, stmt);
        } else if (exp instanceof JCPrimitiveTypeTree) {
            if (exp.type != null) {
                return exp.type.tsym;
            }
            PrimitiveType primitiveType = typeUtils.getPrimitiveType(((JCPrimitiveTypeTree) exp).getPrimitiveTypeKind());
            exp.type = (Type) primitiveType;
            return getSymbol(exp, cut, stmt);
        } else if (exp instanceof JCArrayAccess) {
            Symbol s = getSymbol((((JCArrayAccess) exp).indexed), cut, stmt);
            return s;
        } else if (exp instanceof JCTypeApply) {
            return getSymbol(((JCTypeApply) exp).clazz, cut, stmt);
        }
        throw new RuntimeException(exp.toString());
    }


    public Symbol getAccessor(JCFieldAccess fa, CompilationUnitTree cut, JCStatement stmt, Element e) {
        if (fa.selected instanceof JCIdent) {
            Symbol accessor = getSymbol(cut, stmt, null, ((JCIdent) fa.selected).name, null, e);
            return accessor;
        }
        if (fa.selected instanceof JCFieldAccess) {
            Symbol accessor = getSymbol(cut, stmt, null, getName(fa.selected.toString()), null);
            if (accessor != null) {
                return accessor;
            }
            accessor = getAccessor((JCFieldAccess) fa.selected, cut, stmt, e);
            return getSymbol(((JCFieldAccess) fa.selected).name, accessor, cut, stmt);
        }
        if (fa.selected instanceof JCMethodInvocation) {
            MethodSymbol s = getSymbol((JCMethodInvocation) fa.selected, cut, stmt);
            Type returnType = s.getReturnType();
            return returnType.asElement();
        } else if (fa.selected instanceof JCArrayTypeTree) {
            JCArrayTypeTree arr = (JCArrayTypeTree) fa.selected;
            return getSymbol(arr.elemtype, cut, stmt);
        } else if (fa.selected instanceof JCArrayAccess) {
            Symbol s = getSymbol(((JCArrayAccess) fa.selected).indexed, cut, stmt);
            Type t;
            if (s.type instanceof ArrayType) {
                t = (Type) ((ArrayType) s.type).getComponentType();
                return t.tsym;
            } else {
                Symbol ts = getTypeSymbol(s);
                return ts;
            }

        }
        Symbol s = getSymbol(fa.selected, cut, stmt);
        Symbol ts = getTypeSymbol(s);
        if (ts == null) {
            throw new NoSuchElementException(fa.toString());
        }
        return ts;
    }

    public JCExpression getAccessor(JCFieldAccess fa) {
        return fa.selected;
    }

    public Type getType(JCLiteral ifExp) {
        final int typetag = (ifExp).typetag;
        final Object value = (ifExp).value;
        if (value == null) {
            return (Type) typeUtils.getNullType();
        }
        final JCLiteral Literal = tm.Literal(value);
        if (typetag == TypeTags.BOOLEAN) { //bug fix http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6504896
            Literal.setType(symTable.booleanType.constType(value));
        }
        return Literal.type;
    }

    public Type getType(JCExpression exp, CompilationUnitTree cut, JCStatement stmt) {
        if (exp instanceof JCLiteral) {
            return getType((JCLiteral) exp);
        } else {
            Symbol s = getSymbol(exp, cut, stmt);
            Symbol typeSymbol = getTypeSymbol(s);
            return typeSymbol.asType();
        }
    }

    public JCNewArray getTypedArray(JCNewArray arr, CompilationUnitTree cut, JCStatement stmt) {
        if (arr.elemtype == null) {
            Type type = null;
            if (arr.type == null) {
                for (JCExpression el : arr.elems) {
                    Type elType = getType(el, cut, stmt);
                    if (!typeUtils.getNullType().equals(elType)) {
                        type = elType;
                        break;
                    }
                }
                if (type == null) {
                    type = symTable.objectType;
                }
            } else {
                type = (Type) ((ArrayType) arr.type).getComponentType();
            }
            arr.elemtype = tm.Type(type);
        }
        arr.type = arr.elemtype.type;
        assert (arr.type != null);
        return arr;
    }

    public Name getName(final JCMethodInvocation mi) {
        return getName(mi.meth);
    }

    public Name getName(final JCExpression exp) {
        if (exp instanceof JCIdent) {
            return elementUtils.getName(exp.toString());
        }
        if (exp instanceof JCFieldAccess) {
            return ((JCFieldAccess) exp).name;
        } else if (exp instanceof JCNewClass) {
            return getName(init);
        } else if (exp instanceof JCMethodInvocation) {
            System.out.println(exp);
        } else if (exp instanceof JCTypeApply) {
            return getName(((JCTypeApply) exp).clazz);
        } else {
            return getName(exp.toString());
        }
        throw new NoSuchElementException(exp.toString());
    }

    public Symbol getInvokationTarget(JCMethodInvocation mi, CompilationUnitTree cut, JCStatement stmt) {
        if (mi.meth instanceof JCIdent) { //method name ==> invoked as member of enclosing class
            Symbol symbol = getSymbol(cut, stmt, mi.typeargs, getName(mi), mi.args);
            if (elementUtils.getAllMembers((TypeElement) encClass).contains(symbol)) {
                JCExpression thisExp = tm.This((Type) encClass.asType());
                return getSymbol(thisExp, cut, stmt);
            } else { //static import
                return symbol.owner;
            }
        }
        if (mi.meth instanceof JCFieldAccess) {
//            if(((JCFieldAccess)mi.meth).selected instanceof JCArrayAccess){
//                Symbol symbol = getSymbol(((JCFieldAccess)mi.meth).selected,cut,stmt);
//                return symbol;
//            }
            Symbol s = (Symbol) getAccessor((JCFieldAccess) mi.meth, cut, stmt, null);//FIXME
            return s;
        } else if (mi.meth instanceof JCNewClass) {
            final JCNewClass nc = (JCNewClass) mi.meth;
            final JCExpression clas = nc.clazz;
            Symbol symbol = getSymbol(cut, stmt, mi.typeargs, getName(clas), mi.args);
            return symbol;
        } else if (mi.meth instanceof JCMethodInvocation) {
            MethodSymbol symbol = getSymbol(mi, cut, stmt);
            final Type returnType = symbol.getReturnType();
            return returnType.tsym;
        }
        throw new NoSuchElementException(mi.toString());
    }

    public Symbol getInvokationTarget(JCMethodInvocation mi, CompilationUnitTree cut, JCStatement stmt, Element e) {
        if (mi.meth instanceof JCIdent) { //method name ==> invoked as member of enclosing class
            Symbol symbol = getSymbol(cut, stmt, mi.typeargs, getName(mi), mi.args, e);
            if (elementUtils.getAllMembers((TypeElement) encClass).contains(symbol)) {
                JCExpression thisExp = tm.This((Type) encClass.asType());
                return getSymbol(thisExp, cut, stmt);
            } else { //static import
                return symbol.owner;
            }
        }
        if (mi.meth instanceof JCFieldAccess) {
//            if(((JCFieldAccess)mi.meth).selected instanceof JCArrayAccess){
//                Symbol symbol = getSymbol(((JCFieldAccess)mi.meth).selected,cut,stmt);
//                return symbol;
//            }
            Symbol s = (Symbol) getAccessor((JCFieldAccess) mi.meth, cut, stmt,e);
            return s;
        } else if (mi.meth instanceof JCNewClass) {
            final JCNewClass nc = (JCNewClass) mi.meth;
            final JCExpression clas = nc.clazz;
            Symbol symbol = getSymbol(cut, stmt, mi.typeargs, getName(clas), mi.args);
            return symbol;
        } else if (mi.meth instanceof JCMethodInvocation) {
            MethodSymbol symbol = getSymbol(mi, cut, stmt);
            final Type returnType = symbol.getReturnType();
            return returnType.tsym;
        }
        throw new NoSuchElementException(mi.toString());
    }

    public JCExpression getInvokationExp(JCMethodInvocation mi, CompilationUnitTree cut, JCStatement stmt) {
        if (mi.meth instanceof JCIdent) { //method name ==> invoked as member of enclosing class
            Symbol symbol = getSymbol(cut, stmt, mi.typeargs, getName(mi), mi.args);
            try {
                if (typeUtils.asMemberOf((DeclaredType) encClass.asType(), symbol) != null) {
                    JCExpression thisExp = tm.This((Type) encClass.asType());
                    return thisExp;
                }
            } catch (IllegalArgumentException ie) {
                JCIdent staticImport = tm.Ident(symbol.owner);
                return staticImport;
            }
        }
        if (mi.meth instanceof JCFieldAccess) {
            final Symbol s = getSymbol(mi, cut, stmt);
            if (s.isStatic()) {
                return tm.Literal("");
            }
            JCExpression exp = getAccessor((JCFieldAccess) mi.meth);
            return exp;
        } else if (mi.meth instanceof JCNewClass) {
            return mi.meth;
        } else if (mi.meth instanceof JCMethodInvocation) {
            return mi.meth;
        }
        throw new RuntimeException(mi.toString() + " : error, what accessor is it?");
    }

    public Symbol getBoxedSymbol(Symbol primitive) {
        if (primitive.type.isPrimitive()) {
            primitive = (Symbol) typeUtils.boxedClass(primitive.type);
        }
        return primitive;
    }

    public boolean sameMethod(final List<? extends Symbol> formalArgs, java.util.List<? extends Type> args, final List<? extends Symbol> formalTypeParams, java.util.List<? extends Type> typeParams, final boolean varArgs) {
        if (!sameArgs(formalArgs, args, varArgs)) {
            return false;
        }
        if (!sameArgs(formalTypeParams, typeParams, varArgs)) {
            return false;
        }
        return true;
    }

    private Symbol contains(Scope scope, java.util.List<? extends Type> typeParams, Name varName, java.util.List<Type> args) {
        Symbol t = contains(pkgClasses, typeParams, varName, args);
        while (t == null && scope != null) {
            Iterable<? extends Element> localElements = scope.getLocalElements();
            t = contains(localElements, typeParams, varName, args);
            scope = scope.getEnclosingScope();
        }
        return t;
    }

    public Symbol contains(Iterable<? extends Element> list, java.util.List<? extends Type> typeParams, Name varName, java.util.List<? extends Type> args) {
        Collection<? extends Element> els = listgetSameNameEls(list, varName, args);

        for (Element e : els) {
            if (args != null) { //isEmpty means empty-args method
                final List<VarSymbol> formalArgs;
                List<TypeSymbol> formalTypeParams;
                final boolean varArgs;
                if (e.getKind().equals(ElementKind.METHOD) || e.getKind().equals(ElementKind.CONSTRUCTOR)) {
                    MethodSymbol me = (MethodSymbol) e;
                    formalArgs = me.getParameters();
                    formalTypeParams = me.getTypeParameters();
                    varArgs = me.isVarArgs();
                } else {
                    formalArgs = null;
                    formalTypeParams = null;
                    varArgs = false;
                }
                if (typeParams.isEmpty() && !formalTypeParams.isEmpty() && els.size() == 1) { //basic type inference, shouldn't also check same args-size? Yes, but varargs!
                    formalTypeParams = null;
                    typeParams = null;
                }
                if (!sameMethod(formalArgs, args, formalTypeParams, typeParams, varArgs)) {
                    continue;
                }
            }
            return (Symbol) e;
        }
        return null;
    }

    Collection<? extends Element> listgetSameNameEls(Iterable<? extends Element> list, Name varName, java.util.List<? extends Type> args) {
        Collection<Element> els = new ArrayList<Element>();
        for (Element e : list) {
            final Name elName;
            if (e instanceof ClassSymbol) {
                ClassSymbol ct = (ClassSymbol) e;
                elName = ct.getQualifiedName();
            } else {
                elName = (Name) e.getSimpleName();
            }
            if (elName.equals(varName) || e.getSimpleName().equals(varName)) {
                if (args != null) {//isEmpty means empty-args method
                    if (!e.getKind().equals(ElementKind.METHOD) && !e.getKind().equals(ElementKind.CONSTRUCTOR)) {
                        continue;
                    }
                }
                els.add(e);
            }
        }
        return els;
    }

    public Symbol getTypeSymbol(Symbol s) {

        if (!s.isConstructor() && !(s instanceof ClassSymbol)) {
//
//            Symbol ss =  (Symbol) typeUtils.asElement(s.type); //Problems with Integer.TYPE?
//            if(ss == null){
//                ss = (Symbol) typeUtils.asElement(s.erasure_field);
//            }
//            s = ss;
            if (s instanceof MethodSymbol) {
                Type returnType = ((MethodSymbol) s).getReturnType();
                if (returnType instanceof ArrayType) {
                    returnType = (Type) ((ArrayType) returnType).getComponentType();
                }
                return returnType.tsym;
            }
            if (s instanceof VarSymbol && s.name.toString().equals("TYPE")) {
                return s.type.tsym;
            }
            s = s.type.tsym;
        }
        if (s.isConstructor()) {
            return s.enclClass();
        }
        return s;
    }

    public Type getType(Symbol s) {

        if (!s.isConstructor() && !(s instanceof ClassSymbol)) {
//
//            Symbol ss =  (Symbol) typeUtils.asElement(s.type); //Problems with Integer.TYPE?
//            if(ss == null){
//                ss = (Symbol) typeUtils.asElement(s.erasure_field);
//            }
//            s = ss;
            if (s instanceof MethodSymbol) {
                return ((MethodSymbol) s).getReturnType();
            }
            if (s instanceof VarSymbol && s.name.toString().equals("TYPE")) {
                return s.type;
            }
            return s.type;
        }
        if (s.isConstructor()) {
            return s.enclClass().type;
        }
        return s.type;
    }

    public java.util.List<Symbol> getArgs(List<JCExpression> args, CompilationUnitTree cut, JCStatement stmt) {
        if (args == null) {
            return null;
        }
        java.util.List<Symbol> syms = new ArrayList<Symbol>();
        for (JCExpression arg : args) {
            Symbol s = getSymbol(arg, cut, stmt);
            s = getTypeSymbol(s);
            syms.add(s);
        }
        return syms;
    }

    public java.util.List<Type> getArgTypes(List<JCExpression> args, CompilationUnitTree cut, JCStatement stmt) {
        if (args == null) {
            return null;
        }
        java.util.List<Type> syms = new ArrayList<Type>();
        for (JCExpression arg : args) {
            Symbol s = getSymbol(arg, cut, stmt);
            Type t = getType(s);
            if(arg instanceof JCArrayAccess){
                t = (Type) ((ArrayType)t).getComponentType();
            }
            syms.add(t);
        }
        return syms;
    }

    private boolean sameArgs(List<? extends Symbol> formal, java.util.List<? extends Type> actual, final boolean varArgs) {
        if (formal == null || actual == null) {
            if (formal == null && actual == null) {
                return true;
            } else {
                if (actual == null) {
                    return sameArgs(formal, Collections.EMPTY_LIST, varArgs);
                }
            }
        }
        if (formal.isEmpty() && actual.isEmpty()) {
            return sameArgs(null, null, varArgs);
        }
        /**
         * same number of arguments, or vararg and:
         *  - vararg is not passed
         *  - several varargs are passed
         */
        if (formal.size() == actual.size() || varArgs && ((formal.size() == actual.size() + 1) || actual.size() > formal.size())) {
            if (varArgs) {
                Symbol varArg = formal.last();
                Symbol singleVarArg = ((Type) ((ArrayType) varArg.type).getComponentType()).tsym;
                final int varArgsNo = actual.size() - formal.size();
                if (actual.size() > formal.size()) { //several varargs are passed
                    Symbol[] flattenedVarArgs = new Symbol[varArgsNo + 1];
                    for (int i = 0; i < varArgsNo + 1; i++) {
                        flattenedVarArgs[i] = singleVarArg;
                    }
                    formal = injectBefore(varArg, formal, true, flattenedVarArgs);
                } else if (varArgsNo == 0) {
                    final Type lastArg = actual.get(actual.size() - 1);
                    if (!(lastArg instanceof ArrayType)) {////should be treated as single component
                        formal = injectBefore(varArg, formal, true, singleVarArg);
                    }//assuming it's treated as an array
                } else if (actual.isEmpty() || varArgsNo < 0) {
                    formal = injectBefore(varArg, formal, true, new Symbol[]{});//remove vararg
                }
                return sameArgs(formal, actual, false);
            }

            assert (formal.size() == actual.size());
            int i = 0;
            for (Type arg : actual) {
                Symbol ts = formal.get(i);
                final Type formalType = ts.type;
                if (!sameArg(arg, formalType)) {
                    return false;
                }
                i++;
            }
            return true;
        }
        return false;
    }
    public static final String dot = ".";

    public boolean sameArg(Type actual, Type formal) {
        final Type erArg = (Type) typeUtils.erasure(actual);
        final Type erVar = (Type) typeUtils.erasure(formal);
        boolean sameVar = erVar.equals(formal);
        boolean sameErArg = erArg.equals(actual);
        if ((!sameErArg || !sameVar)) {
            return sameArg(erArg, erVar);
        }
        if (formal.isPrimitive()) {
            if (actual.isPrimitive()) {
                if (actual.toString().equals("int") && (formal.toString().equals("double") || formal.toString().equals("float"))) {
                    return true;
                }
                return formal.equals(actual);
            }
            return sameArg(actual, getBoxedType(formal.tsym));
        }
        if (actual.isPrimitive()) {
            actual = getBoxedType(actual.tsym);
        }
        boolean subs = typeUtils.isSubtype(actual, formal);
        return subs;
    }

    public Type getBoxedType(final Symbol s) {
        Type type = s.type;
        if (s.type.isPrimitive()) {
            final TypeElement boxedClass = typeUtils.boxedClass(s.type);
            type = (Type) boxedClass.asType();
        }
        return type;
    }

    public Type getBoxedType(Type s) {
        if (s.isPrimitive()) {
            final TypeElement boxedClass = typeUtils.boxedClass(s);
            s = (Type) boxedClass.asType();
        }
        return s;
    }

    public Name getName(Name className) {
        return getName(className.toString());
    }

    public Name getName(String className) {
        if (className.startsWith(dot)) {
            className = className.substring(1);
        }
        return elementUtils.getName(className);
    }

    public static <T> com.sun.tools.javac.util.List<T> injectBefore(T stmt, final com.sun.tools.javac.util.List<? extends T> stats, final boolean skipStmt, T... newStmts) {
        if (stmt == null || !stats.contains(stmt)) {
            throw new IllegalArgumentException("" + stmt + " doesn't belong to " + stats);
        }
        final ListBuffer<T> lb = ListBuffer.lb();
        java.util.List<? extends T> pre = stats.subList(0, stats.indexOf(stmt));
        for (T p : pre) {
            lb.append(p);
        }
        for (T newStmt : newStmts) {
            if (newStmt != null) {
                lb.append(newStmt);
            }
        }
        if (!skipStmt) {
            lb.append(stmt);
        }
        java.util.List<? extends T> remStats = stats.subList(stats.indexOf(stmt) + 1, stats.size());
        for (T stat : remStats) {
            lb.append(stat);
        }
        return lb.toList();
    }
}
