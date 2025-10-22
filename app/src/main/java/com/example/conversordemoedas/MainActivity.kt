package com.example.conversordemoedas

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

// Interface para a API (igual ao aula anterior)
interface CurrencyApiService {
    @GET("v6/{apiKey}/latest/{baseCurrency}")
    fun getExchangeRates(
        @Path("apiKey") apiKey: String,
        @Path("baseCurrency") baseCurrency: String
    ): Call<JsonObject>
}

class MainActivity : ComponentActivity() {
    // Substitua "SUA_CHAVE_DE_API" pela chave obetida no site da ExchangeRate-API
    private val apiKey = "666da295ed4917dd7a8f2c"

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

    val currencies = listOf("USD", "EUR", "BRL", "JPY", "GBP")
    val context = LocalContext.current

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
                    getConversionRate(apiKey, amount, fromCurrency, toCurrency) { newResult ->
                        result = newResult
                    }
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
            modifier = Modifier.menuAnchor()
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
    onResult: (String) -> Unit
) {
    val amount = amountStr.toDouble()

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
                    onResult(String.format("%.2f %s = %.2f %s", amount, fromCurrency, result, toCurrency))
                } else {
                    onResult("Erro ao obter a taxa de conversão.")
                }
            } else {
                onResult("Erro na resposta da API.")
            }
        }

        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
            onResult("Falha na conexão: ${t.message}")
        }
    })
}