
package com.dp4j;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
<html>
<body>

<h4>Intent:</h4>
<ul>
  <li>Ensure that only one instance of a class is created.</li>
  <li>Provide a global point of access to the object.</li>
</ul>

<img src="http://www.oodesign.com/images/design_patterns/creational/singleton_implementation_-_uml_class_diagram.gif" alt="Singleton UML Class Diagram"/>

<h4>Motivation</h4>
<p>A Singleton Object maybe used to represent a resource that:
 * <ul>
 <li>is unique e.g. the file system, or a print spooler. Since the OS provides only one print spooler it'd be inconvenient to create multiple instances of our interface to it, since then all those instances must coordinate the fact that they represent the same resource (e.g. merge print job queues).</li>
 * <li>accessed with the same configuration throughout the system. This may refer to a password-protected shared service. Indeed, Facade and State Objects are often Singletons,  [GoF p193,313] and Abstract Factory, Builder, and Prototype can use Singleton in their implementation. [GoF, p134]</li>
 <li>is expensive to instantiate (e.g. remote resources).</li>
 * </ul>
</p>

</body>
</html>

* @see <a href="http://www.oodesign.com/singleton%2Dpattern.html">Singleton</a>
**/
@Documented
@Target(ElementType.TYPE)
public @interface Singleton {
    boolean lazy() default false;
    String getInstance() default "getInstance";
    String instance() default "instance";
}
