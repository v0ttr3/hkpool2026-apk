package com.hkpool.app.util
import com.hkpool.app.data.ResultEntity
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.time.LocalDate

object CalendarImporter {
    fun importFromExcel(input: InputStream, year: Int = 2026): List<ResultEntity> {
        val wb = WorkbookFactory.create(input)
        val sheet = wb.getSheetAt(0)
        val startDate = LocalDate.of(year,1,1)
        val firstDayCol = when(startDate.dayOfWeek.value) {
            1->0;2->1;3->2;4->3;5->4;6->5;7->6; else->3
        }
        val results = mutableListOf<ResultEntity>()
        for (r in 2 until sheet.physicalNumberOfRows) {
            val row = sheet.getRow(r) ?: continue
            for (c in 0..6) {
                val cell = row.getCell(c) ?: continue
                val raw = try {
                    when(cell.cellType) {
                        org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toInt()
                        org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue.trim().toIntOrNull() ?: continue
                        org.apache.poi.ss.usermodel.CellType.FORMULA -> cell.numericCellValue.toInt()
                        else -> continue
                    }
                } catch (e:Exception){ continue }
                if (raw <0 || raw>9999) continue
                val normalized = String.format("%04d", raw)
                val offset = (r-2)*7 + (c - firstDayCol)
                if (offset<0) continue
                val date = startDate.plusDays(offset.toLong())
                if (date.year!=year) continue
                results.add(ResultEntity(date.toString(), normalized, normalized[0].digitToInt(), normalized[1].digitToInt(), normalized[2].digitToInt(), normalized[3].digitToInt()))
            }
        }
        wb.close()
        return results.sortedBy { it.date }
    }

    fun importFromCsv(content: String): List<ResultEntity> {
        return content.lines().mapNotNull { line ->
            if(line.isBlank()) return@mapNotNull null
            val p = line.split(",",";","\t".toRegex())
            if(p.size<2) return@mapNotNull null
            val dateStr = p[0].trim()
            val numStr = p[1].trim()
            if(!dateStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return@mapNotNull null
            val raw = numStr.toIntOrNull() ?: return@mapNotNull null
            val norm = String.format("%04d", raw)
            ResultEntity(dateStr, norm, norm[0].digitToInt(), norm[1].digitToInt(), norm[2].digitToInt(), norm[3].digitToInt())
        }
    }
}
