/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j;

/**
<html>
<body>

<h4>Intent:</h4>
<ul>
  <li>Defines an instance for creating an object but letting subclasses decide which class to instantiate.</li>
  <li>Refers to the newly created object through a common interface.</li>
</ul>

<img src="http://www.oodesign.com/images/creational/builder-pattern.png" alt="Builder Pattern UML Class Diagram"/>

<h4>Motivation</h4>
<p>
The more complex an application is the complexity of classes and objects used increases. Complex objects are made of parts produced by other objects that need special care when being built. An application might need a mechanism for building complex objects that is independent from the ones that make up the object. If this is the problem you are being confronted with, you might want to try using the Builder (or Adaptive Builder) design pattern.
</p><p>
This pattern allows a client object to construct a complex object by specifying only its type and content, being shielded from the details related to the object s representation. This way the construction process can be used to create different representations. The logic of this process is isolated form the actual steps used in creating the complex object, so the process can be used again to create a different object form the same set of simple objects as the first one.
</p>

<h4>Applicability & Examples</h4>
<p>Builder Pattern is used when:</p>

<h5>Example 1 - Vehicle Manufacturer.</h5>
<p>Let us take the case of a vehicle manufacturer that, from a set of parts, can build a car, a bicycle, a motorcycle or a scooter. In this case the Builder will become the VehicleBuilder. It specifies the interface for building any of the vehicles in the list above, using the same set of parts and a different set of rules for every type of type of vehicle. The ConcreteBuilders will be the builders attached to each of the objects that are being under construction. The Product is of course the vehicle that is being constructed and the Director is the manufacturer and its shop.</p>


<h5>Example 2 - Students Exams.</h5>

<p>If we have an application that can be used by the students of a University to provide them with the list of their grades for their exams, this application needs to run in different ways depending on the user that is using it, user that has to log in. This means that, for example, the admin needs to have some buttons enabled, buttons that needs to be disabled for the student, the common user. The Builder provides the interface for building form depending on the login information. The ConcreteBuilders are the specific forms for each type of user. The Product is the final form that the application will use in the given case and the Director is the application that, based on the login information, needs a specific form.</p>

</body>
</html>

@see <a href="http://www.oodesign.com/builder%2Dpattern.html">Builder</a>
 */

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


@Documented
@Target(ElementType.TYPE)
public @interface Builder {

}
