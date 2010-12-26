/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mysimpatico.se.dp4java.annotations.processors;

import com.mysimpatico.se.dp4java.annotations.singleton.Singleton;
import com.mysimpatico.se.dp4java.annotations.singleton.getInstance;
import com.mysimpatico.se.dp4java.annotations.singleton.instance;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic.Kind;
import org.codehaus.plexus.util.DirectoryScanner;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import com.sun.tools.javac.tree.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
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

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);
        elementUtils = JavacElements.instance(((JavacProcessingEnvironment) processingEnv).getContext());
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
            List<? extends Element> enclosedElements = e.getEnclosedElements();
            boolean getInstanceFound = false;
            boolean instanceFound = false;
            boolean privateConstructors = false;
            String enclosingClass = null;
            for (final Element element : enclosedElements) {
                if (element.getAnnotation(instance.class) != null) {
                    if (instanceFound == true) {
                        msgr.printMessage(Kind.ERROR, "Found multiple methods annotated with @getInstance while at most one must be annotated", e);
                    }
                    instanceFound = true;
                }
                if (element.getAnnotation(getInstance.class) != null) {
                    if (getInstanceFound == true) {
                        msgr.printMessage(Kind.ERROR, "Found multiple methods annotated with @getInstance while at most one must be annotated", e);
                    }
                    getInstanceFound = true;
                }

                /**
                 * TODO: figure out implicit empty constructor
                 */
                final Element defaultConstructor;
                Element temp = null;
                //com.sun.tools.javac.code.Flags.GENERATEDCONSTR
                if (element.getKind() == ElementKind.CONSTRUCTOR) {
                    Set<Modifier> constructorMods = element.getModifiers();
                    if (!constructorMods.contains(Modifier.PRIVATE)) {
                        if (constructorMods.contains(Modifier.PUBLIC)) {
                            MethodTree tree = (MethodTree) trees.getTree(element);
                            if (!tree.getParameters().isEmpty() || tree.getBody().getStatements().size() != 1) {
                                msgr.printMessage(Kind.ERROR, "Singleton constructors must be private", element);
                            } else {
                                temp = element;
                            }
                        } else {
                            msgr.printMessage(Kind.ERROR, "Singleton constructors must be private", e);
                        }
                    }
                }

                defaultConstructor = temp;

                enclosingClass = e.toString();
                final String anyDir = "**\\";
                final DirectoryScanner ds = new DirectoryScanner();
                final String file = enclosingClass.replaceAll("\\.", "\\" + File.separator);
                final String srcDir = System.getProperty("user.dir");
                final File classFile = new File(srcDir, file);
                final String classs = anyDir + classFile.getName() + ".java";
                ds.setIncludes(new String[]{classs});
                ds.setBasedir(srcDir);
                ds.scan();
                String[] includedFiles = ds.getIncludedFiles();
                if (includedFiles.length != 1) {
                    msgr.printMessage(Kind.ERROR, "not declared in separate file?" + e);
                }
                File srcFile = new File(srcDir, includedFiles[0]);
                if (!srcFile.exists()) {
                    msgr.printMessage(Kind.ERROR, "file?" + e);
                }
//                FileObject fileObj = FileUtil.toFileObject(srcFile);
//                final JavaSource classSource = JavaSource.forFileObject(fileObj);

                if (!getInstanceFound) {
                }


                //make default constructor private
                if (!privateConstructors) { // == defaultConstructor != null
                    JCCompilationUnit defConstructor = (JCCompilationUnit) trees.getPath(defaultConstructor).getCompilationUnit();
                    Set<Modifier> constructorModifiers = defaultConstructor.getModifiers();
                    JCTree tree = defConstructor.getTree();
                    defConstructor.defs.append(tree);
//                    tree.
                    constructorModifiers.remove(Modifier.PROTECTED);
                    constructorModifiers.remove(Modifier.PUBLIC);
                    constructorModifiers.add(Modifier.PRIVATE);

//                    Task<WorkingCopy> task = new Task<WorkingCopy>()   {
//
//                        @Override
//                        public void run(WorkingCopy workingCopy) throws Exception {
//                           workingCopy.toPhase(Phase.RESOLVED); // is it neccessary?
//
////                           final TreeMaker treeMaker = workingCopy.getTreeMaker();
//
//                            ModifiersTree Modifiers = treeMaker.Modifiers(constructorModifiers);
//
//                            MethodTree singletonConstructor = treeMaker.Constructor(Modifiers, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, "");
//                            ClassTree clazz = (ClassTree) trees.getTree(e);
//                            ClassTree modifiedClazz = treeMaker.addClassMember(clazz, singletonConstructor);
//                            workingCopy.rewrite(clazz, modifiedClazz);
//
//                        }
//                    };
//                    ModificationResult result;
//                    try {
//                        result = classSource.runModificationTask(task);
//                        result.commit();
//                    } catch (IOException ex) {
//                        Exceptions.printStackTrace(ex);
//                    }
                }
            }

            if (!getInstanceFound) {
                final String getInstanceMethod = "@getInstance public static " + enclosingClass + " getInstance(){ return instance; }";
            }
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
