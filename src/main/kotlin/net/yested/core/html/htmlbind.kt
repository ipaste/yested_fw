package net.yested.core.html

import jquery.jq
import net.yested.core.properties.Property
import net.yested.core.properties.ReadOnlyProperty
import net.yested.core.properties.bind
import net.yested.core.utils.*
import net.yested.ext.jquery.slideDownTableRow
import net.yested.ext.jquery.slideUpTableRow
import org.w3c.dom.*
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass

/**
 * Property binding to HTML elements.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/17
 * Time: 11:00 PM
 */
fun HTMLInputElement.bind(property: Property<String>) {
    var updating = false
    property.onNext {
        if (!updating) {
            value = it
        }
    }
    addEventListener("change", { updating = true; property.set(value); updating = false }, false)
    addEventListener("keyup", { updating = true; property.set(value); updating = false }, false)
}

fun <T> HTMLSelectElement.bindMultiselect(selected: Property<List<T>>, options: Property<List<T>>, render: HTMLElement.(T)->Unit) {
    val selectElement = this
    options.onNext {
        removeAllChildElements()
        it.forEachIndexed { index, item ->
            val option: HTMLOptionElement = document.createElement("option").asDynamic()
            option.value = "$index"
            option.render(item)
            appendChild(option)
        }
    }
    var updating = false
    selected.onNext { selectedList ->
        if (!updating) {
            options.get().forEachIndexed { index, option ->
                (selectElement.options.get(index) as HTMLOptionElement).selected = selectedList.contains(option)
            }
        }
    }
    addEventListener("change", {
        val selectOptions = this.options
        val selectedValues = (1..selectOptions.length)
                .map { selectOptions[it - 1] }
                .filter { (it as HTMLOptionElement).selected }
                .map { (it as HTMLOptionElement).value }
                .map { options.get()[parseInt(it)] }
        updating = true
        selected.set(selectedValues)
        updating = false
    }, false)
}

fun <T> HTMLSelectElement.bind(selected: Property<T>, options: Property<List<T>>, render: HTMLElement.(T)->Unit) {
    val multipleSelected = selected.bind({ if (it == null) emptyList() else listOf(it) }, { it.firstOrNull() as T })
    bindMultiselect(multipleSelected, options, render)
}


fun HTMLElement.setClassPresence(className: String, present: ReadOnlyProperty<Boolean>) {
    present.onNext {
        if (it) addClass(className) else removeClass(className)
    }
}

fun HTMLButtonElement.setDisabled(property: ReadOnlyProperty<Boolean>) {
    property.onNext { disabled = it }
}

fun HTMLInputElement.setDisabled(property: ReadOnlyProperty<Boolean>) {
    property.onNext { disabled = it }
}

fun HTMLSelectElement.setDisabled(property: ReadOnlyProperty<Boolean>) {
    property.onNext { disabled = it }
}

fun HTMLFieldSetElement.setDisabled(property: ReadOnlyProperty<Boolean>) {
    property.onNext { disabled = it }
}

fun HTMLInputElement.setReadOnly(property: ReadOnlyProperty<Boolean>) {
    property.onNext { readOnly = it }
}

/**
 * Bind table content to a Property<Iterable<T>>.  The index and value are provided to tbodyItemInit.
 * Example:<pre>
 *   table {
 *       thead {
 *           th { appendText("Name") }
 *           th { appendText("Value") }
 *       }
 *       tbody(myData) { index, item ->
 *           tr { className = if (index % 2 == 0) "even" else "odd"
 *               td { appendText(item.name) }
 *               td { appendText(item.value) }
 *           }
 *       }
 *   }
 * </pre>
 */
fun <T> HTMLTableElement.tbody(orderedData: ReadOnlyProperty<Iterable<T>?>, animate: Boolean = true, tbodyItemInit: TableItemContext.(Int, T)->Unit) {
    if (animate) {
        var tbodyOperableList : TBodyOperableList<T>? = null

        orderedData.onNext { values ->
            val operableListSnapshot = tbodyOperableList
            if (values == null) {
                removeChildByName("tbody")
                tbodyOperableList = null
            } else if (operableListSnapshot == null) {
                val tbody = setTBodyContentsImmediately(values, tbodyItemInit)
                tbodyOperableList = TBodyOperableList(values.toMutableList(), tbody, tbodyItemInit)
            } else {
                operableListSnapshot.reconcileTo(values.toList())
            }
        }
    } else {
        orderedData.onNext { values ->
            setTBodyContentsImmediately(values, tbodyItemInit)
        }
    }
}

private fun <T> HTMLTableElement.setTBodyContentsImmediately(values: Iterable<T>?, tbodyItemInit: TableItemContext.(Int, T) -> Unit): HTMLTableSectionElement {
    removeChildByName("tbody")
    return tbody {
        val tbody = this
        values?.forEachIndexed { index, item ->
            TableItemContext({ rowInit -> tbody.tr(rowInit) }).tbodyItemInit(index, item)
        }
    }
}

/**
 * Bind table content to a Property<Iterable<T>>.  The value is provided to tbodyItemInit.
 * Example:<pre>
 *   table {
 *       thead {
 *           th { appendText("Name") }
 *           th { appendText("Value") }
 *       }
 *       tbody(myData) { item ->
 *           tr { className = if (index % 2 == 0) "even" else "odd"
 *               td { appendText(item.name) }
 *               td { appendText(item.value) }
 *           }
 *       }
 *   }
 * </pre>
 */
fun <T> HTMLTableElement.tbody(orderedData: ReadOnlyProperty<Iterable<T>?>, animate: Boolean = true, tbodyItemInit: TableItemContext.(T)->Unit) {
    return tbody(orderedData, animate) { index, item -> tbodyItemInit(item) }
}

class TableItemContext(private val rowFactory: ((HTMLTableRowElement.()->Unit)?)->HTMLTableRowElement) {
    fun tr(init:(HTMLTableRowElement.()->Unit)? = null): HTMLTableRowElement {
        return rowFactory.invoke(init)
    }
}

fun HTMLCollection.toList(): List<HTMLElement> {
    return (0..(this.length - 1)).map { item(it)!! as HTMLElement }
}

class TBodyOperableList<T>(initialData: MutableList<T>, val tbodyElement: HTMLTableSectionElement,
                           val tbodyItemInit: TableItemContext.(Int, T)->Unit) : InMemoryOperableList<T>(initialData) {
    private val rowsWithoutDelays = tbodyElement.rows.toList().toMutableList()

    override fun add(index: Int, item: T) {
        TableItemContext({ rowInit ->
            val insertIndex = if (index >= tbodyElement.rows.length) -1 else index
            val newRow = tbodyElement.insertRow(insertIndex) as HTMLTableRowElement
            rowsWithoutDelays.add(index, newRow)
            if (rowInit != null) newRow.rowInit()
            val jqNewRow = jq(newRow)
            // start it out hidden in a way that slideDown will show it.
            jqNewRow.slideUpTableRow(duration = 1) {
                // now animate showing it
                jqNewRow.slideDownTableRow()
            }
            newRow
        }).tbodyItemInit(index, item)
        super.add(index, item)
    }

    override fun removeAt(index: Int): T {
        val row = rowsWithoutDelays.removeAt(index)
        val jqRow = jq(row as HTMLTableRowElement)
        jqRow.slideUpTableRow {
            tbodyElement.removeChild(row)
        }
        return super.removeAt(index)
    }

    override fun move(fromIndex: Int, toIndex: Int) {
        val item = removeAt(fromIndex)
        add(toIndex, item)
//        super.move(fromIndex, toIndex)
//        (tbodyElement.parentElement as HTMLTableElement?)?.setTBodyContentsImmediately(toList(), tbodyItemInit)
    }
}
