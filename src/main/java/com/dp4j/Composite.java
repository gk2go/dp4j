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
  <li>The intent of this pattern is to compose objects into tree structures to represent part-whole hierarchies.</li>
  <li>Composite lets clients treat individual objects and compositions of objects uniformly.</li>
</ul>

<img src="http://www.oodesign.com/images/design_patterns/structural/composite-design-pattern-implementation-uml-class-diagram.png" alt="Composite Pattern Implementation UML Class Diagram"/>

<h4>Motivation</h4>
<p>
There are times when a program needs to manipulate a tree data structure and it is necessary to treat both Branches as well as Leaf Nodes uniformly. Consider for example a program that manipulates a file system. A file system is a tree structure that contains Branches which are Folders as well as Leaf nodes which are Files. Note that a folder object usually contains one or more file or folder objects and thus is a complex object where a file is a simple object. Note also that since files and folders have many operations and attributes in common, such as moving and copying a file or a folder, listing file or folder attributes such as file name and size, it would be easier and more convenient to treat both file and folder objects uniformly by defining a File System Resource Interface.
</p>

<h4>Applicability & Examples</h4>
<p>The composite pattern applies when there is a part-whole hierarchy of objects and a client needs to deal with objects uniformly regardless of the fact that an object might be a leaf or a branch.</p>

<h5>Example - Graphics Drawing Editor.</h5>
<p>In graphics editors a shape can be basic or complex. An example of a simple shape is a line, where a complex shape is a rectangle which is made of four line objects. Since shapes have many operations in common such as rendering the shape to screen, and since shapes follow a part-whole hierarchy, composite pattern can be used to enable the program to deal with all shapes uniformly.
In the example we can see the following actors:

   <li>Shape (Component) - Shape is the abstraction for Lines, Rectangles (leafs) and and ComplexShapes (composites).</li>
  <li>Line, Rectangle (Leafs) - objects that have no children. They implement services described by the Shape interface.</li>
  <li>ComplexShape (Composite) - A Composite stores child Shapes in addition to implementing methods defined by the Shape interface.</li>
  <li>GraphicsEditor (Client) - The GraphicsEditor manipulates Shapes in the hierarchy.</li></p>

<p>Alternative Implementation: Note that in the previous example there were times when we have avoided dealing with composite objects through the Shape interface and we have specifically dealt with them as composites (when using the method addToShape()). To avoid such situations and to further increase uniformity one can add methods to add, remove, as well as get child components to the Shape interface. The UML diagram below shows it:</p>

<img src="http://www.oodesign.com/images/design_patterns/structural/composite-design-pattern-alternative-implementation-uml-class-diagram.png" alt="Composite Pattern Implementation UML Class Diagram"/>

</body>
</html>

@see <a href="http://www.oodesign.com/composite%2pattern.html">Composite</a>
 */
@Documented
@Target(ElementType.TYPE)
public @interface Composite {

}
