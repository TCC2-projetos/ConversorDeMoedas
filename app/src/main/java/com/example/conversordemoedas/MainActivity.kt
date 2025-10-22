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
import androidx.compose.material3.MenuAnchorType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

// Interface para a API (igual ao aula anterior)
interface CurrencyApiService {
    @GET("v6/{apiKey}/latest/{baseCurrency}")
    fun getExchangeRates(
        @Path("apiKey") apiKey: String,
        @Path("baseCurrency") baseCurrency: String
    ): Call<JsonObject>
}

class MainActivity : ComponentActivity() {
    // Substitua "SUA_CHAVE_DE_API" pela chave obtidas no site da ExchangeRate-API
    private val apiKey = "5cc45da295ed4917dd7a8f2c"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConversorDeMoedasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CurrencyConverterScreen(apiKey = apiKey)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(apiKey: String) {
    // Estados para armazenar os valores da UI
    var amount by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("BRL") }
    var result by remember { mutableStateOf("Resultado") }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("conversion_history", Context.MODE_PRIVATE) }

    var history by remember {
        val savedHistory = sharedPrefs.getStringSet("history", emptySet())?.toList() ?: emptyList()
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
        // Campo para inserir o valor
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Valor") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Spinners para selecionar as moedas
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            CurrencyDropdown(
                selectedCurrency = fromCurrency,
                onCurrencyChange = { fromCurrency = it },
                currencies = currencies,
                modifier = Modifier.weight(1f)
            )
            Text("para", modifier = Modifier.padding(horizontal = 8.dp))
            CurrencyDropdown(
                selectedCurrency = toCurrency,
                onCurrencyChange = { toCurrency = it },
                currencies = currencies,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botão para converter
        Button(
            onClick = {
                if (amount.isNotEmpty()) {
                    // Lógica da API é chamada aqui
                    getConversionRate(
                        apiKey = apiKey,
                        amountStr = amount,
                        fromCurrency = fromCurrency,
                        toCurrency = toCurrency,
                        onSuccess = { newResult ->
                            result = newResult
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            val currentDate = sdf.format(Date())
                            val historyEntry = "$currentDate: $newResult"

                            val updatedHistory = (listOf(historyEntry) + history).take(5)
                            history = updatedHistory

                            sharedPrefs.edit {
                                putStringSet("history", updatedHistory.toSet())
                            }
                        },
                        onError = { error ->
                            result = error
                        }
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

        // Texto para exibir o resultado
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
                        val sendIntent: Intent = Intent().apply {
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
                    // 1. Limpa a lista que está na memória (para a UI atualizar)
                    history = emptyList()

                    // 2. Limpa a lista que está salva no armazenamento
                    sharedPrefs.edit {
                        // Opção A: Remove a chave completamente. É o mais limpo.
                        remove("history")
                    }
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
            value = selectedCurrency,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable)
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
                    }
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
