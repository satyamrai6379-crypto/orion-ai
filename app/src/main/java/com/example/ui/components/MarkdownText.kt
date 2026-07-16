package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val lines = text.split("\n")
    
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()
        var codeBlockLanguage = ""

        for (line in lines) {
            val trimmedLine = line.trim()
            
            if (trimmedLine.startsWith("```")) {
                if (inCodeBlock) {
                    // End of code block
                    CodeBlock(
                        code = codeBlockLines.joinToString("\n"),
                        language = codeBlockLanguage,
                        onCopy = { copyToClipboard(context, codeBlockLines.joinToString("\n")) }
                    )
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    // Start of code block
                    inCodeBlock = true
                    codeBlockLanguage = trimmedLine.removePrefix("```").trim()
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(line)
                continue
            }

            // Parse blocks outside code block
            when {
                trimmedLine.startsWith("# ") -> {
                    Text(
                        text = parseInlineStyles(trimmedLine.substring(2)),
                        color = OrionSecondary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                trimmedLine.startsWith("## ") -> {
                    Text(
                        text = parseInlineStyles(trimmedLine.substring(3)),
                        color = OrionPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                trimmedLine.startsWith("### ") -> {
                    Text(
                        text = parseInlineStyles(trimmedLine.substring(4)),
                        color = OrionTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            color = OrionPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = parseInlineStyles(trimmedLine.substring(2)),
                            color = textColor,
                            fontSize = 15.sp
                        )
                    }
                }
                trimmedLine.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(OrionSurface.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .border(width = 1.dp, color = OrionSecondary.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(IntrinsicSize.Max)
                                .background(OrionSecondary)
                                .padding(end = 8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineStyles(trimmedLine.substring(2)),
                            color = OrionTextSecondary,
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                    }
                }
                trimmedLine.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseInlineStyles(line),
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Handle case where code block doesn't have closing backticks
        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            CodeBlock(
                code = codeBlockLines.joinToString("\n"),
                language = codeBlockLanguage,
                onCopy = { copyToClipboard(context, codeBlockLines.joinToString("\n")) }
            )
        }
    }
}

@Composable
fun CodeBlock(code: String, language: String, onCopy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (language.isNotEmpty()) language.uppercase() else "CODE",
                color = OrionTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onCopy() }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = OrionPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Copy",
                    color = OrionPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // Code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = highlightCode(code, language),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Custom syntax highlighter for code blocks in dark mode.
 */
fun highlightCode(code: String, language: String): AnnotatedString {
    val keywordColor = Color(0xFFF43F5E) // soft red
    val stringColor = Color(0xFF10B981) // emerald green
    val commentColor = Color(0xFF64748B) // slate grey
    val numberColor = Color(0xFF3B82F6) // blue
    val functionColor = Color(0xFFF59E0B) // amber
    val typeColor = Color(0xFF8B5CF6) // purple
    val defaultColor = Color(0xFFE2E8F0) // off-white
    
    val keywords = setOf(
        "val", "var", "fun", "class", "interface", "object", "import", "package", "return", "if", "else", "when", "for", "while", "do", "break", "continue", "throw", "try", "catch", "finally", "null", "true", "false", "this", "super", "private", "protected", "public", "internal", "abstract", "open", "override", "final", "enum", "sealed", "const", "lateinit", "init", "by", "in", "is", "as",
        "def", "elif", "print", "from", "with", "lambda", "yield", "pass", "assert", "raise", "except", "global", "nonlocal", "and", "or", "not", "import", "as",
        "function", "let", "typeof", "instanceof", "new", "delete", "export", "default", "await", "async", "switch", "case"
    )
    
    val types = setOf(
        "String", "Int", "Double", "Float", "Boolean", "Long", "Short", "Byte", "Char", "Any", "Unit", "Nothing", "List", "Map", "Set", "Array", "Activity", "Context", "Composable", "Modifier"
    )

    return buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            when {
                // Comments: // ...
                code.startsWith("//", i) -> {
                    val endOfLine = code.indexOf('\n', i)
                    val commentEnd = if (endOfLine == -1) code.length else endOfLine
                    pushStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic))
                    append(code.substring(i, commentEnd))
                    pop()
                    i = commentEnd
                }
                // Multi-line Comments: /* ... */
                code.startsWith("/*", i) -> {
                    val endOfComment = code.indexOf("*/", i + 2)
                    val commentEnd = if (endOfComment == -1) code.length else endOfComment + 2
                    pushStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic))
                    append(code.substring(i, commentEnd))
                    pop()
                    i = commentEnd
                }
                // Strings: "..." or '...'
                code[i] == '"' || code[i] == '\'' -> {
                    val quoteChar = code[i]
                    var endOfStr = i + 1
                    var escaped = false
                    while (endOfStr < code.length) {
                        if (code[endOfStr] == '\\') {
                            escaped = !escaped
                        } else if (code[endOfStr] == quoteChar && !escaped) {
                            break
                        } else {
                            escaped = false
                        }
                        endOfStr++
                    }
                    val stringEnd = if (endOfStr < code.length) endOfStr + 1 else code.length
                    pushStyle(SpanStyle(color = stringColor))
                    append(code.substring(i, stringEnd))
                    pop()
                    i = stringEnd
                }
                // Words/Identifiers (Keywords, Types, Functions)
                code[i].isLetter() || code[i] == '_' -> {
                    val start = i
                    var wordEnd = i
                    while (wordEnd < code.length && (code[wordEnd].isLetterOrDigit() || code[wordEnd] == '_')) {
                        wordEnd++
                    }
                    val word = code.substring(start, wordEnd)
                    when {
                        keywords.contains(word) -> {
                            pushStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold))
                            append(word)
                            pop()
                        }
                        types.contains(word) -> {
                            pushStyle(SpanStyle(color = typeColor))
                            append(word)
                            pop()
                        }
                        wordEnd < code.length && code[wordEnd] == '(' -> {
                            pushStyle(SpanStyle(color = functionColor))
                            append(word)
                            pop()
                        }
                        else -> {
                            pushStyle(SpanStyle(color = defaultColor))
                            append(word)
                            pop()
                        }
                    }
                    i = wordEnd
                }
                // Numbers
                code[i].isDigit() -> {
                    val start = i
                    var numEnd = i
                    while (numEnd < code.length && (code[numEnd].isDigit() || code[numEnd] == '.' || code[numEnd] == 'f' || code[numEnd] == 'L')) {
                        numEnd++
                    }
                    pushStyle(SpanStyle(color = numberColor))
                    append(code.substring(start, numEnd))
                    pop()
                    i = numEnd
                }
                else -> {
                    pushStyle(SpanStyle(color = defaultColor))
                    append(code[i].toString())
                    pop()
                    i++
                }
            }
        }
    }
}

/**
 * Parsers simple inline markdown tags:
 * **bold**
 * *italic* or _italic_
 * `inline code`
 */
fun parseInlineStyles(input: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < input.length) {
            when {
                // Bold: **text**
                input.startsWith("**", i) && input.indexOf("**", i + 2) != -1 -> {
                    val end = input.indexOf("**", i + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(input.substring(i + 2, end))
                    pop()
                    i = end + 2
                }
                // Italic: *text*
                input.startsWith("*", i) && !input.startsWith("**", i) && input.indexOf("*", i + 1) != -1 -> {
                    val end = input.indexOf("*", i + 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(input.substring(i + 1, end))
                    pop()
                    i = end + 1
                }
                // Inline Code: `code`
                input.startsWith("`", i) && input.indexOf("`", i + 1) != -1 -> {
                    val end = input.indexOf("`", i + 1)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFF1E293B),
                            color = OrionPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(" ${input.substring(i + 1, end)} ")
                    pop()
                    i = end + 1
                }
                else -> {
                    append(input[i].toString())
                    i++
                }
            }
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Orion AI Code", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}
