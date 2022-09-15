# Midi Control Change Tools #

This utility allows you to observe MIDI Control Change and NOTE messages from every channel in real-time, and associate rules with them. It also features midi port mapping, and a websocket interface. Each Channel/CC# is treated as in individual "Control" so this application can be thought of as an "omni" mode midi input.

Each "Control" can have rules associated with it that will execute "Actions" based on the type of change made to the Control. For instance you can have a rule that will execute whenever the value increases. 

Features
 - Fully functional MIDI router (create virtual connections between midi devices)
 - Plugin support for controlling popular hardware via MIDI (Philips Hue Plugin available)
 - All OS compatable game controllers (Xbox Controller, 8Bitdo, etc) recognized as MIDI inputs
 - RTP MIDI support for use with Apple's network MIDI implementation
 - Detailed MIDI logging for troubleshooting issues with MIDI devices
 - Media Canvas feature lets you create audio/visual content triggered by MIDI messages
 - Mobile friendly remote control interface

![](https://openstatic.org/projects/miditools/img/miditools10.png)

### How do I use it? ###
Simply launch the app and check off what MIDI devices you want to use. Then toggle the ear on the MIDI Controls tab, as you play with the physical controls on your device, those controls will appear in the "Midi Controls" section. Each control represents a channel and a control change number. In order to create a rule for a control, select control by clicking on it. Then click the "script" button (third one on the left of the midi controls tab)

### Creating a rule ###
"Rules" are a way to create mappings from your MIDI Controls to an event 

![](https://openstatic.org/projects/miditools/img/rule6.png)

- Rule Name - This is just a label for the rule
- Rule Group - The group this rule belongs to (optional)
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
	- PLUGIN - Make a call to a MidiTools Plugin
	- Action Type: specify a url http://www.domain.com/program.php?v={{value}}
	- RUN PROGRAM - Execute a program
	  Action Type: specify a program c:\program.exe {{value}}
	- PLAY SOUND - Play a wav file (volume will adjust with value)
	- PLUGIN - Send message/event to a plugin
	  Action Type: you may specify a url or local path to a wav file
	- TRANSMIT CONTROL CHANGE - Send a MIDI Message
	  Action Type: Device Name,channel#,cc#,{{value}}
	- ENABLE RULE GROUP - Enable a group of rules
	- DISABLE RULE GROUP - Disable a group of rules
	- TOGGLE RULE GROUP - Toggle a group of rules
	- LOGGER A MESSAGE - Add some text to Logger A
	- LOGGER B MESSAGE - Add some text to Logger B
	- SHOW IMAGE - Display an image on the Canvas
- Target Canvas - Specify the canvas this action should take place, this applies to PLAY SOUND and SHOW IMAGE. Canvas names can be made up on the fly!
- Action Value - Specify the parameters to an Action Type (variables below)
	- {{value}} - The value(third byte) of the MIDI Control Change Event (0-127)
	- {{value.inv}} - The inverted value of the MIDI Event
	- {{value.old}} - The value previously held by the midi control
	- {{value.old.inv}} - The inverted value previously held by the midi control
	- {{value.change}} - The amount the controls value has changed
	- {{value.map(min,max)}} - Remaps the 0-127 value to a new range specified by min/max think of it like the arduino map function
	- {{cc}} - Control change number of the event
	- {{note}} - Note # that was triggered
	- {{note.name}} - Name of triggered note
	- {{channel}} - Channel number of the event

## Control Change Rules Tab ##

The Rules tab allows you to manage your rules and see their state. When a rule is triggered by a MIDI event it will either turn green(it worked) or red(something failed) for a moment. When a rules fails its because the action could not be performed. In the case of a SHOW IMAGE or PLAY SOUND event the targeted canvas may not be open. Where as a CALL URL might fail due to a bad URL.

![](https://openstatic.org/projects/miditools/img/rulestab.png)

You can edit a rule at any time by clicking on the rule and using the Edit button (second one down) to change its parameters. Once you hit "save" the changes take effect immediately.

NOTE: If you wish to temporarily disable a rule, you can double click on it and it should turn gray.

## Port Mappings Tab ##

The Port Mappings tab allows you to create virtual connections between MIDI devices. Think of it like a virtual MIDI cable, allowing a connection between a device's MIDI OUT and another devices MIDI IN. Whenever data is flowing over the mapping it will turn green to let you know its working. To create a port mapping click the "plug" button on the left hand side.

![](https://openstatic.org/projects/miditools/img/portmappings.png)

## Project Assets Tab ##

The Project Assets tab is for adding media and additional files to your project. These assets will be available to your rules and are saved as part of the project file. To add an asset simply drag and drop the file into this tab, or click on the "Add File" button on the left hand side.

Supported Assets include:
 - Images (gif/jpeg/png)
 - Sound clips (wav files only)
 - Sound Fonts (sf2)
 - Shell Scripts (cmd/bat/sh)
 - MIDI Files (mid)

 However any file type may be added, and will be available using the internal web server at http://127.0.0.1:6123/assets/ allowing external tools to access them via HTTP

![](https://openstatic.org/projects/miditools/img/projectassets.png)
