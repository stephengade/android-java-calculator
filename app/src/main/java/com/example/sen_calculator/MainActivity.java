package com.example.sen_calculator;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvExpression;
    private TextView tvResult;
    private String expression = "";
    private boolean isErrorState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize displays
        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);

        // Bind number buttons
        int[] numberIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };
        for (int id : numberIds) {
            findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MaterialButton btn = (MaterialButton) v;
                    appendNumber(btn.getText().toString());
                }
            });
        }

        // Bind operator buttons
        findViewById(R.id.btnPlus).setOnClickListener(v -> appendOperator("+"));
        findViewById(R.id.btnSubtract).setOnClickListener(v -> appendOperator("-"));
        findViewById(R.id.btnMultiply).setOnClickListener(v -> appendOperator("×"));
        findViewById(R.id.btnDivide).setOnClickListener(v -> appendOperator("÷"));
        findViewById(R.id.btnPercent).setOnClickListener(v -> appendOperator("%"));

        // Bind function buttons
        findViewById(R.id.btnDot).setOnClickListener(v -> appendDot());
        findViewById(R.id.btnAC).setOnClickListener(v -> onClearClicked());
        findViewById(R.id.btnDEL).setOnClickListener(v -> onDeleteClicked());
        findViewById(R.id.btnToggleSign).setOnClickListener(v -> onToggleSignClicked());
        findViewById(R.id.btnEquals).setOnClickListener(v -> onEqualsClicked());
    }

    private void appendNumber(String num) {
        clearErrorIfNeeded();
        if (expression.equals("0")) {
            expression = num;
        } else {
            expression += num;
        }
        updateDisplay();
    }

    private void appendOperator(String op) {
        clearErrorIfNeeded();
        if (expression.isEmpty()) {
            if (op.equals("-")) {
                expression = "-";
            }
            updateDisplay();
            return;
        }

        char lastChar = expression.charAt(expression.length() - 1);

        // If the last character is an operator, replace it
        if (isOperator(lastChar)) {
            expression = expression.substring(0, expression.length() - 1) + op;
        } else if (expression.endsWith("(-")) {
            // If it ends with "(-" and user clicks an operator, replace the entire negative indicator
            expression = expression.substring(0, expression.length() - 2) + op;
        } else {
            expression += op;
        }
        updateDisplay();
    }

    private void appendDot() {
        clearErrorIfNeeded();
        if (expression.isEmpty()) {
            expression = "0.";
            updateDisplay();
            return;
        }

        // Find start of last number to check if it already has a decimal point
        int i = expression.length() - 1;
        boolean hasDot = false;
        while (i >= 0) {
            char c = expression.charAt(i);
            if (Character.isDigit(c)) {
                i--;
            } else if (c == '.') {
                hasDot = true;
                break;
            } else {
                break;
            }
        }

        if (!hasDot) {
            char lastChar = expression.charAt(expression.length() - 1);
            if (isOperator(lastChar) || lastChar == '(' || lastChar == ')') {
                expression += "0.";
            } else {
                expression += ".";
            }
            updateDisplay();
        }
    }

    private void onToggleSignClicked() {
        clearErrorIfNeeded();
        expression = toggleLastNumberSign(expression);
        updateDisplay();
    }

    private void onClearClicked() {
        expression = "";
        isErrorState = false;
        tvExpression.setText("");
        tvResult.setText("0");
    }

    private void onDeleteClicked() {
        clearErrorIfNeeded();
        if (!expression.isEmpty()) {
            expression = expression.substring(0, expression.length() - 1);
            updateDisplay();
        }
    }

    private void onEqualsClicked() {
        if (expression.isEmpty()) return;

        try {
            String prepared = prepareForEvaluation(expression);
            if (prepared.isEmpty()) {
                tvResult.setText("0");
                expression = "";
                return;
            }
            double res = eval(prepared);
            String finalResult = formatResult(res);

            tvExpression.setText(expression);
            tvResult.setText(finalResult);
            expression = finalResult;
        } catch (Exception e) {
            tvResult.setText("Error");
            expression = "";
            isErrorState = true;
        }
    }

    private void updateDisplay() {
        tvExpression.setText(expression);

        if (expression.isEmpty()) {
            tvResult.setText("0");
            return;
        }

        try {
            String prepared = prepareForEvaluation(expression);
            if (prepared.isEmpty()) {
                tvResult.setText("");
                return;
            }
            double res = eval(prepared);
            tvResult.setText(formatResult(res));
        } catch (Exception e) {
            // For real-time updates, suppress error messages
        }
    }

    private void clearErrorIfNeeded() {
        if (isErrorState) {
            expression = "";
            isErrorState = false;
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '%';
    }

    private String toggleLastNumberSign(String expr) {
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

    private String prepareForEvaluation(String expr) {
        if (expr.isEmpty()) return "";

        String balanced = balanceParentheses(expr);

        // Trim trailing operators, open parens, or dots
        while (balanced.length() > 0) {
            char last = balanced.charAt(balanced.length() - 1);
            if (isOperator(last) || last == '.' || last == '(') {
                balanced = balanced.substring(0, balanced.length() - 1);
            } else {
                break;
            }
        }

        return balanceParentheses(balanced);
    }

    private String balanceParentheses(String expr) {
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

    private String formatResult(double value) {
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

    // Custom expression evaluator (recursive-descent parser)
    private double eval(final String str) {
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
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected character: " + (char) ch);
                }

                if (eat('%')) {
                    x = x / 100.0;
                }

                return x;
            }
        }.parse();
    }
}
