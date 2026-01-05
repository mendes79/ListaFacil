package com.example.listafacil

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.UUID
import kotlin.math.min

// --- MODELO ---
data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "un",
    val brand: String = "",
    val isChecked: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ListaFacilApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaFacilApp() {
    val context = LocalContext.current
    val shoppingList = remember { mutableStateListOf<ShoppingItem>() }
    // Usamos a opção DEFAULT, que já suporta Latin (Português incluso)
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // --- LÓGICA DA IA COM CORRETOR (Versão 4.0) ---
    fun processImage(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    var itensAdicionados = 0
                    val delimiters = arrayOf("\n", ",", ";")

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val rawText = line.text
                            val possibleItems = rawText.split(*delimiters)

                            for (part in possibleItems) {
                                // Limpeza básica: remove pontuação inicial
                                val text = part.trim().trimStart('-', '.', '*', '•', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ')', ']')

                                if (text.length > 2) {
                                    val parts = text.split(" ", limit = 2)
                                    var qtd = "1"
                                    var nomeOriginal = text

                                    // Lógica de quantidade separada do nome
                                    // Se a primeira palavra parece um número ou "unidade"
                                    val firstWord = parts[0]

                                    // Tentativa de limpar APENAS a quantidade (l -> 1, o -> 0)
                                    val possibleQtd = firstWord
                                        .replace("l", "1", ignoreCase = true)
                                        .replace("o", "0", ignoreCase = true)

                                    // Se for número, separamos
                                    if (possibleQtd.all { it.isDigit() } && parts.size > 1) {
                                        qtd = possibleQtd
                                        nomeOriginal = parts[1]
                                    }

                                    // --- A MÁGICA DO CORRETOR ---
                                    // Passamos o nome lido pelo "Dicionário"
                                    val nomeCorrigido = AutoCorrector.correct(nomeOriginal)

                                    // Só adiciona se tiver letras
                                    if (nomeCorrigido.any { it.isLetter() }) {
                                        shoppingList.add(ShoppingItem(name = nomeCorrigido, quantity = qtd))
                                        itensAdicionados++
                                    }
                                }
                            }
                        }
                    }
                    if (itensAdicionados > 0) {
                        Toast.makeText(context, "$itensAdicionados itens lidos!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Não identifiquei itens.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Erro IA: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- CROPPER ---
    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful && result.uriContent != null) {
            processImage(result.uriContent!!)
        } else {
            val error = result.error
            if (error != null) Toast.makeText(context, "Erro: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun startScan() {
        val options = CropImageContractOptions(
            uri = null,
            cropImageOptions = CropImageOptions(
                imageSourceIncludeCamera = true,
                imageSourceIncludeGallery = true,
                guidelines = CropImageView.Guidelines.ON,
                activityTitle = "Recortar Lista",
                cropMenuCropButtonTitle = "Confirmar"
            )
        )
        cropImageLauncher.launch(options)
    }

    // --- PERMISSÕES ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) startScan() else Toast.makeText(context, "Câmera necessária", Toast.LENGTH_SHORT).show() }

    fun checkPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startScan()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // --- UI ---
    val allChecked = shoppingList.isNotEmpty() && shoppingList.all { it.isChecked }
    val countChecked = shoppingList.count { it.isChecked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        if (countChecked > 0) Text("$countChecked selecionado(s)", fontSize = 18.sp)
                        else Text("Lista Fácil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    if (countChecked > 0) {
                        IconButton(onClick = {
                            val toKeep = shoppingList.filter { !it.isChecked }
                            shoppingList.clear()
                            shoppingList.addAll(toKeep)
                        }) { Icon(Icons.Default.Delete, "Apagar", tint = Color.Red) }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallFloatingActionButton(
                    onClick = { shoppingList.add(ShoppingItem()) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary
                ) { Icon(Icons.Default.Add, "Add") }

                ExtendedFloatingActionButton(
                    onClick = { checkPermissionAndScan() },
                    icon = { Icon(Icons.Default.CameraAlt, "Camera") },
                    text = { Text("Escanear") }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(4.dp)) {
            if (shoppingList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Lista vazia.\nToque na Câmera para escanear.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Color.Gray)
                }
            } else {
                HeaderRow(allChecked = allChecked, onToggleAll = { shouldCheck ->
                    val iterator = shoppingList.listIterator()
                    while (iterator.hasNext()) {
                        iterator.set(iterator.next().copy(isChecked = shouldCheck))
                    }
                })
                Divider(color = Color.Gray, thickness = 1.dp)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(shoppingList, key = { _, item -> item.id }) { index, item ->
                        ShoppingItemRow(item = item, onUpdate = { shoppingList[index] = it })
                        Divider(color = Color.LightGray, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// --- DICIONÁRIO E CORRETOR AVANÇADO (Versão 5.0) ---
object AutoCorrector {
    // Mega Lista de Supermercado
    private val dictionary = listOf(
        // Básicos
        "Arroz", "Feijão", "Açúcar", "Café", "Leite", "Manteiga", "Pão", "Macarrão", "Óleo",
        "Sal", "Farinha", "Ovos", "Queijo", "Presunto", "Requeijão", "Iogurte", "Mel",
        "Azeite", "Vinagre", "Molho", "Maionese", "Ketchup", "Mostarda", "Tempero", "Alho",
        "Chocolate", "Achocolatado", "Biscoito", "Bolacha", "Torrada", "Pipoca", "Toddy",
        "Nescal", "Ovomaltine", "Geleia", "Miojo", "Farofa", "Farofa Pronta", "Coador",
        "Coador de Café", "Melitta", "Granola", "Chá", "Mate", "Chá Mate", "Chá Mate Leão", "Chia",
        "Torrada", "Torradas", "Pão de Queijo", "Penne",

        // Hortifruti (Onde estava o erro do Abacate)
        "Abacate", "Abacaxi", "Abóbora", "Abobrinha", "Alface", "Alho", "Banana", "Batata",
        "Beterraba", "Brócolis", "Cebola", "Cenoura", "Couve", "Espinafre", "Fruta", "Frutas",
        "Goiaba", "Laranja", "Limão", "Maçã", "Mamão", "Manga", "Maracujá", "Melancia", "Milho",
        "Melão", "Morango", "Pera", "Pimentão", "Repolho", "Rúcula", "Tomate", "Uva",
        "Vagem", "Cheiro Verde", "Salsinha", "Cebolinha", "Verduras", "Folhas", "Legumes",

        // Carnes e Frios
        "Carne", "Frango", "Peixe", "Carne Moída", "Bife", "Linguiça", "Salsicha", "Bacon",
        "Mortadela", "Peito de Peru", "Salame", "Hambúrguer", "Nugets", "Espetinho",

        // Limpeza e Higiene
        "Detergente", "Sabão", "Sabão em Pó", "Amaciante", "Água Sanitária", "Desinfetante",
        "Esponja", "Bombril", "Lã de Aço", "Álcool", "Papel Higiênico", "Papel Toalha",
        "Shampoo", "Condicionador", "Sabonete", "Pasta de Dente", "Fio Dental", "Desodorante",
        "Cotonete", "Algodão", "Absorvente", "Sapólio", "Azulim", "Veja", "Veja Multi Uso",
        "Desodorante Dove Man Care", "OB", "O.B.", "Mods", "Gilette", "Barbeador",
        "Lâmina de Barbear", "Loção pós Barba", "Phebo", "Pinça", "Cortador de Unha", "Esmalte",
        "Tinta", "Tinta para Cabelo", "Tinta p/ Cabelo", "Tinta Cabelo", "Espelho",

        // Bebidas e Outros
        "Água", "Suco", "Refrigerante", "Cerveja", "Vinho", "Gelo", "Fósforo", "Vela",
        "Pilha", "Ração", "Saco de Lixo", "Heineken", "Pepsi", "Coca", "Fanta", "Fanta Uva",
        "Barrinha de cereal", "Barrinha", "Proteína", "Água com Gás", "Água c/ Gás", "Energético"
    )

    fun correct(input: String): String {
        // Se a palavra for muito curta, não tenta corrigir (evita trocar "Uva" por "Ovos")
        if (input.length <= 2) return input

        var bestMatch: String? = null
        var smallestDistance = Int.MAX_VALUE

        val normalizedInput = input.lowercase()

        for (word in dictionary) {
            val normalizedWord = word.lowercase()
            val distance = calculateDistance(normalizedInput, normalizedWord)

            // LÓGICA DE TOLERÂNCIA (O Freio de Mão)
            // Só aceita a correção se a diferença for muito pequena em relação ao tamanho da palavra.
            // Ex: "Abacate" (7 letras) aceita até 2 erros. "Pão" (3 letras) só aceita 1.
            val tolerance = when {
                word.length <= 4 -> 1 // Palavras curtas: tem que ser quase idêntico
                word.length <= 7 -> 2 // Palavras médias: aceita 2 errinhos
                else -> 3             // Palavras longas: aceita 3 errinhos
            }

            if (distance < smallestDistance && distance <= tolerance) {
                smallestDistance = distance
                bestMatch = word
            }
        }

        // Se encontrou uma boa correção, usa ela.
        // Se não (bestMatch é null), mantém o que o usuário escreveu (input).
        return bestMatch ?: input
    }

    private fun calculateDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = Array(lhsLength + 1) { it }
        var newCost = Array(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}

// --- UI COMPONENTS ---
const val W_CHECK = 0.12f
const val W_ITEM = 0.38f
const val W_QTD = 0.12f
const val W_UN = 0.13f
const val W_OBS = 0.25f

@Composable
fun HeaderRow(allChecked: Boolean, onToggleAll: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(W_CHECK), contentAlignment = Alignment.Center) {
            Checkbox(checked = allChecked, onCheckedChange = { onToggleAll(it) }, modifier = Modifier.size(20.dp))
        }
        HeaderCell("Item", W_ITEM, Alignment.CenterStart)
        HeaderCell("Qtd", W_QTD)
        HeaderCell("Un", W_UN)
        HeaderCell("Obs", W_OBS, Alignment.CenterStart)
    }
}

@Composable
fun RowScope.HeaderCell(text: String, weight: Float, align: Alignment = Alignment.Center) {
    Box(modifier = Modifier.weight(weight), contentAlignment = align) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun ShoppingItemRow(item: ShoppingItem, onUpdate: (ShoppingItem) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(W_CHECK), contentAlignment = Alignment.Center) {
            Checkbox(checked = item.isChecked, onCheckedChange = { onUpdate(item.copy(isChecked = it)) }, modifier = Modifier.size(20.dp))
        }
        InputCell(value = item.name, onValueChange = { onUpdate(item.copy(name = it)) }, weight = W_ITEM, isNumber = false)
        InputCell(value = item.quantity, onValueChange = { onUpdate(item.copy(quantity = it)) }, weight = W_QTD, isNumber = true, centerText = true)
        InputCell(value = item.unit, onValueChange = { onUpdate(item.copy(unit = it)) }, weight = W_UN, isNumber = false, centerText = true)
        InputCell(value = item.brand, onValueChange = { onUpdate(item.copy(brand = it)) }, weight = W_OBS, isNumber = false)
    }
}

@Composable
fun RowScope.InputCell(value: String, onValueChange: (String) -> Unit, weight: Float, isNumber: Boolean, centerText: Boolean = false) {
    Box(modifier = Modifier.weight(weight).padding(horizontal = 2.dp), contentAlignment = if (centerText) Alignment.Center else Alignment.CenterStart) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 14.sp, textAlign = if (centerText) TextAlign.Center else TextAlign.Start, color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text, imeAction = ImeAction.Done),
            singleLine = false, maxLines = 3,
            modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(vertical = 4.dp)
        )
    }
}