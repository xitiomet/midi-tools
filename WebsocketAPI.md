# MIDI Control Change Tool #
## Websocket API Document ##
---------
MIDI Control Change Tool features a Websocket API for registering virtual devices and control events.

### Protocol: ###

**Transmitting to Server**

```json
	{
		"do": "commandName",
		....
	}
```

When sending data to the websocket server a json object must be sent as a single line of text. All objects must contain the field "do" which lets the server know the purpose of the packet.

**Receiving Server events**

```json
	{
		"event: "eventName",
		....
	}
```

The server will only send json objects. Each object will contain the field "event". The rest of the object will contain information about the event.

### Registering a virtual device ###
```json
	{
		"do": "registerMidiDevice",
		"name": "Display Name",
		"device": "uniqueId", 
		"type": "input"
	}
```

### Un-Registering a virtual device ###
```json
	{
		"do": "removeMidiDevice",
		"device": "uniqueId"
	}
```

### Receiving Midi Data being written to your device ###
```json
{
	"event":"midiShortMessage", 
	"device": deviceId, 
	"data": [186,10,127],
	"timeStamp": midiMessage.receivedTime
};
```

### Transmitting Midi Data ###
```json
{
	"do":"midiShortMessage", 
	"device": deviceId, 
	"data": [186,10,127],
	"timeStamp": midiMessage.receivedTime
};
```
