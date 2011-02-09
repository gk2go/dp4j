package com.dp4j.singleton;

import com.dp4j.AbstractAnnotationProcessorTest;
import com.dp4j.processors.GetInstanceProcessor;
import com.dp4j.processors.InstanceProcessor;
import com.dp4j.processors.SingletonProcessor;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.processing.Processor;

public abstract class AbstractSingletonProcTest extends AbstractAnnotationProcessorTest{

    @Override
    protected Collection<Processor> getProcessors() {
        return Arrays.<Processor>asList(new SingletonProcessor(), new InstanceProcessor(), new GetInstanceProcessor());
    }
}