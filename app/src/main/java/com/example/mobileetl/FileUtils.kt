package com.example.mobileetl

import java.io.File

object FileUtils {

    // Equivalent to tFileInputDelimited
    fun readCsvText(csvText: String): List<Map<String, String>> {
        val lines = csvText.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val headers = lines[0].split(",").map { it.trim() }
        val dataset = mutableListOf<Map<String, String>>()

        for (i in 1 until lines.size) {
            val values = lines[i].split(",").map { it.trim() }
            if (values.size == headers.size) {
                val row = headers.zip(values).toMap()
                dataset.add(row)
            }
        }
        return dataset
    }

    // Equivalent to tFileOutputDelimited
    fun convertToCsvText(data: List<Map<String, Any>>): String {
        if (data.isEmpty()) return ""
        val headers = data[0].keys.joinToString(",")
        val rows = data.joinToString("\n") { row ->
            row.values.joinToString(",") { it.toString() }
        }
        return "$headers\n$rows"
    }
}
