/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.dp4j.*;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic.Kind;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

/**
 *
 *  TODO: there must be only one instance, getInstance
 * @author simpatico
 */
@SupportedAnnotationTypes(value = {"com.dp4j.Singleton", "org.jpatterns.gof.SingletonPattern", "com.google.code.annotation.pattern.design.creational.Singleton"}) //singleton
public class SingletonProcessor extends DProcessor {

    @Override
    protected void processElement(final Element e, String annName, CompilationUnitTree cut, boolean warningsOnly) {
        Set<Modifier> modifiers = e.getModifiers();
        if (modifiers.contains(Modifier.ABSTRACT)) {
            msgr.printMessage(Kind.ERROR, "a Singleton must not be abstract", e);
        }
        java.util.List<? extends Element> enclosedElements = e.getEnclosedElements();
        boolean getInstanceFound = false;
        boolean instanceFound = false;
        Element instanceElement = null;
        Name getInstanceMethName;
        Name instanceVarName;
        boolean lazy = false;//default
        if (annName.equals(Singleton.class.getCanonicalName())) {
            final Singleton singleton = e.getAnnotation(Singleton.class);
            lazy = singleton.lazy();
            getInstanceMethName = elementUtils.getName(singleton.getInstance());
             instanceVarName = elementUtils.getName(singleton.instance());
        }else{
            getInstanceMethName = elementUtils.getName(getInstance.class.getSimpleName());
            instanceVarName = elementUtils.getName(getInstance.class.getSimpleName());
        }
        for (final Element element : enclosedElements) {
            if (element.getAnnotation(instance.class) != null) {
                if (instanceFound == true) {
                    msgr.printMessage(Kind.ERROR, "Found multiple methods annotated with @instance while at most one must be annotated", e);
                }
                instanceFound = true;
                instanceElement = element;
                instanceVarName = elementUtils.getName(element.getSimpleName());
            }

            if (element.getAnnotation(getInstance.class) != null) {
                if (getInstanceFound == true) {
                    msgr.printMessage(Kind.ERROR, "Found multiple methods annotated with @getInstance while at most one must be annotated", e);
                }
                getInstanceFound = true;
            }
        }

        if(instanceFound){
            VariableTree tree = (VariableTree) trees.getTree(instanceElement);
            ExpressionTree initializer = tree.getInitializer();
            if(initializer == null){
                lazy = true;
            }else{
                lazy = false; //default
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
                    final JCExpression defConInit = getEmptyArgsConstructor(defCon.sym);
                    final JCExpression instanceType = getId(singletonClassName);
                    tm.TypeApply(instanceType, null);

                    if (!instanceFound) {
                        if (defCon == null) {
                            msgr.printMessage(Kind.ERROR, "no singleton instance is declared and there's not any no-args constructor for me to declare one.");
                        } else {
                            final JCTree instanceAnnTree = getIdentAfterImporting(instance.class);
                            final JCAnnotation instanceAnn = tm.Annotation(instanceAnnTree, List.<JCExpression>nil());
                            final JCExpression initVal;
                            long mod = Flags.PRIVATE + Flags.STATIC;
                            if(lazy){
                                initVal = null;
                            }else{
                                initVal = defConInit;
                                mod += Flags.FINAL;
                            }
                            final JCModifiers instanceMods = tm.Modifiers(mod, List.of(instanceAnn));
                            final JCVariableDecl instance = tm.VarDef(instanceMods, instanceVarName, instanceType, initVal);
                            singletonClass.defs = singletonClass.defs.append(instance);
                            instanceFound = true;
                        }
                    }

                    if(!getInstanceFound){
                        final JCIdent instance = tm.Ident(instanceVarName);
                        final JCStatement retType = tm.Return(instance);
                        final JCBlock body;
                        long mod = Flags.PUBLIC + Flags.STATIC;
                        if(lazy){
                            JCAssign init = tm.Assign(instance, defConInit);
                            JCExpressionStatement thenStmt = tm.Exec(init);
                            JCIf initIf = getIfNull(instance, thenStmt);
                            body = tm.Block(0,List.of(initIf,retType));
                            mod += Flags.SYNCHRONIZED;
                        }else{
                            body = tm.Block(0, List.of(retType));
                        }
                        final List<JCVariableDecl> parameters = com.sun.tools.javac.util.List.nil();
                        final List<JCExpression> throwsClauses = com.sun.tools.javac.util.List.nil();
                        final JCTree getInstanceAnnTree = getIdentAfterImporting(getInstance.class);//tm.Ident(getInstanceAnnName);
                        final JCAnnotation getInstanceAnn = tm.Annotation(getInstanceAnnTree, List.<JCExpression>nil());
                        final JCExpression methodType = instanceType;

                        final JCMethodDecl getInstanceM = tm.MethodDef(tm.Modifiers(mod, List.of(getInstanceAnn)), getInstanceMethName, methodType, List.<JCTypeParameter>nil(), parameters, throwsClauses, body, null);

                        singletonClass.defs = singletonClass.defs.append(getInstanceM);
                    }
//                        JCTypeApply valueVarType = tm.TypeApply(singletonClass., List.<JCExpression>nil());
////                        JCNewClass newInstance = maker.NewClass(null, NIL_EXPRESSION, valueVarType, List.<JCExpression>of(field.init), null);
////                        JCStatement statement = maker.Exec(maker.Assign(maker.Ident(valueName), newInstance));
//
//                        instance.getType()

                }
            }
        }
        printVerbose(singletonCU, e);
    }
}
