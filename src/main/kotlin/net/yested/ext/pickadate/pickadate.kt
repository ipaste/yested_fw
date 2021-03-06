package net.yested.ext.pickadate

import net.yested.ext.jquery.YestedJQuery

/**
 * uses library: http://amsul.ca/pickadate.js/
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 1/21/17
 * Time: 6:44 AM
 */
class DateContext {
    var select: Long? = null
}

class PickADateOptions(var format: String, var selectMonths: Boolean = false, var selectYears: Boolean = false,
                       var onSet: (DateContext) -> Unit)

fun YestedJQuery.pickadate(options: PickADateOptions) {
    val param: dynamic = object {}
    param.format = options.format
    param.selectMonths = options.selectMonths
    param.selectYears = options.selectYears
    param.onSet = options.onSet
    pickadate(param as Any)
}

