/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
/**
<html>
<body>

<h4>Intent:</h4>
<ul>
  <li>The intent of this pattern is to provide a  Placeholder  for an object to control references to it.</li>
</ul>

<img src="http://www.oodesign.com/images/design_patterns/structural/proxy-design-pattern-implementation-uml-class-diagram.png" alt="Proxy Pattern UML Class Diagram"/>

<h4>Motivation</h4>
<p>
Sometimes we need the ability to control the access to an object. For example if we need to use only a few methods of some costly objects we'll initialize those objects when we need them entirely. Until that point we can use some light objects exposing the same interface as the heavy objects. These light objects are called proxies and they will instantiate those heavy objects when they are really need and by then we'll use some light objects instead.
</p><p>
This ability to control the access to an object can be required for a variety of reasons: controlling when a costly object needs to be instantiated and initialized, giving different access rights to an object, as well as providing a sophisticated means of accessing and referencing objects running in other processes, on other machines.
</p><p>
Consider for example an image viewer program. An image viewer program must be able to list and display high resolution photo objects that are in a folder, but how often do someone open a folder and view all the images inside. Sometimes you will be looking for a particular photo, sometimes you will only want to see an image name. The image viewer must be able to list all photo objects, but the photo objects must not be loaded into memory until they are required to be rendered.
</p>

<h4>Applicability & Examples</h4>
<p>The Proxy design pattern is applicable when there is a need to control access to an Object, as well as when there is a need for a sophisticated reference to an Object. Common Situations where the proxy pattern is applicable are:</p>

<li>Virtual Proxies: delaying the creation and initialization of expensive objects until needed, where the objects are created on demand (For example creating the RealSubject object only when the doSomething method is invoked).</li>
<li>Remote Proxies: providing a local representation for an object that is in a different address space. A common example is Java RMI stub objects. The stub object acts as a proxy where invoking methods on the stub would cause the stub to communicate and invoke methods on a remote object (called skeleton) found on a different machine.</li>
<li>Protection Proxies: where a proxy controls access to RealSubject methods, by giving access to some objects while denying access to others.</li>
<li>Smart References: providing a sophisticated access to certain objects such as tracking the number of references to an object and denying access if a certain number is reached, as well as loading an object from database into memory on demand.</li>

<h5>Example - Virtual Proxy Example.</h5>
<p>Consider an image viewer program that lists and displays high resolution photos. The program has to show a list of all photos however it does not need to display the actual photo until the user selects an image item from a list.
</p>

<img src="http://www.oodesign.com/images/design_patterns/structural/proxy-design-pattern-implementation-uml-class-diagram.png" alt="Proxy Pattern Virtual Proxy Example UML Class Diagram"/>

</body>
</html>

@see <a href="http://www.oodesign.com/proxy%2Dpattern.html">Proxy</a>
@see <a href="http://www.netobjectives.com/resources/webinars/encapsulate-entities-adapter-proxy-facade-design-patterns">Encapsulation of Entities (Adapter, Proxy, Facade)</a>
 */
@Documented
@Target(ElementType.TYPE)
public @interface Proxy {

}
