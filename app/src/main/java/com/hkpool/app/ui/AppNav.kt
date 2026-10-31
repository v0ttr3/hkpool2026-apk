package com.hkpool.app.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hkpool.app.analysis.PredictionEngine
import com.hkpool.app.data.AppDatabase
import com.hkpool.app.data.ResultEntity
import com.hkpool.app.util.CalendarImporter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AppNav(db: AppDatabase, activityContext: Context) {
    var results by remember { mutableStateOf<List<ResultEntity>>(emptyList()) }
    var tab by remember { mutableStateOf(0) }
    var prediction by remember { mutableStateOf(PredictionEngine.calculate(emptyList())) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun refresh(){
        scope.launch {
            val all = db.resultDao().getAll()
            results = all
            prediction = PredictionEngine.calculate(all)
        }
    }

    LaunchedEffect(Unit){ refresh() }

    val excelLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
        if(uri!=null){
            scope.launch {
                try{
                    val input = context.contentResolver.openInputStream(uri)!!
                    val imported = CalendarImporter.importFromExcel(input)
                    db.resultDao().insertAll(imported)
                    Toast.makeText(context, "Import ${imported.size} data, normalisasi 0 depan OK", Toast.LENGTH_LONG).show()
                    refresh()
                }catch(e:Exception){
                    Toast.makeText(context, "Gagal import: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = { 
            Column(Modifier.padding(16.dp)){
                Text("PENGELUARAN HK POOL 2026", style=MaterialTheme.typography.headlineSmall)
                Text("${results.size} data • terakhir ${results.firstOrNull()?.date ?: "-"} : ${results.firstOrNull()?.number ?: "-"}", style=MaterialTheme.typography.bodySmall)
            }
        },
        bottomBar = {
            NavigationBar{
                NavigationBarItem(selected=tab==0, onClick={tab=0}, label={Text("Kalender")}, icon={Text("📅")})
                NavigationBarItem(selected=tab==1, onClick={tab=1}, label={Text("Daftar")}, icon={Text("📋")})
                NavigationBarItem(selected=tab==2, onClick={tab=2}, label={Text("Analisis")}, icon={Text("📊")})
                NavigationBarItem(selected=tab==3, onClick={tab=3}, label={Text("Prediksi")}, icon={Text("🎯")})
                NavigationBarItem(selected=tab==4, onClick={tab=4}, label={Text("Generator")}, icon={Text("🎲")})
            }
        }
    ){ pad ->
        Column(Modifier.padding(pad).padding(12.dp)){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button(onClick={excelLauncher.launch("*/*")}){ Text("Import Excel/CSV") }
                var showManual by remember { mutableStateOf(false) }
                Button(onClick={showManual=true}){ Text("Input 23:00") }
                if(showManual){ ManualInputDialog(db, onDone={showManual=false; refresh()}) }
            }
            Spacer(Modifier.height(8.dp))
            when(tab){
                0 -> CalendarView(results)
                1 -> ListView(results, db, scope, onRefresh={refresh()})
                2 -> AnalysisView(results, prediction)
                3 -> PredictionView(prediction, results)
                4 -> GeneratorView(prediction)
            }
        }
    }
}

@Composable
fun CalendarView(results:List<ResultEntity>){
    val byDate = results.associateBy { it.date }
    val start = LocalDate.of(2026,1,1)
    val end = LocalDate.of(2026,10,29)
    val days = listOf("Senin","Selasa","Rabu","Kamis","Jumat","Sabtu","Minggu")
    Column{
        Row{ days.forEach{ d-> Box(Modifier.weight(1f).padding(2.dp), contentAlignment=Alignment.Center){ Text(d, style=MaterialTheme.typography.labelSmall) } } }
        LazyColumn{
            var cur = start
            // generate weeks
            val weeks = mutableListOf<List<LocalDate?>>()
            var week = mutableListOf<LocalDate?>()
            val firstCol = start.dayOfWeek.value-1
            repeat(firstCol){ week.add(null) }
            while(cur <= end){
                week.add(cur)
                if(week.size==7){ weeks.add(week.toList()); week=mutableListOf() }
                cur = cur.plusDays(1)
            }
            if(week.isNotEmpty()){ while(week.size<7) week.add(null); weeks.add(week) }
            items(weeks.size){ idx ->
                val w = weeks[idx]
                Row(Modifier.fillMaxWidth()){
                    w.forEach{ date ->
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp).border(0.5.dp, Color.Gray).background(if(date!=null && byDate[date.toString()]!=null) Color(0xFFE3F2FD) else Color.White), contentAlignment=Alignment.Center){
                            if(date!=null){
                                val res = byDate[date.toString()]
                                Column(horizontalAlignment=Alignment.CenterHorizontally){
                                    Text("${date.dayOfMonth}", style=MaterialTheme.typography.labelSmall)
                                    if(res!=null){
                                        val isNorm = res.number.startsWith("0")
                                        Text(res.number, style=MaterialTheme.typography.bodySmall, color=if(isNorm) Color(0xFFD32F2F) else Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListView(results:List<ResultEntity>, db:AppDatabase, scope:kotlinx.coroutines.CoroutineScope, onRefresh:()->Unit){
    var filterMonth by remember { mutableStateOf("") }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(4.dp)){
        items(results.filter { if(filterMonth.isBlank()) true else it.date.startsWith(filterMonth) }.take(300)){ r->
            Card{ Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement=Arrangement.SpaceBetween){ Text(r.date); Text(r.number, style=MaterialTheme.typography.titleMedium) } }
        }
    }
}

@Composable
fun AnalysisView(results:List<ResultEntity>, pred:com.hkpool.app.analysis.PredictionResult){
    if(results.isEmpty()){ Text("Belum ada data"); return }
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Skor Digit (satu mesin):", style=MaterialTheme.typography.titleMedium)
        pred.scores.forEach{ s-> 
            Row{ Text("${s.digit}: score ${"%.3f".format(s.score)} freq=${s.freq} gap=${s.gap} trend=${"%.2f".format(s.trend)}", style=MaterialTheme.typography.bodySmall) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Distribusi: ${results.size} data")
    }
}

@Composable
fun PredictionView(pred:com.hkpool.app.analysis.PredictionResult, results:List<ResultEntity>){
    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{
            Card{ Column(Modifier.padding(12.dp)){ Text(pred.explanation, style=MaterialTheme.typography.bodySmall) } }
            Spacer(Modifier.height(8.dp))
        }
        item{
            PredictionCard(pred)
        }
    }
}

@Composable
fun PredictionCard(p:com.hkpool.app.analysis.PredictionResult){
    Card{ Column(Modifier.padding(12.dp), verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text("Hidup (top max 7): ${p.hidup.joinToString("")}  [${p.hidup}]", style=MaterialTheme.typography.titleMedium)
        Text("Mati (bottom, no overlap): ${p.mati.joinToString("")} [${p.mati}]")
        Text("Ganjil: ${p.ganjil.joinToString("")} | Genap: ${p.genap.joinToString("")}")
        Text("Ikut (co-occurrence): ${p.ikut.joinToString("")} [${p.ikut}]")
        Text("Main (irisan hidup&ikut): ${p.main.joinToString("")}")
        Divider()
        Text("Keluar 4D (dari Hidup): ${p.keluar4D.joinToString(", ")}")
        Text("Keluar 3D (potongan 4D): ${p.keluar3D.joinToString(", ")}")
        Text("Keluar 2D (potongan 4D): ${p.keluar2D.joinToString(", ")}")
    }}
}

@Composable
fun GeneratorView(p:com.hkpool.app.analysis.PredictionResult){
    val bbfs = PredictionEngine.generateBBFS(p.hidup)
    val tarung = PredictionEngine.generateTarung(p.hidup.take(3), p.mati.take(3))
    val colok = PredictionEngine.generateColok(p.hidup.take(4))
    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{ Text("BBFS dari Hidup (${bbfs.size}):", style=MaterialTheme.typography.titleSmall); Text(bbfs.take(20).joinToString(", ")) }
        item{ Text("Pola Tarung Hidup vs Mati (${tarung.size}):", style=MaterialTheme.typography.titleSmall); Text(tarung.take(30).joinToString(", ")) }
        item{ Text("Colok Bebas: ${colok["colokBebas"]?.joinToString(", ")}") ; Text("Colok Macau: ${colok["colokMacau"]?.take(20)?.joinToString(", ")}") ; Text("Colok Naga: ${colok["colokNaga"]?.joinToString(", ")}") }
    }
}

@Composable
fun ManualInputDialog(db:AppDatabase, onDone:()->Unit){
    var dateStr by remember { mutableStateOf(LocalDate.now().toString()) }
    var numStr by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    AlertDialog(onDismissRequest=onDone, title={Text("Input Manual 23:00")}, text={
        Column{ 
            OutlinedTextField(value=dateStr, onValueChange={dateStr=it}, label={Text("Tanggal yyyy-MM-dd")})
            OutlinedTextField(value=numStr, onValueChange={numStr=it}, label={Text("Angka 0-9999 (940 auto jadi 0940)")})
        }
    }, confirmButton={
        Button(onClick={
            scope.launch{
                try{
                    val raw = numStr.trim().toInt()
                    val norm = String.format("%04d", raw)
                    val e = ResultEntity(dateStr, norm, norm[0].digitToInt(), norm[1].digitToInt(), norm[2].digitToInt(), norm[3].digitToInt())
                    db.resultDao().insert(e)
                    Toast.makeText(ctx, "Simpan $norm", Toast.LENGTH_SHORT).show()
                    onDone()
                }catch(e:Exception){ Toast.makeText(ctx, "Error ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }){ Text("Simpan") }
    })
}
