package cpsc326;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cpsc326.TokenType.*;

class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> keywords;

    Lexer(String source) {
        this.source = source;
    }

    static {
        keywords = new HashMap<>();

        keywords.put("(", LEFT_PAREN);
        keywords.put(")", RIGHT_PAREN);
        keywords.put("}", RIGHT_BRACE);
        keywords.put("}", LEFT_BRACE);

        keywords.put(",", COMMA);
        keywords.put(".", DOT);
        keywords.put("+", PLUS);
        keywords.put("-", MINUS);
        keywords.put("*", STAR);
        keywords.put("/", SLASH);
        keywords.put(";", SEMICOLON);

        keywords.put("!", BANG);
        keywords.put("!=", BANG_EQUAL);
        keywords.put(">", GREATER);
        keywords.put(">=", GREATER_EQUAL);
        keywords.put("<", LESS);
        keywords.put("<=", LESS_EQUAL);
        keywords.put("=", EQUAL);
        keywords.put("==", EQUAL_EQUAL);


        //remove keywords before here
        keywords.put("and", AND);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR); // double check this
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("struct", STRUCT);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);

        keywords.put("", EOF);
        
        
    }

    /*- `scanToken()` must call `string()`,
     `number()`, and `identifier()` when needed. */

    List<Token> scanTokens() {
        //while not at the end of stream
        while (!isAtEnd()) {
            //set start to current and begin reading in tokens recursively
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        // current index is past or at the total length of source stream
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        //charAt receives specific index within a string
        return source.charAt(current + 1);
    }

     private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isAlphaNumeric(char c) {
        return c == '_' || isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type) {
        addToken(type,null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

private void string() {
    // continue until we reach the end
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') 
                line++;
            advance();
        }

        if (isAtEnd()) {
            OurPL.error(line, "Unterminated string.");
            return;
        }

            // for the closing " too 
        advance();
        //take whats inside the quotes
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private void number() {
        //while theres a number keep parsing
        while (isDigit(peek())) 
            advance();

        // same thing here but keep parsing after taking dot 
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }
            //parse string into number
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void identifier() {
        
        while (isAlphaNumeric(peek())) 
            advance();

        //extract scanned word
        String text = source.substring(start, current);
        //cbeck if the keyword exists
        TokenType type = keywords.get(text);
        
        if (type == null) {
            type = IDENTIFIER;
        }
        
        addToken(type);
    }

private void scanToken() {
        //consume and advance
    char c = advance();

        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '+': addToken(PLUS); break;
            case '-': addToken(MINUS); break;
            case '*': addToken(STAR); break;
            case '/': addToken(SLASH); break;
            case ';': addToken(SEMICOLON); break;
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '#':
                while (peek() != '\n' && !isAtEnd()) {
                    advance();
                }
                break;

                // whitespace
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                //
                line++;
                break;
            case '"':
                string();
                break;


            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    OurPL.error(line, "Unexpected character.");
                }
                break;
        }
    }
}
