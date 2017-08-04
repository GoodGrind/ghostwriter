Assuming that you have a working Ant/Maven/Gradle installation on your specified path, you can see the
"GhostWriter-ed" FizzBuzz application in action by executing one (or all if you are curious) of the following commands:


### Maven

```
mvn clean package exec:java
```

### Gradle
 
```
gradle clean build run
```

### Plain Javac (used for the screencast)

Without GhostWriter support:
```
echo "Compiling and running a plain Java application (FizzBuzz)..."
javac -d bin/ src/main/java/io/ghostwriter/sample/fizzbuzz/FizzBuzz.java
java -cp bin/ io.ghostwriter.sample.fizzbuzz.FizzBuzz 0 10
echo "Some output, no details on what is going on and why..."
```

With GhostWriter support:
```
echo "Compiling the same Java application with the GhostWriter instrumenter dependency"
javac -d bin/ -cp bin/ghostwriter-jdk-v8.jar:bin/ghostwriter-api-java.jar src/main/java/io/ghostwriter/sample/fizzbuzz/FizzBuzz.java
echo "... then running it with the GhostWriter Tracer runtime..."
java -cp bin/:bin/ghostwriter-api-java.jar:bin/ghostwriter-rt-tracer.jar io.ghostwriter.sample.fizzbuzz.FizzBuzz 0 10
echo "Without chaning the application we have fine-grained logging when we need it!" 
```

### Print the generated source
With the _-printsource_ flag instead of compiling the instrumented code, _javac_ will print it out.
```
javac -printsource -d bin/ -cp bin/ghostwriter-jdk-v8.jar:bin/ghostwriter-api-java.jar src/main/java/io/ghostwriter/sample/fizzbuzz/FizzBuzz.java 
```

