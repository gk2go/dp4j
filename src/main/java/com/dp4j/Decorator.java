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
  <li>The intent of this pattern is to add additional responsibilities dynamically to an object.</li>
</ul>

<img src="http://www.oodesign.com/images/design_patterns/structural/decorator-design-pattern-implementation-uml-class-diagram.png" alt="Decorator Pattern Implementation UML Class Diagram"/>

<h4>Motivation</h4>
<p>
Extending an object s functionality can be done statically (at compile time) by using inheritance however it might be necessary to extend an object s functionality dynamically (at runtime) as an object is used.
</p><p>
Consider the typical example of a graphical window. To extend the functionality of the graphical window for example by adding a frame to the window, would require extending the window class to create a FramedWindow class. To create a framed window it is necessary to create an object of the FramedWindow class. However it would be impossible to start with a plain window and to extend its functionality at runtime to become a framed window.
</p>

<h4>Applicability & Examples</h4>

<h5>Example - Extending capabilities of a Graphical Window at runtime</h5>
<p>In Graphical User Interface toolkits windows behaviors can be added dynamically by using the decorator design pattern.</p>

<img src="http://www.oodesign.com/images/design_patterns/structural/decorator-design-pattern-example-uml-class-diagram.png" alt="Decorator Pattern Example UML Class Diagram"/>

</body>
</html>

@see <a href="http://www.oodesign.com/decorator%2Dpattern.html">Decorator</a>
 */

//FIXME: could not create @component in Windows

@Documented
@Target(ElementType.TYPE)
public @interface Decorator {

}
