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
  <li>The intent of this pattern is to use sharing to support a large number of objects that have part of their internal state in common where the other part of state can vary.</li>
</ul>

<img src="http://www.oodesign.com/images/design_patterns/structural/flyweight-design-pattern-implementation-uml-class-diagram.png" alt="Flyweight Pattern Implementation UML Class Diagram"/>

<h4>Motivation</h4>
<p>
Some programs require a large number of objects that have some shared state among them. Consider for example a game of war, were there is a large number of soldier objects; a soldier object maintain the graphical representation of a soldier, soldier behavior such as motion, and firing weapons, in addition soldier s health and location on the war terrain. Creating a large number of soldier objects is a necessity however it would incur a huge memory cost. Note that although the representation and behavior of a soldier is the same their health and location can vary greatly.
</p>

<h4>Applicability & Examples</h4>
<p>The flyweight pattern applies to a program using a huge number of objects that have part of their internal state in common where the other part of state can vary. The pattern is used when the larger part of the object s state can be made extrinsic (external to that object).</p>

<h5>Example - The war game.</h5>
<p>The war game instantiates 5 Soldier clients, each client maintains its internal state which is extrinsic to the soldier flyweight. And Although 5 clients have been instantiated only one flyweight Soldier has been used.</p>

<img src="http://www.oodesign.com/images/design_patterns/structural/flyweight-design-pattern-example-uml-class-diagram.png" alt="Flyweight Pattern Example UML Class Diagram"/>

</body>
</html>

@see <a href="http://www.oodesign.com/flyweight%2Dpattern.html">Flyweight</a> */@Documented
@Target(ElementType.TYPE)
public @interface Flyweight {

}
