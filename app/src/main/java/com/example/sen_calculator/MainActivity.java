package com.example.sen_calculator;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private TextView tvExpression;
    private TextView tvResult;
    private String expression = "";
    private boolean isErrorState = false;
    private boolean hasFinishedEvaluation = false;

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

        // Bind advanced scientific buttons
        findViewById(R.id.btnSin).setOnClickListener(v -> appendFunction("sin("));
        findViewById(R.id.btnCos).setOnClickListener(v -> appendFunction("cos("));
        findViewById(R.id.btnTan).setOnClickListener(v -> appendFunction("tan("));
        findViewById(R.id.btnAtan).setOnClickListener(v -> appendFunction("arctan("));
        findViewById(R.id.btnAcos).setOnClickListener(v -> appendFunction("arccos("));
        findViewById(R.id.btnAsin).setOnClickListener(v -> appendFunction("arcsin("));
        findViewById(R.id.btnFactorial).setOnClickListener(v -> appendOperator("!"));

        // Bind utility buttons
        findViewById(R.id.btnDot).setOnClickListener(v -> appendDot());
        findViewById(R.id.btnAC).setOnClickListener(v -> onClearClicked());
        findViewById(R.id.btnDEL).setOnClickListener(v -> onDeleteClicked());
        findViewById(R.id.btnToggleSign).setOnClickListener(v -> onToggleSignClicked());
        findViewById(R.id.btnEquals).setOnClickListener(v -> onEqualsClicked());
    }

    private void appendNumber(String num) {
        clearErrorIfNeeded();
        if (hasFinishedEvaluation) {
            expression = num;
            hasFinishedEvaluation = false;
        } else if (expression.equals("0")) {
            expression = num;
        } else {
            expression += num;
        }
        updateDisplay();
    }

    private void appendOperator(String op) {
        clearErrorIfNeeded();
        if (hasFinishedEvaluation) {
            hasFinishedEvaluation = false;
            expression = expression + op;
            updateDisplay();
            return;
        }
        if (expression.isEmpty()) {
            if (op.equals("-")) {
                expression = "-";
            }
            updateDisplay();
            return;
        }

        char lastChar = expression.charAt(expression.length() - 1);

        // Replace operator if last was operator, unless this is the factorial postfix operator
        if (ExpressionEvaluator.isOperator(lastChar) && !op.equals("!")) {
            expression = expression.substring(0, expression.length() - 1) + op;
        } else if (expression.endsWith("(-") && !op.equals("!")) {
            expression = expression.substring(0, expression.length() - 2) + op;
        } else {
            expression += op;
        }
        updateDisplay();
    }

    private void appendFunction(String funcName) {
        clearErrorIfNeeded();
        if (hasFinishedEvaluation) {
            expression = funcName;
            hasFinishedEvaluation = false;
        } else if (expression.equals("0")) {
            expression = funcName;
        } else {
            expression += funcName;
        }
        updateDisplay();
    }

    private void appendDot() {
        clearErrorIfNeeded();
        if (hasFinishedEvaluation) {
            expression = "0.";
            hasFinishedEvaluation = false;
            updateDisplay();
            return;
        }
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
            if (ExpressionEvaluator.isOperator(lastChar) || lastChar == '(' || lastChar == ')') {
                expression += "0.";
            } else {
                expression += ".";
            }
            updateDisplay();
        }
    }

    private void onToggleSignClicked() {
        clearErrorIfNeeded();
        if (hasFinishedEvaluation) {
            hasFinishedEvaluation = false;
        }
        expression = ExpressionEvaluator.toggleLastNumberSign(expression);
        updateDisplay();
    }

    private void onClearClicked() {
        expression = "";
        isErrorState = false;
        hasFinishedEvaluation = false;
        tvExpression.setText("");
        tvResult.setText("0");
    }

    private void onDeleteClicked() {
        clearErrorIfNeeded();
        if (hasFinishedEvaluation) {
            hasFinishedEvaluation = false;
        }
        if (expression.isEmpty()) return;

        // Smart delete for full function strings
        if (expression.endsWith("sin(") || expression.endsWith("cos(") || expression.endsWith("tan(")) {
            expression = expression.substring(0, expression.length() - 4);
        } else if (expression.endsWith("arctan(") || expression.endsWith("arcsin(") || expression.endsWith("arccos(")) {
            expression = expression.substring(0, expression.length() - 7);
        } else {
            expression = expression.substring(0, expression.length() - 1);
        }
        updateDisplay();
    }

    private void onEqualsClicked() {
        if (expression.isEmpty()) return;

        try {
            String prepared = ExpressionEvaluator.prepareForEvaluation(expression);
            if (prepared.isEmpty()) {
                tvResult.setText("0");
                expression = "";
                hasFinishedEvaluation = false;
                return;
            }
            double res = ExpressionEvaluator.eval(prepared);
            String finalResult = ExpressionEvaluator.formatResult(res);

            tvExpression.setText(prepared + " =");
            tvResult.setText(finalResult);
            expression = finalResult;
            hasFinishedEvaluation = true;
        } catch (Exception e) {
            tvResult.setText(R.string.error);
            expression = "";
            isErrorState = true;
            hasFinishedEvaluation = false;
        }
    }

    private void updateDisplay() {
        tvExpression.setText(expression);

        if (expression.isEmpty()) {
            tvResult.setText("0");
            return;
        }

        try {
            String prepared = ExpressionEvaluator.prepareForEvaluation(expression);
            if (prepared.isEmpty()) {
                tvResult.setText("");
                return;
            }
            double res = ExpressionEvaluator.eval(prepared);
            tvResult.setText(ExpressionEvaluator.formatResult(res));
        } catch (Exception e) {
            // Suppress error logs during real-time typing preview
        }
    }

    private void clearErrorIfNeeded() {
        if (isErrorState) {
            expression = "";
            isErrorState = false;
        }
    }
}
