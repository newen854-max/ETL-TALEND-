package com.example.mobileetl

import java.util.Locale

class MobileTalendEngine {

    fun executeJob(
        inputData: List<Map<String, String>>, 
        jobSteps: List<EtlStep>
    ): List<Map<String, Any>> {
        var activeDataset: List<Map<String, Any>> = inputData.map { it.toMap() }

        for (step in jobSteps) {
            activeDataset = when (step.type) {
                "tFilterRow" -> applyFilter(activeDataset, step.rules)
                "tMap" -> applyMap(activeDataset, step.mappings)
                "tAggregateRow" -> applyAggregate(activeDataset, step.groupBy, step.operations)
                "tSortRow" -> applySort(activeDataset, step.sortBy, step.direction)
                else -> activeDataset
            }
        }
        return activeDataset
    }

    private fun applyFilter(data: List<Map<String, Any>>, rules: List<FilterRule>?): List<Map<String, Any>> {
        if (rules == null) return data
        return data.filter { row ->
            rules.all { rule ->
                val rawVal = row[rule.field] ?: return@all false
                val valStr = rawVal.toString()
                when (rule.operator) {
                    "==" -> valStr.equals(rule.value.toString(), ignoreCase = true)
                    "!=" -> !valStr.equals(rule.value.toString(), ignoreCase = true)
                    ">" -> (valStr.toDoubleOrNull() ?: 0.0) > (rule.value.toString().toDoubleOrNull() ?: 0.0)
                    "<" -> (valStr.toDoubleOrNull() ?: 0.0) < (rule.value.toString().toDoubleOrNull() ?: 0.0)
                    "contains" -> valStr.lowercase(Locale.ROOT).contains(rule.value.toString().lowercase(Locale.ROOT))
                    else -> false
                }
            }
        }
    }

    private fun applyMap(data: List<Map<String, Any>>, mappings: Map<String, MappingRule>?): List<Map<String, Any>> {
        if (mappings == null) return data
        return data.map { row ->
            val transformedRow = row.toMutableMap()
            for ((sourceField, rule) in mappings) {
                val value = row[sourceField] ?: continue
                val targetField = rule.target ?: sourceField
                
                transformedRow[targetField] = when (rule.action) {
                    "UPPERCASE" -> value.toString().uppercase(Locale.ROOT)
                    "LOWERCASE" -> value.toString().lowercase(Locale.ROOT)
                    "TRIM" -> value.toString().trim()
                    "MULTIPLY" -> {
                        val num = value.toString().toDoubleOrNull() ?: 0.0
                        num * (rule.factor ?: 1.0)
                    }
                    else -> value
                }
            }
            transformedRow
        }
    }

    private fun applyAggregate(
        data: List<Map<String, Any>>, 
        groupBy: List<String>?, 
        operations: List<AggregateOperation>?
    ): List<Map<String, Any>> {
        if (groupBy == null || operations == null) return data
        val grouped = data.groupBy { row -> groupBy.map { key -> row[key] ?: "" } }
        val result = mutableListOf<Map<String, Any>>()

        for ((groupKeys, rows) in grouped) {
            val aggregatedRow = mutableMapOf<String, Any>()
            groupBy.forEachIndexed { index, key -> aggregatedRow[key] = groupKeys[index] }

            for (op in operations) {
                val fieldName = op.field ?: ""
                val values = rows.mapNotNull { it[fieldName]?.toString()?.toDoubleOrNull() }
                aggregatedRow[op.targetField] = when (op.operation) {
                    "COUNT" -> rows.size
                    "SUM" -> values.sum()
                    "AVG" -> if (values.isNotEmpty()) values.average() else 0.0
                    "MAX" -> values.maxOrNull() ?: 0.0
                    "MIN" -> values.minOrNull() ?: 0.0
                    else -> ""
                }
            }
            result.add(aggregatedRow)
        }
        return result
    }

    private fun applySort(data: List<Map<String, Any>>, sortBy: String?, direction: String?): List<Map<String, Any>> {
        if (sortBy == null) return data
        val isAscending = direction?.uppercase(Locale.ROOT) != "DESC"
        return if (isAscending) {
            data.sortedWith(compareBy { (it[sortBy]?.toString()?.toDoubleOrNull() ?: it[sortBy]?.toString() ?: "") as Comparable<Any> })
        } else {
            data.sortedWith(compareByDescending { (it[sortBy]?.toString()?.toDoubleOrNull() ?: it[sortBy]?.toString() ?: "") as Comparable<Any> })
        }
    }
}

// Data models representing rules
data class EtlStep(val type: String, val rules: List<FilterRule>? = null, val mappings: Map<String, MappingRule>? = null, val groupBy: List<String>? = null, val operations: List<AggregateOperation>? = null, val sortBy: String? = null, val direction: String? = null)
data class FilterRule(val field: String, val operator: String, val value: Any)
data class MappingRule(val action: String, val target: String?, val factor: Double? = null)
data class AggregateOperation(val targetField: String, val field: String?, val operation: String)
