package com.mcis.memoir.ui.artifact

data class QuestionHighlight(
    val prefix: String,
    val label: String,
    val suffix: String
)

fun computeHighlight(question: String, label: String): QuestionHighlight {
    if (label.isEmpty()) return QuestionHighlight(prefix = question, label = "", suffix = "")
    val idx = question.indexOf(label)
    if (idx < 0) return QuestionHighlight(prefix = question, label = "", suffix = "")
    return QuestionHighlight(
        prefix = question.substring(0, idx),
        label = label,
        suffix = question.substring(idx + label.length)
    )
}
