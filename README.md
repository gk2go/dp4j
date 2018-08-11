Dp4j is a Java Annotations Processor for testing private methods and implementing Design Patterns. You can see it in action with this testscript (for details, [see wiki](https://github.com/gk2go/dp4j/wiki/Testscript)):
```
wget https://github.com/gk2go/dp4j/releases/download/dp4j-1.2/TESTDRIVE ; chmod +x TESTDRIVE ; ./TESTDRIVE
``` 

# Testing Private Methods in Java
In a nutshell, when you add ```dp4j.jar``` to your project's ```CLASSPATH```, Dp4j's Annotations Processor will analyze your ```@Test``` annotated methods and find out if you are trying to access private methods. It will then replace invalid code with equivalent Reflection API code that will work. The reflection code is injected directly in the AST of your code every time you compile it, and not in the source file so that you can freely edit your code. To see the generated reflection code, add the compiler parameter ```-Averbose=true``` as shown here:

```javac -cp dp4j-1.0-jar-with-dependencies.jar -Averbose=true REPLACE-WITH-YOUR-JAVA-CLASS.java```

# Publications
[Implementing patterns with annotations](https://github.com/gk2go/dp4j/wiki/Testscript)

[Compile-time checked Reflection API](Compile-Time%20Checked%20Reflection%20API.pdf) 

# P.S. Contributions welcome!

