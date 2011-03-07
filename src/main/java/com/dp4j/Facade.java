/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
/** Facade is a single object that represent an entire subsystem. <b>Intent:</b> Provide a unified interface  to a set of interfaces in a subsystem. Facade defines a higher-level interface that makes the subsystem easier to use. [GoF, p185]
<html>
<body>
<img src="http://home.earthlink.net/~huston2/images/gof10.jpg" alt="Facade UML Class Diagram"/>


<h4>Example</h4>
<p>Consumers encounter a Facade when ordering from a catalog. The consumer calls one number and speaks with a customer service representative. The customer service representative acts as a Facade, providing an interface to the order fulfillment department, the billing department, and the shipping department. [Michael Duell, "Non-software examples of software design patterns", Object Magazine, Jul 97, p54]</p>


</body>
</html>

* @see <a href="http://home.earthlink.net/~huston2/dp/facade.html">Facade</a>
* @see <a href="http://www.netobjectives.com/resources/webinars/encapsulate-entities-adapter-proxy-facade-design-patterns">Encapsulation of Entities (Adapter, Proxy, Facade)</a>
**/
@Documented
@Target(ElementType.TYPE)
public @interface Facade {

}
