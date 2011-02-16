/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.*;
import java.util.Map.Entry;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import java.util.*;
import com.dp4j.templateMethod;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import java.util.Map;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.model.FilteredMemberList;
import javax.lang.model.type.ArrayType;
import org.apache.commons.lang.StringUtils;
import com.dp4j.ast.Resolver;
/**
 *
 * @author simpatico
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public abstract class DProcessor extends AbstractProcessor {

    public static final String clazz = "class";
    public final String arrayBrac = "[]";
    public final String dot = ".";
    protected Trees trees;
    protected TreeMaker tm;
    protected JavacElements elementUtils;
    protected Messager msgr;
    protected Types typeUtils;
    protected Symtab symTable;
    protected TypeElement encClass;
    protected Resolver rs;

    public JCNewArray getArray(Type t, java.util.List<JCExpression> args) {
        JCExpression[] toArray = args.toArray(new JCExpression[0]);
        List<JCExpression> toList = toList(toArray);
        JCExpression tExp = tm.Type(t);
        final JCExpression dim = tm.Literal(args.size());
        com.sun.tools.javac.util.List<JCExpression> dims = com.sun.tools.javac.util.List.of(dim);
        return tm.NewArray(tExp, dims, toList);

    }

    protected void printMsg(final String msg, final Element e, final boolean warningsOnly) {
        final Kind kind = (warningsOnly) ? Kind.WARNING : Kind.ERROR;
        msgr.printMessage(kind, msg, e);
    }

    public JCVariableDecl getVarDecl(JCModifiers mods, final String varName, final String idName, final String methodName, final String... params) {
        JCMethodInvocation valueSetter = (methodName != null) ? getMethodInvoc(methodName, params) : null;
        JCVariableDecl VarDef = tm.VarDef(mods, elementUtils.getName(varName), getId(idName), valueSetter);
        return VarDef;
    }

    public JCVariableDecl getVarDecl(final String varName, final String idName, final String methodName, final String... params) {
        return getVarDecl(tm.Modifiers(Flags.FINAL), varName, idName, methodName, params);
    }

    public JCVariableDecl getVarDecl(final String varName, final String idName, final String methodName, final Name stringParam, final JCExpression... params) {
        final JCMethodInvocation valueSetter = (methodName != null) ? getMethodInvoc(methodName, stringParam, params) : null;
        return tm.VarDef(tm.Modifiers(Flags.FINAL), elementUtils.getName(varName), getId(idName), valueSetter);
    }

//    public JCVariableDecl getVarDecl(String varName, String idName, String methodName, String stringParam, JCExpression[] params, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
//        final JCMethodInvocation valueSetter = (methodName != null) ? getMethodInvoc(methodName, stringParam, toList(params), vars, cut, packageName, scope, stmt, varSyms) : null;
//        return tm.VarDef(tm.Modifiers(Flags.FINAL), elementUtils.getName(varName), getId(idName), valueSetter);
//    }

    public JCVariableDecl getArrayDecl(JCModifiers mods, final String varName, final String idName, final JCNewArray array) {
        return tm.VarDef(mods, elementUtils.getName(varName), getId(idName), array);
    }

    public JCVariableDecl getArrayDecl(final String varName, final String idName, final JCNewArray array) {
        return tm.VarDef(tm.Modifiers(Flags.FINAL), elementUtils.getName(varName), getId(idName), array);
    }

    private List<JCExpression> getParamsList(final Boolean... params) {
        final ListBuffer<JCExpression> lb = ListBuffer.lb();
        for (boolean param : params) {

            int v = ((Boolean) param) ? 1 : 0;
            lb.append(tm.Literal(TypeTags.BOOLEAN, v));
        }
        final List<JCExpression> paramsList = lb.toList();
        return paramsList;
    }

    private List<JCExpression> getParamsList(final Name... params) {
        final ListBuffer<JCExpression> lb = ListBuffer.lb();
        for (Name param : params) {
            lb.append(tm.Ident(param));
        }
        final List<JCExpression> paramsList = lb.toList();
        return paramsList;
    }

    public List<JCExpression> toList(final JCExpression... params) {
        final ListBuffer<JCExpression> lb = ListBuffer.lb();
        for (JCExpression param : params) {
            lb.append(param);
        }
        final List<JCExpression> paramsList = lb.toList();
        return paramsList;
    }
    public final static String javaLangClass = "java.lang.Class";

    public JCExpression[] getTypes(java.util.List<VarSymbol> params) {
        int length = 0;
        if (params != null) {
            length = params.size();
        }
        JCExpression[] exps = new JCExpression[length];
        int i = 0;
        for (VarSymbol param : params) {
            Symbol boxedS = rs.getBoxedSymbol(param);
            final Type type = param.type;
            JCExpression classExp;
            if (type.isPrimitive()) {
                java.util.List<Symbol> enclosedElements = boxedS.getEnclosedElements();
                Symbol symbol = rs.contains(enclosedElements, null, elementUtils.getName("TYPE"), null);
                JCExpression primitiveClass = tm.QualIdent(symbol);
                classExp = primitiveClass;
            } else {
                final JCExpression t = tm.Type(type);
                final com.sun.tools.javac.util.Name c = elementUtils.getName(clazz);
                final JCFieldAccess fa = tm.Select(t, c);
                classExp = fa;
            }
            exps[i++] = classExp;
        }
        return exps;
    }
    public final static String varArgsDots = "...";

    public java.util.List<Symbol> getMethodsWithSameName(String mName, Collection<Symbol> list) {
        java.util.List<Symbol> ret = new ArrayList<Symbol>();
        for (Symbol s : list) {
            final String n = s.toString();
            int argsBegin = n.indexOf("(");
            if (argsBegin < 0) {
                argsBegin = n.length() - 1;
            }
            String qualifiedName = n.substring(0, argsBegin + 1);
            if (qualifiedName.equals(mName)) {
                ret.add(s);
            }
        }
        return ret;
    }

    public Type getBoxedType(final Symbol s) {
        Type type = s.type;
        if (s.type.isPrimitive()) {
            final TypeElement boxedClass = typeUtils.boxedClass(s.type);
            type = (Type) boxedClass.asType();
        }
        return type;
    }

//    public JCMethodInvocation getMethodInvoc(final String methodName, final JCExpression param, final List<JCExpression> otherParams, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
//        final JCExpression methodN = getIdAfterImporting(methodName);
//        final List<JCExpression> paramsList = injectBefore(otherParams.head, otherParams, param);
//        final JCMethodInvocation mi = tm.Apply(List.<JCExpression>nil(), methodN, paramsList);
//        java.util.List<Type> args = getArgs(mi, vars, cut, packageName, scope, stmt, varSyms);
//        //FIXME: set type
//        return mi;
//    }
//
//    public JCMethodInvocation getRefMethodInvoc(final String methodName, final Object param, final com.sun.tools.javac.util.List<JCExpression> otherParams, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
//        if (param instanceof JCExpression) {
//            return getMethodInvoc(methodName, (JCExpression) param, otherParams, vars, cut, packageName, scope, stmt, varSyms);
//        } else {
//            return getMethodInvoc(methodName, (String) param, otherParams, vars, cut, packageName, scope, stmt, varSyms);
//        }
//    }

//    public JCMethodInvocation getMethodInvoc(String methodName, String param, final com.sun.tools.javac.util.List<JCExpression> otherParams, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
//        return getMethodInvoc(methodName, tm.Literal(param), otherParams, vars, cut, packageName, scope, stmt, varSyms);
//    }
    public JCMethodInvocation getRefMethodInvoc(final String methodName, final Object param) {
        if (param instanceof JCExpression) {
            return getMethodInvoc(methodName, (JCExpression) param);
        } else {
            return getMethodInvoc(methodName, (String) param);
        }
    }

    public JCMethodInvocation getMethodInvoc(final String methodName, final JCExpression... params) {
        final JCExpression methodN = getIdAfterImporting(methodName);
        final List<JCExpression> paramsList = toList(params);
        final JCMethodInvocation mInvoc = tm.Apply(List.<JCExpression>nil(), methodN, paramsList);
        return mInvoc;
    }

    public JCMethodInvocation getMethodInvoc(final String methodName, final Name stringParam, final JCExpression[] exps) {
        final JCExpression methodN = getIdAfterImporting(methodName);
        final JCExpression lit = tm.Ident(stringParam);
        final List<JCExpression> paramsList = injectBefore(exps[0], toList(exps), lit);
        final JCMethodInvocation mInvoc = tm.Apply(List.<JCExpression>nil(), methodN, paramsList);
        return mInvoc;
    }

    public JCMethodInvocation getMethodInvoc(final String methodName, final Boolean... boolParams) {
        final JCExpression methodN = getIdAfterImporting(methodName);
        final List<JCExpression> paramsList = getParamsList(boolParams);
        final JCMethodInvocation mInvoc = tm.Apply(List.<JCExpression>nil(), methodN, paramsList);
        return mInvoc;
    }

    public JCMethodInvocation getMethodInvoc(final String methodName, final Name... objs) {
        final JCExpression methodN = getIdAfterImporting(methodName);
        final List<JCExpression> paramsList = getParamsList(objs);
        final JCMethodInvocation mInvoc = tm.Apply(List.<JCExpression>nil(), methodN, paramsList);
        return mInvoc;
    }

    public JCMethodInvocation getMethodInvoc(final String methodName, final String... stringParams) {
        JCExpression methodN = getIdAfterImporting(methodName);
        final ListBuffer<JCExpression> lb = ListBuffer.lb();
        for (String param : stringParams) {
            lb.append(tm.Literal(param));
        }
        final List<JCExpression> paramsList = lb.toList();
        final JCMethodInvocation mInvoc = tm.Apply(List.<JCExpression>nil(), methodN, paramsList);
        return mInvoc;
    }

    public JCExpression getId(final String typeName) {
        return getIdAfterImporting(typeName);
    }

    public JCExpression getId(final Name typeName) {
        return getId(typeName.toString());
    }
    protected JCExpression thisExp;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        final Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        symTable = Symtab.instance(context);
        trees = Trees.instance(processingEnv);
        elementUtils = JavacElements.instance(context);
        msgr = processingEnv.getMessager();
        tm = TreeMaker.instance(((JavacProcessingEnvironment) processingEnv).getContext());
        typeUtils = processingEnv.getTypeUtils();
    }

    protected Map<Element, TypeElement> getElementsAnnotated(final RoundEnvironment roundEnv, Set<? extends TypeElement> annotations) {
        final Map<Element, TypeElement> annotatatedElements = new HashMap<Element, TypeElement>();
        for (TypeElement ann : annotations) {
            final Set<? extends Element> annElements = roundEnv.getElementsAnnotatedWith(ann);
            for (Element element : annElements) {
                annotatatedElements.put(element, ann);
            }
        }
        return annotatatedElements;
    }

    @templateMethod
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Map<Element, TypeElement> elementsAnnotated = getElementsAnnotated(roundEnv, annotations);
        final Set<Entry<Element, TypeElement>> entrySet = elementsAnnotated.entrySet();
        try {
            for (Entry<? extends Element, ? extends TypeElement> entry : entrySet) {
                processElement(entry.getKey(), entry.getValue(), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return onlyHandler(annotations);
    }

    protected boolean onlyHandler(Set<? extends TypeElement> annotations) {
        return true;
    }

    protected JCExpression getIdentAfterImporting(final Class clazz) {
        return getIdAfterImporting(clazz.getCanonicalName());
    }

    protected JCExpression getIdAfterImporting(final String methodName) {
        final String[] names = methodName.split("\\.");
        JCExpression e = tm.Ident(elementUtils.getName(names[0]));

        for (int i = 1; i < names.length; i++) {
            String name = names[i];
            e = tm.Select(e, elementUtils.getName(name));
        }
        return e;
    }

    public List<JCStatement> emptyList() {
        final ListBuffer<JCStatement> lb = ListBuffer.lb();
        return lb.toList();
    }

    protected static <T> com.sun.tools.javac.util.List<T> injectBefore(T stmt, final com.sun.tools.javac.util.List<T> stats, T... newStmts) {
        return injectBefore(stmt, stats, false, newStmts);
    }

    protected static <T> com.sun.tools.javac.util.List<T> injectBefore(T stmt, final com.sun.tools.javac.util.List<T> stats, final boolean skipStmt, T... newStmts) {
        final ListBuffer<T> lb = ListBuffer.lb();
        int i = 0;
        final int index = skipStmt ? stats.indexOf(stmt) + 1 : stats.indexOf(stmt);
        for (; i < index; i++) {
            lb.append(stats.get(i));
        }
        for (T newStmt : newStmts) {
            if (newStmt != null) {
                lb.append(newStmt);
            }
        }
        if (index > -1) {
            for (i = index; i < stats.size(); i++) {
                lb.append(stats.get(i));
            }
        }
        return lb.toList();
    }

    protected static <T> com.sun.tools.javac.util.List<T> merge(final Collection<T> stats, Collection<T> newStmts) {
        final ListBuffer<T> lb = ListBuffer.lb();
        for (T stat : stats) {
            lb.append(stat);
        }
        for (T newStmt : newStmts) {
            if (newStmt != null && !lb.contains(newStmt)) {
                lb.append(newStmt);
            }
        }
        return lb.toList();
    }

    public <T> List<String> toString(final java.util.List<T> params) {
        final ListBuffer<String> lb = ListBuffer.lb();
        for (T param : params) {
            lb.append(param.toString());
        }
        return lb.toList();
    }

    protected abstract void processElement(final Element e, TypeElement ann, boolean warningsOnly);

    public DeclaredType getDeclaredType(String className) {
        ClassSymbol typ = getTypeElement(className);
        return typeUtils.getDeclaredType(typ);
    }

    public ClassSymbol getTypeElement(String className) {
        if (className.startsWith(dot)) {
            className = className.substring(1);
        }
        final ClassSymbol typ = elementUtils.getTypeElement(className);
        return typ;
    }

    public boolean sameArg(Type arg, Type varSymbol) {
        final Type erArg = (Type) typeUtils.erasure(arg);
        final Type erVar = (Type) typeUtils.erasure(varSymbol);
        if (varSymbol instanceof VarArgType) {
            if (arg instanceof ArrayType) {
                arg = (Type) ((ArrayType) arg).getComponentType();
            }
            return sameArg(arg, ((VarArgType) varSymbol).t);
        }
        boolean subs = typeUtils.isSubtype(arg, varSymbol);
        boolean sameVar = erVar.equals(varSymbol);
        boolean sameErArg = erArg.equals(arg);
        return subs || ((!sameErArg || !sameVar) && sameArg(erArg, erVar));
    }

    public boolean differentArg(Type arg, Type varSymbol) {
        return !sameArg(arg, varSymbol);
    }

    public Collection<Symbol> getSmallerList(Collection<Symbol> methodsWithSameName, Symbol ms) {
        Collection<Symbol> tmp = new ArrayList<Symbol>(methodsWithSameName);
        tmp.remove(ms);
        return tmp;
    }

//    public java.util.List<Type> getArgs(final JCMethodInvocation mi, final Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
//        java.util.List<Type> args = new ArrayList<Type>();
//        if (!mi.args.isEmpty()) {
//            for (JCExpression arg : mi.args) {
//                java.util.List<Type> argArgs;
//                if (arg instanceof JCMethodInvocation) {
//                    argArgs = null; //call will figure out
//                } else {
//                    argArgs = Collections.EMPTY_LIST;
//                    stmt = null;
//                }
//                Type t = getType(arg, vars, cut, packageName, scope, stmt, argArgs, varSyms);
//                if (t != null) {
//                    args.add(t);
//                }
//            }
//        }
//        return args;
//    }

}
