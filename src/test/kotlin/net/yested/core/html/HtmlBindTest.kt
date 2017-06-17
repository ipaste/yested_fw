package net.yested.core.html

import QUnit.Assert
import net.yested.core.properties.*
import net.yested.core.utils.Div
import net.yested.core.utils.NoEffect
import net.yested.ext.bootstrap3.Collapse
import org.junit.Test
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTableElement
import spec.*
import kotlin.browser.window
import kotlin.dom.appendText

/**
 * A test for [tbody].
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/29/16
 * Time: 1:51 PM
 */
class HtmlBindTest {
    data class ListAssert(val list: List<Int>, val expectedText: String, val expectedIds: String)

    @Test
    fun tableShouldReflectData(assert: Assert) {
        tableShouldReflectData(assert, animate = false)
    }

    @Test
    fun tableShouldReflectData_animated(assert: Assert) {
        tableShouldReflectData(assert, animate = true)
    }

    private fun tableShouldReflectData(assert: Assert, animate: Boolean) {
        val done = assert.async()
        val data: Property<List<Int>?> = listOf(1).toProperty()
        var table: HTMLTableElement? = null
        var nextId = 1
        Div {
            table = table {
                thead {
                    th {
                        appendText("Items")
                    }
                }
                tbody(data, if (animate) Collapse() else NoEffect) { item ->
                    tr { id = (nextId++).toString()
                        td { appendText(item.toString()) }
                    }
                }
            }
        }
        table!!.textContent.mustBe("Items1")

        val listAssertSequence = listOf(
                ListAssert(listOf(1, 2), "Items12", "1,2"),
                ListAssert(listOf(1, 2, 3), "Items123", "1,2,3"),
                ListAssert(listOf(2, 3, 1), "Items231", "2,3,4"),
                ListAssert(listOf(1, 2, 3), "Items123", "5,2,3"),
                ListAssert(listOf(1, 3, 2), "Items132", "5,6,2"),
                ListAssert(listOf(1, 2, 3, 4, 5, 6, 7, 8), "Items12345678", "5,7,6,8,9,10,11,12"),
                ListAssert(listOf(1, 2, 3, 8, 7, 4, 5, 6), "Items12387456", "5,7,6,13,14,8,9,10"),
                ListAssert(listOf(1, 2, 3, 4, 5, 6, 7, 8), "Items12345678", "5,7,6,8,9,10,16,15"),
                ListAssert(listOf(1, 2, 3, 6, 7, 8, 4, 5), "Items12367845", "5,7,6,10,16,15,18,17"),
                ListAssert(listOf(1, 2, 3, 6, 8, 4, 5), "Items1236845", "5,7,6,10,15,18,17"),
                ListAssert(listOf(1, 2, 3, 4, 5), "Items12345", "5,7,6,18,17"),
                ListAssert(listOf(5, 4, 3, 2, 1), "Items54321", "19,20,21,22,5"),
                ListAssert(listOf(10, 11, 12, 13), "Items10111213", "23,24,25,26"),
                ListAssert(listOf(12, 13, 14), "Items121314", "25,26,27"))

        val listAssertIterator = listAssertSequence.iterator()
        validateListAsserts(listAssertIterator, table, data, done, if (animate) 500 else 0)
    }

    private fun validateListAsserts(listAssertIterator: Iterator<ListAssert>, table: HTMLTableElement?, data: Property<List<Int>?>, done: () -> Unit, stepDelay: Int) {
        if (listAssertIterator.hasNext()) {
            val listAssert = listAssertIterator.next()
            // This callback will be called once tbody animation rendering is done
            if (data.get() != listAssert.list) {
                data.set(listAssert.list)
                window.setTimeout({
                    table!!.textContent.mustBe(listAssert.expectedText)
                    getRowIdsAsString(table).mustBe(listAssert.expectedIds)
                    table.styleContent.mustBe("")
                    validateListAsserts(listAssertIterator, table, data, done, stepDelay)
                }, stepDelay)
            } else {
                console.info("skipping since equal")
                validateListAsserts(listAssertIterator, table, data, done, stepDelay)
            }
        } else {
            console.info("all validateListAsserts are done")
            done()
        }
    }

    private val HTMLElement.styleContent: String
        get() = (getAttribute("style") ?: "") + children.toList().map { it.styleContent }.joinToString("")

    private fun getRowIdsAsString(table: HTMLTableElement?): String {
        return (table!!.lastChild!! as HTMLElement).children.toList().map { it.getAttribute("id")}.joinToString(",")
    }
}
