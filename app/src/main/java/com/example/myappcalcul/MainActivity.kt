package com.example.myappcalcul

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvOperation: TextView
    private lateinit var tvResult: TextView
    private var currentExpression = ""
    private var isResultShown = false
    private var memory = 0.0
    private var lastAnswer = 0.0
    private val historyList = mutableListOf<String>()

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "CalculatorPrefs"
        private const val KEY_EXPRESSION = "currentExpression"
        private const val KEY_MEMORY = "memory"
        private const val KEY_LAST_ANSWER = "lastAnswer"
        private const val KEY_HISTORY = "history"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        initializeViews()
        restoreState(savedInstanceState)
        setupNumberButtons()
        setupOperationButtons()
        setupScientificButtons()
        setupMemoryButtons()
        setupClearButtons()
        setupSpecialButtons()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_EXPRESSION, currentExpression)
        outState.putDouble(KEY_MEMORY, memory)
        outState.putDouble(KEY_LAST_ANSWER, lastAnswer)
        outState.putStringArrayList(KEY_HISTORY, ArrayList(historyList))
    }

    override fun onPause() {
        super.onPause()
        saveToPreferences()
    }

    private fun saveToPreferences() {
        sharedPreferences.edit().apply {
            putString(KEY_EXPRESSION, currentExpression)
            putFloat(KEY_MEMORY, memory.toFloat())
            putFloat(KEY_LAST_ANSWER, lastAnswer.toFloat())
            putString(KEY_HISTORY, gson.toJson(historyList))
            apply()
        }
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            currentExpression = savedInstanceState.getString(KEY_EXPRESSION, "") ?: ""
            memory = savedInstanceState.getDouble(KEY_MEMORY, 0.0)
            lastAnswer = savedInstanceState.getDouble(KEY_LAST_ANSWER, 0.0)
            historyList.clear()
            historyList.addAll(savedInstanceState.getStringArrayList(KEY_HISTORY) ?: emptyList())
        } else {
            restoreFromPreferences()
        }
        updateDisplay()
    }

    private fun restoreFromPreferences() {
        currentExpression = sharedPreferences.getString(KEY_EXPRESSION, "") ?: ""
        memory = sharedPreferences.getFloat(KEY_MEMORY, 0f).toDouble()
        lastAnswer = sharedPreferences.getFloat(KEY_LAST_ANSWER, 0f).toDouble()

        val historyJson = sharedPreferences.getString(KEY_HISTORY, "[]")
        val type = object : TypeToken<MutableList<String>>() {}.type
        val savedHistory: MutableList<String> = gson.fromJson(historyJson, type) ?: mutableListOf()
        historyList.clear()
        historyList.addAll(savedHistory)
    }

    private fun initializeViews() {
        tvOperation = findViewById(R.id.tvOperation)
        tvResult = findViewById(R.id.tvResult)
    }

    private fun setupNumberButtons() {
        val numberButtonIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        numberButtonIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener { v ->
                val number = (v as Button).text.toString()
                if (isResultShown) {
                    currentExpression = number
                    isResultShown = false
                } else {
                    currentExpression += number
                }
                updateDisplay()
            }
        }

        findViewById<Button>(R.id.btnDot).setOnClickListener {
            if (isResultShown) {
                currentExpression = "0."
                isResultShown = false
            } else {
                val lastPart = currentExpression.split(Regex("[+\\-×÷*^()]")).lastOrNull() ?: ""
                if (!lastPart.contains(".") && lastPart.isNotEmpty()) {
                    currentExpression += "."
                } else if (lastPart.isEmpty() || currentExpression.isEmpty() ||
                    (currentExpression.isNotEmpty() && currentExpression.last().toString().matches(Regex("[+\\-×÷*^(]")))) {
                    currentExpression += "0."
                }
            }
            updateDisplay()
        }
    }

    private fun setupOperationButtons() {
        mapOf(
            R.id.btnPlus to "+",
            R.id.btnMinus to "-",
            R.id.btnMultiply to "*",
            R.id.btnDivide to "/"
        ).forEach { (id, op) ->
            findViewById<Button>(id).setOnClickListener {
                if (isResultShown) {
                    currentExpression = formatNumber(lastAnswer) + op
                    isResultShown = false
                } else {
                    if (currentExpression.isNotEmpty() &&
                        currentExpression.last().toString().matches(Regex("[+\\-×÷*/^]"))) {
                        currentExpression = currentExpression.dropLast(1) + op
                    } else {
                        currentExpression += op
                    }
                }
                updateDisplay()
            }
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            evaluateExpression()
        }

        findViewById<Button>(R.id.btnPercent).setOnClickListener {
            if (currentExpression.isNotEmpty()) {
                currentExpression += "/100"
                evaluateExpression()
            }
        }
    }

    private fun setupScientificButtons() {
        findViewById<Button>(R.id.btnSqrt).setOnClickListener {
            addFunction("sqrt(")
        }
        findViewById<Button>(R.id.btnX2).setOnClickListener {
            if (currentExpression.isNotEmpty()) {
                currentExpression += "^2"
                updateDisplay()
            }
        }
        findViewById<Button>(R.id.btnPi).setOnClickListener {
            if (isResultShown) {
                currentExpression = "π"
                isResultShown = false
            } else {
                currentExpression += "π"
            }
            updateDisplay()
        }
        findViewById<Button>(R.id.btnE).setOnClickListener {
            if (isResultShown) {
                currentExpression = "e"
                isResultShown = false
            } else {
                currentExpression += "e"
            }
            updateDisplay()
        }

        findViewById<Button>(R.id.btnSin).setOnClickListener { addFunction("sin(") }
        findViewById<Button>(R.id.btnCos).setOnClickListener { addFunction("cos(") }
        findViewById<Button>(R.id.btnTan).setOnClickListener { addFunction("tan(") }
        findViewById<Button>(R.id.btnSinInv).setOnClickListener { addFunction("asin(") }
        findViewById<Button>(R.id.btnCosInv).setOnClickListener { addFunction("acos(") }
        findViewById<Button>(R.id.btnTanInv).setOnClickListener { addFunction("atan(") }
        findViewById<Button>(R.id.btnLog).setOnClickListener { addFunction("log(") }
        findViewById<Button>(R.id.btnLn).setOnClickListener { addFunction("ln(") }
        findViewById<Button>(R.id.btn10x).setOnClickListener { addFunction("10^(") }
        findViewById<Button>(R.id.btnEx).setOnClickListener { addFunction("exp(") }
        findViewById<Button>(R.id.btnPow).setOnClickListener {
            if (currentExpression.isNotEmpty()) {
                currentExpression += "^"
                updateDisplay()
            }
        }
        findViewById<Button>(R.id.btnFact).setOnClickListener {
            if (currentExpression.isNotEmpty()) {
                currentExpression += "!"
                updateDisplay()
            }
        }
        findViewById<Button>(R.id.btnAbs).setOnClickListener { addFunction("abs(") }
        findViewById<Button>(R.id.btnOpen).setOnClickListener {
            if (isResultShown) {
                currentExpression = "("
                isResultShown = false
            } else {
                currentExpression += "("
            }
            updateDisplay()
        }
        findViewById<Button>(R.id.btnClose).setOnClickListener {
            currentExpression += ")"
            updateDisplay()
        }
    }

    private fun addFunction(functionName: String) {
        if (isResultShown) {
            currentExpression = functionName
            isResultShown = false
        } else {
            currentExpression += functionName
        }
        updateDisplay()
    }

    private fun setupMemoryButtons() {
        findViewById<Button>(R.id.btnSto).setOnClickListener {
            if (isResultShown && lastAnswer != 0.0) {
                memory = lastAnswer
                showToast("Saved: ${formatNumber(memory)}")
                tvOperation.text = "M = ${formatNumber(memory)}"
            } else if (currentExpression.isNotEmpty()) {
                try {
                    val value = evaluateExpressionValue(currentExpression)
                    memory = value
                    showToast("Saved: ${formatNumber(memory)}")

                } catch (e: Exception) {
                    showToast("Error evaluating expression")
                }
            } else {
                showToast("Enter a number first")
            }
        }

        findViewById<Button>(R.id.btnRcl).setOnClickListener {
            if (isResultShown) {
                currentExpression = formatNumber(memory)
                isResultShown = false
            } else {
                currentExpression += formatNumber(memory)
            }
            updateDisplay()
        }

        findViewById<Button>(R.id.btnMPlus).setOnClickListener {
            if (isResultShown && lastAnswer != 0.0) {
                memory += lastAnswer
                showToast("M+ = ${formatNumber(memory)}")
                tvOperation.text = "M = ${formatNumber(memory)}"
            } else if (currentExpression.isNotEmpty()) {
                try {
                    val value = evaluateExpressionValue(currentExpression)
                    memory += value
                    showToast("M+ = ${formatNumber(memory)}")

                } catch (e: Exception) {
                    showToast("Error evaluating expression")
                }
            } else {
                showToast("Enter a number first")
            }
        }

        findViewById<Button>(R.id.btnMMinus).setOnClickListener {
            if (isResultShown && lastAnswer != 0.0) {
                memory -= lastAnswer
                showToast("M- = ${formatNumber(memory)}")
                tvOperation.text = "M = ${formatNumber(memory)}"
            } else if (currentExpression.isNotEmpty()) {
                try {
                    val value = evaluateExpressionValue(currentExpression)
                    memory -= value
                    showToast("M- = ${formatNumber(memory)}")
                } catch (e: Exception) {
                    showToast("Error evaluating expression")
                }
            } else {
                showToast("Enter a number first")
            }
        }
    }

    private fun setupClearButtons() {
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            clearAll()
            showToast("Cleared")
        }
        findViewById<Button>(R.id.btndel).setOnClickListener {
            if (currentExpression.isNotEmpty()) {
                currentExpression = currentExpression.dropLast(1)
                updateDisplay()
            }
        }
    }

    private fun setupSpecialButtons() {
        findViewById<Button>(R.id.btnAns).setOnClickListener {
            if (lastAnswer != 0.0) {
                if (isResultShown) {
                    currentExpression = formatNumber(lastAnswer)
                    isResultShown = false
                } else {
                    currentExpression += formatNumber(lastAnswer)
                }
                updateDisplay()
            } else {
                showToast("No previous answer")
            }
        }

        findViewById<Button>(R.id.btnExp).setOnClickListener {
            if (currentExpression.isNotEmpty()) {
                currentExpression += "E"
                updateDisplay()
            }
        }

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            showHistory()
        }
    }

    private fun clearAll() {
        currentExpression = ""
        isResultShown = false
        updateDisplay()
    }

    private fun updateDisplay() {
        tvOperation.text = currentExpression.ifEmpty { "" }
        tvResult.text = if (isResultShown && currentExpression.isNotEmpty()) currentExpression else "0"
    }

    private fun evaluateExpression() {
        if (currentExpression.isEmpty()) return

        try {
            val result = evaluateExpressionValue(currentExpression)

            if (result.isNaN() || result.isInfinite()) {
                showError("Math Error")
                return
            }

            val formattedResult = formatNumber(result)
            val historyEntry = "$currentExpression = $formattedResult"
            historyList.add(0, historyEntry)
            if (historyList.size > 50) historyList.removeAt(historyList.size - 1)

            lastAnswer = result
            currentExpression = formattedResult
            isResultShown = true
            updateDisplay()
        } catch (e: Exception) {
            showError("Error: ${e.message}")
        }
    }

    private fun evaluateExpressionValue(expr: String): Double {
        var expression = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", Math.PI.toString())
            .replace(" ", "")

        expression = replaceEConstant(expression)

        expression = processFunctions(expression)
        expression = processPowers(expression)
        expression = processFactorials(expression)

        return evaluateSimpleExpression(expression)
    }

    private fun replaceEConstant(expr: String): String {
        val result = StringBuilder()
        var i = 0
        while (i < expr.length) {
            if (expr[i] == 'e' && i + 1 < expr.length && expr[i + 1] == 'x' &&
                i + 2 < expr.length && expr[i + 2] == 'p') {
                result.append("exp")
                i += 3
            } else if (expr[i] == 'e') {
                val before = if (i > 0) expr[i - 1] else ' '
                val after = if (i < expr.length - 1) expr[i + 1] else ' '
                if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) {
                    result.append(Math.E.toString())
                    i++
                } else {
                    result.append('e')
                    i++
                }
            } else {
                result.append(expr[i])
                i++
            }
        }
        return result.toString()
    }

    private fun processFunctions(expr: String): String {
        var result = expr
        val functions = mapOf(
            "sin" to { x: Double -> sin(Math.toRadians(x)) },
            "cos" to { x: Double -> cos(Math.toRadians(x)) },
            "tan" to { x: Double -> tan(Math.toRadians(x)) },
            "asin" to { x: Double -> if (x in -1.0..1.0) Math.toDegrees(asin(x)) else Double.NaN },
            "acos" to { x: Double -> if (x in -1.0..1.0) Math.toDegrees(acos(x)) else Double.NaN },
            "atan" to { x: Double -> Math.toDegrees(atan(x)) },
            "log10" to { x: Double -> if (x > 0) log10(x) else Double.NaN },
            "log" to { x: Double -> if (x > 0) ln(x) else Double.NaN },
            "sqrt" to { x: Double -> if (x >= 0) sqrt(x) else Double.NaN },
            "abs" to { x: Double -> abs(x) },
            "exp" to { x: Double -> exp(x) }
        )

        var changed = true
        while (changed) {
            changed = false
            for ((name, func) in functions) {
                val pattern = "$name("
                var index = result.lastIndexOf(pattern)
                if (index >= 0) {
                    val start = index + pattern.length
                    val end = findMatchingParen(result, start - 1)
                    if (end > start) {
                        val arg = result.substring(start, end)
                        val value = evaluateExpressionValue(arg)
                        val funcResult = func(value)
                        if (funcResult.isNaN() || funcResult.isInfinite()) {
                            throw IllegalArgumentException("Math error in $name")
                        }
                        result = result.substring(0, index) + funcResult.toString() + result.substring(end + 1)
                        changed = true
                        break
                    }
                }
            }

            val pow10Index = result.lastIndexOf("10^(")
            if (pow10Index >= 0) {
                val start = pow10Index + 4
                val end = findMatchingParen(result, start - 1)
                if (end > start) {
                    val arg = result.substring(start, end)
                    val value = evaluateExpressionValue(arg)
                    val powResult = 10.0.pow(value)
                    result = result.substring(0, pow10Index) + powResult.toString() + result.substring(end + 1)
                    changed = true
                }
            }
        }

        return result
    }

    private fun processPowers(expr: String): String {
        var result = expr
        var changed = true
        while (changed) {
            changed = false
            val powIndex = result.lastIndexOf("^")
            if (powIndex > 0 && powIndex < result.length - 1) {
                val baseEnd = powIndex
                var baseStart = powIndex - 1
                while (baseStart >= 0 && (result[baseStart].isDigit() || result[baseStart] == '.' ||
                            result[baseStart] == 'E' || result[baseStart] == 'e' ||
                            (result[baseStart] == '-' && (baseStart == 0 || result[baseStart - 1].toString().matches(Regex("[+\\-*/^()]")))))) {
                    baseStart--
                }
                baseStart++

                var expEnd = powIndex + 1
                if (expEnd < result.length && result[expEnd] == '-') expEnd++
                while (expEnd < result.length && (result[expEnd].isDigit() || result[expEnd] == '.' ||
                            result[expEnd] == 'E' || result[expEnd] == 'e')) {
                    expEnd++
                }

                if (baseStart < baseEnd && expEnd > powIndex + 1) {
                    try {
                        val base = result.substring(baseStart, baseEnd).toDouble()
                        val exp = result.substring(powIndex + 1, expEnd).toDouble()
                        val powResult = base.pow(exp)
                        result = result.substring(0, baseStart) + powResult.toString() + result.substring(expEnd)
                        changed = true
                    } catch (e: Exception) {
                        break
                    }
                } else {
                    break
                }
            }
        }
        return result
    }

    private fun processFactorials(expr: String): String {
        var result = expr
        val pattern = Regex("(\\d+(?:\\.\\d+)?)!")
        while (pattern.containsMatchIn(result)) {
            result = pattern.replace(result) { matchResult ->
                val num = matchResult.groupValues[1].toDouble().toInt()
                if (num in 0..20) {
                    factorial(num).toString()
                } else {
                    throw IllegalArgumentException("Factorial too large")
                }
            }
        }
        return result
    }

    private fun findMatchingParen(str: String, start: Int): Int {
        var depth = 1
        for (i in start + 1 until str.length) {
            when (str[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun evaluateSimpleExpression(expr: String): Double {
        if (expr.isEmpty()) return 0.0
        var expression = expr.replace(" ", "")

        while (expression.contains("(")) {
            val start = expression.lastIndexOf("(")
            val end = findMatchingParen(expression, start)
            if (end == -1) break

            val subExpr = expression.substring(start + 1, end)
            val subResult = evaluateSimpleExpression(subExpr)
            expression = expression.substring(0, start) + subResult.toString() + expression.substring(end + 1)
        }

        var result = expression
        var changed = true
        while (changed) {
            changed = false
            val mulIndex = result.indexOf("*")
            val divIndex = result.indexOf("/")
            val opIndex = when {
                mulIndex > 0 && divIndex > 0 -> minOf(mulIndex, divIndex)
                mulIndex > 0 -> mulIndex
                divIndex > 0 -> divIndex
                else -> -1
            }

            if (opIndex > 0 && opIndex < result.length - 1) {
                val left = extractNumber(result, opIndex - 1, -1)
                val right = extractNumber(result, opIndex + 1, 1)
                if (left != null && right != null) {
                    val calcResult = when (result[opIndex]) {
                        '*' -> left.first * right.first
                        '/' -> {
                            if (right.first == 0.0) throw IllegalArgumentException("Division by zero")
                            left.first / right.first
                        }
                        else -> break
                    }
                    result = result.substring(0, left.second) + calcResult.toString() + result.substring(right.third)
                    changed = true
                }
            }
        }


        changed = true
        while (changed) {
            changed = false
            var opIndex = -1
            var op: Char? = null
            for (i in 1 until result.length) {
                if ((result[i] == '+' || result[i] == '-') && result[i - 1].toString().matches(Regex("[0-9.]"))) {
                    opIndex = i
                    op = result[i]
                    break
                }
            }

            if (opIndex > 0 && op != null) {
                val left = extractNumber(result, opIndex - 1, -1)
                val right = extractNumber(result, opIndex + 1, 1)
                if (left != null && right != null) {
                    val calcResult = when (op) {
                        '+' -> left.first + right.first
                        '-' -> left.first - right.first
                        else -> break
                    }
                    result = result.substring(0, left.second) + calcResult.toString() + result.substring(right.third)
                    changed = true
                }
            }
        }

        return result.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid expression")
    }

    private fun extractNumber(str: String, start: Int, direction: Int): Triple<Double, Int, Int>? {
        var i = start
        val builder = StringBuilder()
        var numStart = start

        if (direction < 0) {
            while (i >= 0 && (str[i].isDigit() || str[i] == '.' || str[i] == 'E' || str[i] == 'e' ||
                        (str[i] == '-' && (i == 0 || str[i - 1].toString().matches(Regex("[+\\-*/]")))))) {
                builder.insert(0, str[i])
                numStart = i
                i--
            }
            val numStr = builder.toString()
            return try {
                Triple(numStr.toDouble(), numStart, start + 1)
            } catch (e: Exception) {
                null
            }
        } else {
            numStart = i
            if (i < str.length && str[i] == '-') {
                builder.append(str[i])
                i++
            }
            while (i < str.length && (str[i].isDigit() || str[i] == '.' || str[i] == 'E' || str[i] == 'e')) {
                builder.append(str[i])
                i++
            }
            val numStr = builder.toString()
            return try {
                Triple(numStr.toDouble(), numStart, i)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun factorial(n: Int): Long {
        if (n <= 1) return 1
        var result = 1L
        for (i in 2..n) result *= i
        return result
    }

    private fun formatNumber(number: Double): String {
        return if (number == number.toLong().toDouble()) {
            number.toLong().toString()
        } else {
            val formatted = "%.10f".format(number)
            formatted.trimEnd('0').trimEnd('.')
        }
    }

    private fun showError(message: String) {
        tvResult.text = message
        currentExpression = ""
        isResultShown = false
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showHistory() {
        if (historyList.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("📜 Calculation History")
                .setMessage("No calculations yet.\n\nPerform some operations and they will appear here!")
                .setPositiveButton("ok", null)
                .show()
            return
        }

        val historyText = buildString {
            append("═══ Last ${historyList.size} calculations ═══\n\n")
            historyList.forEachIndexed { index, item ->
                append("${index + 1}. $item\n\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("📜 Calculation History")
            .setMessage(historyText)
            .setPositiveButton("ok", null)
            .setNegativeButton("clear all") { _, _ ->
                historyList.clear()
                showToast("History cleared")
            }
            .show()
    }
}
