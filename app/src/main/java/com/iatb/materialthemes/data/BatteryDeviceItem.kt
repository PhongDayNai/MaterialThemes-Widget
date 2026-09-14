package com.iatb.materialthemes.data

enum class BatteryDeviceType {
    PHONE,
    HEADPHONES,
    WATCH,
    SPEAKER,
    GENERIC_BLUETOOTH
}

data class BatteryDeviceItem(
    val id: String,
    val name: String,
    val type: BatteryDeviceType,
    val levelPercent: Int, // 0..100 or -1 if unknown
    val isCharging: Boolean = false,
    val statusText: String? = null
) {
    val iconResId: Int
        get() = when (type) {
            BatteryDeviceType.PHONE -> com.iatb.materialthemes.R.drawable.ic_phone_android
            BatteryDeviceType.HEADPHONES -> com.iatb.materialthemes.R.drawable.ic_headphones
            BatteryDeviceType.WATCH -> com.iatb.materialthemes.R.drawable.ic_watch
            BatteryDeviceType.SPEAKER -> com.iatb.materialthemes.R.drawable.ic_speaker
            BatteryDeviceType.GENERIC_BLUETOOTH -> com.iatb.materialthemes.R.drawable.ic_bluetooth
        }
}
