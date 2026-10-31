package com.policyregistration.app.domain

import android.content.Context
import android.net.Uri
import com.policyregistration.app.data.local.CampaignEntity
import com.policyregistration.app.data.repository.DecryptedRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class ExcelExporter {

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        campaign: CampaignEntity?,
        registrations: List<DecryptedRegistration>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(Exception("Could not open destination file output stream"))

            outputStream.use { out ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Policy Registrations")

                // Styles - Plain clean styles without any fill colors
                val mainHeaderFont = workbook.createFont().apply {
                    fontHeightInPoints = 11
                    bold = true
                    color = IndexedColors.BLACK.index
                }
                val mainHeaderStyle = workbook.createCellStyle().apply {
                    setFont(mainHeaderFont)
                    alignment = HorizontalAlignment.CENTER
                    verticalAlignment = VerticalAlignment.CENTER
                    wrapText = true
                    borderBottom = BorderStyle.THIN
                    borderTop = BorderStyle.THIN
                    borderLeft = BorderStyle.THIN
                    borderRight = BorderStyle.THIN
                }

                val colHeaderFont = workbook.createFont().apply {
                    fontHeightInPoints = 10
                    bold = true
                    color = IndexedColors.BLACK.index
                }
                val colHeaderStyle = workbook.createCellStyle().apply {
                    setFont(colHeaderFont)
                    alignment = HorizontalAlignment.CENTER
                    verticalAlignment = VerticalAlignment.CENTER
                    wrapText = true
                    borderBottom = BorderStyle.THIN
                    borderTop = BorderStyle.THIN
                    borderLeft = BorderStyle.THIN
                    borderRight = BorderStyle.THIN
                }

                val dataTextStyle = workbook.createCellStyle().apply {
                    borderBottom = BorderStyle.THIN
                    borderTop = BorderStyle.THIN
                    borderLeft = BorderStyle.THIN
                    borderRight = BorderStyle.THIN
                    // Text format so numbers don't convert to scientific notation
                    val dataFormat = workbook.createDataFormat()
                    setDataFormat(dataFormat.getFormat("@"))
                }

                val dataCenterStyle = workbook.createCellStyle().apply {
                    cloneStyleFrom(dataTextStyle)
                    alignment = HorizontalAlignment.CENTER
                }

                // Row 1: Single Merged Header (Merged A1:L1 across all 12 columns)
                val row0 = sheet.createRow(0)
                row0.heightInPoints = 56f
                for (c in 0..11) {
                    val cell = row0.createCell(c)
                    cell.cellStyle = mainHeaderStyle
                }

                // Compose the unified header text from campaign settings
                val headerCompany = campaign?.insuranceCompany?.ifBlank { "UNITED INDIA INSURANCE COMPANY LIMITED, Tirupathi" }
                    ?: "UNITED INDIA INSURANCE COMPANY LIMITED, Tirupathi"
                val headerDistrict = campaign?.district?.ifBlank { "CHITTOOR(Dist)" } ?: "CHITTOOR(Dist)"
                val headerMeeting = campaign?.meetingPlace?.ifBlank { "Duddeda" } ?: "Duddeda"
                val headerDate = campaign?.campaignDate?.ifBlank { "14-10-2026" } ?: "14-10-2026"
                val headerCert = campaign?.certificateNo?.ifBlank { "051200/47" } ?: "051200/47"
                val headerRange = campaign?.serialNumberRange?.ifBlank { "10119 to 10151" } ?: "10119 to 10151"
                val headerFrom = campaign?.policyValidFrom?.ifBlank { "15-11-2026" } ?: "15-11-2026"
                val headerTo = campaign?.policyValidTo?.ifBlank { "14-11-2027" } ?: "14-11-2027"

                val singleMergedHeaderText = "$headerCompany, $headerDistrict.\n" +
                        "Meeting Place: $headerMeeting; Date: $headerDate Certificate No: $headerCert\n" +
                        "($headerRange) Valid From $headerFrom to $headerTo"

                row0.getCell(0).setCellValue(singleMergedHeaderText)
                sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 11))

                // Row 2: Exact Column Headers matching the required layout
                val row1 = sheet.createRow(1)
                row1.heightInPoints = 40f
                val headers = listOf(
                    "S. No",
                    "Aadhaar No",
                    "Name of the Mason",
                    "Address",
                    "Age\n(Years)",
                    "Nominee",
                    "Relation",
                    "Phone Number",
                    "DOB (DD.MM.YY)",
                    "Gender\n(Male/\nFemale)",
                    "Have you met\nany accident\nin previous\n(Yes/No)",
                    "Do you have\nany other Existing\nAccidental policy\n(Yes/No)"
                )
                headers.forEachIndexed { idx, title ->
                    val cell = row1.createCell(idx)
                    cell.setCellValue(title)
                    cell.cellStyle = colHeaderStyle
                }

                // Row 3+: Data rows
                var rowIdx = 2
                registrations.sortedBy { it.entity.sNo }.forEach { item ->
                    val dataRow = sheet.createRow(rowIdx++)
                    val ent = item.entity

                    // Column A: S No
                    dataRow.createCell(0).apply {
                        setCellValue(ent.sNo.toString())
                        cellStyle = dataCenterStyle
                    }
                    // Column B: Aadhaar No (Plain 12-digit string format)
                    dataRow.createCell(1).apply {
                        setCellValue(item.aadhaarPlain)
                        cellStyle = dataCenterStyle
                    }
                    // Column C: Name of the Mason
                    dataRow.createCell(2).apply {
                        setCellValue(ent.nameOfTheMason)
                        cellStyle = dataTextStyle
                    }
                    // Column D: Address
                    dataRow.createCell(3).apply {
                        setCellValue(ent.address)
                        cellStyle = dataTextStyle
                    }
                    // Column E: Age
                    dataRow.createCell(4).apply {
                        setCellValue(ent.age.toString())
                        cellStyle = dataCenterStyle
                    }
                    // Column F: Nominee
                    dataRow.createCell(5).apply {
                        setCellValue(ent.nominee)
                        cellStyle = dataTextStyle
                    }
                    // Column G: Relation
                    dataRow.createCell(6).apply {
                        setCellValue(ent.relation)
                        cellStyle = dataCenterStyle
                    }
                    // Column H: Phone Number
                    dataRow.createCell(7).apply {
                        setCellValue(ent.phoneNumber)
                        cellStyle = dataCenterStyle
                    }
                    // Column I: DOB
                    dataRow.createCell(8).apply {
                        setCellValue(ent.dob)
                        cellStyle = dataCenterStyle
                    }
                    // Column J: Gender
                    dataRow.createCell(9).apply {
                        setCellValue(ent.gender)
                        cellStyle = dataCenterStyle
                    }
                    // Column K: Accident Previous
                    dataRow.createCell(10).apply {
                        setCellValue(ent.accidentPrevious)
                        cellStyle = dataCenterStyle
                    }
                    // Column L: Existing Policy
                    dataRow.createCell(11).apply {
                        setCellValue(ent.existingPolicy)
                        cellStyle = dataCenterStyle
                    }
                }

                // Auto-size columns with sensible padding matching official template
                sheet.setColumnWidth(0, 8 * 256)   // S. No
                sheet.setColumnWidth(1, 16 * 256)  // Aadhaar No
                sheet.setColumnWidth(2, 26 * 256)  // Name of the Mason
                sheet.setColumnWidth(3, 24 * 256)  // Address
                sheet.setColumnWidth(4, 12 * 256)  // Age (Years)
                sheet.setColumnWidth(5, 22 * 256)  // Nominee
                sheet.setColumnWidth(6, 14 * 256)  // Relation
                sheet.setColumnWidth(7, 16 * 256)  // Phone Number
                sheet.setColumnWidth(8, 16 * 256)  // DOB (DD.MM.YY)
                sheet.setColumnWidth(9, 14 * 256)  // Gender (Male/ Female)
                sheet.setColumnWidth(10, 18 * 256) // Have you met any accident in previous (Yes/No)
                sheet.setColumnWidth(11, 20 * 256) // Do you have any other Existing Accidental policy (Yes/No)

                workbook.write(out)
                workbook.close()
            }
            Result.success(registrations.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
