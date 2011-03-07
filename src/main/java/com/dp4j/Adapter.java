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
  <li>Convert the interface of a class into an interface clients expect.</li>
  <li>Adapter lets classes work together, that could not otherwise because of incompatible interfaces.</li>
</ul>

<img src="http://www.oodesign.com/images/structural/adapter-pattern.png" alt="Adapter Pattern Implementation UML Class Diagram"/>

<h4>Motivation</h4>
<p>
The adapter pattern adapts between classes and objects. Like any adapter in the real world it is used to be an interface, a bridge between two objects. In real world we have adapters for power supplies, adapters for camera memory cards, and so on. Probably everyone have seen some adapters for memory cards. If you can not plug in the camera memory in your laptop you can use and adapter. You plug the camera memory in the adapter and the adapter in to laptop slot. That's it, it's really simple.
</p><p>
What about software development? It's the same. Can you imagine an situation when you have some class expecting some type of object and you have an object offering the same features, but exposing a different interface? Of course, you want to use both of them so you don't to implement again one of them, and you don't want to change existing classes, so why not create an adapter...
</p>

<h4>Applicability & Examples</h4>
<p>The visitor pattern is used when:</p>

<li>When you have a class(Target) that invokes methods defined in an interface and you have a another class(Adapter) that doesn't implement the interface but implements the operations that should be invoked from the first class through the interface. You can change none of the existing code. The adapter will implement the interface and will be the bridge between the 2 classes.</li>
  <li>When you write a class (Target) for a generic use relying on some general interfaces and you have some implemented classes, not implementing the interface, that needs to be invoked by the Target class.</li>
<p>Adapters are encountered everywhere. From real world adapters to software adapters</p>
  <li># Non Software Examples of Adapter Patterns : Power Supply Adapters, card readers and adapters, ...
Software Examples of Adapter Patterns: Wrappers used to adopt 3rd parties libraries and frameworks - most of the applications using third party libraries use adapters as a middle layer between the application and the 3rd party library to decouple the application from the library. If another library has to be used only an adapter for the new library is required without having to change the application code.</li>

</body>
</html>

@see <a href="http://www.oodesign.com/adapter%2pattern.html">Adapter</a>
@see <a href="http://www.netobjectives.com/resources/webinars/encapsulate-entities-adapter-proxy-facade-design-patterns">Encapsulation of Entities (Adapter, Proxy, Facade)</a>
 */
@Documented
@Target(ElementType.TYPE)
public @interface Adapter {

}
