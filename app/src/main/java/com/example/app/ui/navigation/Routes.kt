package com.example.odootask.ui.navigation

object Routes {
    const val ITEMS = "items"
    const val ITEM_DETAIL = "item/{id}"

    fun itemDetail(id: String) = "item/$id"
}
