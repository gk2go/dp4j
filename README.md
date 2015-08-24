Dp4j is a Java Annotations Processor for testing private methods and implementing Design Patterns, find out more 
at www.dp4j.com/testscript and www.dp4j.com/singleton, or email me at ```gk@gk2go.com``` 

# Testing Private Methods in Java with Dp4j
In a nutshell, when you add dp4j.jar to your project's CLASSPATH, Dp4j's Annotations Processor will analyze your @Test
annotated methods, and find out if you are trying to access private methods. If so, it will remove your invalid code 
and replace it with the equivalent Reflection code that will work. The reflection code is injected directly in the AST
of your code every time you compile it, and not in the source file so that you can freely edit your code. To see the 
generated reflection code, add the compiler parameter ```-Averbose=true``` as shown here:

```javac -cp dp4j-1.0-jar-with-dependencies.jar -Averbose=true REPLACE-WITH-YOUR-JAVA-CLASS.java```


#### P.S. Contributions to this project are welcome!

