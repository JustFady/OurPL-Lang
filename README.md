# OurPL-Lang

Small language I built from scratch — lexer, parser, and interpreter in Java.

## dependencies

- [Java 21](https://www.oracle.com/java/technologies/downloads/#java21)
- [Maven](https://maven.apache.org/install.html)

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

## what's in it

- variables, strings, numbers, nil
- arithmetic, comparison, and logical operators
- short-circuit evaluation (`and`, `or`)
- `if` / `else`, `while`, `for`
- block scoping
- `print`
- functions and closures
- `return` statements

## what's next

- classes and structs
- arrays
- string interpolation
