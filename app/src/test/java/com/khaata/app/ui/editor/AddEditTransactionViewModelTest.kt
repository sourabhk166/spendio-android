package com.khaata.app.ui.editor

import com.khaata.app.ai.NoOpAiEngine
import com.khaata.app.domain.model.CategorySlice
import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.Transaction
import com.khaata.app.domain.repository.MonthlyTotals
import com.khaata.app.domain.repository.RuleRepository
import com.khaata.app.domain.repository.TransactionRepository
import com.khaata.app.parser.Categorizer
import com.khaata.app.parser.MerchantNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class AddEditTransactionViewModelTest {

    private fun newViewModel(): AddEditTransactionViewModel {
        val categorizer = Categorizer(NoRuleRepository(), NoOpAiEngine, MerchantNormalizer())
        return AddEditTransactionViewModel(EmptyRepository(), categorizer).also { it.load(null) }
    }

    @Test fun `switching to income defaults the category to Income`() {
        val vm = newViewModel()
        vm.onDirectionChange(Direction.CREDIT)
        assertEquals(Direction.CREDIT, vm.form.value.direction)
        assertEquals(Categories.INCOME, vm.form.value.categoryId)
    }

    @Test fun `switching back to expense clears the Income category`() {
        val vm = newViewModel()
        vm.onDirectionChange(Direction.CREDIT)
        vm.onDirectionChange(Direction.DEBIT)
        assertEquals(Categories.UNCATEGORIZED, vm.form.value.categoryId)
    }

    @Test fun `a chosen expense category survives staying on expense`() {
        val vm = newViewModel()
        vm.onCategoryChange(Categories.FOOD)
        vm.onDirectionChange(Direction.DEBIT)
        assertEquals(Categories.FOOD, vm.form.value.categoryId)
    }

    @Test fun `switching to expense clears an income sub-category`() {
        val vm = newViewModel()
        vm.onDirectionChange(Direction.CREDIT)
        vm.onCategoryChange(Categories.INCOME_SALARY)
        vm.onDirectionChange(Direction.DEBIT)
        assertEquals(Categories.UNCATEGORIZED, vm.form.value.categoryId)
    }

    @Test fun `income sub-category is preserved when direction stays credit`() {
        val vm = newViewModel()
        vm.onDirectionChange(Direction.CREDIT)
        vm.onCategoryChange(Categories.INCOME_FREELANCE)
        vm.onDirectionChange(Direction.CREDIT)
        assertEquals(Categories.INCOME_FREELANCE, vm.form.value.categoryId)
    }
}

private class NoRuleRepository : RuleRepository {
    override suspend fun categoryFor(merchantKey: String): String? = null
    override suspend fun put(merchantKey: String, categoryId: String, userConfirmed: Boolean) = Unit
}

private class EmptyRepository : TransactionRepository {
    override suspend fun insertIfNew(txn: Transaction): Boolean = true
    override fun observeRecent(limit: Int): Flow<List<Transaction>> = emptyFlow()
    override fun observeMonth(month: YearMonth): Flow<List<Transaction>> = emptyFlow()
    override fun observeMonthlyTotals(month: YearMonth): Flow<MonthlyTotals> =
        flowOf(MonthlyTotals(Money.ZERO, Money.ZERO))
    override fun observeCategoryBreakdown(month: YearMonth): Flow<List<CategorySlice>> = emptyFlow()
    override fun observeIncomeBreakdown(month: YearMonth): Flow<List<CategorySlice>> = emptyFlow()
    override fun search(query: String, limit: Int): Flow<List<Transaction>> = emptyFlow()
    override fun observeNeedsReview(): Flow<List<Transaction>> = emptyFlow()
    override fun observeNeedsReviewCount(): Flow<Int> = flowOf(0)
    override suspend fun approveReview(id: String) = Unit
    override suspend fun recategorize(id: String, categoryId: String) = Unit
    override suspend fun getById(id: String): Transaction? = null
    override suspend fun allTransactions(): List<Transaction> = emptyList()
    override suspend fun update(txn: Transaction) = Unit
    override suspend fun delete(txn: Transaction) = Unit
    override suspend fun count(): Int = 0
    override suspend fun wipeAll() = Unit
}
