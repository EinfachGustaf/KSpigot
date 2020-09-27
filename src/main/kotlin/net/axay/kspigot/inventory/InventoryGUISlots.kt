@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package net.axay.kspigot.inventory

// INVENTORY

data class InventoryDimensions(val width: Int, val heigth: Int) {

    val invSlots by lazy {
        ArrayList<InventorySlot>().apply {
            (1 .. heigth).forEach { row ->
            (1 .. width).forEach { slotInRow ->
                    this += InventorySlot(row, slotInRow)
            } }
        }
    }

    val realSlots by lazy {
        ArrayList<Int>().apply {
            invSlots.forEach { curSlot ->
                curSlot.realSlotIn(this@InventoryDimensions)?.let { this += it }
            }
        }
    }

    val invSlotsWithRealSlots by lazy {
        HashMap<InventorySlot, Int>().apply {
            invSlots.forEach { curSlot ->
                curSlot.realSlotIn(this@InventoryDimensions)?.let { this[curSlot] = it }
            }
        }
    }

}

// SLOTS

data class InventorySlot(val row: Int, val slotInRow: Int) : Comparable<InventorySlot> {

    companion object {
        fun fromRealSlot(realSlot: Int, dimensions: InventoryDimensions)
                = dimensions.invSlotsWithRealSlots.toList().find { it.second == realSlot }?.first
    }

    override fun compareTo(other: InventorySlot) = when {
        row > other.row -> 1
        row < other.row -> -1
        else -> when {
            slotInRow > other.slotInRow -> 1
            slotInRow < other.slotInRow -> -1
            else -> 0
        }
    }

    fun realSlotIn(inventoryDimensions: InventoryDimensions): Int? {
        if (!isInDimension(inventoryDimensions)) return null
        val realRow = inventoryDimensions.heigth - (row - 1)
        val rowsUnder = if (realRow - 1 >= 0) realRow - 1 else 0
        return ((rowsUnder * inventoryDimensions.width) + slotInRow) - 1
    }

    fun isInDimension(inventoryDimensions: InventoryDimensions)
            = (1 .. inventoryDimensions.width).contains(slotInRow) && (1 .. inventoryDimensions.heigth).contains(row)

    fun add(offsetHorizontally: Int, offsetVertically: Int) = InventorySlot(
            row + offsetVertically,
            slotInRow + offsetHorizontally
    )

}

interface InventorySlotCompound<out T : ForInventory> {
    fun withGUIType(invType: InventoryGUIType<in T>): Collection<InventorySlot>
}

open class SingleInventorySlot<out T : ForInventory>(val inventorySlot: InventorySlot)
    : InventorySlotCompound<T> {

    constructor(row: Int, slotInRow: Int) : this(InventorySlot(row, slotInRow))

    private val slotAsList = listOf(inventorySlot)

    override fun withGUIType(invType: InventoryGUIType<in T>) = slotAsList

}

enum class InventorySlotRangeType { LINEAR, RECTANGLE }

class InventorySlotRange<out T : ForInventory> (

        startSlot: SingleInventorySlot<T>,
        endSlot: SingleInventorySlot<T>,

        private val type: InventorySlotRangeType

) : InventorySlotCompound<T>, ClosedRange<InventorySlot> {

    override val start: InventorySlot
    override val endInclusive: InventorySlot

    init {
        if (startSlot.inventorySlot <= endSlot.inventorySlot) {
            start = startSlot.inventorySlot
            endInclusive = endSlot.inventorySlot
        } else {
            start = endSlot.inventorySlot
            endInclusive = startSlot.inventorySlot
        }
    }

    override fun withGUIType(invType: InventoryGUIType<in T>)
        = LinkedHashSet<InventorySlot>().apply {
            when (type) {

                InventorySlotRangeType.RECTANGLE -> {
                    // all possible combinations between the two slots
                    // -> form a rectangle
                    for (row in start.row .. endInclusive.row)
                        for (slotInRow in start.slotInRow .. endInclusive.slotInRow)
                            this += InventorySlot(row, slotInRow)
                }

                InventorySlotRangeType.LINEAR -> {
                    if (endInclusive.row > start.row) {
                        // from start --->| to end of row
                        for (slotInRow in start.slotInRow .. invType.dimensions.width)
                            this += InventorySlot(start.row, slotInRow)
                        // all rows in between
                        if (endInclusive.row > start.row + 1)
                            for (row in start.row + 1 until endInclusive.row)
                                for (slotInRow in 1 .. invType.dimensions.width)
                                    this += InventorySlot(row, slotInRow)
                        // from start of row |----> to endInclusive
                        for (slotInRow in 1 .. endInclusive.slotInRow)
                            this += InventorySlot(endInclusive.row, slotInRow)
                    } else if (endInclusive.row == start.row) {
                        // from start ---> to endInclusive in the same row
                        for (slotInRow in start.slotInRow .. endInclusive.slotInRow)
                            this += InventorySlot(start.row, slotInRow)
                    }
                }

            }
        }

}

// SLOT RANGE OPERATOR FUNCTIONS

infix fun <T : ForInventory> SingleInventorySlot<T>.linTo(slot: SingleInventorySlot<T>)
        = InventorySlotRange(this, slot, InventorySlotRangeType.LINEAR)

infix fun <T : ForInventory> SingleInventorySlot<T>.rectTo(slot: SingleInventorySlot<T>)
        = InventorySlotRange(this, slot, InventorySlotRangeType.RECTANGLE)

// SLOT TYPE SAFETY

// ROWS
interface ForRowOne : ForInventoryOneByNine, ForInventoryTwoByNine, ForInventoryThreeByNine, ForInventoryFourByNine, ForInventoryFiveByNine, ForInventorySixByNine
interface ForRowTwo : ForInventoryTwoByNine, ForInventoryThreeByNine, ForInventoryFourByNine, ForInventoryFiveByNine, ForInventorySixByNine
interface ForRowThree : ForInventoryThreeByNine, ForInventoryFourByNine, ForInventoryFiveByNine, ForInventorySixByNine
interface ForRowFour : ForInventoryFourByNine, ForInventoryFiveByNine, ForInventorySixByNine
interface ForRowFive : ForInventoryFiveByNine, ForInventorySixByNine
interface ForRowSix : ForInventorySixByNine

// EDGE CASES:
// ROW ONE
interface ForRowOneSlotOneToThree : ForRowOne, ForInventoryOneByFive, ForInventoryThreeByThree
interface ForRowOneSlotFourToFive : ForRowOne, ForInventoryOneByFive
// ROW TWO
interface ForRowTwoSlotOneToThree : ForRowTwo, ForInventoryThreeByThree
// ROW THREE
interface ForRowThreeSlotOneToThree : ForRowThree, ForInventoryThreeByThree

object Slots {
    // ROW ONE
    val RowOneSlotOne = SingleInventorySlot<ForRowOneSlotOneToThree>(1, 1)
    val RowOneSlotTwo = SingleInventorySlot<ForRowOneSlotOneToThree>(1, 2)
    val RowOneSlotThree = SingleInventorySlot<ForRowOneSlotOneToThree>(1, 3)
    val RowOneSlotFour = SingleInventorySlot<ForRowOneSlotFourToFive>(1, 4)
    val RowOneSlotFive = SingleInventorySlot<ForRowOneSlotFourToFive>(1, 5)
    val RowOneSlotSix = SingleInventorySlot<ForRowOne>(1, 6)
    val RowOneSlotSeven = SingleInventorySlot<ForRowOne>(1, 7)
    val RowOneSlotEight = SingleInventorySlot<ForRowOne>(1, 8)
    val RowOneSlotNine = SingleInventorySlot<ForRowOne>(1, 9)
    // ROW TWO
    val RowTwoSlotOne = SingleInventorySlot<ForRowTwoSlotOneToThree>(2, 1)
    val RowTwoSlotTwo = SingleInventorySlot<ForRowTwoSlotOneToThree>(2, 2)
    val RowTwoSlotThree = SingleInventorySlot<ForRowTwoSlotOneToThree>(2, 3)
    val RowTwoSlotFour = SingleInventorySlot<ForRowTwo>(2, 4)
    val RowTwoSlotFive = SingleInventorySlot<ForRowTwo>(2, 5)
    val RowTwoSlotSix = SingleInventorySlot<ForRowTwo>(2, 6)
    val RowTwoSlotSeven = SingleInventorySlot<ForRowTwo>(2, 7)
    val RowTwoSlotEight = SingleInventorySlot<ForRowTwo>(2, 8)
    val RowTwoSlotNine = SingleInventorySlot<ForRowTwo>(2, 9)
    // ROW THREE
    val RowThreeSlotOne = SingleInventorySlot<ForRowThreeSlotOneToThree>(3, 1)
    val RowThreeSlotTwo = SingleInventorySlot<ForRowThreeSlotOneToThree>(3, 2)
    val RowThreeSlotThree = SingleInventorySlot<ForRowThreeSlotOneToThree>(3, 3)
    val RowThreeSlotFour = SingleInventorySlot<ForRowThree>(3, 4)
    val RowThreeSlotFive = SingleInventorySlot<ForRowThree>(3, 5)
    val RowThreeSlotSix = SingleInventorySlot<ForRowThree>(3, 6)
    val RowThreeSlotSeven = SingleInventorySlot<ForRowThree>(3, 7)
    val RowThreeSlotEight = SingleInventorySlot<ForRowThree>(3, 8)
    val RowThreeSlotNine = SingleInventorySlot<ForRowThree>(3, 9)
    // ROW FOUR
    val RowFourSlotOne = SingleInventorySlot<ForRowFour>(4, 1)
    val RowFourSlotTwo = SingleInventorySlot<ForRowFour>(4, 2)
    val RowFourSlotThree = SingleInventorySlot<ForRowFour>(4, 3)
    val RowFourSlotFour = SingleInventorySlot<ForRowFour>(4, 4)
    val RowFourSlotFive = SingleInventorySlot<ForRowFour>(4, 5)
    val RowFourSlotSix = SingleInventorySlot<ForRowFour>(4, 6)
    val RowFourSlotSeven = SingleInventorySlot<ForRowFour>(4, 7)
    val RowFourSlotEight = SingleInventorySlot<ForRowFour>(4, 8)
    val RowFourSlotNine = SingleInventorySlot<ForRowFour>(4, 9)
    // ROW FIVE
    val RowFiveSlotOne = SingleInventorySlot<ForRowFive>(5, 1)
    val RowFiveSlotTwo = SingleInventorySlot<ForRowFive>(5, 2)
    val RowFiveSlotThree = SingleInventorySlot<ForRowFive>(5, 3)
    val RowFiveSlotFour = SingleInventorySlot<ForRowFive>(5, 4)
    val RowFiveSlotFive = SingleInventorySlot<ForRowFive>(5, 5)
    val RowFiveSlotSix = SingleInventorySlot<ForRowFive>(5, 6)
    val RowFiveSlotSeven = SingleInventorySlot<ForRowFive>(5, 7)
    val RowFiveSlotEight = SingleInventorySlot<ForRowFive>(5, 8)
    val RowFiveSlotNine = SingleInventorySlot<ForRowFive>(5, 9)
    // ROW SIX
    val RowSixSlotOne = SingleInventorySlot<ForRowSix>(6, 1)
    val RowSixSlotTwo = SingleInventorySlot<ForRowSix>(6, 2)
    val RowSixSlotThree = SingleInventorySlot<ForRowSix>(6, 3)
    val RowSixSlotFour = SingleInventorySlot<ForRowSix>(6, 4)
    val RowSixSlotFive = SingleInventorySlot<ForRowSix>(6, 5)
    val RowSixSlotSix = SingleInventorySlot<ForRowSix>(6, 6)
    val RowSixSlotSeven = SingleInventorySlot<ForRowSix>(6, 7)
    val RowSixSlotEight = SingleInventorySlot<ForRowSix>(6, 8)
    val RowSixSlotNine = SingleInventorySlot<ForRowSix>(6, 9)
}