# TrafficIQ Edge Vision Models

This directory holds TensorFlow Lite models and label files used by the on-device inference engine.

## Supported Models
TrafficIQ is configured to run lightweight, real-time object detection models:
1. **SSD MobileNet V2 / V3 (TFLite quantized)** - Default model (e.g. `ssd_mobilenet_v2_coco_quant.tflite`)
   - Input: `[1, 300, 300, 3]` (UINT8 / FLOAT32)
   - Outputs:
     - Locations (Bounding Boxes): `[1, 10, 4]` (normalized [top, left, bottom, right])
     - Classes: `[1, 10]`
     - Scores: `[1, 10]`
     - Number of detections: `[1]`
2. **YOLOv8-Nano / YOLOv11-Nano (TFLite)**
   - Input: `[1, 640, 640, 3]`

## Target Vehicle Classes for Indian Traffic
The classifier filters and maps COCO classes into 4 primary municipal transport categories:
- **BIKE**: `motorcycle`, `bicycle` (Critical focus: accounts for 45% accident deaths per MoRTH 2023 report)
- **CAR**: `car`, plus auto-rickshaw custom detection
- **BUS**: `bus` (City buses like Nagpur Aapli Bus, MSRTC)
- **TRUCK**: `truck` (Heavy transport, container trucks, tippers)

## Fallback & Graceful Degradation
If no `.tflite` binary file is placed in this directory, `TrafficDetector` automatically activates an intelligent edge camera simulation detector with realistic vehicle flow kinematics, allowing UI development and end-to-end testing without blocking builds!
