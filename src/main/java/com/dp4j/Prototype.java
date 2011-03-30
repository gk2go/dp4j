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
  <li>Specifying the kind of objects to create using a prototypical instance.</li>
  <li>Creating new objects by copying this prototype.</li>
</ul>

<img src="http://www.oodesign.com/images/stories/prototype%20implementation%20-%20uml%20class%20diagram.gif" alt="Prototype Implementation UML Class Diagram"/>

<h4>Motivation</h4>
<p>
Today's programming is all about costs. Saving is a big issue when it comes to using computer resources, so programmers are doing their best to find ways of improving the performance When we talk about object creation we can find a better way to have new objects: cloning. To this idea one particular design pattern is related: rather than creation it uses cloning. If the cost of creating a new object is large and creation is resource intensive, we clone the object.
</p><p>
The Prototype design pattern is the one in question. It allows an object to create customized objects without knowing their class or any details of how to create them. Up to this point it sounds a lot like the Factory Method pattern, the difference being the fact that for the Factory the palette of prototypical objects never contains more than one object.
</p>

<h4>Applicability & Examples</h4>
<p>Use Prototype Pattern when a system should be independent of how its products are created, composed, and represented, and:</p>

<li>Classes to be instantiated are specified at run-time</li>
<li>Avoiding the creation of a factory hierarchy is needed</li>
<li>It is more convenient to copy an existing instance than to create a new one.</li>

<h5>Example 1</h5>
<p>In building stages for a game that uses a maze and different visual objects that the character encounters it is needed a quick method of generating the haze map using the same objects: wall, door, passage, room... The Prototype pattern is useful in this case because instead of hard coding (using new operation) the room, door, passage and wall objects that get instantiated, CreateMaze method will be parameterized by various prototypical room, door, wall and passage objects, so the composition of the map can be easily changed by replacing the prototypical objects with different ones.
</p><p>
The Client is the CreateMaze method and the ConcretePrototype classes will be the ones creating copies for different objects.</p>

<h5>Example 2</h5>
<p>
Suppose we are doing a sales analysis on a set of data from a database. Normally, we would copy the information from the database, encapsulate it into an object and do the analysis. But if another analysis is needed on the same set of data, reading the database again and creating a new object is not the best idea. If we are using the Prototype pattern then the object used in the first analysis will be cloned and used for the other analysis.</p>
<p>
The Client is here one of the methods that process an object that encapsulates information from the database. The ConcretePrototype classes will be classes that, from the object created after extracting data from the database, will copy it into objects used for analysis.</p>

</body>
</html>

@see <a href="http://www.oodesign.com/prototype%2Dpattern.html">Prototype</a> */@Documented
@Target(ElementType.TYPE)
public @interface Prototype {

}
