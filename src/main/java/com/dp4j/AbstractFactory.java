/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
/**<b>Intent:</b> Provide an interface for creating families of related or dependent objects without specifying their concrete classes. [GoF, p87]
<html>
<body>
<img src="http://www.oodesign.com/images/creational/abstract-factory-pattern.png" alt="Abstract Factory UML Class Diagram"/>

<h4>Motivation</h4>
<p>If an application is to be portable, it needs to encapsulate platform dependencies. These "platforms" might include: windowing system, operating system, database, etc.<br/>
An Abstract Factory is implemented by a concrete factory for each platform. Each factory has the responsibility for providing creation services for the entire platform family. Clients never create platform objects directly, they ask the factory to do that for them. The binding to a concrete factory occurs at run-time.</p>


<h4>Usage & Examples</h4>
 Used when:
<li>the system should be configurable to work with multiple families of products.</li>
<li>a family of products is designed to work only all together.</li>
<li>the system needs to be independent from the way the products it works with are created.</li>

<h5>Look & Feel Example</h5>
<p>Building a GUI that works on top of different platforms Look&Feels, like MS-Windows and Motif, is the most common example for using an Abstract Factory. The client interacts with a LookAndFeel Abstract Factory  which defines the methods to create the desired Widgets (buttons, Menus, etc.), and the interfaces of those widgets. Then for each Look&Feel supported, a concrete subclass implements LookAndFeel factory methods, returning the concrete widget for each platform.
Finally, at run-time (through a preference option, for example) the client is bound to the desired concrete LookAndFeel factory.</p>

</body>
</html>
@see <a href="http://en.wikipedia.org/wiki/Abstract_factory_pattern#Java">GUIFactory Example Code</a>
@see <a href="http://www.oodesign.com/abstract%2factory%2pattern.html">Abstract Factory</a>
 */
@Documented
@Target(ElementType.TYPE)
public @interface AbstractFactory {

}
