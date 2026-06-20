package com.example.sen_calculator;

import java.util.Locale;

public class ExpressionEvaluator {

    public static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '%';
    }

    public static String balanceParentheses(String expr) {
        int openCount = 0;
        int closeCount = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') openCount++;
            else if (c == ')') closeCount++;
        }
        StringBuilder balanced = new StringBuilder(expr);
        while (openCount > closeCount) {
            balanced.append(')');
            closeCount++;
        }
        return balanced.toString();
    }

    public static String prepareForEvaluation(String expr) {
        if (expr.isEmpty()) return "";

        String balanced = balanceParentheses(expr);

        // Trim trailing operators, open parentheses, or dots
        while (!balanced.isEmpty()) {
            char last = balanced.charAt(balanced.length() - 1);
            if (isOperator(last) || last == '.' || last == '(') {
                balanced = balanced.substring(0, balanced.length() - 1);
            } else {
                break;
            }
        }

        return balanceParentheses(balanced);
    }

    public static String formatResult(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        } else {
            String str = String.format(Locale.US, "%.10f", value);
            if (str.contains(".")) {
                str = str.replaceAll("0+$", "");
                if (str.endsWith(".")) {
                    str = str.substring(0, str.length() - 1);
                }
            }
            return str;
        }
    }

    public static String toggleLastNumberSign(String expr) {
        if (expr.isEmpty()) {
            return "(-";
        }

        int len = expr.length();

        // Check if expression ends with a wrapped negative number: "...(-123)"
        if (expr.endsWith(")")) {
            int openParen = expr.lastIndexOf("(");
            if (openParen != -1 && openParen < len - 2 && expr.charAt(openParen + 1) == '-') {
                String prefix = expr.substring(0, openParen);
                String number = expr.substring(openParen + 2, len - 1);
                return prefix + number;
            }
        }

        // Scan backwards to find the boundaries of the last number
        int i = len - 1;
        while (i >= 0) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                i--;
            } else {
                break;
            }
        }

        int startOfNumber = i + 1;
        if (startOfNumber < len) {
            String prefix = expr.substring(0, startOfNumber);
            String number = expr.substring(startOfNumber);

            if (prefix.endsWith("(-")) {
                return prefix.substring(0, prefix.length() - 2) + number;
            } else {
                return prefix + "(-" + number + ")";
            }
        } else {
            return expr + "(-";
        }
    }

    public static double factorial(double n) {
        if (n < 0 || n != (long) n) {
            throw new ArithmeticException("Invalid factorial argument");
        }
        if (n > 170) {
            return Double.POSITIVE_INFINITY;
        }
        double res = 1;
        for (int i = 2; i <= (long) n; i++) {
            res *= i;
        }
        return res;
    }

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            boolean eatFunction(String name) {
                int len = name.length();
                if (pos + len < str.length() && str.substring(pos, pos + len).equals(name) && str.charAt(pos + len) == '(') {
                    pos += len + 1; // consume name and "("
                    ch = (pos < str.length()) ? str.charAt(pos) : -1;
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected character: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*') || eat('×')) x *= parseFactor();
                    else if (eat('/') || eat('÷')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x /= divisor;
                    } else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if (eatFunction("sin")) {
                    x = parseExpression();
                    eat(')');
                    x = Math.sin(Math.toRadians(x));
                } else if (eatFunction("cos")) {
                    x = parseExpression();
                    eat(')');
                    x = Math.cos(Math.toRadians(x));
                } else if (eatFunction("tan")) {
                    x = parseExpression();
                    eat(')');
                    // Handle tan(90) which is undefined
                    if (Math.abs(x % 180) == 90) throw new ArithmeticException("Undefined");
                    x = Math.tan(Math.toRadians(x));
                } else if (eatFunction("arctan")) {
                    x = parseExpression();
                    eat(')');
                    x = Math.toDegrees(Math.atan(x));
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected character: " + (char) ch);
                }

                // Check for percentage suffix or factorial suffix
                while (true) {
                    if (eat('%')) {
                        x = x / 100.0;
                    } else if (eat('!')) {
                        x = factorial(x);
                    } else {
                        break;
                    }
                }

                return x;
            }
        }.parse();
    }
}
