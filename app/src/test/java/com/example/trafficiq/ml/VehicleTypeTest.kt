package com.example.trafficiq.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VehicleTypeTest {

    @Test
    fun testIndianVehicleClassMapping() {
        assertEquals(VehicleType.BIKE, VehicleType.fromLabel("motorcycle"))
        assertEquals(VehicleType.BIKE, VehicleType.fromLabel("bicycle"))
        assertEquals(VehicleType.CAR, VehicleType.fromLabel("car"))
        assertEquals(VehicleType.BUS, VehicleType.fromLabel("bus"))
        assertEquals(VehicleType.TRUCK, VehicleType.fromLabel("truck"))
    }

    @Test
    fun testPcuCalculation() {
        val counts = VehicleCounts(
            bikeCount = 10,  // 10 * 0.5 = 5.0 PCU
            carCount = 5,    // 5 * 1.0  = 5.0 PCU
            busCount = 2,    // 2 * 3.0  = 6.0 PCU
            truckCount = 1   // 1 * 3.0  = 3.0 PCU
        )
        // Total PCU = 5.0 + 5.0 + 6.0 + 3.0 = 19.0
        assertEquals(19.0f, counts.totalPcu, 0.01f)
        assertEquals(18, counts.totalCount)
    }
}
