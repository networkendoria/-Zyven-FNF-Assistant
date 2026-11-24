package com.zyven.fnfassistant

enum class FnfProfile(val id: String, val title: String, val engine: String) {
    OFFICIAL_ANDROID("official_android", "Resmî FNF (Google Play / Android)", "V-Slice"),
    PSYCH_073("psych_073", "Psych Engine 0.7.3", "Psych Engine"),
    VS_SLICE_MOD("vslice_mod", "V-Slice tabanlı mod", "V-Slice");

    companion object {
        fun fromId(id: String?): FnfProfile =
            entries.firstOrNull { it.id == id } ?: OFFICIAL_ANDROID
    }
}
