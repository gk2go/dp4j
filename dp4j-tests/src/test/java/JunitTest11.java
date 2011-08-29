/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import static org.junit.Assert.*;

/**
 * Works also with dp4j-1.0.
 *
 * For more details visit www.dp4j.com
 *
 * @author simpatico
 */
class Junit11 {
   private int one = 1;
   private static void p(int i, Double d, String... s){}
}

public class JunitTest11{

   @org.junit.Test
   public void t() {
      int two = new Junit11().one + 1;
      assertEquals(2,two);
      Junit11.p(two,new Double(2),"who", "said", "varargs are difficult to test with the reflection API?");
   }
}
