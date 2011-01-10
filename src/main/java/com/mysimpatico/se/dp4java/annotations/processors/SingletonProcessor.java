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
    private Name.Table nameTable;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        final Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        nameTable = Name.Table.instance(context);
        trees = Trees.instance(processingEnv);
        elementUtils = JavacElements.instance(context);
    }

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

            final Name singletonClassName = nameTable.fromString(e.getSimpleName());

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
                        final Name instanceName = nameTable.fromString(instance.class.getSimpleName()); //just to say: instance as variable name
                        final Name instanceAnnName = nameTable.fromString(instance.class.getSimpleName());
                        final JCTree instanceAnnTree = tm.Ident(instanceAnnName);
                        final JCExpression initVal = null;
                        final JCAnnotation instanceAnn = tm.Annotation(instanceAnnTree, List.<JCExpression>nil());
                        final JCVariableDecl instance = tm.VarDef(tm.Modifiers(Flags.PRIVATE + Flags.STATIC, List.of(instanceAnn)), instanceName, instanceType, initVal);
                        if (!instanceFound) {
                            singletonClass.defs = singletonClass.defs.append(instance);
                        }

                        if (!getInstanceFound) {
                            final JCStatement retType = tm.Return(tm.Ident(instance.getName()));
                            final List<JCStatement> retStmt = List.of(retType);
                            final JCBlock body = tm.Block(0, retStmt);
                            final List<JCVariableDecl> parameters = com.sun.tools.javac.util.List.nil();
                            final List<JCExpression> throwsClauses = com.sun.tools.javac.util.List.nil();
                            final Name getInstanceAnnName = nameTable.fromString(getInstance.class.getSimpleName());
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
