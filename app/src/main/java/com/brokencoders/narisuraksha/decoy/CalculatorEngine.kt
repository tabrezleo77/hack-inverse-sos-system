package com.brokencoders.narisuraksha.decoy

import java.text.DecimalFormat

data class CalculatorState(
    val displayText: String = "0",
    val expression: String = "",
    val isResultShown: Boolean = false
)

class CalculatorEngine {

    private var currentInput = StringBuilder("0")
    private var operand1: Double? = null
    private var pendingOperation: String? = null
    private var isNewEntry = true

    private val numberFormat = DecimalFormat("#.########")

    companion object {
        private const val MAX_INPUT_LENGTH = 15
    }

    fun onDigit(digit: String): CalculatorState {
        // Validate single-digit input; ignore automation/multi-char bypasses.
        if (digit.length != 1 || !digit[0].isDigit()) return getState()
        // Bound input length to prevent OOM/ANR from unbounded hold-to-type.
        if (!isNewEntry && currentInput.length >= MAX_INPUT_LENGTH) return getState()
        if (isNewEntry || currentInput.toString() == "0") {
            currentInput.clear()
            currentInput.append(digit)
            isNewEntry = false
        } else {
            currentInput.append(digit)
        }
        return getState()
    }

    fun onDecimal(): CalculatorState {
        if (isNewEntry) {
            currentInput.clear()
            currentInput.append("0.")
            isNewEntry = false
        } else if (!currentInput.contains(".")) {
            currentInput.append(".")
        }
        return getState()
    }

    fun onOperation(op: String): CalculatorState {
        val currentVal = currentInput.toString().toDoubleOrNull() ?: 0.0

        if (operand1 != null && pendingOperation != null && !isNewEntry) {
            val result = calculate(operand1!!, currentVal, pendingOperation!!)
            operand1 = result
            currentInput.clear()
            currentInput.append(formatNumber(result))
        } else {
            operand1 = currentVal
        }

        pendingOperation = op
        isNewEntry = true
        return CalculatorState(
            displayText = currentInput.toString(),
            expression = "${formatNumber(operand1 ?: 0.0)} $op"
        )
    }

    fun onEquals(): CalculatorState {
        val currentVal = currentInput.toString().toDoubleOrNull() ?: 0.0

        if (operand1 != null && pendingOperation != null) {
            val result = calculate(operand1!!, currentVal, pendingOperation!!)
            val expr = "${formatNumber(operand1!!)} $pendingOperation ${formatNumber(currentVal)} ="
            operand1 = null
            pendingOperation = null
            currentInput.clear()
            currentInput.append(formatNumber(result))
            isNewEntry = true
            return CalculatorState(
                displayText = currentInput.toString(),
                expression = expr,
                isResultShown = true
            )
        }
        return getState()
    }

    fun onClear(): CalculatorState {
        currentInput.clear()
        currentInput.append("0")
        operand1 = null
        pendingOperation = null
        isNewEntry = true
        return CalculatorState("0", "")
    }

    fun onBackspace(): CalculatorState {
        if (!isNewEntry && currentInput.isNotEmpty()) {
            currentInput.deleteCharAt(currentInput.length - 1)
            if (currentInput.isEmpty()) {
                currentInput.append("0")
                isNewEntry = true
            }
        }
        return getState()
    }

    fun getCurrentInputString(): String = currentInput.toString()

    private fun calculate(op1: Double, op2: Double, operation: String): Double {
        return when (operation) {
            "+" -> op1 + op2
            "-" -> op1 - op2
            "×", "*" -> op1 * op2
            "÷", "/" -> if (op2 != 0.0) op1 / op2 else 0.0
            "%" -> op1 * (op2 / 100.0)
            else -> op2
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            numberFormat.format(value)
        }
    }

    private fun getState(): CalculatorState {
        val expr = if (operand1 != null && pendingOperation != null) {
            "${formatNumber(operand1!!)} $pendingOperation"
        } else {
            ""
        }
        return CalculatorState(
            displayText = currentInput.toString(),
            expression = expr
        )
    }
}
