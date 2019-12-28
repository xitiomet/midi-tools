var localOutputDevices = new Map();
var localInputDevices = new Map();
var connection;
var debugMode = false;
var reconnectTimeout;

// midi functions
function onMIDISuccess(midiAccess) {
    // when we get a succesful response, run this code
    console.log('MIDI Access Object', midiAccess);
    logIt("Scanning for local midi devices");
    if (midiAccess.outputs && midiAccess.outputs.size > 0)
    {
        var outputs = midiAccess.outputs.values();
        for (output = outputs.next(); output && !output.done; output = outputs.next())
        {
            var v = output.value
            localOutputDevices.set(v.name+".output", v);
            logIt("Local Output Device found: " + v.name);
            registerMidiDevice("output", v);
        }
        var inputs = midiAccess.inputs.values();
        for (input = inputs.next(); input && !input.done; input = inputs.next())
        {
            var v = input.value
            localInputDevices.set(v.name+".input", v);
            logIt("Local Input Device found: " + v.name);
            registerMidiDevice("input", v);
        }
    } else {
        console.log("No outputs");
    }
}

function changeView(radio)
{
    if (radio.value == "controls")
    {
        document.getElementById('console').style.display = 'none';
        document.getElementById('controlsTable').style.display = 'table';
        document.getElementById('mappingsTable').style.display = 'none';
        document.getElementById('devicesTable').style.display = 'none';
    } else if (radio.value == "mappings") {
        document.getElementById('console').style.display = 'none';
        document.getElementById('controlsTable').style.display = 'none';
        document.getElementById('mappingsTable').style.display = 'table';
        document.getElementById('devicesTable').style.display = 'none';
    } else if (radio.value == "devices") {
        document.getElementById('console').style.display = 'none';
        document.getElementById('controlsTable').style.display = 'none';
        document.getElementById('mappingsTable').style.display = 'none';
        document.getElementById('devicesTable').style.display = 'table';
    } else if (radio.value == "console") {
        document.getElementById('console').style.display = 'block';
        document.getElementById('controlsTable').style.display = 'none';
        document.getElementById('mappingsTable').style.display = 'none';
        document.getElementById('devicesTable').style.display = 'none';
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
    logIt("No access to MIDI devices or your browser doesn't support WebMIDI API. " + e);
}

function sendEvent(wsEvent)
{
    var out_event = JSON.stringify(wsEvent);
    if (debugMode)
        console.log("Transmit: " + out_event);
    try
    {
        connection.send(out_event);
    } catch (err) {
        console.log(err);
    }
}

function inputMappingElement(element, mappingId)
{
    console.log(mappingId + ": " + element.checked);
    if (element.checked)
    {
        var mm = {"do":"openMapping", "mappingId": mappingId};
        sendEvent(mm);
    } else {
        var mm = {"do":"closeMapping", "mappingId": mappingId};
        sendEvent(mm);
    }
}

function inputDeviceElement(element, deviceName, deviceType)
{
    console.log(deviceName + ": " + element.checked);
    if (element.checked)
    {
        var mm = {"do":"openDevice", "device": deviceName, "type": deviceType};
        sendEvent(mm);
    } else {
        var mm = {"do":"closeDevice", "device": deviceName, "type": deviceType};
        sendEvent(mm);
    }
}

function removeDeviceElement(idx, dev)
{
    var id = "dev_" + dev.name + "." + dev.type;
    var trow = document.getElementById(id);
    document.getElementById('devicesTable').removeChild(trow);
}

function createDeviceElement(idx, dev)
{
    var id = "dev_" + dev.name + "." + dev.type;
    if (document.getElementById(id) == undefined)
    {
        var trow = document.createElement("tr");
        trow.id = id;
        var input_id = 'checkbox' + id;
        var checkedText = '';
        if (dev.opened)
            checkedText = ' checked';
        var direction = "&#10094;&#10095;";
        if (dev.type == 'both')
        {
          direction = "&#10094;&#10095;";
        } else if (dev.type == 'output') {
          direction = "&#10095;&#10095;";
        } else if (dev.type == 'input') {
          direction = "&#10094;&#10094;";
        }
        trow.innerHTML = '<td style="max-width: 100%;"><input type="checkbox" oninput="inputDeviceElement(this,\'' + dev.name + '\',\'' + dev.type + '\')" id="' + input_id + '" value="' + dev.name + '"' + checkedText + ' /><label for="' + input_id + '">' + direction + ' ' + dev.name + '</label></td>';
        document.getElementById('devicesTable').appendChild(trow);
    }
}

function removeMappingElement(idx, mapping)
{
    var id = "mapping_" + mapping.mappingId;
    var trow = document.getElementById(id);
    document.getElementById('mappingsTable').removeChild(trow);
}

function createMappingElement(idx, mapping)
{
    var id = "mapping_" + mapping.mappingId;
    if (document.getElementById(id) == undefined)
    {
        var trow = document.createElement("tr");
        trow.id = id;
        var input_id = 'checkbox' + id;
        var checkedText = '';
        if (mapping.opened)
            checkedText = ' checked';
        trow.innerHTML = '<td style="max-width: 100%;"><input type="checkbox" oninput="inputMappingElement(this,\'' + mapping.mappingId + '\')" id="' + input_id + '" value="' + mapping.mappingId + '"' + checkedText + ' /><label for="' + input_id + '">' + mapping.nickname + '</label></td>';
        document.getElementById('mappingsTable').appendChild(trow);
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
        trow.innerHTML = '<td><b id="nickname_' + idPostfix + '" style="font-size: 18px;">' + control.nickname + '</b><br /><i id="italic_' + idPostfix + '" style="font-size: 10px;">ch=' + control.channel + ' cc=' + control.cc + ' v=' + control.value + '</i></td>' +
                         '<td><progress id="progress_' + idPostfix + '" style="min-width: 99%;" max="127" value="' + control.value + '"></progress><br />' +
                         '<input style="min-width: 99%;" type="range" min="0" max="127" value="0" oninput="sendEvent({&quot;do&quot;:&quot;changeControlValue&quot;, &quot;channel&quot;: ' + control.channel + ', &quot;cc&quot;: ' + control.cc + ', &quot;value&quot;: parseInt(this.value)});" /></td>';
        document.getElementById('controlsTable').appendChild(trow);
    }
}

function logIt(message)
{
    var console = document.getElementById('console');
    var d = new Date();
    var dString = d.toLocaleTimeString();
    if (console.innerHTML != "") console.innerHTML += "<br />";
    console.innerHTML += "&lt;" + dString + "&gt; " + message;
    console.scrollTop = console.scrollHeight;
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
        logIt("Attempting to connect to WebSocket backend");
        var hostname = location.hostname;
        var protocol = location.protocol;
        var port = location.port;
        var wsProtocol = 'ws';
        if (hostname == '')
        {
            debugMode = true;
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
            logIt("Connected to WebSocket backend!");
            // request MIDI access
            if (navigator.requestMIDIAccess)
            {
                logIt("Checking for WebMIDI access");
                navigator.requestMIDIAccess({
                    sysex: false // this defaults to 'false' and we won't be covering sysex in this article.
                }).then(onMIDISuccess, onMIDIFailure);
            } else {
                logIt("No MIDI support in your browser.");
            }
        };
        
        connection.onerror = function (error) {
          logIt("WebSocket error!");
        };

        //Code for handling incoming Websocket messages from the server
        connection.onmessage = function (e) {
            if (debugMode)
                console.log("Receive: " + e.data);
            var jsonObject = JSON.parse(e.data);
            var event = jsonObject.event;
            if (event == "controlAdded")
            {
                var control = jsonObject.control;
                createControlElement(control);
                logIt("Control Added: " + jsonObject.control.nickname);
            } else if (event == 'controlValueChanged') {
                updateControl(jsonObject);
            } else if (event == 'controlRemoved') {
                removeControlElement(jsonObject.control);
                logIt("Control Removed: " + jsonObject.control.nickname);
            } else if (event == 'deviceAdded') {
                createDeviceElement(jsonObject.id, jsonObject.device);
                logIt("Device Added: " + jsonObject.device.name);
            } else if (event == 'deviceRemoved') {
                removeDeviceElement(jsonObject.id, jsonObject.device);
                logIt("Device Removed: " + jsonObject.device.name);
            } else if (event == 'deviceClosed') {
                var dev = jsonObject.device;
                var id = "checkboxdev_" + dev.name + "." + dev.type;
                document.getElementById(id).checked = false;
                logIt("Device Closed: " + dev.name + "." + dev.type);
            } else if (event == 'deviceOpened') {
                var dev = jsonObject.device;
                var id = "checkboxdev_" + dev.name + "." + dev.type;
                document.getElementById(id).checked = true;
                logIt("Device Opened: " + dev.name + "." + dev.type);
            } else if (event == 'mappingAdded') {
                createMappingElement(jsonObject.id, jsonObject.mapping);
                logIt("Mapping Added: " + jsonObject.mapping.nickname);
            } else if (event == 'mappingRemoved') {
                removeMappingElement(jsonObject.id, jsonObject.mapping);
                logIt("Mapping Removed: " + jsonObject.mapping.nickname);
            } else if (event == 'mappingClosed') {
                var mapping = jsonObject.mapping;
                var id = "checkboxmapping_" + mapping.mappingId;
                document.getElementById(id).checked = false;
                logIt("Mapping Closed: " + mapping.nickname);
            } else if (event == 'mappingOpened') {
                var mapping = jsonObject.mapping;
                var id = "checkboxmapping_" + mapping.mappingId;
                document.getElementById(id).checked = true;
                logIt("Mapping Opened: " + mapping.nickname);
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
          logIt('WebSocket connection closed');
          reconnectTimeout = setTimeout(setupWebsocket, 10000);
        };
    } catch (err) {
        console.log(err);
    }
}

window.onload = function() {
    setupWebsocket();
};

