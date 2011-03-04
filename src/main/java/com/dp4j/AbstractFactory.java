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
  <li>Abstract Factory offers the interface for creating a family of related objects, without explicitly specifying their classes.</li>
</ul>
 *
<img src="http://www.oodesign.com/images/creational/abstract-factory-pattern.png" alt="Abstract Factory UML Class Diagram"/>

<h4>Motivation</h4>
<p>
Modularization is a big issue in today's programming. Programmers all over the world are trying to avoid the idea of adding code to existing classes in order to make them support encapsulating more general information. Take the case of a information manager which manages phone number. Phone numbers have a particular rule on which they get generated depending on areas and countries. If at some point the application should be changed in order to support adding numbers form a new country, the code of the application would have to be changed and it would become more and more complicated.
</p><p>
In order to prevent it, the Abstract Factory design pattern is used. Using this pattern a framework is defined, which produces objects that follow a general pattern and at runtime this factory is paired with any concrete factory to produce objects that follow the pattern of a certain country. In other words, the Abstract Factory is a super-factory which creates other factories (Factory of factories).
</p>


<h4>Applicability & Examples</h4>
<p>We should use the Abstract Factory design pattern when:</p>
<li>the system needs to be independent from the way the products it works with are created.</li>
<li>the system is or should be configured to work with multiple families of products.</li>
<li>a family of products is designed to work only all together.</li>
<li>the creation of a library of products is needed, for which is relevant only the interface, not the implementation, too.</li>

<h5>Phone Number Example</h5>
<p>The example at the beginning of the article can be extended to addresses, too. The AbstractFactory class will contain methods for creating a new entry in the information manager for a phone number and for an address, methods that produce the abstract products Address and PhoneNumber, which belong to AbstractProduct classes. The AbstractProduct classes will define methods that these products support: for the address get and set methods for the street, city, region and postal code members and for the phone number get and set methods for the number.
</p><p>
The ConcreteFactory and ConcreteProduct classes will implement the interfaces defined above and will appear in our example in the form of the USAddressFactory class and the USAddress and USPhoneNumber classes. For each new country that needs to be added to the application, a new set of concrete-type classes will be added. This way we can have the EnglandAddressFactory and the EnglandAddress and EnglandPhoneNumber that are files for English address information.</p>


<h5>Pizza Factory Example</h5>

<p>Another example, this time more simple and easier to understand, is the one of a pizza factory, which defines method names and returns types to make different kinds of pizza. The abstract factory can be named AbstractPizzaFactory, RomeConcretePizzaFactory and MilanConcretePizzaFactory being two extensions of the abstract class. The abstract factory will define types of toppings for pizza, like pepperoni, sausage or anchovy, and the concrete factories will implement only a set of the toppings, which are specific for the area and even if one topping is implemented in both concrete factories, the resulting pizzas will be different subclasses, each for the area it was implemented in.</p>


<h5>Look & Feel Example</h5>

<p>
Look & Feel Abstract Factory is the most common example. For example, a GUI framework should support several look and feel themes, such as Motif and Windows look. Each style defines different looks and behaviors for each type of controls: Buttons and Edit Boxes. In order to avoid the hardociding it for each type of control we define an abstract class LookAndFeel. This calls will instantiate, depending on a configuration parameter in the application one of the concrete factories: WindowsLookAndFeel or MotifLookAndFeel. Each request for a new object will be delegated to the instatiated concrete factory which will return the controls with the specific flavor.</p>

</body>
</html>

@see http://www.oodesign.com/abstract%2factory%2pattern.html
 */
@Documented
@Target(ElementType.TYPE)
public @interface AbstractFactory {

}
