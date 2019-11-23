var localOutputDevices = new Map();
var localInputDevices = new Map();
var connection;

// midi functions
function onMIDISuccess(midiAccess) {
    // when we get a succesful response, run this code
    console.log('MIDI Access Object', midiAccess);
    if (midiAccess.outputs && midiAccess.outputs.size > 0)
    {
        var outputs = midiAccess.outputs.values();
        for (output = outputs.next(); output && !output.done; output = outputs.next())
        {
            var v = output.value
            localOutputDevices.set(v.name+".output", v);
            console.log("output Device: " + v.name);
            registerMidiDevice("output", v);
        }
        var inputs = midiAccess.inputs.values();
        for (input = inputs.next(); input && !input.done; input = inputs.next())
        {
            var v = input.value
            localInputDevices.set(v.name+".input", v);
            console.log("input Device: " + v.name);
            registerMidiDevice("input", v);
        }
    } else {
        console.log("No outputs");
    }
}

function registerMidiDevice(type, v)
{
    var deviceId = v.name + "." + type;
    var mm = {"do":"registerMidiDevice", "name": v.name, "device": deviceId, "type": type};
    sendEvent(mm);
    v.onmidimessage = function(midiMessage) {
        if (midiMessage.data.length == 3)
        {
            var mm = {"do":"midiShortMessage", "device": deviceId, "data": [midiMessage.data[0], midiMessage.data[1], midiMessage.data[2]], "timeStamp": midiMessage.receivedTime};
            sendEvent(mm);
        } else {
            //console.log("Non-short message received");
        }
    }
}

function onMIDIFailure(e) {
    // when we get a failed response, run this code
    console.log("No access to MIDI devices or your browser doesn't support WebMIDI API. Please use WebMIDIAPIShim " + e);
}

function sendEvent(wsEvent)
{
    var out_event = JSON.stringify(wsEvent);
    console.log("Transmit: " + out_event);
    try
    {
        connection.send(out_event);
    } catch (err) {
        console.log(err);
    }
}

function removeControlElement(control)
{
    var idPostfix = control.channel + '_' + control.cc;
    var trow = document.getElementById('ctrl_' + idPostfix);
    document.getElementById('controlsTable').removeChild(trow);
}

function createControlElement(control)
{
    var idPostfix = control.channel + '_' + control.cc;
    var rowId = 'ctrl_' + idPostfix;
    if (document.getElementById(rowId) == undefined)
    {
        var trow = document.createElement("tr");
        trow.id = rowId;
        trow.style.height = '48px';
        trow.innerHTML = '<td style="max-width: 35%;"><b id="nickname_' + idPostfix + '" style="font-size: 18px;">' + control.nickname + '</b><br /><i id="italic_' + idPostfix + '" style="font-size: 10px;">ch=' + control.channel + ' cc=' + control.cc + ' v=' + control.value + '</i></td>' +
                         '<td style="min-width: 65%; max-width: 85%;"><progress id="progress_' + idPostfix + '" style="min-width: 99%;" max="127" value="' + control.value + '"></progress><br />' +
                         '<input style="min-width: 99%;" type="range" min="0" max="127" value="0" oninput="sendEvent({&quot;do&quot;:&quot;changeControlValue&quot;, &quot;channel&quot;: ' + control.channel + ', &quot;cc&quot;: ' + control.cc + ', &quot;value&quot;: parseInt(this.value)});" /></td>';
        document.getElementById('controlsTable').appendChild(trow);
    }
}

function updateControl(event)
{
    var control = event.control;
    var idPostfix = control.channel + '_' + control.cc;
    var pb = document.getElementById('progress_' + idPostfix);
    var italic = document.getElementById('italic_' + idPostfix);
    pb.value = event.newValue;
    italic.innerHTML = 'ch=' + control.channel + ' cc=' + control.cc + ' v=' + event.newValue;
}

function setupWebsocket()
{
    try
    {
        var hostname = location.hostname;
        var protocol = location.protocol;
        var port = location.port;
        var wsProtocol = 'ws';
        if (hostname == '')
        {
            hostname = '192.168.34.129';
            protocol = 'https';
            port = 6124;
        }
        if (protocol.startsWith('https'))
        {
            wsProtocol = 'wss';
        }
        connection = new WebSocket(wsProtocol + '://' + hostname + ':' + port + '/events/');
        
        connection.onopen = function () {
            console.log("Connected to device!");
            // request MIDI access
            if (navigator.requestMIDIAccess)
            {
                navigator.requestMIDIAccess({
                    sysex: false // this defaults to 'false' and we won't be covering sysex in this article.
                }).then(onMIDISuccess, onMIDIFailure);
            } else {
                console.log("No MIDI support in your browser.");
            }
        };
        
        connection.onerror = function (error) {
          console.log(error);
        };

        //Code for handling incoming Websocket messages from the server
        connection.onmessage = function (e) {
            //console.log("Receive: " + e.data);
            var jsonObject = JSON.parse(e.data);
            var event = jsonObject.event;
            if (event == "controlAdded")
            {
                var control = jsonObject.control;
                createControlElement(control);
            } else if (event == 'controlValueChanged') {
                updateControl(jsonObject);
            } else if (event == 'controlRemoved') {
                removeControlElement(jsonObject.control);
            } else if (event == 'midiShortMessage') {
                var marray = jsonObject.data;
                try
                {
                    var outputDevice = localOutputDevices.get(jsonObject.device)
                    if (outputDevice != undefined)
                    {
                        outputDevice.send( new Uint8Array( marray ) );
                    } else {
                        console.log("output device not found");
                    }
                } catch (err_msm) {
                    console.log(err_msm);
                }
            }
        };
        
        connection.onclose = function () {
          console.log('WebSocket connection closed');
          setTimeout(setupWebsocket(), 10000);
        };
    } catch (err) {
        console.log(err);
    }
}

window.onload = function() {
    setupWebsocket();
};

