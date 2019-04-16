//
//  PdfReaderPlugin.swift
//  testPdfReader
//
//  Created by Daniele De Matteo on 21/03/19.
//

import Foundation


@objc(PdfReader) class PdfReader : CDVPlugin {
    @objc(fromBase64:)
    func fromBase64(command: CDVInvokedUrlCommand) {
        
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        
        let base64PDF = command.arguments[0] as? String ?? ""
        let watermark = command.arguments[1] as? String ?? ""
        let showThumbnails = command.arguments[2] as! Bool
        let pdfName = command.arguments[3] as? String ?? ""
        
        if base64PDF.count > 0 {
            if #available(iOS 11.0, *) {
                let pdfController = PdfViewController()
                self.viewController?.present(
                    pdfController,
                    animated: true,
                    completion: {
                        pdfController.pdfBase64 = base64PDF
                        pdfController.buildThumbnails = showThumbnails
                        pdfController.pdfName = pdfName
                        if watermark.count > 0 {
                            pdfController.watermark = watermark
                        }
                }
                )
            } else {
                // Fallback on earlier versions
                
                // todo propagate error result
            }
            
            pluginResult = CDVPluginResult(
                status: CDVCommandStatus_OK
            )
        }
        
        self.commandDelegate!.send(
            pluginResult,
            callbackId: command.callbackId
        )
    }
}
