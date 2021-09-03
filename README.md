# Midi Control Change Tools #

This utility allows you to observe MIDI control change messages from every channel in real-time, and associate rules with them. It also features midi port mapping, and a websocket interface. Each Channel/CC# is treated as in individual "Control" so this application can be thought of as an "omni" mode midi input.

Each "Control" can have rules associated with it that will execute "Actions" based on the type of change made to the Control. For instance you can have a rule that will execute whenever the value increases.

Possible "Actions" are executing a script or program, calling a url or api, playing a sound, or Transmitting a midi message to another device.

![](https://openstatic.org/projects/miditools/miditools3.gif)

### How do I use it? ###
Simply launch the app and check off what MIDI devices you want to listen to, as you play with the physical controls on your device, those controls will appear in the "Midi Controls" section. Each control represents a channel and a control change number. Simply double-click or right click on a control to change its name, and create rules.

### Creating a rule ###
"Rules" are a way to create mappings from your MIDI Controls to an event

![](https://openstatic.org/projects/miditools/rule4.png)

- Rule Name - This is just a label for the rule
- Select Control - Midi Control to listen for events on (A control represents a channel and cc#)
- Select Event - When should this rule trigger?
	- onChange - Whenever there is a change in value
	- onSettled - When the value has settled for at least 500ms
	- onHigh (64+) - whenever the value passes the middle of its range in the upperbound direction
	- onLow (63-) - whenever the value passes the middle of its range in the lowerbound direction
	- onIncrease - whenever the value increases
	- onDecrease - whenever the value decreases
	- onSettledIncrease - whenever the value settles higher then the previous value
	- onSettledDecrease - whenever the value settles lower then the previous value
- Action Type - What Kind of action do you want to take?
	- CALL URL - Make a request to a URL (response is ignored)
	- Action Type: specify a url http://www.domain.com/program.php?v={{value}}
	- RUN PROGRAM - Execute a program
	  Action Type: specify a program c:\program.exe {{value}}
	- PLAY SOUND - Play a wav file
	  Action Type: you may specify a url or local path to a wav file
	- TRANSMIT MIDI - Send a MIDI Message
	  Action Type: Device Name,channel#,cc#,{{value}}
- Action Value - Specify the parameters to an Action Type (variables below)
	- {{value}} - The value(third byte) of the MIDI Control Change Event (0-127)
	- {{value.inv}} - The inverted value of the MIDI Event
	- {{value.old}} - The value previously held by the midi control
	- {{value.old.inv}} - The inverted value previously held by the midi control
	- {{value.change}} - The amount the controls value has changed
	- {{cc}} - Control change number of the event.
	- {{channel}} - Channel number of the event.

