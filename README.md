To start the chart deep link:
```bash
./adb shell am start -a android.intent.action.VIEW -d "bscm://chart/details/8d3d2eb4-5218-48af-ae33-a73413889353" com.meninocoiso.bscm
```

To convert `keystore.jks` to `keystore.b64`:
```bash
base64 -w 0 app/keystore.jks > keystore.b64
```