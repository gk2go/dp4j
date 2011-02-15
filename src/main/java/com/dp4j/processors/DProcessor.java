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

    public JCNewArray getArray(Type t, java.util.List<JCExpression> args) {
        JCExpression[] toArray = args.toArray(new JCExpression[0]);
        List<JCExpression> toList = toList(toArray);
        JCExpression tExp = tm.Type(t);
        final JCExpression dim = tm.Literal(args.size());
        com.sun.tools.javac.util.List<JCExpression> dims = com.sun.tools.javac.util.List.of(dim);
        return tm.NewArray(tExp, dims, toList);
    }

    public JCNewArray getArray(String t, java.util.List<JCExpression> args, CompilationUnitTree cut) {
        return getArray(getType(t, cut, args), args);
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

    public JCVariableDecl getVarDecl(String varName, String idName, String methodName, String stringParam, JCExpression[] params, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
        final JCMethodInvocation valueSetter = (methodName != null) ? getMethodInvoc(methodName, stringParam, toList(params), vars, cut, packageName, scope, stmt, varSyms) : null;
        return tm.VarDef(tm.Modifiers(Flags.FINAL), elementUtils.getName(varName), getId(idName), valueSetter);
    }

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

    public Symbol getSymbol(JCMethodInvocation mi, java.util.List<Type> args, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
        if (args == null) { //why args are passed as an arg in the first place? Posterity?
            args = getArgs(mi, vars, cut, packageName, scope, stmt, varSyms);
        }
        final String meth = mi.meth.toString();
        int dotI = meth.lastIndexOf(dot) + 1;
        if (dotI < 1) {
            dotI = 0;
        }
        String mName = meth.substring(dotI) + "(";
        Symbol ms = contains(varSyms, mName, args, cut, packageName);
        if (ms != null) {
            return ms;
        }
        boolean a = mi.meth instanceof JCFieldAccess;
        final JCFieldAccess fa = (JCFieldAccess) mi.meth;
        mName = fa.getIdentifier().toString() + "(";
        ClassSymbol cs = getAccessorClassSymbol(fa, vars, cut, packageName, scope, stmt, args, varSyms);
        args = getArgs(mi, vars, cut, packageName, scope, stmt, varSyms);
        return getMSymbol(cs, mName, args, cut, packageName);
    }

    public JCExpression[] getTypes(java.util.List<VarSymbol> params) {
        int length = 0;
        if (params != null) {
            length = params.size();
        }
        JCExpression[] exps = new JCExpression[length];
        int i = 0;
        for (VarSymbol param : params) {
            String typeName = getBoxedType(param).toString();
            final Type type = param.type;
            JCExpression classExp;
            if (type.isPrimitive()) {
                Symbol symbol = getSymbol(typeName, "TYPE");
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

    public String getQualifiedClassName(String className, final CompilationUnitTree cut, Object packageName) {
        if (!className.contains(dot)) {
            java.util.List<? extends ImportTree> imports = cut.getImports();
            boolean imported = false;
            for (ImportTree importTree : imports) {
                if (importTree.toString().contains(dot + className + ";")) {
                    Tree qualifiedIdentifier = importTree.getQualifiedIdentifier();
                    className = qualifiedIdentifier.toString();
                    imported = true;
                    break;
                }
            }
            if (!imported) {
                String tmp = className;
                if (packageName != null) {
                    className = packageName.toString() + dot + className;
                }
                ClassSymbol te = getTypeElement(className);
                if (te == null) { //must be java.lang,
                    //FIXME: or some .* .. this is a good moment to try-catch
                    return getQualifiedClassName("java.lang." + tmp, cut, packageName);
                }
            }
        } else if (className.endsWith(".class")) {
            className = javaLangClass;
        }
        return className;
    }

    public Type getReturnType(JCMethodInvocation mi, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, java.util.List<Type> args, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
        MethodSymbol symbol = (MethodSymbol) getSymbol(mi, args, vars, cut, packageName, scope, stmt, varSyms);
        return symbol.getReturnType();
    }

    public MethodSymbol getMSymbol(Symbol className, String mName, java.util.List<Type> args, CompilationUnitTree cut, Object packageName) {
        final ClassSymbol typ = (ClassSymbol) className;
        FilteredMemberList allMembers = elementUtils.getAllMembers(typ);
        Symbol ret = contains(allMembers, mName, args, cut, packageName);
        if (ret == null) {
            ret = contains(typ.getEnclosedElements(), mName, args, cut, packageName);
        }
        return (MethodSymbol) ret;
    }

    public MethodSymbol getMSymbol(String className, String mName, java.util.List<Type> args, CompilationUnitTree cut, Object packageName) {
        final ClassSymbol typ = getTypeElement(className);
        return getMSymbol(typ, mName, args, cut, packageName);
    }

    public Symbol contains(final Collection<Symbol> list, final String objName) {
        for (Symbol symbol : list) {
            String qualifiedName = symbol.toString();
            if (objName.equals(qualifiedName)) {
                return symbol;
            }
        }
        return null;
    }

    public Symbol contains(final Collection<Symbol> list, final Name objName) {
        for (Symbol symbol : list) {
            if (objName.equals(symbol.name)) {
                return symbol;
            }
        }
        return null;
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

    //Template method?
    public Symbol contains(final Collection<Symbol> list, String mName, java.util.List<Type> args, CompilationUnitTree cut, Object packageName) {
        Collection<Symbol> methodsWithSameName = getMethodsWithSameName(mName, list); //FIXME: either this method is used only with methods or methods are ended with (
        for (Symbol ms : methodsWithSameName) {
            final String qualifiedName = ms.toString();
            int args1 = qualifiedName.indexOf("(") + 1;
            if (args1 < 1) { //this is a field
                assert (methodsWithSameName.size() == 1);
                return ms;
            }
            int argsEnd = qualifiedName.indexOf(")");
            final String typesLine = qualifiedName.substring(args1, argsEnd);
            java.util.List<Type> types = getTypes(typesLine, cut, packageName);
            if (args == null) {
                args = Collections.EMPTY_LIST;
            }
            final boolean varArgs = typesLine.contains(varArgsDots);
            if (types.size() == args.size() || varArgs && ((types.size() == args.size() + 1) || args.size() > types.size())) {
                int i = 0;

                boolean sameArgs = true;
                for (Type varSymbol : types) {
                    final boolean varArg = varSymbol instanceof VarArgType || varSymbol.toString().contains(varArgsDots);
                    if (i >= args.size() && varArg) {
                        break;
                    }
                    Type arg = args.get(i);
                    if (differentArg(arg, varSymbol)) { //FIXME: choose method with closest subtype and cast only if necessary
//                        Tip: Always pass the upper bound of the parameterized type when searching for a method.
                        Type boxedType = getBoxedType(arg.asElement());
                        if (arg.isPrimitive()) {
                            Collection<Symbol> tmp = getSmallerList(methodsWithSameName, ms);
                            Symbol s = contains(tmp, mName, args, cut, packageName);
                            if (s != null) { //there's a method with the primitive signature. Return that.
                                return s;
                            }
                            java.util.List<Type> boxedArgs = new ArrayList<Type>(args);
                            boxedArgs.remove(i);
                            boxedArgs.add(i, boxedType);
                            s = contains(list, mName, boxedArgs, cut, packageName);
                            if (s != null) {
                                return s;
                            }
                        }
                        if (varArg) {
                            Type t = ((VarArgType) varSymbol).t;
                            typeUtils.erasure(t);
                            if (!differentArg(arg, t)) {
                                continue;
                            }
                        }
                        sameArgs = false;
                        break;
                    }
                    i++;
                }
                if (sameArgs) {
                    return ms;
                }
            }
        }
        return null;
    }

    public Type getBoxedType(final Symbol s) {
        Type type = s.type;
        if (s.type.isPrimitive()) {
            final TypeElement boxedClass = typeUtils.boxedClass(s.type);
            type = (Type) boxedClass.asType();
        }
        return type;
    }

    public JCMethodInvocation getMethodInvoc(final String methodName, final JCExpression param, final List<JCExpression> otherParams, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
        final JCExpression methodN = getIdAfterImporting(methodName);
        final List<JCExpression> paramsList = injectBefore(otherParams.head, otherParams, param);
        final JCMethodInvocation mi = tm.Apply(List.<JCExpression>nil(), methodN, paramsList);
        java.util.List<Type> args = getArgs(mi, vars, cut, packageName, scope, stmt, varSyms);
        mi.type = getReturnType(mi, vars, cut, packageName, args, scope, stmt, varSyms);
        return mi;
    }

    public JCMethodInvocation getRefMethodInvoc(final String methodName, final Object param, final com.sun.tools.javac.util.List<JCExpression> otherParams, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
        if (param instanceof JCExpression) {
            return getMethodInvoc(methodName, (JCExpression) param, otherParams, vars, cut, packageName, scope, stmt, varSyms);
        } else {
            return getMethodInvoc(methodName, (String) param, otherParams, vars, cut, packageName, scope, stmt, varSyms);
        }
    }

    public JCMethodInvocation getMethodInvoc(String methodName, String param, final com.sun.tools.javac.util.List<JCExpression> otherParams, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
        return getMethodInvoc(methodName, tm.Literal(param), otherParams, vars, cut, packageName, scope, stmt, varSyms);
    }

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
//
//    public static <T, V> List<T> castList(final List<V> params) {
//        final ListBuffer<T> lb = ListBuffer.lb();
//        for (V param : params) {
//            lb.append((T) param);
//        }
//        final List<T> paramsList = lb.toList();
//        return paramsList;
//    }

    public <T> List<String> toString(final java.util.List<T> params) {
        final ListBuffer<String> lb = ListBuffer.lb();
        for (T param : params) {
            lb.append(param.toString());
        }
        return lb.toList();
    }

    protected abstract void processElement(final Element e, TypeElement ann, boolean warningsOnly);

    public Type getType(String type, CompilationUnitTree cut, Object packageName) {
        if (type.equals(StringUtils.EMPTY)) {
            return Type.noType;
        }
        ClassSymbol typeElement = getTypeElement(type);
        final Type t;
        typeElement = getTypeElement(getQualifiedClassName(type, cut, packageName));

        if (typeElement == null) {
            if (type.endsWith(varArgsDots) || type.endsWith(arrayBrac)) {
                if (type.contains(varArgsDots)) {
                    type = type.replace(varArgsDots, StringUtils.EMPTY);
                    Type com = getType(type, cut, packageName);
                    return new VarArgType(typeUtils.getArrayType(com));
                }
                if (type.contains(arrayBrac)) {
                    type = type.replace(arrayBrac, StringUtils.EMPTY);
                }
                Type com = getType(type, cut, packageName);
                return (Type) typeUtils.getArrayType(com);
            }
            int typeParamIndex = type.indexOf("<") + 1;
            if (typeParamIndex > 0) {
                final int paramsEnd = type.lastIndexOf(">");
                final String typeParamsLine = type.substring(typeParamIndex, paramsEnd);
                final java.util.List<Type> typeParams = getTypes(typeParamsLine, cut, packageName);
                String paramsLess = type.replace("<" + typeParamsLine + ">", StringUtils.EMPTY);
                TypeSymbol paramsLessType = getType(paramsLess, cut, packageName).asElement();
                DeclaredType declaredType = typeUtils.getDeclaredType((TypeElement) paramsLessType, typeParams.toArray(new Type[0]));
                return (Type) declaredType;
            }
            if (type.contains("?")) {
                if (type.equals("?")) {
                    return (Type) typeUtils.getWildcardType(null, null);
                }
                int extendsIndex = type.indexOf(" extends ") + 9;
                if (extendsIndex > 8) {
                    String ext = type.substring(extendsIndex);
                    Type extType = getType(ext, cut, packageName);
                    return (Type) typeUtils.getWildcardType(extType, null);
                } //FIXME: do the same for super
                //FIXME: do the same for super
                //FIXME: do the same for super
                //FIXME: do the same for super
            }
            if (type.equals("T")) {
                return getType("?", cut, packageName);
            }
            t = (Type) typeUtils.getPrimitiveType(TypeKind.valueOf(type.toUpperCase()));
        } else {
            t = typeElement.type;
        }
        return t;
    }

    public java.util.List<Type> getTypes(final String argsLine, CompilationUnitTree cut, Object packageName) {
        String[] args = argsLine.split(",");
        java.util.List<Type> types = new ArrayList<Type>();
        for (String type : args) {
            Type t = getType(type, cut, packageName);
            if (!t.equals(Type.noType)) {
                types.add(t);
            }
        }
        return types;
    }

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

    public String getClassNameOfAccessor(JCFieldAccess fa, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, java.util.List<Type> args, Collection<Symbol> varSyms) {
        final JCExpression exp = fa.getExpression();
        String className = exp.toString();
        if (exp instanceof JCNewClass) {//constructed instance on the fly
            final JCNewClass nc = (JCNewClass) exp;
            final JCExpression clas = nc.clazz;
            if (clas != null) {
                className = clas.toString();
            }
        } else if (exp instanceof JCIdent) {
            //is an instance or static
            final JCIdent id = (JCIdent) exp;
            className = id.name.toString();
            final ClassSymbol typ = getTypeElement(className);
            if (typ == null) {
                final JCExpression get = vars.get(className);
                if (get != null) {
                    //is instance
                    className = get.toString();
                } //else is static
            } else {
                className = typ.fullname.toString();
            }
        } else if (exp instanceof JCMethodInvocation) {
            JCMethodInvocation mi = (JCMethodInvocation) exp;

            className = getReturnType(mi, vars, cut, packageName, args, scope, stmt, varSyms).toString();
            //FIXME: what about chains and args of the method?
        }
        return getQualifiedClassName(className, cut, packageName);
    }

    public ClassSymbol getClassSymbol(JCExpression exp, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, java.util.List<Type> args, Collection<Symbol> varSyms) {
        String className = exp.toString().toString().replace(arrayBrac, StringUtils.EMPTY);
        if (exp instanceof JCNewClass) { //constructed instance on the fly
            final JCNewClass nc = (JCNewClass) exp;
            final JCExpression clas = nc.clazz;
            className = clas.toString();
        } else if (exp instanceof JCIdent) {
            //is an instance or static
            final JCIdent id = (JCIdent) exp;
            className = id.name.toString();
            ClassSymbol typ = getTypeElement(className);
            if (typ == null) {
                typ = getTypeElement(getQualifiedClassName(className, cut, packageName));
                if (typ != null) {
                    return typ;
                }

                Symbol s = contains(varSyms, className, Collections.EMPTY_LIST, cut, packageName);
                if (s == null) {
                    final JCExpression get = vars.get(className);
                    if (get != null) {
                        //is instance
                        if (get.type != null && get.type != null && get.type.tsym != null) {
                            return (ClassSymbol) get.type.tsym;
                        }
                        className = get.toString();
                    } //else is static
                } else {
                    boolean f = s instanceof ClassSymbol;
                    if (f) {
                        return (ClassSymbol) s;
                    } else {
                        return getTypeElement(s.type.toString());
                    }
                }
            } else {
                return typ;
            }
        } else if (exp instanceof JCMethodInvocation) {
            JCMethodInvocation mi = (JCMethodInvocation) exp;
            args = getArgs(mi, vars, cut, packageName, scope, stmt, varSyms);
            className = getReturnType(mi, vars, cut, packageName, args, scope, stmt, varSyms).toString();
            //FIXME: what about chains and args of the method?
        } else if (exp instanceof JCFieldAccess) {
            Type type = getType((JCFieldAccess) exp, vars, cut, packageName, scope, stmt, args, varSyms);
            return (ClassSymbol) type.tsym;
        }
        return (ClassSymbol) getType(getQualifiedClassName(className, cut, packageName), cut, packageName).tsym;
    }

    public ClassSymbol getAccessorClassSymbol(JCFieldAccess fa, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, java.util.List<Type> args, Collection<Symbol> varSyms) {
        final JCExpression exp = fa.getExpression();
        return getClassSymbol(exp, vars, cut, packageName, scope, stmt, args, varSyms);
    }

    public Type getType(JCFieldAccess fa, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, java.util.List<Type> args, Collection<Symbol> varSyms) {
        final Symbol s = getSymbol(fa, vars, cut, packageName, scope, args, stmt, varSyms);
        return s.type;
    }

    public Symbol getSymbol(JCFieldAccess fa, Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, java.util.List<Type> args, JCStatement stmt, Collection<Symbol> varSyms) {
        ClassSymbol typ = getTypeElement(fa.toString());
        if (typ != null) {
            return typ;
        }

        ClassSymbol cs = getAccessorClassSymbol(fa, vars, cut, packageName, scope, stmt, args, varSyms);
        final String objName = fa.name.toString();
        return getSymbol(cs, objName);
    }

    public Symbol getSymbol(final ClassSymbol typ, final String objName) {
        if (objName.equals(com.dp4j.processors.DProcessor.clazz)) {
            return getTypeElement(com.dp4j.processors.DProcessor.javaLangClass);
        }
        FilteredMemberList allMembers = elementUtils.getAllMembers(typ);
        Symbol ret = contains(allMembers, objName);
        if (ret == null) {
            ret = contains(typ.getEnclosedElements(), objName);
        }
        return ret;
    }

    public Symbol getSymbol(final String className, final String objName) {
        final ClassSymbol typ = getTypeElement(className);
        return getSymbol(typ, objName);
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

    public java.util.List<Type> getArgs(final JCMethodInvocation mi, final Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
        java.util.List<Type> args = new ArrayList<Type>();
        if (!mi.args.isEmpty()) {
            for (JCExpression arg : mi.args) {
                java.util.List<Type> argArgs;
                if (arg instanceof JCMethodInvocation) {
                    argArgs = null; //call will figure out
                } else {
                    argArgs = Collections.EMPTY_LIST;
                    stmt = null;
                }
                Type t = getType(arg, vars, cut, packageName, scope, stmt, argArgs, varSyms);
                if (t != null) {
                    args.add(t);
                }
            }
        }
        return args;
    }

    protected Type getType(JCExpression ifExp, Map<String, JCExpression> vars, final CompilationUnitTree cut, Object packageName, com.sun.source.tree.Scope scope, JCStatement stmt, java.util.List<Type> args, Collection<Symbol> varSyms) {
        if (ifExp instanceof JCFieldAccess) {
            final JCFieldAccess fa = (JCFieldAccess) ifExp;
            ifExp.type = getType(fa, vars, cut, packageName, scope, stmt, args, varSyms);
        } else if (ifExp instanceof JCMethodInvocation) {
            final JCMethodInvocation mi = (JCMethodInvocation) ifExp;
            args = getArgs(mi, vars, cut, packageName, scope, stmt, varSyms);
            ifExp.type = ((MethodSymbol) getSymbol(mi, args, vars, cut, packageName, scope, stmt, varSyms)).getReturnType();
        } else if (ifExp instanceof JCNewClass) {
            //TODO: it's a method too! handle similarly
            ClassSymbol cs = getClassSymbol(ifExp, vars, cut, packageName, scope, stmt, args, varSyms);
            ifExp.type = cs.type;
        } else if (ifExp instanceof JCTypeCast) {
            JCTypeCast cast = (JCTypeCast) ifExp;
            cast.type = getType(cast.expr, vars, cut, packageName, scope, stmt, args, varSyms);
            ifExp.type = cast.type;
        } else if (ifExp instanceof JCParens) {
            JCParens parensExp = (JCParens) ifExp;
            ifExp.type = getType(parensExp.expr, vars, cut, packageName, scope, stmt, args, varSyms);
        } else if (ifExp instanceof JCLiteral) {
            ifExp.type = getType((JCLiteral) ifExp);
        } else if (ifExp instanceof JCIdent) {
            final String exp = ifExp.toString();
            final Symbol sym = contains(varSyms, exp, args, cut, packageName);
            if (sym != null) {
                ifExp.type = sym.type;
                return ifExp.type;
            }
            JCExpression get = vars.get(exp);
            if (get == null) {
                Type type = getType(exp.toString(), cut, packageName);
                return type;
            }
            ifExp.type = get.type;
            if (ifExp.type != null) {
                return ifExp.type;
            }
            ifExp.type = getType(get.toString(), cut, packageName);
        } else if (ifExp instanceof JCPrimitiveTypeTree) {
            return getType(ifExp.toString(), cut, packageName);
        } else if (ifExp instanceof JCBinary) {
            return getType(((JCBinary) ifExp).lhs, vars, cut, packageName, scope, stmt, args, varSyms);
        } else if (ifExp instanceof JCNewArray) {
            Type eType = getType(((JCNewArray) ifExp).elemtype, vars, cut, packageName, scope, stmt, args, varSyms);
            return (Type) typeUtils.getArrayType(eType);
        }
        return ifExp.type;
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
}
