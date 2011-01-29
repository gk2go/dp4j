/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.processors;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;

/**
 *
 * @author simpatico
 */
@SupportedAnnotationTypes(value={"com.dp4j.Builder", "org.jpatterns.gof.BuilderPattern"}) //singleton instance
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class BuilderProcessor extends DProcessor{

    @Override
    protected void processElement(Element e) {
        //TODO: make sure @product, @getResult are present
    }

}
