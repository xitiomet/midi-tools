var localOutputDevices = [];
var localInputDevices = [];

/*
// request MIDI access
if (navigator.requestMIDIAccess) {
    navigator.requestMIDIAccess({
        sysex: false // this defaults to 'false' and we won't be covering sysex in this article.
    }).then(onMIDISuccess, onMIDIFailure);
} else {
    alert("No MIDI support in your browser.");
}
*/

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
            localOutputDevices.push(v);
            console.log("output Device: " + v.name);
        }
        var inputs = midiAccess.inputs.values();
        var inputsTable = document.getElementById('inputsTable');
        for (input = inputs.next(); input && !input.done; input = inputs.next())
        {
            var v = input.value
            localInputDevices.push(v);
            console.log("input Device: " + v.name);
            /*
            var inputRow = document.createElement("input");
            inputRow.setAttribute("type", "checkbox");
            inputRow.id = "dev_" + v.name;
            inputsTable.appendChild(inputRow);
            */
        }
    } else {
        console.log("No outputs");
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
    var trow = document.createElement("tr");
    trow.id = 'ctrl_' + idPostfix;
    trow.innerHTML = '<td style="max-width: 35%;"><b id="nickname_' + idPostfix + '" style="font-size: 18px;">' + control.nickname + '</b><br /><i id="italic_' + idPostfix + '" style="font-size: 10px;">ch=' + control.channel + ' cc=' + control.cc + ' v=' + control.value + '</i></td>' +
                     '<td style="min-width: 65%; max-width: 85%;"><progress id="progress_' + idPostfix + '" style="min-width: 99%;" max="127" value="' + control.value + '"></td>';
    document.getElementById('controlsTable').appendChild(trow);
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

try
{
    var hostname = location.hostname;
    if (hostname == '')
        hostname = '192.168.34.129';
    var connection = new WebSocket('ws://' + hostname + ':6123/events/');
    connection.onopen = function () {
      console.log("Connected to device!");
    };
    connection.onerror = function (error) {
      console.log(error);
    };

    //Code for handling incoming Websocket messages from the server
    connection.onmessage = function (e) {
        console.log("Receive: " + e.data);
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
        }
    };
    connection.onclose = function () {
      console.log('WebSocket connection closed');
      updateStatus("Connection LOST!", "#FF4444");
    };
} catch (err) {
    console.log(err);
}
