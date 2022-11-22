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

## Screens

![Alt text](previews/first.png?raw=true "First load")
![Alt text](previews/search.png?raw=true "Search")

## Android usage

You will need to add some color definitions to your android project (if using cordova < 11, create a file named `colors.xml` inside `platforms/android/app/src/res/values`)
```xml
<color name="colorPrimary">#008577</color>
<color name="colorPrimaryDark">#00574B</color>
<color name="colorAccent">#D81B60</color>
<color name="page_indicator">#C0202020</color>
<color name="toolbar">#C0202020</color>
```

If you do not have a `colors.xml` file, this is a full example
```xml
<?xml version='1.0' encoding='utf-8'?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <color name="colorPrimary">#008577</color>
    <color name="colorPrimaryDark">#00574B</color>
    <color name="colorAccent">#D81B60</color>
    <color name="page_indicator">#C0202020</color>
    <color name="toolbar">#C0202020</color>
</resources>
```
