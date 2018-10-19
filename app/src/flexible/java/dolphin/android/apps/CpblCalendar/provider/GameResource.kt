package dolphin.android.apps.CpblCalendar.provider

import java.util.*

data class GameResource(var year: Int = 2018, var monthOfDay: Int = Calendar.JUNE,
                        var dataList: List<Game>? = null) {

    constructor(year: Int, monthOfDay: Int, progress: Int, messages: String? = null,
                dataList: List<Game>? = null) : this(year, monthOfDay, dataList) {
        this.progress = progress
        this.messages = messages
    }

    var progress: Int = 0
    var messages: String? = null
}