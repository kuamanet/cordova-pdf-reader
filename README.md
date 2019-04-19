# Cordova PDF reader
This plugin allows to show, search and watermark a base64 encoded pdf.
It leverages swift's [PDFKit](https://developer.apple.com/documentation/pdfkit) and [mupdf](https://mupdf.com/) android porting to show and search the pdf.
## Installation
```
cordova plugin add net-kuama-plugins-pdfreader
```

## Supported Platform
- Android
- iOS

## Usage

```javascript

function fetchLocal(url) {
    return new Promise(function (resolve, reject) {
        var xhr = new XMLHttpRequest
        xhr.onload = function () {
            resolve(new Response(xhr.responseText, {status: xhr.status}))
        }
        xhr.onerror = function () {
            reject(new TypeError('Local request failed'))
        }
        xhr.open('GET', url)
        xhr.send(null)
    })
}

var app = {
    // Application Constructor
    initialize: function () {
        document.addEventListener('deviceready', this.onDeviceReady.bind(this), false);
    },

    // deviceready Event Handler

    onDeviceReady: function () {
        app.receivedEvent('deviceready');
        const promisedBase64 =
            cordova.platformId === 'android' ? fetchLocal('res.json') : fetch('res.json');

        promisedBase64
            .then(resp => resp.json())
            .then(res => {
                KPdfReader.fromBase64(
                    res.rawData, // the base 64 pdf
                    {
                        watermark: "A nice watermark",
                        fileName: "Test file pdf" // the title of the pdf
                    },
                )
                    .then(() => {
                        // pdf was closed
                    })
                    .catch(console.error);
            });
    },

    // Update DOM on a Received Event
    receivedEvent: function (id) {
        var parentElement = document.getElementById(id);
        var listeningElement = parentElement.querySelector('.listening');
        var receivedElement = parentElement.querySelector('.received');

        listeningElement.setAttribute('style', 'display:none;');
        receivedElement.setAttribute('style', 'display:block;');

        console.log('Received Event: ' + id);
    }
};

app.initialize();

```

### Preview
