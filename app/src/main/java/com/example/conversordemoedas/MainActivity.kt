package com.example.conversordemoedas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.conversordemoedas.ui.theme.ConversorDeMoedasTheme
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info


// Interface para a API
interface CurrencyApiService {
    @GET("v6/{apiKey}/latest/{baseCurrency}")
    fun getExchangeRates(
        @Path("apiKey") apiKey: String,
        @Path("baseCurrency") baseCurrency: String
    ): Call<JsonObject>
}

class MainActivity : ComponentActivity() {
    private val apiKey = "5cc45da295ed4917dd7a8f2c"

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConversorDeMoedasTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Conversor de Moedas") },
                            actions = {
                                val context = LocalContext.current
                                IconButton(onClick = {
                                    context.startActivity(Intent(context, AboutActivity::class.java))
                                }) {
                                    Icon(Icons.Default.Info, contentDescription = "Sobre")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        CurrencyConverterScreen(apiKey = apiKey)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(apiKey: String) {
    var amount by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("BRL") }
    var result by remember { mutableStateOf("Resultado") }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("conversion_history", Context.MODE_PRIVATE) }

    // Carrega o histórico garantindo a ordem
    var history by remember {
        val savedHistoryString = sharedPrefs.getString("history_list", null)
        val savedHistory = savedHistoryString?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        mutableStateOf(savedHistory)
    }

    val currencies = listOf("USD", "EUR", "BRL", "JPY", "GBP")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Guia Rápido:\n1. Insira o valor.\n2. Selecione as moedas.\n3. Clique em 'Converter'.",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Valor") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            CurrencyDropdown(selectedCurrency = fromCurrency, onCurrencyChange = { fromCurrency = it }, currencies = currencies, modifier = Modifier.weight(1f))
            Text("para", modifier = Modifier.padding(horizontal = 8.dp))
            CurrencyDropdown(selectedCurrency = toCurrency, onCurrencyChange = { toCurrency = it }, currencies = currencies, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (amount.isNotEmpty()) {
                    getConversionRate(
                        apiKey = apiKey, amountStr = amount, fromCurrency = fromCurrency, toCurrency = toCurrency,
                        onSuccess = { newResult ->
                            result = newResult
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            val currentDate = sdf.format(Date())
                            val historyEntry = "$currentDate: $newResult"

                            val updatedHistory = (listOf(historyEntry) + history).take(5)
                            history = updatedHistory

                            // Salva o histórico como uma única String para manter a ordem
                            sharedPrefs.edit().putString("history_list", updatedHistory.joinToString("\n")).apply()
                        },
                        onError = { error -> result = error }
                    )
                } else {
                    Toast.makeText(context, "Por favor, insira um valor", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Converter")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = result, fontSize = 24.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Histórico:", fontSize = 18.sp)
            Row {
                Button(onClick = {
                    if (history.isNotEmpty()) {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Minhas últimas conversões:\n\n" + history.joinToString("\n"))
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Histórico")
                        context.startActivity(shareIntent)
                    } else {
                        Toast.makeText(context, "Histórico está vazio.", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Compartilhar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    history = emptyList()
                    sharedPrefs.edit().remove("history_list").apply()
                }) {
                    Text(text = "Limpar")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column {
            history.forEach { item ->
                Text(item)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown(
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit,
    currencies: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            value = selectedCurrency,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onCurrencyChange(currency)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

private fun getConversionRate(
    apiKey: String,
    amountStr: String,
    fromCurrency: String,
    toCurrency: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val amount = amountStr.toDoubleOrNull()
    if (amount == null) {
        onError("Valor inválido")
        return
    }

    val retrofit = Retrofit.Builder()
        .baseUrl("https://v6.exchangerate-api.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(CurrencyApiService::class.java)
    val call = service.getExchangeRates(apiKey, fromCurrency)

    call.enqueue(object : Callback<JsonObject> {
        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
            if (response.isSuccessful) {
                val rates = response.body()?.getAsJsonObject("conversion_rates")
                val rate = rates?.get(toCurrency)?.asDouble
                if (rate != null) {
                    val result = amount * rate
                    onSuccess(String.format(Locale.US, "%.2f %s = %.2f %s", amount, fromCurrency, result, toCurrency))
                } else {
                    onError("Erro ao obter a taxa de conversão.")
                }
            } else {
                onError("Erro na resposta da API.")
            }
        }

        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
            onError("Falha na conexão: ${t.message}")
        }
    })
}
