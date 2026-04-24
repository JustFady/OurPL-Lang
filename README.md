# OurPL-Lang

Small language I built from scratch — lexer, parser, and interpreter in Java.

Needs Java 21 and [Maven](https://maven.apache.org/install.html).

## run

```bash
mvn exec:java -Dexec.args="examples/example1.opl"
```

if something looks off, clean build first:

```bash
mvn clean compile exec:java -Dexec.args="examples/example1.opl"
```

## test

```bash
mvn test
```
