/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.dp4j.*;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic.Kind;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import java.util.Collections;
import java.util.HashSet;

/**
 *
 *  TODO: there must be only one instance, getInstance
 * @author simpatico
 */
@SupportedAnnotationTypes(value = {"com.dp4j.Singleton", "org.jpatterns.gof.SingletonPattern", "com.google.code.annotation.pattern.design.creational.Singleton"}) //singleton
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

    protected Set<? extends Element> getElementsAnnotated(final RoundEnvironment roundEnv, Set<? extends TypeElement> annotations) {
        final Set<Element> annotatatedElements = new HashSet<Element>();
        for (TypeElement ann : annotations) {
            final Set<? extends Element> annElements = roundEnv.getElementsAnnotatedWith(ann);
            annotatatedElements.addAll(annElements);
        }
        return annotatatedElements;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        final Messager msgr = processingEnv.getMessager();
        tm = TreeMaker.instance(((JavacProcessingEnvironment) processingEnv).getContext());

        for (final Element e : getElementsAnnotated(roundEnv, annotations)) {
            Set<Modifier> modifiers = e.getModifiers();
            if (modifiers.contains(Modifier.ABSTRACT)) {
                msgr.printMessage(Kind.ERROR, "a Singleton must not be abstract", e);
            }
            java.util.List<? extends Element> enclosedElements = e.getEnclosedElements();
            boolean getInstanceFound = false;
            boolean instanceFound = false;
            Name instanceName = elementUtils.getName(instance.class.getSimpleName()); //just to say: instance as variable name
            for (final Element element : enclosedElements) {
                if (element.getAnnotation(instance.class) != null) {
                    if (instanceFound == true) {
                        msgr.printMessage(Kind.ERROR, "Found multiple methods annotated with @instance while at most one must be annotated", e);
                    }
                    instanceFound = true;
                    instanceName = elementUtils.getName(element.getSimpleName());
                }

                if (element.getAnnotation(getInstance.class) != null) {
                    if (getInstanceFound == true) {
                        msgr.printMessage(Kind.ERROR, "Found multiple methods annotated with @getInstance while at most one must be annotated", e);
                    }
                    getInstanceFound = true;
                }
            }

            final Name singletonClassName = elementUtils.getName(e.getSimpleName());

            JCCompilationUnit singletonCU = (JCCompilationUnit) trees.getPath(e).getCompilationUnit();
            JCMethodDecl defCon = null;
            for (JCTree def : singletonCU.defs) {
                if (def instanceof JCClassDecl) {
                    JCClassDecl singletonClass = (JCClassDecl) def;
                    if (singletonClass.name.equals(singletonClassName)) {
                        for (JCTree singletonMethod : singletonClass.defs) {
                            try {
                                JCMethodDecl constructor = (JCMethodDecl) singletonMethod;
                                if (constructor.name.contentEquals("<init>")) {
                                    if (constructor.params.isEmpty()) {
                                        defCon = constructor;
                                        if ((constructor.mods.flags & Flags.GENERATEDCONSTR) != 0) {
                                            defCon.mods = tm.Modifiers(Flags.PRIVATE);
                                        }
                                    }
                                    if ((constructor.mods.flags & Flags.PRIVATE) == 0) {
                                        msgr.printMessage(Kind.ERROR, "Singleton constructors must be private, or else it will be possible to instantiate them: " + constructor);
                                    }
                                }
                            } catch (ClassCastException ce) {
                                //it wasn't a method
                            }
                        }
                        final JCIdent instanceType = tm.Ident(singletonClassName);

                        final JCTree instanceAnnTree = getIdentAfterImporting(instance.class);//tm.Ident(instanceAnnName);
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
                            final Name getInstanceAnnName = elementUtils.getName(getInstance.class.getSimpleName());
                            final JCTree getInstanceAnnTree = getIdentAfterImporting(getInstance.class);//tm.Ident(getInstanceAnnName);
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

    JCExpression getIdentAfterImporting(final Class clazz) {
        final String fullName = clazz.getCanonicalName();
        final String[] names = fullName.split("\\.");
        JCExpression e = tm.Ident(elementUtils.getName(names[0]));

        for (int i = 1; i < names.length; i++) {
            String name = names[i];
            e = tm.Select(e, elementUtils.getName(name));
        }
        return e;
    }
}
