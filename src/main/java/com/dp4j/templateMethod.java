/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j;
import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;


/**
<html>
<body>

<h4>Intent:</h4>
<ul>
  <li>lets subclasses redefine certain steps of an algorithm without changing the algorithm's structure.</li>
  <li>Define the skeleton of an algorithm.</li>
</ul>

<img src="http://www.oodesign.com/images/design_patterns/behavioral/template_method_implementation_-_uml_class_diagram.gif" alt="Template Method Pattern Implementation UML Class Diagram"/>

@see <a href="http://www.oodesign.com/composite%2pattern.html">Template Method</a>
 */
@Documented
@Target(ElementType.METHOD)
public @interface templateMethod {

}
