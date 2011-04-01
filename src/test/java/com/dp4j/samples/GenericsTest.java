/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import junit.framework.Assert;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 * @author simpatico
 */
public class GenericsTest {

    @Test
    public void test() {
        Genericer.getAll(String.class);
        Assert.assertTrue(Genericer.getAll(String.class) == null);
        assertThat(4, is(4));
    }
}
