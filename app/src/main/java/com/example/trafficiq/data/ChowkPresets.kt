package com.example.trafficiq.data

data class ChowkInfo(
    val name: String,
    val road: String,
    val zone: String,
    val typicalPeakPcu: Int
)

object ChowkPresets {
    val NAGPUR_CHOWKS = listOf(
        ChowkInfo("Variety Square", "Mahatma Gandhi Road / Amravati Rd", "Dharampeth Zone", 4200),
        ChowkInfo("Sitabuldi Interchange", "Wardha Road / Central Avenue", "Dhantoli Zone", 5800),
        ChowkInfo("Rahate Colony Chowk", "Wardha Road", "Dhantoli Zone", 3900),
        ChowkInfo("Medical Square", "Medical College Road / Ajni", "Nehru Nagar Zone", 3600),
        ChowkInfo("Zero Mile Square", "Station Road / Wardha Rd", "Civil Lines", 4100),
        ChowkInfo("Manewada Chowk", "Outer Ring Road", "Hanuman Nagar Zone", 3400),
        ChowkInfo("Sadar Residency Road", "Residency Rd / Mount Rd", "Mangalwari Zone", 2800),
        ChowkInfo("RBI Square", "Kingsway / Station Approach", "Civil Lines", 3200)
    )
}
