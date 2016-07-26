package net.yested.ext.bootstrap3

import net.yested.core.html.*
import net.yested.core.properties.Property
import net.yested.core.properties.ReadOnlyProperty
import net.yested.core.utils.with
import org.w3c.dom.*
import org.w3c.dom.events.Event
import kotlin.dom.addClass
import kotlin.dom.plus
import kotlin.dom.removeClass

class NavbarMenuDropDown(val ul: HTMLUListElement) {

    fun item(active: Property<Boolean>? = null, init: HTMLLIElement.()->Unit) {
        ul with {
            li {
                active?.onNext { if (it) addClass("active") else removeClass("active") }
                this.init()
            }
        }
    }

    fun separator() {
        ul with {
            li { className = "divider" }
        }
    }

    fun dropDownHeader(init: HTMLLIElement.()->Unit) {
        ul with {
            li { className = "dropdown-header"
                this.init()
            }
        }
    }

}

class NavbarMenu(val ul: HTMLUListElement) {

    fun item(active: Property<Boolean>? = null, init: HTMLLIElement.()->Unit) {
        ul with {
            li {
                active?.onNext { if (it) addClass("active") else removeClass("active") }
                this.init()
            }
        }
    }

    fun dropDown(label: String, init: NavbarMenuDropDown.()->Unit) {

        var el:HTMLUListElement? = null
        ul with {
            li {
                a {
                    href = "#"; className = "dropdown-toggle"
                    setAttribute("data-toggle", "dropdown")
                    plus(label)
                    span { className = "caret" }
                }
                ul {
                    className = "dropdown-menu"
                    el = this
                }
            }
        }
        NavbarMenuDropDown(el!!).init()
    }

}

enum class NavbarPosition(val code: String) {
    Left("navbar-left"),
    Right("navbar-right")
}

class NavbarContext(
        val brand:HTMLElement,
        val contentElement: HTMLDivElement) {

    fun brand(init:HTMLElement.()->Unit) {
        brand.init()
    }

    fun menu(
            position: NavbarPosition = NavbarPosition.Left,
            init:NavbarMenu.()->Unit) {
        contentElement.ul { className = "nav navbar-nav ${position.code}"
            NavbarMenu(ul = this).init()
        }
    }

    fun form(
            position: NavbarPosition = NavbarPosition.Left,
            init: HTMLFormElement.()->Unit) {
        contentElement.form { className = "navbar-form ${position.code}"
            init()
        }
    }

    fun button(
            position: NavbarPosition = NavbarPosition.Left,
            look: ButtonLook = ButtonLook.Default,
            onclick: ((Event)->Unit)? = null,
            active: Property<Boolean>? = null,
            disabled: ReadOnlyProperty<Boolean>? = null,
            init: HTMLButtonElement.()->Unit) {
        contentElement.btsButton { className = "btn navbar-btn ${position.code} btn-${look.code}"; type = "submit"
            addEventListener("click", {  event ->
                onclick?.let { onclick(event) }
                active?.set(!active.get())
            }, false)
            init()
            active?.onNext {
                if (it) {
                    addClass("active")
                } else {
                    removeClass("active")
                }
            }
            disabled?.onNext {
                this.disabled = it
            }
        }
    }

    fun text(
            position: NavbarPosition = NavbarPosition.Left,
            init: HTMLParagraphElement.()->Unit) {
        contentElement.p { className = "navbar-text ${position.code}"
            init()
        }
    }

}

enum class NavbarCompletePosition(val code: String) {
    Top(code = ""),
    FixedTop(code = "navbar-fixed-top"),
    FixedBottom(code = "navbar-fixed-bottom"),
    StaticTop(code = "navbar-static-top")
}

fun HTMLElement.navbar(
        position: NavbarCompletePosition = NavbarCompletePosition.Top,
        inverted: Boolean = false,
        init: NavbarContext.()->Unit) {

    var brand:HTMLElement? = null
    var contentElement:HTMLDivElement? = null

    nav {  className = "navbar ${if (inverted) "navbar-inverse" else ""} ${position.code}"
        div { className = "container"
            div { className = "navbar-header"
                /*
                <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                  </button>
                 */
                a { className = "navbar-brand"; href = "#"
                    brand = this
                }
            }
            div { id = "navbar"; className = "navbar-collapse collapse"
                contentElement = this
            }
        }
    }

    NavbarContext(brand = brand!!,  contentElement = contentElement!!).init()

}