package com.example.trafficiq.data

import com.example.trafficiq.ml.VehicleCounts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Repository managing offline session records, local CSV reporting,
 * and periodic batch sync with the Smart City central dashboard.
 */
class TrafficRepository {

    private val _currentSession = MutableStateFlow(
        TrafficSession(
            chowkName = ChowkPresets.NAGPUR_CHOWKS[0].name,
            roadName = ChowkPresets.NAGPUR_CHOWKS[0].road
        )
    )
    val currentSession: StateFlow<TrafficSession> = _currentSession.asStateFlow()

    private val _sessionHistory = MutableStateFlow<List<TrafficSession>>(emptyList())
    val sessionHistory: StateFlow<List<TrafficSession>> = _sessionHistory.asStateFlow()

    fun updateChowk(chowk: ChowkInfo) {
        val current = _currentSession.value
        _currentSession.value = current.copy(
            chowkName = chowk.name,
            roadName = chowk.road
        )
    }

    fun updateLiveCounts(counts: VehicleCounts, currentFps: Float) {
        val current = _currentSession.value
        _currentSession.value = current.copy(
            counts = counts,
            averageFps = (current.averageFps * 0.9f) + (currentFps * 0.1f)
        )
    }

    fun completeAndSaveSession(): TrafficSession {
        val completed = _currentSession.value.copy(
            endTimeMillis = System.currentTimeMillis()
        )
        _sessionHistory.value = listOf(completed) + _sessionHistory.value

        // Start fresh session at same chowk
        _currentSession.value = TrafficSession(
            chowkName = completed.chowkName,
            roadName = completed.roadName,
            officerBadge = completed.officerBadge
        )
        return completed
    }

    fun markSessionSynced(sessionId: String) {
        _sessionHistory.value = _sessionHistory.value.map {
            if (it.sessionId == sessionId) it.copy(isSynced = true) else it
        }
    }

    /**
     * Generates standard comma-separated tabular data for urban planning reports.
     */
    fun exportToCsv(session: TrafficSession): String {
        val sb = StringBuilder()
        sb.append("SessionID,City,Chowk,Road,OfficerBadge,StartTime,DurationSeconds,TotalVehicles,Bikes,Cars,Buses,Trucks,Inbound,Outbound,PCU,FlowRate_VehiclesPerMin\n")
        sb.append("\"${session.sessionId}\",")
        sb.append("\"${session.city}\",")
        sb.append("\"${session.chowkName}\",")
        sb.append("\"${session.roadName}\",")
        sb.append("\"${session.officerBadge}\",")
        sb.append("\"${session.formattedStartTime()}\",")
        sb.append("${session.durationSeconds},")
        sb.append("${session.counts.totalCount},")
        sb.append("${session.counts.bikeCount},")
        sb.append("${session.counts.carCount},")
        sb.append("${session.counts.busCount},")
        sb.append("${session.counts.truckCount},")
        sb.append("${session.counts.inboundCount},")
        sb.append("${session.counts.outboundCount},")
        sb.append(String.format("%.2f", session.counts.totalPcu) + ",")
        sb.append(String.format("%.2f", session.vehiclesPerMinute) + "\n")
        return sb.toString()
    }

    /**
     * Prepares compact JSON summary payload (no video, only count telemetry)
     * as required by Section 5 of the specification.
     */
    fun buildSyncPayload(session: TrafficSession): String {
        return """
        {
          "sessionId": "${session.sessionId}",
          "city": "${session.city}",
          "chowk": "${session.chowkName}",
          "road": "${session.roadName}",
          "officerBadge": "${session.officerBadge}",
          "timestamp": ${session.startTimeMillis},
          "durationSec": ${session.durationSeconds},
          "counts": {
            "bike": ${session.counts.bikeCount},
            "car": ${session.counts.carCount},
            "bus": ${session.counts.busCount},
            "truck": ${session.counts.truckCount},
            "inbound": ${session.counts.inboundCount},
            "outbound": ${session.counts.outboundCount},
            "total": ${session.counts.totalCount},
            "pcu": ${session.counts.totalPcu}
          },
          "metrics": {
            "avgFps": ${session.averageFps},
            "flowRateVpm": ${session.vehiclesPerMinute}
          }
        }
        """.trimIndent()
    }
}
