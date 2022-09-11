var connection;
var debugMode = false;
var reconnectTimeout;
var hostname = location.hostname;
var protocol = location.protocol;
var port = location.port;
var wsProtocol = 'ws';
        
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

function updateImage(imageName, opacity)
{
    var imgUrl = '/assets/' + imageName;
    var imgElement = document.getElementById('imgTag');
    imgElement.src = imgUrl;
    imgElement.style.display = 'inline-block';
    imgElement.style.opacity = (opacity / 100);
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
            if (jsonObject.hasOwnProperty("image"))
            {
                updateImage(jsonObject.image, jsonObject.opacity);
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

