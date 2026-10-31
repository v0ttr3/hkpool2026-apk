package com.hkpool.app.analysis
import com.hkpool.app.data.ResultEntity
import kotlin.math.max

data class DigitScore(val digit:Int, val score:Double, val freq:Int, val gap:Int, val trend:Double)
data class PredictionResult(
    val scores:List<DigitScore>,
    val hidup:List<Int>,
    val mati:List<Int>,
    val ganjil:List<Int>,
    val genap:List<Int>,
    val ikut:List<Int>,
    val main:List<Int>,
    val keluar4D:List<String>,
    val keluar3D:List<String>,
    val keluar2D:List<String>,
    val explanation:String
)

object PredictionEngine {
    fun calculate(results:List<ResultEntity>, maxDigits:Int=7): PredictionResult {
        if(results.isEmpty()) return PredictionResult(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),"Belum ada data")
        val sorted = results.sortedBy { it.date }
        val last = sorted.last()
        val lastDigits = last.number.map { it.digitToInt() }
        val last90 = sorted.takeLast(90)
        val last30 = sorted.takeLast(30)
        val freq90 = IntArray(10)
        val freq30 = IntArray(10)
        val freqPos = Array(4){IntArray(10)}
        for(r in last90){ for((i,ch) in r.number.withIndex()){ val d=ch.digitToInt(); freq90[d]++; freqPos[i][d]++ } }
        for(r in last30){ for(ch in r.number) freq30[ch.digitToInt()]++ }
        val lastSeenGap = IntArray(10)
        for(d in 0..9){
            var gap=0; var found=false
            for(i in sorted.indices.reversed()){
                if(sorted[i].number.contains(d.toString())){found=true; break}
                gap++
            }
            lastSeenGap[d]=if(found) gap else 90
        }
        val coOcc = Array(10){IntArray(10)}
        for(i in 0 until sorted.size-1){
            val cur = sorted[i].number.map { it.digitToInt() }.toSet()
            val nxt = sorted[i+1].number.map { it.digitToInt() }.toSet()
            for(cd in cur) for(nd in nxt) coOcc[cd][nd]++
        }
        val ikutScore = IntArray(10)
        for(ld in lastDigits) for(d in 0..9) ikutScore[d]+=coOcc[ld][d]

        val scores = (0..9).map { d->
            val freqNorm = freq90[d] / max(1.0, last90.size*4.0)
            val gapScore = 1.0/(lastSeenGap[d]+1.0)
            val trend = if(freq90[d]==0) 0.0 else (freq30[d]/30.0)/(freq90[d]/90.0)
            val trendNorm = (trend.coerceIn(0.0,2.0)/2.0)
            val posVariance = freqPos.map { it[d] }.maxOrNull() ?:0
            val posScore = posVariance / max(1.0, last90.size.toDouble())
            val finalScore = 0.40*freqNorm + 0.30*gapScore + 0.20*trendNorm + 0.10*posScore
            DigitScore(d, finalScore, freq90[d], lastSeenGap[d], trend)
        }.sortedByDescending { it.score }

        val hidup = scores.take(maxDigits).map { it.digit }
        val matiRaw = scores.takeLast(maxDigits).map { it.digit }
        val mati = matiRaw.filter { it !in hidup }
        val ganjil = scores.filter { it.digit%2==1 }.take(maxDigits).map { it.digit }
        val genap = scores.filter { it.digit%2==0 }.take(maxDigits).map { it.digit }
        val ikutRanked = (0..9).sortedByDescending { ikutScore[it] }.take(maxDigits)
        val main = hidup.intersect(ikutRanked.toSet()).toList().ifEmpty { hidup.take(3) }

        val keluar4D = generateKeluar(hidup, 4, 12)
        val keluar3D = keluar4D.map { it.takeLast(3) }.distinct().take(12)
        val keluar2D = keluar4D.map { it.takeLast(2) }.distinct().take(12)

        val expl = "Mesin skor tunggal (freq40%+gap30%+trend20%+pos10%) dari ${last90.size} hari. Hidup=${hidup.joinToString("")}, Mati=${mati.joinToString("")} no-overlap, Ikut dari co-occurrence ${last.number}=${ikutRanked.joinToString("")}, Main=irisan."
        return PredictionResult(scores, hidup, mati, ganjil, genap, ikutRanked, main, keluar4D, keluar3D, keluar2D, expl)
    }

    private fun generateKeluar(hidup:List<Int>, len:Int, count:Int): List<String>{
        if(hidup.isEmpty()) return emptyList()
        val res = mutableListOf<String>()
        if(hidup.size>=4) res.add(hidup.take(4).joinToString("") { it.toString() })
        for(a in hidup.take(6)) for(b in hidup.take(6)) for(c in hidup.take(6)) for(d in hidup.take(6)){
            if(res.size>=30) break
            if(setOf(a,b,c,d).size==1) continue
            res.add("$a$b$c$d")
        }
        return res.distinct().take(count)
    }

    fun generateBBFS(digits:List<Int>): List<String>{
        val res = mutableSetOf<String>()
        for(a in digits) for(b in digits) for(c in digits) for(d in digits){
            if(setOf(a,b,c,d).size<2) continue
            res.add("$a$b$c$d")
        }
        return res.toList().take(200)
    }
    fun generateTarung(groupA:List<Int>, groupB:List<Int>): List<String>{
        val res = mutableListOf<String>()
        for(a in groupA) for(b in groupB){ res.add("$a$b"); res.add("$b$a"); for(c in groupA) for(d in groupB) res.add("$a$c$b$d") }
        return res.distinct().take(120)
    }
    fun generateColok(bebas:List<Int>): Map<String, List<String>>{
        return mapOf(
            "colokBebas" to bebas.map { it.toString() },
            "colokMacau" to bebas.flatMap { a-> bebas.filter { it!=a }.map { "$a$b" } }.distinct(),
            "colokNaga" to generateBBFS(bebas).map { it.take(3) }.distinct().take(20)
        )
    }
}
