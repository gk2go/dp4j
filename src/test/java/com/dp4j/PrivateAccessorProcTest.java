/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j;

import com.dp4j.processors.PrivateAccessProcessor;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.processing.Processor;

import org.junit.Test;

/**
 * Unit tests for the {@code CompositeAnnotationValidationProcessor}.
 * <p>
 * Requires the <em>source</em> of the sample classes (i.e. the {@code .java}
 * files) to be available on the classpath.
 *
 * @author aphillips
 * @since 26 May 2009
 *
 */
public class PrivateAccessorProcTest extends AbstractAnnProcTest {

    @Override
    protected Collection<Processor> getProcessors() {
        return Arrays.<Processor> asList(new PrivateAccessProcessor());
    }

    @Test
    public void leafAnnotationOnNonCompositeMember() {
        String pathForTestCase = getPathForTestCase("T");
        assertCompilationSuccessful(compileTestCase(pathForTestCase));
    }

//    @Test
//    public void invalidLeafAnnotation_nonAnnotationReturnType() {
//        assertCompilationReturned(Kind.ERROR, 26,
//                compileTestCase(InvalidLeafAnnotationNonAnnotationReturnType.class));
//    }
//
//    @Test
//    public void invalidLeafAnnotation_invalidRetention() {
//        assertCompilationReturned(Kind.ERROR, 28,
//                compileTestCase(InvalidLeafAnnotationInvalidRetention.class));
//    }
//
//    @Test
//    public void invalidLeafAnnotation_invalidCustomFactories() {
//        assertCompilationReturned(new Kind[] { Kind.ERROR, Kind.ERROR }, new long[] { 26, 30 },
//                compileTestCase(InvalidLeafAnnotationInvalidCustomFactories.class));
//    }
//
//    @Test
//    public void invalidCompositeAnnotation_invalidRetention() {
//        assertCompilationReturned(Kind.ERROR, 22,
//                compileTestCase(InvalidCompositeAnnotationInvalidRetention.class));
//    }
//
//    @Test
//    public void invalidCompositeAnnotation_inconsistentTarget() {
//        assertCompilationReturned(Kind.ERROR, 27,
//                compileTestCase(InvalidCompositeAnnotationInconsistentTarget.class));
//    }
//
//    @Test
//    public void invalidCompositeAnnotation_nonuniqueLeafAnnotationType() {
//        assertCompilationReturned(Kind.ERROR, 24,
//                compileTestCase(InvalidCompositeAnnotationNonuniqueLeafAnnotationTypes.class));
//    }
//
//    @Test
//    public void nonuniqueAnnotationType_fromStandardAndComposite() {
//        assertCompilationReturned(Kind.ERROR, 22,
//                compileTestCase(InvalidStandardAndCompositeUsage.class));
//    }
//
//    @Test
//    public void validCompositeAnnotation() {
//        assertCompilationSuccessful(compileTestCase(ValidCompositeAnnotation.class));
//    }

}
