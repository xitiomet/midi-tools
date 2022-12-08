var connection;
var debugMode = false;
var reconnectTimeout;
var hostname = location.hostname;
var protocol = location.protocol;
var port = location.port;
var wsProtocol = 'ws';
var sounds = new Map();
var images = new Map();
var httpUrl = '';
var canvasStarted = false;

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

function handleEffects(jsonObject)
{
    var imgElement = document.getElementById('img_' + jsonObject.image);
    if (imgElement.style.display == 'none')
    {
        //console.log("bringing image visible: " + jsonObject.image);
        imgElement.style.display = 'inline-block';
    }
    if (jsonObject.hasOwnProperty('solo'))
    {
        if (jsonObject.solo)
            hideImagesExcept(jsonObject.image);
    }
    if (jsonObject.hasOwnProperty('none'))
    {
        imgElement.style.height = '100%';
        imgElement.style.width = 'auto';
        imgElement.style.top = '0px';
        imgElement.style.left = "50%;"
        imgElement.style.transform = 'translateX(-50%) rotate(0deg)';
        imgElement.style.opacity = 1;
    } else {
        if (jsonObject.hasOwnProperty('curtain'))
        {
            imgElement.style.top = '-' + (jsonObject.curtain * 100) + '%';
            imgElement.style.height = '100%';
            imgElement.style.width = '100%';
            imgElement.style.transform = 'translateX(-50%) rotate(0deg)';
        }
        if (jsonObject.hasOwnProperty('scale')) 
        {
            imgElement.style.height = (jsonObject.scale * 100)+'%';
            imgElement.style.top = (50-(jsonObject.scale * 50))+'%';
            imgElement.style.width = 'auto';
        }
        if (jsonObject.hasOwnProperty('opacity'))
        {
            imgElement.style.opacity = jsonObject.opacity;
        }

        if (jsonObject.hasOwnProperty('rotate'))
        {
            imgElement.style.transform = 'translateX(-50%) rotate(' + jsonObject.rotate + 'deg)';
        }
    }
}

function updateImage(jsonObject)
{
    //console.log("Show/effect Image: " + jsonObject.image);
    handleEffects(jsonObject);
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

function loadImage(file)
{
    var nextZ = 0;
    var existingImg = document.getElementById('img_' + file);
    if (existingImg == null)
    {
        console.log("loading Image: " + file);
        var img = document.createElement("img");
        img.id = 'img_' + file;
        img.src = httpUrl + '/assets/' + file;
        img.style.display = "none";
        img.style.width = "auto"; 
        img.style.height = "100%";
        img.style.maxWidth = "100%";
        img.style.align = "center";
        img.style.position = "absolute";
        img.style.top = "0px";
        img.style.left = "50%;"
        img.style.transform = "translateX(-50%)";
        img.style.zIndex = nextZ;
        nextZ++;
        images.set(file, img);
        document.getElementById('bodyId').appendChild(img);
    }
}

function hideImagesExcept(file)
{
    for (let [key, value] of images)
    {
        if (key != file)
        {
            value.style.display = 'none';
        }
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
    canvasName = document.getElementById('selectCanvas').value;
    console.log("Starting canvas: " + canvasName);
    document.getElementById('selectCanvasDiv').style.display = "none";
    sendEvent({"identify": canvasName});
    canvasStarted = true;
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
            {
                console.log("Receive: " + e.data);
            }
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
                        for (const s of canvasNames)
                        { 
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
                        canvasStarted = true;
                    }
                } else {
                    sendEvent({"identify": canvasName});
                    canvasStarted = true;
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

            if (jsonObject.hasOwnProperty("images"))
            {
                var images = jsonObject.images;
                for (const i of images) { 
                    loadImage(i);
                }
            }

            if (jsonObject.hasOwnProperty("canvas"))
            {
                if (jsonObject.canvas != canvasName && jsonObject.canvas != "(ALL)")
                    return;
            } else if (canvasName != null) {
                return;
            }

            if (canvasStarted)
            {
                if (jsonObject.hasOwnProperty("image"))
                {
                    updateImage(jsonObject);
                }
                if (jsonObject.hasOwnProperty("sound"))
                {
                    playSound(jsonObject.sound, jsonObject.volume);
                }
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

