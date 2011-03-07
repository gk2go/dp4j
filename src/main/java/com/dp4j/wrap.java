/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j;

/**
 * <h5>Facade vs Adapter</h5>
 <p>
Facade defines a new interface, whereas Adapter uses an old interface. Adapter makes two existing interfaces work together as opposed to defining an entirely new one. [GoF, p219]</p>

<h5>Mediator vs Facade</h5>
<p>Mediator is similar to Facade in that it abstracts functionality of existing classes. Mediator abstracts/centralizes arbitrary communications between colleague objects. It routinely "adds value", and it is known/referenced by the colleague objects. In contrast, Facade defines a simpler interface to a subsystem, it doesn't add new functionality, and it is not known by the subsystem classes. [GoF. p193]</p>

 *
@see <a href="http://www.netobjectives.com/resources/webinars/encapsulate-entities-adapter-proxy-facade-design-patterns">Encapsulation of Entities (Adapter, Proxy, Facade)</a>
 */
public @interface wrap {
    Class pattern() default wrap.class; //notSpecified
    //TODO: processor error if the class is not a pattern && not annotated with a pattern
}
