package cpsc326;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            OurPL.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        if (stmt != null) {
            stmt.accept(this);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {

        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        // current env
        Environment previous = this.environment;

        // switch to new env
        this.environment = new Environment(previous);

        // execute statements in new env
        for (Stmt statement : stmt.statements) {
            execute(statement);
        }

        // switch back to the old env when done
        this.environment = previous;

        return null;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        // Evaluate the right side expression
        Object right = evaluate(expr.right);

        // Check the type of the unary operator
        switch (expr.operator.type) {
            case BANG:
                // For !, return the opposite boolean value
                return !isTruthy(right);
            case MINUS:
                // For -, negate the number
                checkNumberOperand(expr.operator, right);
                return -(double) right;
        }

        // Return null if there's no match
        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        // Check if the operand is a Double
        // If it's valid, return
        // Otherwise, throw an error
        try {
            if (operand instanceof Double)
                return;
        } catch (RuntimeError error) {
            throw new RuntimeError(operator, "Operand must be a number.");
        }
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        // Check if both left and right operands are Doubles
        // If both are valid numbers, return
        if (left instanceof Double && right instanceof Double)
            return;

        // Otherwise, throw an error
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        // Null is false, Booleans are themselves, everything else evaluates to true
        if (object == null)
            return false;

        // Extract explicit Boolean typings and cast them immediately
        if (object instanceof Boolean)
            return (Boolean) object;

        return true;
    }

    private boolean isEqual(Object left, Object right) {
        // If both are null, they are equal
        if (left == null && right == null)
            return true;

        // If only left is null, they are not equal
        if (left == null)
            return false;

        // Otherwise, check equality using Java's equals
        return left.equals(right);
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        // A grouping expression simply evaluates to its inner expression.
        return expr.expression.accept(this);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        // Evaluate the left side expression
        Object left = evaluate(expr.left);
        // Evaluate the right side expression
        Object right = evaluate(expr.right);

        // Check the type of the binary operator
        switch (expr.operator.type) {

            // Do the math for -, /, and *
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;

            // Add numbers or concatenate strings for +
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");

            // Compare numbers for >, >=, <, and <=
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;

            // Check equality for == and !=
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);

        }

        // Return null if there's no match
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == cpsc326.TokenType.OR) {
            if (isTruthy(left))
                return left;
        } else {
            if (!isTruthy(left))
                return left;
        }

        return evaluate(expr.right);
    }

}
