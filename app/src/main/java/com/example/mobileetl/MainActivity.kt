package com.example.mobileetl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EtlStudioScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtlStudioScreen() {
    // 1. Textbox inputs
    var inputCsv by remember { mutableStateOf("id,name,score,status\n1,Alpha,45,Active\n2,Beta,90,Active\n3,Gamma,110,Inactive") }
    var jsonConfig by remember { mutableStateOf(
        "[\n  {\n    \"type\": \"tFilterRow\",\n    \"rules\": [\n      {\"field\": \"status\", \"operator\": \"==\", \"value\": \"Active\"}\n    ]\n  },\n  {\n    \"type\": \"tMap\",\n    \"mappings\": {\n      \"name\": {\"action\": \"UPPERCASE\", \"target\": \"name_clean\"}\n    }\n  }\n]"
    ) }
    var outputResult by remember { mutableStateOf("Press 'Execute Job' to run pipeline.") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mobile ETL Studio (Talend Engine)", style = MaterialTheme.typography.headlineMedium)

        // CSV Input Field
        OutlinedTextField(
            value = inputCsv,
            onValueChange = { inputCsv = it },
            label = { Text("tFileInputDelimited (Raw CSV)") },
            modifier = Modifier.fillMaxWidth().height(150.dp)
        )

        // Job Rule configuration
        OutlinedTextField(
            value = jsonConfig,
            onValueChange = { jsonConfig = it },
            label = { Text("Talend Component Sequence (JSON Config)") },
            modifier = Modifier.fillMaxWidth().height(180.dp)
        )

        // Action Button
        Button(
            onClick = {
                try {
                    val rawDataset = FileUtils.readCsvText(inputCsv)
                    
                    // Parse Job config steps
                    val gson = Gson()
                    val stepType = object : TypeToken<List<EtlStep>>() {}.type
                    val steps: List<EtlStep> = gson.fromJson(jsonConfig, stepType)
                    
                    // Execute ETL Engine
                    val engine = MobileTalendEngine()
                    val outputData = engine.executeJob(rawDataset, steps)
                    
                    // Format output as delimited string
                    outputResult = FileUtils.convertToCsvText(outputData)
                } catch (e: Exception) {
                    outputResult = "Error in ETL execution: ${e.localizedMessage}"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Execute Job")
        }

        // CSV Output Field
        OutlinedTextField(
            value = outputResult,
            onValueChange = {},
            readOnly = true,
            label = { Text("tFileOutputDelimited (Output Result)") },
            modifier = Modifier.fillMaxWidth().height(150.dp)
        )
    }
}
