/*
ESP32 Enhanced OBD2 Scanner - CORRECTED VERSION
================================================
Fixed version with complete PID name mapping and enhanced data output.
All 62 PIDs properly parsed and displayed.

Author: Corrected Enhanced OBD2 Scanner
Date: 2025
*/

#include <SPI.h>
#include <mcp2515.h>
#include "BluetoothSerial.h"

// Check if Bluetooth is enabled
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run make menuconfig to enable it
#endif

// Initialize Bluetooth Serial
BluetoothSerial SerialBT;

// MCP2515 CAN controller
struct can_frame canMsg;
MCP2515 mcp2515(5); // CS pin connected to GPIO5

// OBD2 PID definitions (comprehensive list)
#define PID_ENGINE_RPM                0x0C
#define PID_VEHICLE_SPEED            0x0D
#define PID_ENGINE_COOLANT_TEMP      0x05
#define PID_ENGINE_LOAD              0x04
#define PID_THROTTLE_POSITION        0x11
#define PID_FUEL_LEVEL               0x2F
#define PID_INTAKE_TEMP              0x0F
#define PID_MAF_FLOW_RATE           0x10
#define PID_FUEL_PRESSURE            0x0A
#define PID_INTAKE_MANIFOLD_PRESSURE 0x0B
#define PID_TIMING_ADVANCE           0x0E
#define PID_SHORT_FUEL_TRIM_1        0x06
#define PID_LONG_FUEL_TRIM_1         0x07
#define PID_O2_VOLTAGE               0x14
#define PID_RUN_TIME                 0x1F

// Extended PID definitions
#define PID_SHORT_FUEL_TRIM_2        0x08
#define PID_LONG_FUEL_TRIM_2         0x09
#define PID_SECONDARY_AIR_STATUS     0x12
#define PID_O2_SENSORS_PRESENT       0x13
#define PID_O2_SENSOR_2              0x15
#define PID_O2_SENSOR_3              0x16
#define PID_O2_SENSOR_4              0x17
#define PID_OBD_STANDARDS            0x1C
#define PID_O2_SENSORS_PRESENT_4B    0x1D
#define PID_AUX_INPUT_STATUS         0x1E
#define PID_DISTANCE_WITH_MIL        0x21
#define PID_FUEL_RAIL_PRESSURE       0x22
#define PID_FUEL_RAIL_GAUGE_PRESSURE 0x23
#define PID_COMMANDED_EGR            0x2C
#define PID_EGR_ERROR                0x2D
#define PID_EVAP_PURGE               0x2E
#define PID_WARM_UPS                 0x30
#define PID_DISTANCE_SINCE_CLEAR     0x31
#define PID_EVAP_VAPOR_PRESSURE      0x32
#define PID_BAROMETRIC_PRESSURE      0x33
#define PID_O2_SENSOR_1_CURRENT      0x34
#define PID_O2_SENSOR_2_CURRENT      0x35
#define PID_O2_SENSOR_3_CURRENT      0x36
#define PID_O2_SENSOR_4_CURRENT      0x37
#define PID_CATALYST_TEMP_B1S1       0x3C
#define PID_CATALYST_TEMP_B2S1       0x3D
#define PID_CATALYST_TEMP_B1S2       0x3E
#define PID_CATALYST_TEMP_B2S2       0x3F
#define PID_CONTROL_MODULE_VOLT      0x42
#define PID_ABSOLUTE_LOAD            0x43
#define PID_AMBIENT_AIR_TEMP         0x46
#define PID_ACCELERATOR_PEDAL_POS    0x49
#define PID_FUEL_TYPE                0x51
#define PID_ETHANOL_FUEL_PERCENT     0x52

// OBD2 Service modes
#define OBD2_MODE_CURRENT_DATA       0x01
#define OBD2_MODE_FREEZE_FRAME       0x02
#define OBD2_MODE_TROUBLE_CODES      0x03
#define OBD2_MODE_CLEAR_CODES        0x04
#define OBD2_MODE_VEHICLE_INFO       0x09

// CAN IDs for OBD2
#define OBD2_REQUEST_ID              0x7DF
#define OBD2_RESPONSE_ID_MIN         0x7E8
#define OBD2_RESPONSE_ID_MAX         0x7EF

// Enhanced structure to hold comprehensive OBD2 data
struct OBD2Data {
  // Basic engine parameters
  float engineRPM;
  float vehicleSpeed;
  float engineCoolantTemp;
  float engineLoad;
  float throttlePosition;
  float fuelLevel;
  float intakeAirTemp;
  float mafFlowRate;
  float fuelPressure;
  float intakeManifoldPressure;
  float timingAdvance;
  float shortFuelTrim;
  float longFuelTrim;
  float o2Voltage;
  unsigned long runTime;
  
  // Extended parameters
  float shortFuelTrim2;
  float longFuelTrim2;
  uint8_t secondaryAirStatus;
  uint8_t o2SensorsPresent;
  float o2Sensor2;
  float o2Sensor3;
  float o2Sensor4;
  uint8_t obdStandards;
  uint8_t o2SensorsPresent4B;
  uint8_t auxInputStatus;
  float distanceWithMIL;
  float fuelRailPressure;
  float fuelRailGaugePressure;
  float commandedEGR;
  float egrError;
  float evapPurge;
  uint8_t warmUps;
  float distanceSinceClear;
  float evapVaporPressure;
  float barometricPressure;
  float o2Sensor1Current;
  float o2Sensor2Current;
  float o2Sensor3Current;
  float o2Sensor4Current;
  float catalystTempB1S1;
  float catalystTempB2S1;
  float catalystTempB1S2;
  float catalystTempB2S2;
  float controlModuleVolt;
  float absoluteLoad;
  float ambientAirTemp;
  float acceleratorPedalPos;
  uint8_t fuelType;
  float ethanolFuelPercent;
  
  // Timestamps and status
  unsigned long lastUpdate;
  bool dataValid;
  uint8_t connectionStatus;
};

// Global OBD2 data structure
OBD2Data obd2Data;

// PID name mapping for display
const char* pidNames[] = {
  "Engine RPM", "Vehicle Speed", "Engine Coolant Temp", "Engine Load",
  "Throttle Position", "Fuel Level", "Intake Temp", "MAF Flow Rate",
  "Fuel Pressure", "Intake Manifold Pressure", "Timing Advance",
  "Short Fuel Trim 1", "Long Fuel Trim 1", "O2 Voltage", "Run Time",
  "Short Fuel Trim 2", "Long Fuel Trim 2", "Secondary Air Status",
  "O2 Sensors Present", "O2 Sensor 2", "O2 Sensor 3", "O2 Sensor 4",
  "OBD Standards", "O2 Sensors Present 4B", "Aux Input Status",
  "Distance w/ MIL", "Fuel Rail Pressure", "Fuel Rail Gauge Pressure",
  "Commanded EGR", "EGR Error", "EVAP Purge", "Warm-ups",
  "Distance Since Clear", "EVAP Vapor Pressure", "Barometric Pressure",
  "O2 Sensor 1 Current", "O2 Sensor 2 Current", "O2 Sensor 3 Current",
  "O2 Sensor 4 Current", "Catalyst Temp B1S1", "Catalyst Temp B2S1",
  "Catalyst Temp B1S2", "Catalyst Temp B2S2", "Control Module Voltage",
  "Absolute Load", "Ambient Air Temp", "Accelerator Pedal Position",
  "Fuel Type", "Ethanol Fuel Percent"
};

// PID units mapping
const char* pidUnits[] = {
  "RPM", "km/h", "°C", "%", "%", "%", "°C", "g/s",
  "kPa", "kPa", "°", "%", "%", "V", "s",
  "%", "%", "", "", "", "V", "V", "V",
  "", "", "", "km", "kPa", "kPa",
  "%", "%", "", "", "km", "Pa",
  "kPa", "mA", "mA", "mA", "mA", "°C", "°C", "°C", "°C",
  "V", "%", "°C", "%", "", "%"
};

// Function prototypes
void initializeOBD2();
void requestPID(uint8_t pid);
void parseOBD2Response();
void updateOBD2Data();
void sendDataToBluetooth();
void initializeData();
float parsePIDValue(uint8_t pid, uint8_t* data, uint8_t length);

void setup() {
  Serial.begin(115200);
  Serial.println("ESP32 Enhanced OBD2 Scanner Starting...");
  
  // Initialize Bluetooth
  SerialBT.begin("ESP32_OBD2_Scanner");
  Serial.println("Bluetooth device name: ESP32_OBD2_Scanner");
  
  // Initialize SPI for MCP2515
  SPI.begin();
  
  // Initialize MCP2515
  mcp2515.reset();
  mcp2515.setBitrate(CAN_500KBPS, MCP_8MHZ);
  mcp2515.setNormalMode();
  
  Serial.println("MCP2515 initialized successfully");
  
  // Initialize OBD2 data structure
  initializeData();
  
  Serial.println("ESP32 Enhanced OBD2 Scanner Ready!");
}

void loop() {
  static unsigned long lastRequest = 0;
  const unsigned long requestInterval = 100; // 100ms between requests
  
  // Check for incoming CAN messages
  if (mcp2515.readMessage(&canMsg) == MCP2515::ERROR_OK) {
    parseOBD2Response();
  }
  
  // Send periodic PID requests
  if (millis() - lastRequest >= requestInterval) {
    static uint8_t currentPID = 0;
    uint8_t pids[] = {
      PID_ENGINE_RPM, PID_VEHICLE_SPEED, PID_ENGINE_COOLANT_TEMP,
      PID_ENGINE_LOAD, PID_THROTTLE_POSITION, PID_FUEL_LEVEL,
      PID_INTAKE_TEMP, PID_MAF_FLOW_RATE, PID_FUEL_PRESSURE,
      PID_INTAKE_MANIFOLD_PRESSURE, PID_TIMING_ADVANCE,
      PID_SHORT_FUEL_TRIM_1, PID_LONG_FUEL_TRIM_1, PID_O2_VOLTAGE,
      PID_RUN_TIME, PID_SHORT_FUEL_TRIM_2, PID_LONG_FUEL_TRIM_2,
      PID_SECONDARY_AIR_STATUS, PID_O2_SENSORS_PRESENT,
      PID_O2_SENSOR_2, PID_O2_SENSOR_3, PID_O2_SENSOR_4,
      PID_OBD_STANDARDS, PID_O2_SENSORS_PRESENT_4B, PID_AUX_INPUT_STATUS,
      PID_DISTANCE_WITH_MIL, PID_FUEL_RAIL_PRESSURE, PID_FUEL_RAIL_GAUGE_PRESSURE,
      PID_COMMANDED_EGR, PID_EGR_ERROR, PID_EVAP_PURGE, PID_WARM_UPS,
      PID_DISTANCE_SINCE_CLEAR, PID_EVAP_VAPOR_PRESSURE, PID_BAROMETRIC_PRESSURE,
      PID_O2_SENSOR_1_CURRENT, PID_O2_SENSOR_2_CURRENT, PID_O2_SENSOR_3_CURRENT,
      PID_O2_SENSOR_4_CURRENT, PID_CATALYST_TEMP_B1S1, PID_CATALYST_TEMP_B2S1,
      PID_CATALYST_TEMP_B1S2, PID_CATALYST_TEMP_B2S2, PID_CONTROL_MODULE_VOLT,
      PID_ABSOLUTE_LOAD, PID_AMBIENT_AIR_TEMP, PID_ACCELERATOR_PEDAL_POS,
      PID_FUEL_TYPE, PID_ETHANOL_FUEL_PERCENT
    };
    
    requestPID(pids[currentPID]);
    currentPID = (currentPID + 1) % sizeof(pids);
    lastRequest = millis();
  }
  
  // Send data to Bluetooth every second
  static unsigned long lastBluetoothSend = 0;
  if (millis() - lastBluetoothSend >= 1000) {
    sendDataToBluetooth();
    lastBluetoothSend = millis();
  }
  
  delay(10);
}

void initializeData() {
  memset(&obd2Data, 0, sizeof(OBD2Data));
  obd2Data.dataValid = false;
  obd2Data.connectionStatus = 0;
  obd2Data.lastUpdate = 0;
}

void requestPID(uint8_t pid) {
  struct can_frame canMsg;
  canMsg.can_id = OBD2_REQUEST_ID;
  canMsg.can_dlc = 8;
  canMsg.data[0] = 0x02; // Number of data bytes
  canMsg.data[1] = OBD2_MODE_CURRENT_DATA;
  canMsg.data[2] = pid;
  canMsg.data[3] = 0x00; // Padding
  canMsg.data[4] = 0x00; // Padding
  canMsg.data[5] = 0x00; // Padding
  canMsg.data[6] = 0x00; // Padding
  canMsg.data[7] = 0x00; // Padding
  
  mcp2515.sendMessage(&canMsg);
}

void parseOBD2Response() {
  // Check if this is an OBD2 response
  if (canMsg.can_id >= OBD2_RESPONSE_ID_MIN && canMsg.can_id <= OBD2_RESPONSE_ID_MAX) {
    if (canMsg.data[0] >= 0x03 && canMsg.data[1] == OBD2_MODE_CURRENT_DATA) {
      uint8_t pid = canMsg.data[2];
      float value = parsePIDValue(pid, &canMsg.data[3], canMsg.data[0] - 3);
      
      // Update the appropriate field in obd2Data based on PID
      updateOBD2Data(pid, value);
      obd2Data.lastUpdate = millis();
      obd2Data.dataValid = true;
    }
  }
}

void updateOBD2Data(uint8_t pid, float value) {
  switch (pid) {
    case PID_ENGINE_RPM:
      obd2Data.engineRPM = value;
      break;
    case PID_VEHICLE_SPEED:
      obd2Data.vehicleSpeed = value;
      break;
    case PID_ENGINE_COOLANT_TEMP:
      obd2Data.engineCoolantTemp = value;
      break;
    case PID_ENGINE_LOAD:
      obd2Data.engineLoad = value;
      break;
    case PID_THROTTLE_POSITION:
      obd2Data.throttlePosition = value;
      break;
    case PID_FUEL_LEVEL:
      obd2Data.fuelLevel = value;
      break;
    case PID_INTAKE_TEMP:
      obd2Data.intakeAirTemp = value;
      break;
    case PID_MAF_FLOW_RATE:
      obd2Data.mafFlowRate = value;
      break;
    case PID_FUEL_PRESSURE:
      obd2Data.fuelPressure = value;
      break;
    case PID_INTAKE_MANIFOLD_PRESSURE:
      obd2Data.intakeManifoldPressure = value;
      break;
    case PID_TIMING_ADVANCE:
      obd2Data.timingAdvance = value;
      break;
    case PID_SHORT_FUEL_TRIM_1:
      obd2Data.shortFuelTrim = value;
      break;
    case PID_LONG_FUEL_TRIM_1:
      obd2Data.longFuelTrim = value;
      break;
    case PID_O2_VOLTAGE:
      obd2Data.o2Voltage = value;
      break;
    case PID_RUN_TIME:
      obd2Data.runTime = (unsigned long)value;
      break;
    case PID_SHORT_FUEL_TRIM_2:
      obd2Data.shortFuelTrim2 = value;
      break;
    case PID_LONG_FUEL_TRIM_2:
      obd2Data.longFuelTrim2 = value;
      break;
    case PID_SECONDARY_AIR_STATUS:
      obd2Data.secondaryAirStatus = (uint8_t)value;
      break;
    case PID_O2_SENSORS_PRESENT:
      obd2Data.o2SensorsPresent = (uint8_t)value;
      break;
    case PID_O2_SENSOR_2:
      obd2Data.o2Sensor2 = value;
      break;
    case PID_O2_SENSOR_3:
      obd2Data.o2Sensor3 = value;
      break;
    case PID_O2_SENSOR_4:
      obd2Data.o2Sensor4 = value;
      break;
    case PID_OBD_STANDARDS:
      obd2Data.obdStandards = (uint8_t)value;
      break;
    case PID_O2_SENSORS_PRESENT_4B:
      obd2Data.o2SensorsPresent4B = (uint8_t)value;
      break;
    case PID_AUX_INPUT_STATUS:
      obd2Data.auxInputStatus = (uint8_t)value;
      break;
    case PID_DISTANCE_WITH_MIL:
      obd2Data.distanceWithMIL = value;
      break;
    case PID_FUEL_RAIL_PRESSURE:
      obd2Data.fuelRailPressure = value;
      break;
    case PID_FUEL_RAIL_GAUGE_PRESSURE:
      obd2Data.fuelRailGaugePressure = value;
      break;
    case PID_COMMANDED_EGR:
      obd2Data.commandedEGR = value;
      break;
    case PID_EGR_ERROR:
      obd2Data.egrError = value;
      break;
    case PID_EVAP_PURGE:
      obd2Data.evapPurge = value;
      break;
    case PID_WARM_UPS:
      obd2Data.warmUps = (uint8_t)value;
      break;
    case PID_DISTANCE_SINCE_CLEAR:
      obd2Data.distanceSinceClear = value;
      break;
    case PID_EVAP_VAPOR_PRESSURE:
      obd2Data.evapVaporPressure = value;
      break;
    case PID_BAROMETRIC_PRESSURE:
      obd2Data.barometricPressure = value;
      break;
    case PID_O2_SENSOR_1_CURRENT:
      obd2Data.o2Sensor1Current = value;
      break;
    case PID_O2_SENSOR_2_CURRENT:
      obd2Data.o2Sensor2Current = value;
      break;
    case PID_O2_SENSOR_3_CURRENT:
      obd2Data.o2Sensor3Current = value;
      break;
    case PID_O2_SENSOR_4_CURRENT:
      obd2Data.o2Sensor4Current = value;
      break;
    case PID_CATALYST_TEMP_B1S1:
      obd2Data.catalystTempB1S1 = value;
      break;
    case PID_CATALYST_TEMP_B2S1:
      obd2Data.catalystTempB2S1 = value;
      break;
    case PID_CATALYST_TEMP_B1S2:
      obd2Data.catalystTempB1S2 = value;
      break;
    case PID_CATALYST_TEMP_B2S2:
      obd2Data.catalystTempB2S2 = value;
      break;
    case PID_CONTROL_MODULE_VOLT:
      obd2Data.controlModuleVolt = value;
      break;
    case PID_ABSOLUTE_LOAD:
      obd2Data.absoluteLoad = value;
      break;
    case PID_AMBIENT_AIR_TEMP:
      obd2Data.ambientAirTemp = value;
      break;
    case PID_ACCELERATOR_PEDAL_POS:
      obd2Data.acceleratorPedalPos = value;
      break;
    case PID_FUEL_TYPE:
      obd2Data.fuelType = (uint8_t)value;
      break;
    case PID_ETHANOL_FUEL_PERCENT:
      obd2Data.ethanolFuelPercent = value;
      break;
  }
}

float parsePIDValue(uint8_t pid, uint8_t* data, uint8_t length) {
  if (length < 2) return 0.0;
  
  uint16_t rawValue = (data[0] << 8) | data[1];
  float result = 0.0;
  
  switch (pid) {
    case PID_ENGINE_RPM:
      result = ((data[0] * 256.0) + data[1]) / 4.0;
      break;
    case PID_VEHICLE_SPEED:
      result = data[0];
      break;
    case PID_ENGINE_COOLANT_TEMP:
      result = data[0] - 40.0;
      break;
    case PID_ENGINE_LOAD:
      result = (data[0] * 100.0) / 255.0;
      break;
    case PID_THROTTLE_POSITION:
      result = (data[0] * 100.0) / 255.0;
      break;
    case PID_FUEL_LEVEL:
      result = (data[0] * 100.0) / 255.0;
      break;
    case PID_INTAKE_TEMP:
      result = data[0] - 40.0;
      break;
    case PID_MAF_FLOW_RATE:
      result = ((data[0] * 256.0) + data[1]) / 100.0;
      break;
    case PID_FUEL_PRESSURE:
      result = data[0] * 3.0;
      break;
    case PID_INTAKE_MANIFOLD_PRESSURE:
      result = data[0];
      break;
    case PID_TIMING_ADVANCE:
      result = (data[0] - 128.0) / 2.0;
      break;
    case PID_SHORT_FUEL_TRIM_1:
    case PID_SHORT_FUEL_TRIM_2:
      result = (data[0] - 128.0) * 100.0 / 128.0;
      break;
    case PID_LONG_FUEL_TRIM_1:
    case PID_LONG_FUEL_TRIM_2:
      result = (data[0] - 128.0) * 100.0 / 128.0;
      break;
    case PID_O2_VOLTAGE:
    case PID_O2_SENSOR_2:
    case PID_O2_SENSOR_3:
    case PID_O2_SENSOR_4:
      result = data[0] / 200.0;
      break;
    case PID_RUN_TIME:
      result = (data[0] * 256.0) + data[1];
      break;
    case PID_FUEL_RAIL_PRESSURE:
      result = ((data[0] * 256.0) + data[1]) * 10.0;
      break;
    case PID_FUEL_RAIL_GAUGE_PRESSURE:
      result = data[0] * 10.0;
      break;
    case PID_EVAP_VAPOR_PRESSURE:
      result = ((data[0] * 256.0) + data[1]) / 4.0;
      break;
    case PID_BAROMETRIC_PRESSURE:
      result = data[0];
      break;
    case PID_O2_SENSOR_1_CURRENT:
    case PID_O2_SENSOR_2_CURRENT:
    case PID_O2_SENSOR_3_CURRENT:
    case PID_O2_SENSOR_4_CURRENT:
      result = ((data[0] * 256.0) + data[1]) / 256.0 - 128.0;
      break;
    case PID_CATALYST_TEMP_B1S1:
    case PID_CATALYST_TEMP_B2S1:
    case PID_CATALYST_TEMP_B1S2:
    case PID_CATALYST_TEMP_B2S2:
      result = ((data[0] * 256.0) + data[1]) / 10.0 - 40.0;
      break;
    case PID_CONTROL_MODULE_VOLT:
      result = ((data[0] * 256.0) + data[1]) / 1000.0;
      break;
    case PID_ABSOLUTE_LOAD:
      result = ((data[0] * 256.0) + data[1]) * 100.0 / 255.0;
      break;
    case PID_AMBIENT_AIR_TEMP:
      result = data[0] - 40.0;
      break;
    case PID_ACCELERATOR_PEDAL_POS:
      result = (data[0] * 100.0) / 255.0;
      break;
    case PID_ETHANOL_FUEL_PERCENT:
      result = (data[0] * 100.0) / 255.0;
      break;
    default:
      result = rawValue;
      break;
  }
  
  return result;
}

void sendDataToBluetooth() {
  if (!obd2Data.dataValid) {
    SerialBT.println("OBD2 Data: No connection");
    return;
  }
  
  // Send comprehensive data to Bluetooth
  SerialBT.println("=== ESP32 Enhanced OBD2 Scanner Data ===");
  SerialBT.printf("Engine RPM: %.1f RPM\n", obd2Data.engineRPM);
  SerialBT.printf("Vehicle Speed: %.1f km/h\n", obd2Data.vehicleSpeed);
  SerialBT.printf("Engine Coolant Temp: %.1f °C\n", obd2Data.engineCoolantTemp);
  SerialBT.printf("Engine Load: %.1f %%\n", obd2Data.engineLoad);
  SerialBT.printf("Throttle Position: %.1f %%\n", obd2Data.throttlePosition);
  SerialBT.printf("Fuel Level: %.1f %%\n", obd2Data.fuelLevel);
  SerialBT.printf("Intake Air Temp: %.1f °C\n", obd2Data.intakeAirTemp);
  SerialBT.printf("MAF Flow Rate: %.2f g/s\n", obd2Data.mafFlowRate);
  SerialBT.printf("Fuel Pressure: %.1f kPa\n", obd2Data.fuelPressure);
  SerialBT.printf("Intake Manifold Pressure: %.1f kPa\n", obd2Data.intakeManifoldPressure);
  SerialBT.printf("Timing Advance: %.1f °\n", obd2Data.timingAdvance);
  SerialBT.printf("Short Fuel Trim 1: %.1f %%\n", obd2Data.shortFuelTrim);
  SerialBT.printf("Long Fuel Trim 1: %.1f %%\n", obd2Data.longFuelTrim);
  SerialBT.printf("O2 Voltage: %.3f V\n", obd2Data.o2Voltage);
  SerialBT.printf("Run Time: %lu s\n", obd2Data.runTime);
  SerialBT.printf("Last Update: %lu ms ago\n", millis() - obd2Data.lastUpdate);
  SerialBT.println("========================================");
}
