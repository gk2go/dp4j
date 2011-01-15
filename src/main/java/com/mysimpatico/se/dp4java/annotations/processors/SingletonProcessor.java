/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mysimpatico.se.dp4java.annotations.processors;

import com.mysimpatico.se.dp4java.annotations.singleton.Singleton;
import com.mysimpatico.se.dp4java.annotations.singleton.getInstance;
import com.mysimpatico.se.dp4java.annotations.singleton.instance;
import com.sun.source.util.Trees;
import com.sun.tools.internal.ws.processor.generator.Names;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import java.io.File;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic.Kind;
import org.codehaus.plexus.util.DirectoryScanner;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.code.Type;

/**
 *
 *  TODO: there must be only one instance, getInstance

 * @author simpatico
 */
@SupportedAnnotationTypes("com.mysimpatico.se.dp4java.annotations.singleton.Singleton") //singleton
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SingletonProcessor extends AbstractProcessor {

    private Trees trees;
    private TreeMaker tm;
    private static JavacElements elementUtils;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        final Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        trees = Trees.instance(processingEnv);
        elementUtils = JavacElements.instance(context);
    }

//    private List<JCStatement> createLazyGetterBody(TreeMaker maker, JavacNode fieldNode) {
//        /*
//        java.util.concurrent.atomic.AtomicReference<ValueType> value = this.fieldName.get();
//        if (value == null) {
//        synchronized (this.fieldName) {
//        value = this.fieldName.get();
//        if (value == null) {
//        value = new java.util.concurrent.atomic.AtomicReference<ValueType>(new ValueType());
//        this.fieldName.set(value);
//        }
//        }
//        }
//        return value.get();
//         */
//
//        ListBuffer<JCStatement> statements = ListBuffer.lb();
//
//        JCVariableDecl field = (JCVariableDecl) fieldNode.get();
//        field.type = null;
//        if (field.vartype instanceof JCPrimitiveTypeTree) {
//            String boxed = TYPE_MAP.get(((JCPrimitiveTypeTree) field.vartype).typetag);
//            if (boxed != null) {
//                field.vartype = chainDotsString(maker, fieldNode, boxed);
//            }
//        }
//
//        Name valueName = fieldNode.toName("value");
//
//        /* java.util.concurrent.atomic.AtomicReference<ValueType> value = this.fieldName.get();*/ {
//            JCTypeApply valueVarType = maker.TypeApply(chainDotsString(maker, fieldNode, AR), List.of(copyType(maker, field)));
//            statements.append(maker.VarDef(maker.Modifiers(0), valueName, valueVarType, callGet(fieldNode, createFieldAccessor(maker, fieldNode, FieldAccess.ALWAYS_FIELD))));
//        }
//
//        /* if (value == null) { */ {
//            JCSynchronized synchronizedStatement;
//            /* synchronized (this.fieldName) { */ {
//                ListBuffer<JCStatement> synchronizedStatements = ListBuffer.lb();
//                /* value = this.fieldName.get(); */ {
//                    JCExpressionStatement newAssign = maker.Exec(maker.Assign(maker.Ident(valueName), callGet(fieldNode, createFieldAccessor(maker, fieldNode, FieldAccess.ALWAYS_FIELD))));
//                    synchronizedStatements.append(newAssign);
//                }
//
//                /* if (value == null) { */ {
//                    ListBuffer<JCStatement> innerIfStatements = ListBuffer.lb();
//                    /* value = new java.util.concurrent.atomic.AtomicReference<ValueType>(new ValueType());*/ {
//                        JCTypeApply valueVarType = maker.TypeApply(chainDotsString(maker, fieldNode, AR), List.of(copyType(maker, field)));
//                        JCNewClass newInstance = maker.NewClass(null, NIL_EXPRESSION, valueVarType, List.<JCExpression>of(field.init), null);
//
//                        JCStatement statement = maker.Exec(maker.Assign(maker.Ident(valueName), newInstance));
//                        innerIfStatements.append(statement);
//                    }
//                    /* this.fieldName.set(value); */ {
//                        JCStatement statement = callSet(fieldNode, createFieldAccessor(maker, fieldNode, FieldAccess.ALWAYS_FIELD), maker.Ident(valueName));
//                        innerIfStatements.append(statement);
//                    }
//
//                    JCBinary isNull = maker.Binary(JCTree.EQ, maker.Ident(valueName), maker.Literal(Javac.getCTCint(TypeTags.class, "BOT"), null));
//                    JCIf ifStatement = maker.If(isNull, maker.Block(0, innerIfStatements.toList()), null);
//                    synchronizedStatements.append(ifStatement);
//                }
//
//                synchronizedStatement = maker.Synchronized(createFieldAccessor(maker, fieldNode, FieldAccess.ALWAYS_FIELD), maker.Block(0, synchronizedStatements.toList()));
//            }
//
//            JCBinary isNull = maker.Binary(JCTree.EQ, maker.Ident(valueName), maker.Literal(Javac.getCTCint(TypeTags.class, "BOT"), null));
//            JCIf ifStatement = maker.If(isNull, maker.Block(0, List.<JCStatement>of(synchronizedStatement)), null);
//            statements.append(ifStatement);
//        }
//        /* return value.get(); */
//        statements.append(maker.Return(callGet(fieldNode, maker.Ident(valueName))));
//
//// update the field type and init last
//
//        /* private final java.util.concurrent.atomic.AtomicReference<java.util.concurrent.atomic.AtomicReference<ValueType> fieldName = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.atomic.AtomicReference<ValueType>>(); */ {
//            field.vartype = maker.TypeApply(chainDotsString(maker, fieldNode, AR), List.<JCExpression>of(maker.TypeApply(chainDotsString(maker, fieldNode, AR), List.of(copyType(maker, field)))));
//            field.init = maker.NewClass(null, NIL_EXPRESSION, copyType(maker, field), NIL_EXPRESSION, null);
//        }
//
//        return statements.toList();
//    }
//
//    private JCExpression copyType(JCVariableDecl fieldNode) {
//		return fieldNode.type != null ? tm.Type(fieldNode.type) : fieldNode.vartype;
//	}
//
//    public static JCExpression chainDotsString(TreeMaker maker, JavacNode node, String elems) {
//		return chainDots(maker, node, elems.split("\\."));
//	}
//
//    	/**
//	 * In javac, dotted access of any kind, from {@code java.lang.String} to {@code var.methodName}
//	 * is represented by a fold-left of {@code Select} nodes with the leftmost string represented by
//	 * a {@code Ident} node. This method generates such an expression.
//	 *
//	 * For example, maker.Select(maker.Select(maker.Ident(NAME[java]), NAME[lang]), NAME[String]).
//	 *
//	 * @see com.sun.tools.javac.tree.JCTree.JCIdent
//	 * @see com.sun.tools.javac.tree.JCTree.JCFieldAccess
//	 */
//	public static JCExpression chainDots(TreeMaker maker, JavacNode node, String... elems) {
//		assert elems != null;
//		assert elems.length > 0;
//
//		JCExpression e = maker.Ident(node.toName(elems[0]));
//		for (int i = 1 ; i < elems.length ; i++) {
//			e = maker.Select(e, node.toName(elems[i]));
//		}
//
//		return e;
//	}
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        final Messager msgr = processingEnv.getMessager();
        tm = TreeMaker.instance(((JavacProcessingEnvironment) processingEnv).getContext());

        for (final Element e : roundEnv.getElementsAnnotatedWith(Singleton.class)) {
            Set<Modifier> modifiers = e.getModifiers();
            if (modifiers.contains(Modifier.ABSTRACT)) {
                msgr.printMessage(Kind.ERROR, "a Singleton must not be abstract", e);
            }
            java.util.List<? extends Element> enclosedElements = e.getEnclosedElements();
            boolean getInstanceFound = false;
            boolean instanceFound = false;
            boolean privateConstructors = false;
            for (final Element element : enclosedElements) {
                if (element.getAnnotation(instance.class) != null) {
                    if (instanceFound == true) {
                        msgr.printMessage(Kind.ERROR, "Found multiple methods annotated with @instance while at most one must be annotated", e);
                    }
                    instanceFound = true;
                }
                if (element.getAnnotation(getInstance.class) != null) {
                    if (getInstanceFound == true) {
                        msgr.printMessage(Kind.ERROR, "Found multiple methods annotated with @getInstance while at most one must be annotated", e);
                    }
                    getInstanceFound = true;
                }
            }

            final Name singletonClassName = elementUtils.getName(e.getSimpleName());

            //make default constructor private

            JCCompilationUnit singletonCU = (JCCompilationUnit) trees.getPath(e).getCompilationUnit();
            JCMethodDecl defCon = null;
            for (JCTree def : singletonCU.defs) {
                if (def instanceof JCClassDecl) {
                    JCClassDecl singletonClass = (JCClassDecl) def;
                    if (singletonClass.name.equals(singletonClassName)) {
                        if (!privateConstructors) {
                            for (JCTree singletonMethod : singletonClass.defs) {

                                if (singletonMethod instanceof JCMethodDecl) {
                                    defCon = (JCMethodDecl) singletonMethod;
                                    if ((defCon.mods.flags & Flags.GENERATEDCONSTR) != 0) {
                                        defCon.mods = tm.Modifiers(Flags.PRIVATE);
                                    }
                                }
                            }
                        }
                        final JCIdent instanceType = tm.Ident(singletonClassName);
                        final Name instanceName = elementUtils.getName(instance.class.getSimpleName()); //just to say: instance as variable name
                        final Name instanceAnnName = elementUtils.getName(instance.class.getSimpleName());
                        final JCTree instanceAnnTree = tm.Ident(instanceAnnName);
                        tm.TypeApply(instanceType, null);
                        final JCAnnotation instanceAnn = tm.Annotation(instanceAnnTree, List.<JCExpression>nil());
                        final JCExpression initVal = tm.Create(defCon.sym, List.<JCExpression>nil());
                        final JCVariableDecl instance = tm.VarDef(tm.Modifiers(Flags.PRIVATE + Flags.STATIC + Flags.FINAL, List.of(instanceAnn)), instanceName, instanceType, initVal);

                        if (!instanceFound) {
                            singletonClass.defs = singletonClass.defs.append(instance);
                        }

//                        JCTypeApply valueVarType = tm.TypeApply(singletonClass., List.<JCExpression>nil());
////                        JCNewClass newInstance = maker.NewClass(null, NIL_EXPRESSION, valueVarType, List.<JCExpression>of(field.init), null);
////                        JCStatement statement = maker.Exec(maker.Assign(maker.Ident(valueName), newInstance));
//
//                        instance.getType()

                        if (!getInstanceFound) {
                            final JCStatement retType = tm.Return(tm.Ident(instance.getName()));
                            final List<JCStatement> retStmt = List.of(retType);
                            final JCBlock body = tm.Block(0, retStmt);
                            final List<JCVariableDecl> parameters = com.sun.tools.javac.util.List.nil();
                            final List<JCExpression> throwsClauses = com.sun.tools.javac.util.List.nil();
                            final Name getInstanceAnnName = elementUtils.getName(getInstance.class.getSimpleName()); //TODO: chainDots to avoid import statements
                            final JCTree getInstanceAnnTree = tm.Ident(getInstanceAnnName);
                            final JCAnnotation getInstanceAnn = tm.Annotation(getInstanceAnnTree, List.<JCExpression>nil());
                            final JCExpression methodType = instance.type != null ? tm.Type(instance.type) : instance.vartype;
                            final JCMethodDecl getInstanceM = tm.MethodDef(tm.Modifiers(Flags.PUBLIC + Flags.STATIC, List.of(getInstanceAnn)), getInstanceAnnName, methodType, List.<JCTypeParameter>nil(), parameters, throwsClauses, body, null);
                            singletonClass.defs = singletonClass.defs.append(getInstanceM);
                        }
                    }
                }
            }
            System.out.println(singletonCU);
        }
        return true;
    }
}
