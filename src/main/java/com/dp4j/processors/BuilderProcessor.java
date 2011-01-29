/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.processors;

import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.dp4j.*;

/**
 *
 * @author simpatico
 */
@SupportedAnnotationTypes("com.dp4j.Builder") //singleton instance
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class BuilderProcessor extends AbstractProcessor{

      @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Messager msgr = processingEnv.getMessager();
        for (Element e : roundEnv.getElementsAnnotatedWith(templateMethod.class)) {
           //TODO: make sure @product, @getResult are present
        }
        return true;
    }
    
}
