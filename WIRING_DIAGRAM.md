# ESP32 to MCP2515 Wiring Diagram

## Pin Connections

```
ESP32                    MCP2515 CAN Controller
=====                    ======================
GPIO 5  (CS)    -----> CS (Chip Select)
GPIO 23 (MOSI)  -----> SI (Data In)
GPIO 19 (MISO)  -----> SO (Data Out)
GPIO 18 (SCK)   -----> SCK (Clock)
3.3V            -----> VCC (Power)
GND             -----> GND (Ground)

OBD2 Cable      -----> CANH, CANL (High/Low CAN signals)
```

## Connection Details

- **CS (Chip Select)**: GPIO 5 - Controls when the MCP2515 is active
- **MOSI (Master Out Slave In)**: GPIO 23 - ESP32 sends data to MCP2515
- **MISO (Master In Slave Out)**: GPIO 19 - ESP32 receives data from MCP2515
- **SCK (Serial Clock)**: GPIO 18 - Provides timing for SPI communication
- **VCC**: 3.3V power supply from ESP32
- **GND**: Common ground connection

## OBD2 Cable Connection

The OBD2 cable typically has these connections:
- **Pin 6**: CAN High (CANH)
- **Pin 14**: CAN Low (CANL)
- **Pin 16**: +12V (vehicle power - optional)
- **Pin 5**: Ground

## Power Considerations

- ESP32 operates on 3.3V
- MCP2515 can handle 3.3V logic levels
- Ensure stable power supply for reliable operation
- Consider using external power for vehicle applications
