dp4j.jar in your project classpath lets you access from your tests private methods as if they were public!
It also helps you implement Design Patterns such as the Singleton Pattern.

No changes are made to the source code when testing private methods, rather dp4j injects the Reflection API required to access the method in your body of your test method at compile-time.