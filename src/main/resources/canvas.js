var connection;
var debugMode = false;
var reconnectTimeout;
var hostname = location.hostname;
var protocol = location.protocol;
var port = location.port;
var wsProtocol = 'ws';
var sounds = new Map();
var images = new Map();
var imageEffects = new Map();
var httpUrl = '';
var canvasStarted = false;
var nextZ = 0;

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
    var imageName = jsonObject.image;
    var imgElement = document.getElementById('img_' + imageName);
    var fill = 'y';
    var height = '100%';
    var width = '100%';
    var left = '50%';
    var top = '50%';
    var rotate = 0;
    var opacity = 1;
    if (imageEffects.has(imageName))
    {
        var ie = imageEffects.get(imageName);
        height = ie.height;
        width = ie.width;
        rotate = ie.rotate;
        left = ie.left;
        top = ie.top;
        opacity = ie.opacity;
    }
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
    if (jsonObject.hasOwnProperty('fill'))
    {
        fill = jsonObject.fill;
    }
    
    if (jsonObject.hasOwnProperty('none'))
    {
        top = '50%';
        left = "50%;"
        rotate = 0;
        imgElement.style.opacity = 1;
    } else {
        if (jsonObject.hasOwnProperty('curtain'))
        {
            top = ((-100 + (jsonObject.curtain * 100))+50) + '%';
            rotate = 0;
        }
        if (jsonObject.hasOwnProperty('riser'))
        {
            top = (150 - (jsonObject.riser * 100)) + '%';
            rotate = 0;
        }
        if (jsonObject.hasOwnProperty('scale')) 
        {
            if (fill == 'y')
            {
                height = (jsonObject.scale * 100)+'%';
            } else {
                width = (jsonObject.scale * 100)+'%';
            }
        }
        if (jsonObject.hasOwnProperty('opacity'))
        {
            opacity = jsonObject.opacity;
        }
        if (jsonObject.hasOwnProperty('rotate'))
        {
            rotate = jsonObject.rotate;
        }
    }
    if (fill == 'x')
    {
        imgElement.style.height = 'auto';
        imgElement.style.width = width;
    } else if (fill == 'y') {
        imgElement.style.height = height;
        imgElement.style.width = 'auto';
    }
    imgElement.left = left;
    imgElement.top = top;
    imgElement.style.opacity = opacity;
    imgElement.style.transform = 'translateX(-50%) translateY(-50%) rotate(' + rotate + 'deg)';
    imageEffects.set(imageName, {"width": width, "height": height, "rotate": rotate, "left": left, "top": top, "opacity": opacity});
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
        img.style.top = "50%";
        img.style.left = "50%;"
        img.style.transform = "translateX(-50%) translateY(-50%)";
        img.style.zIndex = nextZ;
        nextZ++;
        images.set(file, img);
        imageEffects.set(file, {"width": "auto", "height": "100%", "rotate": 0, "left": "50%", "top": "50%", "opacity": 1});
        document.getElementById('bodyId').appendChild(img);
    }
}

function hideAllImages()
{
    for (let [key, value] of images)
    {
        value.style.display = 'none';
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
            document.getElementById('connectingDiv').style.display = 'none';
        };
        
        connection.onerror = function (error) {
            document.getElementById('connectingDiv').style.display = 'inline-block';
            document.getElementById('selectCanvasDiv').style.display = 'none';
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
                var canvasNames = jsonObject.canvasList;
                if (canvasName == null || !canvasNames.includes(canvasName))
                {
                    var canvasSelect = document.getElementById('selectCanvas');
                    canvasSelect.innerHTML = "";
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
                        hideAllImages();
                        document.getElementById('selectCanvasDiv').style.display = "block";
                    } else {
                        sendEvent({"identify": "(ALL)"});
                        canvasStarted = true;
                        document.getElementById('selectCanvasDiv').style.display = "none";
                    }
                } else {
                    sendEvent({"identify": canvasName});
                    canvasStarted = true;
                    document.getElementById('selectCanvasDiv').style.display = "none";
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
          document.getElementById('connectingDiv').style.display = 'inline-block';
          document.getElementById('selectCanvasDiv').style.display = 'none';
          reconnectTimeout = setTimeout(setupWebsocket, 10000);
        };
    } catch (err) {
        console.log(err);
    }
}

window.onload = function() {
    setupWebsocket();
};

