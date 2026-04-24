package cpsc326;

import java.util.*;
import static cpsc326.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = logic_or();

        if (match(EQUAL)) {
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;

                return new Expr.Assign(name, value);
            }

            throw error(previous(), "Invalid assignment target.");
        }
        return expr;
    }

    private Expr logic_or() {// done? updated from binary to logical
        Expr expr = logic_and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = logic_and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr logic_and() { // done? updated from binary to logical
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Stmt exprStmt() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt printStmt() {

        Expr expr = expression();
        consume(SEMICOLON, "");

        return new Stmt.Print(expr);
    }

    private Stmt varDecl() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;

        if (match(EQUAL))
            initializer = expression();

        // incase its something like just var x;
        consume(SEMICOLON, "Expecting ';'");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {

        if (match(PRINT))
            return printStmt();

        // same idea for the next 3, I don't need to match on the
        // statements first cause they're handled here
        if (match(IF))
            return ifStmt();
        if (match(WHILE))
            return whileStmt();
        if (match(FOR))
            return forStmt();

        if (match(LEFT_BRACE)) // don't need to match in block because its here
            return block();
        return exprStmt();
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDecl();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt whileStmt() {
        consume(LEFT_PAREN, "Expecting '(' after while statement.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expecting ')' after while statement.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt ifStmt() {
        consume(LEFT_PAREN, "Expecting '(' after while statement.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expecting ')' after while statement.");
        Stmt body = statement();

        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, body, elseBranch);
    }

    private Stmt forStmt() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        // get the first part of the for loop, which happens once.
        // could be empty, a new variable, or just an expression
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDecl();
        } else {
            initializer = exprStmt();
        }

        // grab the condition that runs before each loop
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        // get the increment part that runs at the end of each loop
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        // the actual code inside the loop
        Stmt body = statement();

        // since we don't have a real for loop we turn it into a while loop

        // slap the increment on the end of the body
        if (increment != null) {
            body = new Stmt.Block(List.of(body, new Stmt.Expression(increment)));
        }

        // if theres no condition like for(;;) make it run forever
        if (condition == null) {
            condition = new Expr.Literal(true);
        }

        // turn it into a while loop
        body = new Stmt.While(condition, body);

        // put the initializer right before the while loop starts
        if (initializer != null) {
            body = new Stmt.Block(List.of(initializer, body));
        }

        return body;
    }

    private Stmt block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expecting '}' after block.");

        return new Stmt.Block(statements);
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() { // same
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();

            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() { // same
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() { // same
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() { // same
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() { // updated for identifier
        if (match(FALSE))
            return new Expr.Literal(false);
        if (match(TRUE))
            return new Expr.Literal(true);

        if (match(NIL))
            return new Expr.Literal(null);
        if (match(NUMBER, STRING))
            return new Expr.Literal(previous().literal);

        if (match(IDENTIFIER))
            return new Expr.Variable(previous());

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')'");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        OurPL.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON)
                return;
            switch (peek().type) {
                case STRUCT:
                case FOR:
                case FUN:
                case IF:
                case PRINT:
                case RETURN:
                case VAR:
                case WHILE:
                    return;
            }

            advance();
        }
    }

    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
