package com.example.conversordemoedas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.conversordemoedas.ui.theme.ConversorDeMoedasTheme

class AboutActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConversorDeMoedasTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Sobre o App") }
                        )
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AboutScreenContent {
                            // Fecha a tela atual para voltar à anterior
                            finish()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreenContent(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Autores do App", fontSize = 22.sp, modifier = Modifier.padding(bottom = 16.dp))
        Text("TCC do INTIN IFPB/CZ:", fontSize = 22.sp, modifier = Modifier.padding(bottom = 16.dp))
        Text("José Frankarllyson", fontSize = 18.sp)
        Text("Kaio Gabriel", fontSize = 18.sp)
        Text("Samuel Lins ", fontSize = 18.sp)
        Text("Orientador: Andre de Paula R. G. Luz", fontSize = 18.sp)
        Text("Coorientador:  Michel Silva", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBackClick) {
            Text("Voltar")
        }
    }
}
