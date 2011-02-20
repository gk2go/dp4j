/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.samples;

import com.dp4j.InjectReflection;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author simpatico
 */
public class PrivateConstructorTest {

    @InjectReflection
    public PrivateConstructorTest() {
        ASingleton singleton = new ASingleton();
        final List<String[]> mengsWithSharedExp = new LinkedList<String[]>();
        final String[][] mengs = {{"...so geht die Legende ...", ""}};
//        final String[] expressions = uniqueStrings(mengs).toArray(new String[0]);
    }

    public static LinkedList<String> uniqueStrings(final String[][] mengs) {
        final LinkedList<String> exps = new LinkedList<String>();
        for (String[] meng : mengs) {
            if (!exps.contains(meng[0])) {
                exps.add(meng[0]);
            }
            if (!exps.contains(meng[1])) {
                exps.add(meng[1]);
            }
        }
        return exps;
    }

    @Test
    public void test() {
        ASingleton singleton = new ASingleton();
    }
//FIXME: injected code is not available to other processors already!
//    @Test
//    public void instanceTest() {
//        ASingleton.instance = null;
//    }
}
