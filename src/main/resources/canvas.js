var connection;
var debugMode = false;
var reconnectTimeout;
var hostname = location.hostname;
var protocol = location.protocol;
var port = location.port;
var wsProtocol = 'ws';
var sounds = new Map();
var httpUrl = '';
var lastImageFile = null;

function getParameterByName(name, url = window.location.href) 
{
    name = name.replace(/[\[\]]/g, '\\$&');
    var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

var canvasName = getParameterByName("canvas");

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

function handleEffects(jsonObject, imageChange)
{
    var imgElement = document.getElementById('imgTag');
    if (jsonObject.hasOwnProperty('scale'))
    {
        imgElement.style.height = (jsonObject.scale * 100)+'%';
        imgElement.style.top = (50-(jsonObject.scale * 50))+'%';
    } else if (imageChange) {
        imgElement.style.height = '100%';
        imgElement.style.top = '0px';
    }
    if (jsonObject.hasOwnProperty('opacity'))
    {
        imgElement.style.opacity = jsonObject.opacity;
    } else if (imageChange) {
        imgElement.style.opacity = 1;
    }
    if (jsonObject.hasOwnProperty('rotate'))
    {
        imgElement.style.transform = 'rotate(' + jsonObject.rotate + 'deg)';
    } else if (imageChange) {
        imgElement.style.transform = 'rotate(0deg)';
    }
}

function updateImage(jsonObject)
{
    var imgUrl = httpUrl + '/assets/' + jsonObject.image;
    var imageChange = (jsonObject.image != lastImageFile);
    var imgElement = document.getElementById('imgTag');
    var showImage = false;
    if (jsonObject.hasOwnProperty('show'))
        showImage = jsonObject.show;
    if (showImage)
    {
        console.log("Show Image: " + jsonObject.image);
        imgElement.src = imgUrl;
        imgElement.style.display = 'inline-block';
        lastImageFile = jsonObject.image;
        handleEffects(jsonObject, imageChange);
    } else if (!imageChange) {
        console.log("Effect Image: " + lastImageFile);
        handleEffects(jsonObject, imageChange);
    }
}

function loadSound(file)
{
    if (!sounds.has(file))
    {
        console.log("loading sound: " + file);
        audio = new Audio(httpUrl + '/assets/' + file);
        sounds.set(file, audio)
    }
}

function playSound(file, volume)
{
    var audio2;
    if (sounds.has(file))
    {
        audio2 = sounds.get(file).cloneNode();
    } else {
        console.log("loading sound: " + file);
        audio2 = new Audio('/assets/' + file);
        sounds.set(file, audio2)
    }
    audio2.volume = volume;
    audio2.play();
}

function startcanvas()
{
    document.getElementById('selectCanvasDiv').style.display = "none";
    canvasName = document.getElementById('selectCanvas').value;
    sendEvent({"identify": canvasName});
}

function setupWebsocket()
{
    try
    {
        if (hostname == '')
        {
            debugMode = true;
            hostname = '127.0.0.1';
            protocol = 'http';
            port = 6123;
            httpUrl = "http://127.0.0.1:6123";
        }
        if (protocol.startsWith('https'))
        {
            wsProtocol = 'wss';
        }
        connection = new WebSocket(wsProtocol + '://' + hostname + ':' + port + '/canvas/');
        
        connection.onopen = function () {
            
        };
        
        connection.onerror = function (error) {
        };

        //Code for handling incoming Websocket messages from the server
        connection.onmessage = function (e) {
            if (debugMode)
                console.log("Receive: " + e.data);
            var jsonObject = JSON.parse(e.data);
            if (jsonObject.hasOwnProperty("canvasList"))
            {
                if (canvasName == null)
                {
                    var canvasSelect = document.getElementById('selectCanvas');
                    var canvasNames = jsonObject.canvasList;
                    if (canvasNames.length > 2)
                    {
                        canvasNames.sort();
                        for (const s of canvasNames) { 
                            if (s != "(ALL)" && s != "(NONE)")
                            {
                                var option = document.createElement("option");
                                option.value = s;
                                option.innerHTML = s;
                                canvasSelect.appendChild(option);
                            }
                        }
                        document.getElementById('selectCanvasDiv').style.display = "block";
                    } else {
                        sendEvent({"identify": "(ALL)"});
                    }
                } else {
                    sendEvent({"identify": canvasName});
                }
            }

            if (jsonObject.hasOwnProperty("projectName"))
            {
                document.getElementById('projectName').innerHTML = "[" + jsonObject.projectName + "]";
            }

            if (jsonObject.hasOwnProperty("sounds"))
            {
                var sounds = jsonObject.sounds;
                for (const s of sounds) { 
                    loadSound(s);
                }
            }

            if (jsonObject.hasOwnProperty("canvas"))
            {
                if (jsonObject.canvas != canvasName && jsonObject.canvas != "(ALL)")
                    return;
            } else if (canvasName != null) {
                return;
            }

            if (jsonObject.hasOwnProperty("image"))
            {
                updateImage(jsonObject);
            }
            if (jsonObject.hasOwnProperty("sound"))
            {
                playSound(jsonObject.sound, jsonObject.volume);
            }
        };
        
        connection.onclose = function () {
          console.log('WebSocket connection closed');
          reconnectTimeout = setTimeout(setupWebsocket, 10000);
        };
    } catch (err) {
        console.log(err);
    }
}

window.onload = function() {
    setupWebsocket();
};

