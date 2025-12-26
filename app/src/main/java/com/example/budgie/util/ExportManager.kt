package com.example.budgie.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.FinancialGoal
import com.example.budgie.data.model.Income
import com.example.budgie.data.model.Bill
import com.example.budgie.data.model.Loan
import com.example.budgie.data.model.ShoppingListWithItems
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())

    // Professional Color Palette
    private val NAVY_PRIMARY = DeviceRgb(11, 31, 42)           // #0B1F2A
    private val EMERALD = DeviceRgb(15, 174, 150)              // #0FAE96
    private val EMERALD_LIGHT = DeviceRgb(15, 174, 150)        // Light emerald for backgrounds
    private val GOLD = DeviceRgb(201, 161, 74)                 // #C9A14A
    private val SOFT_WHITE = DeviceRgb(230, 241, 240)          // #E6F1F0
    private val MUTED_RED = DeviceRgb(229, 115, 115)           // #E57373
    private val BLUE = DeviceRgb(92, 156, 229)                 // #5C9CE5
    private val AMBER = DeviceRgb(255, 183, 77)                // #FFB74D
    private val PURPLE = DeviceRgb(149, 117, 205)              // #9575CD
    private val LIGHT_GRAY = DeviceRgb(245, 247, 250)          // Light background
    private val TABLE_HEADER_BG = DeviceRgb(20, 45, 60)        // Dark header
    private val TABLE_ROW_ALT = DeviceRgb(240, 248, 247)       // Alternating row

    /**
     * Export data to Professional PDF format with colored tables
     * Order: Income (FIRST) → Expenses → Bills → Loans → Goals → Shopping Lists (LAST)
     */
    fun exportToPdf(
        context: Context,
        userName: String,
        expenses: List<Expense>,
        incomes: List<Income>,
        bills: List<Bill>,
        loans: List<Loan> = emptyList(),
        goals: List<FinancialGoal> = emptyList(),
        shoppingLists: List<ShoppingListWithItems> = emptyList()
    ): File? {
        return try {
            val fileName = "Budgie_Financial_Report_${fileNameDateFormat.format(Date())}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // Set document margins
            document.setMargins(36f, 36f, 36f, 36f)

            // Build document
            buildPdfContent(document, userName, incomes, expenses, bills, loans, goals, shoppingLists)

            document.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildPdfContent(
        document: Document,
        userName: String,
        incomes: List<Income>,
        expenses: List<Expense>,
        bills: List<Bill>,
        loans: List<Loan>,
        goals: List<FinancialGoal>,
        shoppingLists: List<ShoppingListWithItems>
    ) {
        // ========== HEADER SECTION ==========
        addProfessionalHeader(document, userName)

        // ========== SUMMARY SECTION ==========
        addSummarySection(document, incomes, expenses, bills, loans, goals)

        // ========== INCOME TABLE (FIRST) ==========
        if (incomes.isNotEmpty()) {
            addIncomeTable(document, incomes)
        }

        // ========== EXPENSES TABLE ==========
        if (expenses.isNotEmpty()) {
            addExpenseTable(document, expenses)
        }

        // ========== BILLS TABLE ==========
        if (bills.isNotEmpty()) {
            addBillsTable(document, bills)
        }

        // ========== LOANS TABLE (AFTER BILLS) ==========
        if (loans.isNotEmpty()) {
            addLoansTable(document, loans)
        }

        // ========== GOALS TABLE ==========
        if (goals.isNotEmpty()) {
            addGoalsTable(document, goals)
        }

        // ========== SHOPPING LISTS TABLE (LAST) ==========
        if (shoppingLists.isNotEmpty()) {
            addShoppingListsTable(document, shoppingLists)
        }

        // ========== FOOTER ==========
        addProfessionalFooter(document)
    }

    private fun addProfessionalHeader(document: Document, userName: String) {
        // Main Title with brand styling
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()

        val headerCell = Cell()
            .setBackgroundColor(NAVY_PRIMARY)
            .setPadding(20f)
            .setBorder(Border.NO_BORDER)

        headerCell.add(
            Paragraph("BUDGIE")
                .setFontSize(28f)
                .setBold()
                .setFontColor(EMERALD)
                .setTextAlignment(TextAlignment.CENTER)
        )

        headerCell.add(
            Paragraph("FINANCIAL REPORT")
                .setFontSize(16f)
                .setFontColor(SOFT_WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(-5f)
        )

        headerCell.add(
            Paragraph("Your Personal Finance Companion")
                .setFontSize(10f)
                .setFontColor(DeviceRgb(150, 180, 175))
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic()
                .setMarginTop(8f)
        )

        headerTable.addCell(headerCell)
        document.add(headerTable)

        // User info bar
        val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()

        val nameCell = Cell()
            .setBackgroundColor(DeviceRgb(15, 40, 55))
            .setPadding(12f)
            .setBorder(Border.NO_BORDER)
        nameCell.add(
            Paragraph("Prepared for: $userName")
                .setFontSize(11f)
                .setFontColor(SOFT_WHITE)
        )

        val dateCell = Cell()
            .setBackgroundColor(DeviceRgb(15, 40, 55))
            .setPadding(12f)
            .setBorder(Border.NO_BORDER)
        dateCell.add(
            Paragraph("Generated: ${displayDateFormat.format(Date())}")
                .setFontSize(11f)
                .setFontColor(SOFT_WHITE)
                .setTextAlignment(TextAlignment.RIGHT)
        )

        infoTable.addCell(nameCell)
        infoTable.addCell(dateCell)
        document.add(infoTable)

        document.add(Paragraph("\n"))
    }

    private fun addSummarySection(
        document: Document,
        incomes: List<Income>,
        expenses: List<Expense>,
        bills: List<Bill>,
        loans: List<Loan>,
        goals: List<FinancialGoal>
    ) {
        val totalIncome = incomes.sumOf { it.amount }
        val totalExpenses = expenses.sumOf { it.amount }
        val totalBills = bills.sumOf { it.amount }
        val totalLoanRemaining = loans.sumOf { it.remainingAmount }
        val totalGoalsProgress = if (goals.isNotEmpty()) goals.sumOf { it.currentAmount } else 0.0
        val totalGoalsTarget = if (goals.isNotEmpty()) goals.sumOf { it.targetAmount } else 0.0
        val netSavings = totalIncome - totalExpenses - totalBills
        val savingsRate = if (totalIncome > 0) (netSavings / totalIncome * 100) else 0.0

        // Section title
        document.add(
            Paragraph("FINANCIAL SUMMARY")
                .setFontSize(14f)
                .setBold()
                .setFontColor(NAVY_PRIMARY)
                .setMarginBottom(10f)
        )

        // Summary cards as a table - First row
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 25f, 25f))).useAllAvailableWidth()

        // Income Card
        summaryTable.addCell(createSummaryCard("Total Income", totalIncome, EMERALD, "↑"))

        // Expenses Card
        summaryTable.addCell(createSummaryCard("Total Expenses", totalExpenses, MUTED_RED, "↓"))

        // Bills Card
        summaryTable.addCell(createSummaryCard("Bills Due", totalBills, BLUE, "📋"))

        // Net Savings Card
        val savingsColor = if (netSavings >= 0) EMERALD else MUTED_RED
        summaryTable.addCell(createSummaryCard("Net Savings", netSavings, savingsColor, if (netSavings >= 0) "✓" else "!"))

        document.add(summaryTable)

        // Second row for Loans and Goals (if applicable)
        if (loans.isNotEmpty() || goals.isNotEmpty()) {
            val secondaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()

            if (loans.isNotEmpty()) {
                secondaryTable.addCell(createSummaryCard("Loan Remaining", totalLoanRemaining, PURPLE, "💳"))
            } else {
                secondaryTable.addCell(Cell().setBorder(Border.NO_BORDER))
            }

            if (goals.isNotEmpty()) {
                val goalsProgress = if (totalGoalsTarget > 0) (totalGoalsProgress / totalGoalsTarget * 100) else 0.0
                secondaryTable.addCell(createSummaryCard("Goals Progress", goalsProgress, GOLD, "🎯", isPercentage = true))
            } else {
                secondaryTable.addCell(Cell().setBorder(Border.NO_BORDER))
            }

            document.add(secondaryTable)
        }

        // Savings rate indicator
        if (totalIncome > 0) {
            val rateColor = when {
                savingsRate >= 20 -> EMERALD
                savingsRate >= 10 -> AMBER
                else -> MUTED_RED
            }
            document.add(
                Paragraph("Savings Rate: ${String.format("%.1f", savingsRate)}%")
                    .setFontSize(11f)
                    .setFontColor(rateColor)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(8f)
            )
        }

        document.add(Paragraph("\n"))
    }

    private fun createSummaryCard(title: String, amount: Double, color: DeviceRgb, icon: String, isPercentage: Boolean = false): Cell {
        val cell = Cell()
            .setBackgroundColor(LIGHT_GRAY)
            .setPadding(12f)
            .setBorder(SolidBorder(color, 2f))
            .setBorderLeft(SolidBorder(color, 4f))

        cell.add(
            Paragraph(title)
                .setFontSize(9f)
                .setFontColor(DeviceRgb(100, 100, 100))
        )

        val displayValue = if (isPercentage) {
            "${String.format("%.1f", amount)}%"
        } else {
            "$${String.format("%,.2f", amount)}"
        }

        cell.add(
            Paragraph(displayValue)
                .setFontSize(14f)
                .setBold()
                .setFontColor(color)
                .setMarginTop(4f)
        )

        return cell
    }

    private fun addIncomeTable(document: Document, incomes: List<Income>) {
        // Section Header with icon styling
        val sectionHeader = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
        val headerCell = Cell()
            .setBackgroundColor(EMERALD)
            .setPadding(10f)
            .setBorder(Border.NO_BORDER)
        headerCell.add(
            Paragraph("💰 INCOME (${incomes.size} records)")
                .setFontSize(12f)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
        )
        sectionHeader.addCell(headerCell)
        document.add(sectionHeader)

        // Income Table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(35f, 20f, 20f, 25f))).useAllAvailableWidth()

        // Table Headers
        addTableHeader(table, listOf("Source / Title", "Amount", "Type", "Date"), EMERALD)

        // Table Data
        incomes.forEachIndexed { index, income ->
            val bgColor = if (index % 2 == 0) ColorConstants.WHITE else TABLE_ROW_ALT

            table.addCell(createDataCell(income.title, bgColor))
            table.addCell(createDataCell("$${String.format("%,.2f", income.amount)}", bgColor, EMERALD, true))
            table.addCell(createDataCell(income.source.displayName, bgColor))
            table.addCell(createDataCell(dateFormat.format(Date(income.date)), bgColor))
        }

        // Total Row
        val totalIncome = incomes.sumOf { it.amount }
        addTotalRow(table, "Total Income", totalIncome, EMERALD, 4)

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addExpenseTable(document: Document, expenses: List<Expense>) {
        // Section Header
        val sectionHeader = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
        val headerCell = Cell()
            .setBackgroundColor(MUTED_RED)
            .setPadding(10f)
            .setBorder(Border.NO_BORDER)
        headerCell.add(
            Paragraph("💳 EXPENSES (${expenses.size} records)")
                .setFontSize(12f)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
        )
        sectionHeader.addCell(headerCell)
        document.add(sectionHeader)

        // Expense Table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 18f, 22f, 30f))).useAllAvailableWidth()

        // Table Headers
        addTableHeader(table, listOf("Description", "Amount", "Category", "Date"), MUTED_RED)

        // Table Data
        expenses.forEachIndexed { index, expense ->
            val bgColor = if (index % 2 == 0) ColorConstants.WHITE else TABLE_ROW_ALT

            table.addCell(createDataCell(expense.title, bgColor))
            table.addCell(createDataCell("$${String.format("%,.2f", expense.amount)}", bgColor, MUTED_RED, true))
            table.addCell(createDataCell("${expense.category.icon} ${expense.category.displayName}", bgColor))
            table.addCell(createDataCell(dateFormat.format(Date(expense.date)), bgColor))
        }

        // Total Row
        val totalExpenses = expenses.sumOf { it.amount }
        addTotalRow(table, "Total Expenses", totalExpenses, MUTED_RED, 4)

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addBillsTable(document: Document, bills: List<Bill>) {
        // Section Header
        val sectionHeader = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
        val headerCell = Cell()
            .setBackgroundColor(BLUE)
            .setPadding(10f)
            .setBorder(Border.NO_BORDER)
        headerCell.add(
            Paragraph("📋 BILLS (${bills.size} records)")
                .setFontSize(12f)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
        )
        sectionHeader.addCell(headerCell)
        document.add(sectionHeader)

        // Bills Table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 20f, 25f, 25f))).useAllAvailableWidth()

        // Table Headers
        addTableHeader(table, listOf("Bill Name", "Amount", "Due Date", "Status"), BLUE)

        // Table Data
        bills.forEachIndexed { index, bill ->
            val bgColor = if (index % 2 == 0) ColorConstants.WHITE else TABLE_ROW_ALT

            table.addCell(createDataCell(bill.title, bgColor))
            table.addCell(createDataCell("$${String.format("%,.2f", bill.amount)}", bgColor, BLUE, true))
            table.addCell(createDataCell(dateFormat.format(Date(bill.dueDate)), bgColor))

            // Status with color
            val statusColor = if (bill.isPaid) EMERALD else MUTED_RED
            val statusText = if (bill.isPaid) "✓ Paid" else "⏳ Unpaid"
            table.addCell(createDataCell(statusText, bgColor, statusColor, false))
        }

        // Total Row
        val totalBills = bills.sumOf { it.amount }
        addTotalRow(table, "Total Bills", totalBills, BLUE, 4)

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addLoansTable(document: Document, loans: List<Loan>) {
        // Section Header
        val sectionHeader = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
        val headerCell = Cell()
            .setBackgroundColor(PURPLE)
            .setPadding(10f)
            .setBorder(Border.NO_BORDER)
        headerCell.add(
            Paragraph("💳 LOANS (${loans.size} records)")
                .setFontSize(12f)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
        )
        sectionHeader.addCell(headerCell)
        document.add(sectionHeader)

        // Loans Table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(25f, 18f, 18f, 18f, 21f))).useAllAvailableWidth()

        // Table Headers
        addTableHeader(table, listOf("Loan Name", "Principal", "Remaining", "Monthly", "Status"), PURPLE)

        // Table Data
        loans.forEachIndexed { index, loan ->
            val bgColor = if (index % 2 == 0) ColorConstants.WHITE else TABLE_ROW_ALT

            table.addCell(createDataCell(loan.title, bgColor))
            table.addCell(createDataCell("$${String.format("%,.2f", loan.principalAmount)}", bgColor, null, false))
            table.addCell(createDataCell("$${String.format("%,.2f", loan.remainingAmount)}", bgColor, PURPLE, true))
            table.addCell(createDataCell("$${String.format("%,.2f", loan.monthlyPayment)}", bgColor, null, false))

            // Status with color
            val statusColor = when (loan.status) {
                com.example.budgie.data.model.LoanStatus.ACTIVE -> BLUE
                com.example.budgie.data.model.LoanStatus.PAID_OFF -> EMERALD
                com.example.budgie.data.model.LoanStatus.DEFAULTED -> MUTED_RED
                else -> AMBER
            }
            table.addCell(createDataCell(loan.status.displayName, bgColor, statusColor, false))
        }

        // Total Row
        val totalRemaining = loans.sumOf { it.remainingAmount }
        addTotalRow(table, "Total Remaining", totalRemaining, PURPLE, 5)

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addGoalsTable(document: Document, goals: List<FinancialGoal>) {
        // Section Header
        val sectionHeader = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
        val headerCell = Cell()
            .setBackgroundColor(GOLD)
            .setPadding(10f)
            .setBorder(Border.NO_BORDER)
        headerCell.add(
            Paragraph("🎯 FINANCIAL GOALS (${goals.size} records)")
                .setFontSize(12f)
                .setBold()
                .setFontColor(NAVY_PRIMARY)
        )
        sectionHeader.addCell(headerCell)
        document.add(sectionHeader)

        // Goals Table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(28f, 18f, 18f, 18f, 18f))).useAllAvailableWidth()

        // Table Headers
        addTableHeader(table, listOf("Goal Name", "Target", "Progress", "Remaining", "Status"), GOLD)

        // Table Data
        goals.forEachIndexed { index, goal ->
            val bgColor = if (index % 2 == 0) ColorConstants.WHITE else TABLE_ROW_ALT

            table.addCell(createDataCell("${goal.category.emoji} ${goal.title}", bgColor))
            table.addCell(createDataCell("$${String.format("%,.2f", goal.targetAmount)}", bgColor, null, false))
            table.addCell(createDataCell("${String.format("%.1f", goal.progressPercentage)}%", bgColor, EMERALD, true))
            table.addCell(createDataCell("$${String.format("%,.2f", goal.remainingAmount)}", bgColor, null, false))

            // Status with color
            val statusColor = when {
                goal.isCompleted -> EMERALD
                goal.isOnTrack -> BLUE
                else -> MUTED_RED
            }
            val statusText = when {
                goal.isCompleted -> "✓ Completed"
                goal.isOnTrack -> "On Track"
                else -> "Behind"
            }
            table.addCell(createDataCell(statusText, bgColor, statusColor, false))
        }

        // Summary Row
        val totalTarget = goals.sumOf { it.targetAmount }
        val totalProgress = goals.sumOf { it.currentAmount }
        val overallProgress = if (totalTarget > 0) (totalProgress / totalTarget * 100) else 0.0

        // Add overall progress row
        table.addCell(
            Cell(1, 3)
                .setBackgroundColor(DeviceRgb(250, 252, 255))
                .setPadding(10f)
                .setBorderTop(SolidBorder(GOLD, 2f))
                .add(
                    Paragraph("Overall Goals Progress")
                        .setFontSize(11f)
                        .setBold()
                        .setFontColor(NAVY_PRIMARY)
                        .setTextAlignment(TextAlignment.RIGHT)
                )
        )
        table.addCell(
            Cell()
                .setBackgroundColor(GOLD)
                .setPadding(10f)
                .add(
                    Paragraph("${String.format("%.1f", overallProgress)}%")
                        .setFontSize(12f)
                        .setBold()
                        .setFontColor(NAVY_PRIMARY)
                        .setTextAlignment(TextAlignment.CENTER)
                )
        )
        table.addCell(
            Cell()
                .setBackgroundColor(DeviceRgb(250, 252, 255))
                .setBorder(Border.NO_BORDER)
        )

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addShoppingListsTable(document: Document, shoppingLists: List<ShoppingListWithItems>) {
        // Section Header
        val sectionHeader = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
        val headerCell = Cell()
            .setBackgroundColor(EMERALD)
            .setPadding(10f)
            .setBorder(Border.NO_BORDER)
        headerCell.add(
            Paragraph("🛒 SHOPPING LISTS (${shoppingLists.size} lists)")
                .setFontSize(12f)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
        )
        sectionHeader.addCell(headerCell)
        document.add(sectionHeader)

        // Shopping Lists Table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 15f, 18f, 18f, 19f))).useAllAvailableWidth()

        // Table Headers
        addTableHeader(table, listOf("List Name", "Items", "Budget", "Actual", "Status"), EMERALD)

        // Table Data
        shoppingLists.forEachIndexed { index, listWithItems ->
            val bgColor = if (index % 2 == 0) ColorConstants.WHITE else TABLE_ROW_ALT
            val list = listWithItems.shoppingList

            table.addCell(createDataCell(list.title, bgColor))
            table.addCell(createDataCell("${listWithItems.itemCount} items", bgColor, null, false))
            table.addCell(createDataCell("$${String.format("%,.2f", list.totalBudget)}", bgColor, BLUE, false))
            table.addCell(createDataCell("$${String.format("%,.2f", listWithItems.totalEstimated)}", bgColor, null, false))

            // Status with color
            val statusColor = if (list.isCompleted) EMERALD else AMBER
            val statusText = if (list.isCompleted) "✓ Completed" else "In Progress"
            table.addCell(createDataCell(statusText, bgColor, statusColor, false))
        }

        // Total Row
        val totalBudget = shoppingLists.sumOf { it.shoppingList.totalBudget }
        val totalEstimated = shoppingLists.sumOf { it.totalEstimated }

        table.addCell(
            Cell(1, 2)
                .setBackgroundColor(DeviceRgb(250, 252, 255))
                .setPadding(10f)
                .setBorderTop(SolidBorder(EMERALD, 2f))
                .add(
                    Paragraph("Total")
                        .setFontSize(11f)
                        .setBold()
                        .setFontColor(NAVY_PRIMARY)
                        .setTextAlignment(TextAlignment.RIGHT)
                )
        )
        table.addCell(
            Cell()
                .setBackgroundColor(EMERALD)
                .setPadding(10f)
                .add(
                    Paragraph("$${String.format("%,.2f", totalBudget)}")
                        .setFontSize(11f)
                        .setBold()
                        .setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                )
        )
        table.addCell(
            Cell()
                .setBackgroundColor(DeviceRgb(250, 252, 255))
                .setPadding(10f)
                .setBorderTop(SolidBorder(EMERALD, 2f))
                .add(
                    Paragraph("$${String.format("%,.2f", totalEstimated)}")
                        .setFontSize(11f)
                        .setFontColor(NAVY_PRIMARY)
                        .setTextAlignment(TextAlignment.CENTER)
                )
        )
        table.addCell(
            Cell()
                .setBackgroundColor(DeviceRgb(250, 252, 255))
                .setBorder(Border.NO_BORDER)
        )

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addTableHeader(table: Table, headers: List<String>, accentColor: DeviceRgb) {
        headers.forEach { header ->
            val cell = Cell()
                .setBackgroundColor(TABLE_HEADER_BG)
                .setPadding(8f)
                .setBorderBottom(SolidBorder(accentColor, 2f))
            cell.add(
                Paragraph(header)
                    .setFontSize(10f)
                    .setBold()
                    .setFontColor(SOFT_WHITE)
            )
            table.addHeaderCell(cell)
        }
    }

    private fun createDataCell(
        text: String,
        bgColor: com.itextpdf.kernel.colors.Color,
        textColor: DeviceRgb? = null,
        isBold: Boolean = false
    ): Cell {
        val cell = Cell()
            .setBackgroundColor(bgColor)
            .setPadding(8f)
            .setBorderBottom(SolidBorder(DeviceRgb(220, 220, 220), 0.5f))

        val paragraph = Paragraph(text).setFontSize(10f)
        if (textColor != null) paragraph.setFontColor(textColor)
        if (isBold) paragraph.setBold()

        cell.add(paragraph)
        return cell
    }

    private fun addTotalRow(table: Table, label: String, amount: Double, color: DeviceRgb, colSpan: Int) {
        // Empty cells for spacing
        for (i in 0 until colSpan - 2) {
            table.addCell(
                Cell()
                    .setBackgroundColor(DeviceRgb(250, 252, 255))
                    .setBorder(Border.NO_BORDER)
            )
        }

        // Label cell
        val labelCell = Cell()
            .setBackgroundColor(DeviceRgb(250, 252, 255))
            .setPadding(10f)
            .setBorderTop(SolidBorder(color, 2f))
        labelCell.add(
            Paragraph(label)
                .setFontSize(11f)
                .setBold()
                .setFontColor(NAVY_PRIMARY)
                .setTextAlignment(TextAlignment.RIGHT)
        )
        table.addCell(labelCell)

        // Amount cell
        val amountCell = Cell()
            .setBackgroundColor(color)
            .setPadding(10f)
        amountCell.add(
            Paragraph("$${String.format("%,.2f", amount)}")
                .setFontSize(12f)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
        )
        table.addCell(amountCell)
    }

    private fun addProfessionalFooter(document: Document) {
        document.add(Paragraph("\n"))

        // Footer bar
        val footerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
        val footerCell = Cell()
            .setBackgroundColor(NAVY_PRIMARY)
            .setPadding(15f)
            .setBorder(Border.NO_BORDER)

        footerCell.add(
            Paragraph("Generated by Budgie - Your AI-Powered Financial Companion")
                .setFontSize(9f)
                .setFontColor(EMERALD)
                .setTextAlignment(TextAlignment.CENTER)
        )
        footerCell.add(
            Paragraph("This report is for personal use only. Data is stored securely on your device.")
                .setFontSize(8f)
                .setFontColor(DeviceRgb(150, 170, 165))
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic()
                .setMarginTop(4f)
        )

        footerTable.addCell(footerCell)
        document.add(footerTable)
    }

    /**
     * Export data to Excel format
     * Order: Summary → Income → Expenses → Bills → Loans → Goals → Shopping Lists
     */
    fun exportToExcel(
        context: Context,
        userName: String,
        expenses: List<Expense>,
        incomes: List<Income>,
        bills: List<Bill>,
        loans: List<Loan> = emptyList(),
        goals: List<FinancialGoal> = emptyList(),
        shoppingLists: List<ShoppingListWithItems> = emptyList()
    ): File? {
        return try {
            val fileName = "Budgie_Financial_Report_${fileNameDateFormat.format(Date())}.xlsx"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            val workbook = XSSFWorkbook()

            // Summary Sheet (First)
            val summarySheet = workbook.createSheet("Summary")
            var rowNum = 0

            // Title
            summarySheet.createRow(rowNum++).createCell(0).setCellValue("BUDGIE FINANCIAL REPORT")
            summarySheet.createRow(rowNum++).createCell(0).setCellValue("Name: $userName")
            summarySheet.createRow(rowNum++).createCell(0).setCellValue("Export Date: ${displayDateFormat.format(Date())}")
            rowNum++

            // Summary data
            val totalIncome = incomes.sumOf { it.amount }
            val totalExpenses = expenses.sumOf { it.amount }
            val totalBills = bills.sumOf { it.amount }
            val totalLoanRemaining = loans.sumOf { it.remainingAmount }
            val totalGoalsProgress = goals.sumOf { it.currentAmount }
            val totalGoalsTarget = goals.sumOf { it.targetAmount }
            val totalShoppingBudget = shoppingLists.sumOf { it.shoppingList.totalBudget }
            val netSavings = totalIncome - totalExpenses - totalBills

            summarySheet.createRow(rowNum++).apply {
                createCell(0).setCellValue("Total Income:")
                createCell(1).setCellValue(totalIncome)
            }
            summarySheet.createRow(rowNum++).apply {
                createCell(0).setCellValue("Total Expenses:")
                createCell(1).setCellValue(totalExpenses)
            }
            summarySheet.createRow(rowNum++).apply {
                createCell(0).setCellValue("Total Bills:")
                createCell(1).setCellValue(totalBills)
            }
            summarySheet.createRow(rowNum++).apply {
                createCell(0).setCellValue("Net Savings:")
                createCell(1).setCellValue(netSavings)
            }
            if (loans.isNotEmpty()) {
                summarySheet.createRow(rowNum++).apply {
                    createCell(0).setCellValue("Loan Remaining:")
                    createCell(1).setCellValue(totalLoanRemaining)
                }
            }
            if (goals.isNotEmpty()) {
                val goalsProgress = if (totalGoalsTarget > 0) (totalGoalsProgress / totalGoalsTarget * 100) else 0.0
                summarySheet.createRow(rowNum++).apply {
                    createCell(0).setCellValue("Goals Progress:")
                    createCell(1).setCellValue("${String.format("%.1f", goalsProgress)}%")
                }
            }

            // Income Sheet (SECOND - right after summary)
            if (incomes.isNotEmpty()) {
                val incomeSheet = workbook.createSheet("Income")
                var incRow = 0

                // Headers
                incomeSheet.createRow(incRow++).apply {
                    createCell(0).setCellValue("Title")
                    createCell(1).setCellValue("Amount")
                    createCell(2).setCellValue("Source")
                    createCell(3).setCellValue("Date")
                    createCell(4).setCellValue("Recurring")
                }

                incomes.forEach { income ->
                    incomeSheet.createRow(incRow++).apply {
                        createCell(0).setCellValue(income.title)
                        createCell(1).setCellValue(income.amount)
                        createCell(2).setCellValue(income.source.displayName)
                        createCell(3).setCellValue(dateFormat.format(Date(income.date)))
                        createCell(4).setCellValue(if (income.isRecurring) income.recurringType.displayName else "One-time")
                    }
                }

                // Total row
                incomeSheet.createRow(incRow++).apply {
                    createCell(0).setCellValue("TOTAL")
                    createCell(1).setCellValue(totalIncome)
                }
            }

            // Expenses Sheet (THIRD)
            if (expenses.isNotEmpty()) {
                val expenseSheet = workbook.createSheet("Expenses")
                var expRow = 0

                // Headers
                expenseSheet.createRow(expRow++).apply {
                    createCell(0).setCellValue("Title")
                    createCell(1).setCellValue("Amount")
                    createCell(2).setCellValue("Category")
                    createCell(3).setCellValue("Date")
                    createCell(4).setCellValue("Notes")
                }

                expenses.forEach { expense ->
                    expenseSheet.createRow(expRow++).apply {
                        createCell(0).setCellValue(expense.title)
                        createCell(1).setCellValue(expense.amount)
                        createCell(2).setCellValue(expense.category.displayName)
                        createCell(3).setCellValue(dateFormat.format(Date(expense.date)))
                        createCell(4).setCellValue(expense.notes)
                    }
                }

                // Total row
                expenseSheet.createRow(expRow++).apply {
                    createCell(0).setCellValue("TOTAL")
                    createCell(1).setCellValue(totalExpenses)
                }
            }

            // Bills Sheet (FOURTH)
            if (bills.isNotEmpty()) {
                val billsSheet = workbook.createSheet("Bills")
                var billRow = 0

                // Headers
                billsSheet.createRow(billRow++).apply {
                    createCell(0).setCellValue("Title")
                    createCell(1).setCellValue("Amount")
                    createCell(2).setCellValue("Due Date")
                    createCell(3).setCellValue("Status")
                    createCell(4).setCellValue("Category")
                }

                bills.forEach { bill ->
                    billsSheet.createRow(billRow++).apply {
                        createCell(0).setCellValue(bill.title)
                        createCell(1).setCellValue(bill.amount)
                        createCell(2).setCellValue(dateFormat.format(Date(bill.dueDate)))
                        createCell(3).setCellValue(if (bill.isPaid) "Paid" else "Unpaid")
                        createCell(4).setCellValue(bill.category.displayName)
                    }
                }

                // Total row
                billsSheet.createRow(billRow++).apply {
                    createCell(0).setCellValue("TOTAL")
                    createCell(1).setCellValue(totalBills)
                }
            }

            // Loans Sheet (FIFTH)
            if (loans.isNotEmpty()) {
                val loansSheet = workbook.createSheet("Loans")
                var loanRow = 0

                // Headers
                loansSheet.createRow(loanRow++).apply {
                    createCell(0).setCellValue("Loan Name")
                    createCell(1).setCellValue("Lender")
                    createCell(2).setCellValue("Principal")
                    createCell(3).setCellValue("Total Amount")
                    createCell(4).setCellValue("Paid")
                    createCell(5).setCellValue("Remaining")
                    createCell(6).setCellValue("Monthly Payment")
                    createCell(7).setCellValue("Interest Rate")
                    createCell(8).setCellValue("Status")
                }

                loans.forEach { loan ->
                    loansSheet.createRow(loanRow++).apply {
                        createCell(0).setCellValue(loan.title)
                        createCell(1).setCellValue(loan.lenderName)
                        createCell(2).setCellValue(loan.principalAmount)
                        createCell(3).setCellValue(loan.totalAmount)
                        createCell(4).setCellValue(loan.amountPaid)
                        createCell(5).setCellValue(loan.remainingAmount)
                        createCell(6).setCellValue(loan.monthlyPayment)
                        createCell(7).setCellValue("${loan.interestRate}%")
                        createCell(8).setCellValue(loan.status.displayName)
                    }
                }

                // Total row
                loansSheet.createRow(loanRow++).apply {
                    createCell(0).setCellValue("TOTAL REMAINING")
                    createCell(5).setCellValue(totalLoanRemaining)
                }
            }

            // Goals Sheet (SIXTH - LAST)
            if (goals.isNotEmpty()) {
                val goalsSheet = workbook.createSheet("Goals")
                var goalRow = 0

                // Headers
                goalsSheet.createRow(goalRow++).apply {
                    createCell(0).setCellValue("Goal Name")
                    createCell(1).setCellValue("Category")
                    createCell(2).setCellValue("Target Amount")
                    createCell(3).setCellValue("Current Amount")
                    createCell(4).setCellValue("Remaining")
                    createCell(5).setCellValue("Progress %")
                    createCell(6).setCellValue("Target Date")
                    createCell(7).setCellValue("Status")
                }

                goals.forEach { goal ->
                    goalsSheet.createRow(goalRow++).apply {
                        createCell(0).setCellValue(goal.title)
                        createCell(1).setCellValue(goal.category.displayName)
                        createCell(2).setCellValue(goal.targetAmount)
                        createCell(3).setCellValue(goal.currentAmount)
                        createCell(4).setCellValue(goal.remainingAmount)
                        createCell(5).setCellValue("${String.format("%.1f", goal.progressPercentage)}%")
                        createCell(6).setCellValue(dateFormat.format(Date(goal.targetDate)))
                        val status = when {
                            goal.isCompleted -> "Completed"
                            goal.isOnTrack -> "On Track"
                            else -> "Behind"
                        }
                        createCell(7).setCellValue(status)
                    }
                }

                // Summary row
                val overallProgress = if (totalGoalsTarget > 0) (totalGoalsProgress / totalGoalsTarget * 100) else 0.0
                goalsSheet.createRow(goalRow++).apply {
                    createCell(0).setCellValue("OVERALL")
                    createCell(2).setCellValue(totalGoalsTarget)
                    createCell(3).setCellValue(totalGoalsProgress)
                    createCell(5).setCellValue("${String.format("%.1f", overallProgress)}%")
                }
            }

            // Shopping Lists Sheet (SEVENTH - LAST)
            if (shoppingLists.isNotEmpty()) {
                val shoppingSheet = workbook.createSheet("Shopping Lists")
                var shoppingRow = 0

                // Headers
                shoppingSheet.createRow(shoppingRow++).apply {
                    createCell(0).setCellValue("List Name")
                    createCell(1).setCellValue("Items Count")
                    createCell(2).setCellValue("Budget")
                    createCell(3).setCellValue("Estimated Total")
                    createCell(4).setCellValue("Created Date")
                    createCell(5).setCellValue("Status")
                }

                shoppingLists.forEach { listWithItems ->
                    val list = listWithItems.shoppingList
                    shoppingSheet.createRow(shoppingRow++).apply {
                        createCell(0).setCellValue(list.title)
                        createCell(1).setCellValue(listWithItems.itemCount.toDouble())
                        createCell(2).setCellValue(list.totalBudget)
                        createCell(3).setCellValue(listWithItems.totalEstimated)
                        createCell(4).setCellValue(dateFormat.format(Date(list.createdAt)))
                        createCell(5).setCellValue(if (list.isCompleted) "Completed" else "In Progress")
                    }
                }

                // Total row
                shoppingSheet.createRow(shoppingRow++).apply {
                    createCell(0).setCellValue("TOTAL")
                    createCell(2).setCellValue(totalShoppingBudget)
                    createCell(3).setCellValue(shoppingLists.sumOf { it.totalEstimated })
                }
            }

            // Write to file
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Share file using system share sheet
     */
    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Export"))
    }

    /**
     * Open file with default app
     */
    fun openFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val mimeType = when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            else -> "*/*"
        }

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(openIntent)
        } catch (e: Exception) {
            // If no app can handle the file, show chooser
            val chooserIntent = Intent.createChooser(openIntent, "Open with")
            context.startActivity(chooserIntent)
        }
    }
}

