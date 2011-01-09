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

/**
 *
 *  TODO: there must be only one instance, getInstance

 * @author simpatico
 */
@SupportedAnnotationTypes("com.mysimpatico.se.dp4java.annotations.singleton.Singleton") //singleton
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SingletonProcessor extends AbstractProcessor {

    private Trees trees;
    private TreeMaker treeMaker;
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
        treeMaker = TreeMaker.instance(((JavacProcessingEnvironment) processingEnv).getContext());

        for (final Element e : roundEnv.getElementsAnnotatedWith(Singleton.class)) {
            Set<Modifier> modifiers = e.getModifiers();
            if (modifiers.contains(Modifier.ABSTRACT)) {
                msgr.printMessage(Kind.ERROR, "a Singleton must not be abstract", e);
            }
            java.util.List<? extends Element> enclosedElements = e.getEnclosedElements();
            boolean getInstanceFound = false;
            boolean instanceFound = false;
            boolean privateConstructors = false;
            String enclosingClass = null;
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

            enclosingClass = e.toString();
//            final String anyDir = "**\\";
//            final DirectoryScanner ds = new DirectoryScanner();
//            final String file = enclosingClass.replaceAll("\\.", "\\" + File.separator);
//            final String srcDir = System.getProperty("user.dir");
//            final File classFile = new File(srcDir, file);
//            final String classs = anyDir + classFile.getName() + ".java";
//            ds.setIncludes(new String[]{classs});
//            ds.setBasedir(srcDir);
//            ds.scan();
//            String[] includedFiles = ds.getIncludedFiles();
//            if (includedFiles.length != 1) {
//                msgr.printMessage(Kind.ERROR, "not declared in separate file?" + e);
//            }
//            File srcFile = new File(srcDir, includedFiles[0]);
//            if (!srcFile.exists()) {
//                msgr.printMessage(Kind.ERROR, "file?" + e);
//            }
//                FileObject fileObj = FileUtil.toFileObject(srcFile);
//                final JavaSource classSource = JavaSource.forFileObject(fileObj);

            final Name singletonClassName = nameTable.fromString(e.getSimpleName());

            //make default constructor private

            JCCompilationUnit singletonCU = (JCCompilationUnit) trees.getPath(e).getCompilationUnit();
            for (JCTree def : singletonCU.defs) {
                if (def instanceof JCClassDecl) {
                    JCClassDecl singletonClass = (JCClassDecl) def;

                    if (singletonClass.name.equals(singletonClassName)) {
                        if (!privateConstructors) {
                            for (JCTree singletonMethod : singletonClass.defs) {

                                if (singletonMethod instanceof JCMethodDecl) {
                                    JCMethodDecl m = (JCMethodDecl) singletonMethod;
                                    if ((m.mods.flags & Flags.GENERATEDCONSTR) != 0) {
                                        m.mods = treeMaker.Modifiers(Flags.PRIVATE);
                                    }
                                }
                            }
                        }
                        final JCModifiers privateStaticMods = treeMaker.Modifiers(Flags.PRIVATE + Flags.STATIC);
                        final JCIdent instanceType = treeMaker.Ident(singletonClassName);

                        Name instanceName = nameTable.fromString(instance.class.getSimpleName());
                        if (!instanceFound) {
                            final JCVariableDecl instance = treeMaker.VarDef(privateStaticMods, instanceName, instanceType, null);
                            singletonClass.defs = singletonClass.defs.append(instance);
                        }

                        if (!getInstanceFound) {
                            final String getInstanceMethod = "@getInstance public static " + enclosingClass + " getInstance(){ return instance; }";

                            final JCStatement retType = treeMaker.Return(treeMaker.Ident(instanceName));
                            final com.sun.tools.javac.util.List<JCStatement> retStmt = com.sun.tools.javac.util.List.of(retType);
                            final JCBlock body = treeMaker.Block(0, retStmt);
                            final com.sun.tools.javac.util.List<JCStatement> methodGenericParams = com.sun.tools.javac.util.List.nil();
                            final List<JCVariableDecl> parameters = com.sun.tools.javac.util.List.nil();
                            final List<JCExpression> throwsClauses = com.sun.tools.javac.util.List.nil();
                            final Name getInstanceName = nameTable.fromString(getInstance.class.getSimpleName());
                            final JCTree getInstanceAnnTree = treeMaker.Ident(getInstanceName);
                            final JCExpression annotationMethodDefaultValue = treeMaker.Annotation(getInstanceAnnTree, List.<JCExpression>nil());
//                                treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC, List.<JCAnnotation>nil()), getInstanceName, instanceType, methodGenericParams, parameters, throwsClauses, body, annotationMethodDefaultValue);
                        }
                    }
                }
            }
            System.out.println(singletonCU);
        }
        return true;
    }

//    private VariableTree createInstance(TypeElement classElement){
//        final Name varName = elementUtils.getName("instance");
//        JCCompilationUnit classCU = (JCCompilationUnit) trees.getPath(classElement).getCompilationUnit();
//        new JCVariableDecl();
//
//        Processor.buildTypeExpressionForClass(this.treeMaker,elementUtils,instance.class, null);
//        treeMaker.VarDef(treeMaker.Modifiers(Flags.FINAL), varName, null, null);
//    }
//
//
//    private JCExpression ident( String fcn )
//    {
//        String[] parts = fcn.split( "\\." );
//        JCExpression e = treeMaker.Ident( ctx.fromString( parts[0] ) );
//        for ( int i = 1; i < parts.length; i++ )
//        {
//            e = ctx.maker.Select( e, ctx.fromString( parts[i] ) );
//        }
//        return e;
//    }
//
//
//    private JCMethodDecl createGetter() {
//
//        JCVariableDecl fieldNode = (JCVariableDecl) field.get();
//		JCStatement returnStatement = treeMaker.Return(treeMaker.Ident(fieldNode.getName()));
//
//		JCBlock methodBody = treeMaker.Block(0, List.of(returnStatement));
//		Name methodName = field.toName(toGetterName(fieldNode));
//		JCExpression methodType = fieldNode.type != null ? treeMaker.Type(fieldNode.type) : fieldNode.vartype;
//
//		List<JCTypeParameter> methodGenericParams = List.nil();
//		List<JCVariableDecl> parameters = List.nil();
//		List<JCExpression> throwsClauses = List.nil();
//		JCExpression annotationMethodDefaultValue = null;
//
//		return treeMaker.MethodDef(treeMaker.Modifiers(access, List.<JCAnnotation>nil()), methodName, methodType,
//				methodGenericParams, parameters, throwsClauses, methodBody, annotationMethodDefaultValue);
//	}
    private boolean isDefaultConstructor() {
        return true;
    }
//    private void addMissingSuperCall(final TypeElement element) {
//        final String className = element.getQualifiedName().toString();
//        final JCClassDecl classDeclaration =
//                // look up class declaration from a local map
//                this.findClassDeclarationForName(className);
//        if (classDeclaration == null) {
//            this.error(element, "Can't find class declaration for " + className);
//        } else {
//            this.info(element, "Creating renderHead(response) method");
//            final JCTree extending = classDeclaration.extending;
//            if (extending != null) {
//                final String p = extending.toString();
//                if (p.startsWith("com.myclient")) {
//                    // leave it alone, we'll edit the super class instead, if
//                    // necessary
//                    return;
//                } else {
//                    // @formatter:off (turns off eclipse formatter if configured)
//
//                    // define method parameter name
//                    final com.sun.tools.javac.util.Name paramName =
//                            elementUtils.getName("response");
//                    // Create @Override annotation
//                    final JCAnnotation overrideAnnotation =
//                            this.treeMaker.Annotation(
//                            Processor.buildTypeExpressionForClass(
//                            this.treeMaker,
//                            elementUtils,
//                            Override.class),
//                            // with no annotation parameters
//                            List.<JCExpression>nil());
//                    // public
//                    final JCModifiers mods =
//                            this.treeMaker.Modifiers(Flags.PUBLIC,
//                            List.of(overrideAnnotation));
//                    // parameters:(final IHeaderResponse response)
//                    final List<JCVariableDecl> params =
//                            List.of(this.treeMaker.VarDef(this.treeMaker.Modifiers(Flags.FINAL),
//                            paramName,
//                            Processor.buildTypeExpressionForClass(this.treeMaker,
//                            elementUtils,
//                            IHeaderResponse.class),
//                            null));
//
//                    //method return type: void
//                    final JCExpression returnType =
//                            this.treeMaker.TypeIdent(TypeTags.VOID);
//
//
//                    // super.renderHead(response);
//                    final List<JCStatement> statements =
//                            List.<JCStatement>of(
//                            // Execute this:
//                            this.treeMaker.Exec(
//                            // Create a Method call:
//                            this.treeMaker.Apply(
//                            // (no generic type arguments)
//                            List.<JCExpression>nil(),
//                            // super.renderHead
//                            this.treeMaker.Select(
//                            this.treeMaker.Ident(
//                            elementUtils.getName("super")),
//                            elementUtils.getName("renderHead")),
//                            // (response)
//                            List.<JCExpression>of(this.treeMaker.Ident(paramName)))));
//                    // build code block from statements
//                    final JCBlock body = this.treeMaker.Block(0, statements);
//                    // build method
//                    final JCMethodDecl methodDef =
//                            this.treeMaker.MethodDef(
//                            // public
//                            mods,
//                            // renderHead
//                            elementUtils.getName("renderHead"),
//                            // void
//                            returnType,
//                            // <no generic parameters>
//                            List.<JCTypeParameter>nil(),
//                            // (final IHeaderResponse response)
//                            params,
//                            // <no declared exceptions>
//                            List.<JCExpression>nil(),
//                            // super.renderHead(response);
//                            body,
//                            // <no default value>
//                            null);
//
//                    // add this method to the class tree
//                    classDeclaration.defs =
//                            classDeclaration.defs.append(methodDef);
//
//                    // @formatter:on turn eclipse formatter on again
//                    this.info(element,
//                            "Created renderHead(response) method successfully");
//
//                }
//            }
//
//        }
//    }
//    private static Filer filer;
//    private static Types types;
//    private IdentityHashMap<JCCompilationUnit, Void> compilationUnits;
//    private Map<String, JCCompilationUnit> typeMap;
}
