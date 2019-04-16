//
//  NonSelectablePdfView.swift
//  testPdfReader
//
//  Created by Daniele De Matteo on 21/03/19.
//
import PDFKit
@available(iOS 11.0, *)
class NonSelectablePDFView: PDFView {
    
    override func addGestureRecognizer(_ gestureRecognizer: UIGestureRecognizer) {
        (gestureRecognizer as? UILongPressGestureRecognizer)?.isEnabled = false
        super.addGestureRecognizer(gestureRecognizer)
    }
    
    override func canPerformAction(_ action: Selector, withSender sender: Any?) -> Bool {
        return false
    }
    
}
