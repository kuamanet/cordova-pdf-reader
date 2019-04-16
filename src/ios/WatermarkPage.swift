//
//  WatermarkPage.swift
//  testPdfReader
//
//  Created by Daniele De Matteo on 21/03/19.
//
import PDFKit

@available(iOS 11.0, *)
class WatermarkPage: PDFPage {

    static var watermark:String = ""

    private static let attributes = [
        NSFontAttributeName: UIFont.boldSystemFont(ofSize: 34),
        NSForegroundColorAttributeName:UIColor(red: 0.5, green: 0.5, blue: 0.5, alpha: 0.2)
    ]

    private static var pdfBounds:CGRect?


    override func draw(with box: PDFDisplayBox, to context: CGContext) {

        // Draw original content
        super.draw(with: box, to: context)

        // Draw rotated overlay string
        UIGraphicsPushContext(context)
        context.saveGState()

        let pageBounds = self.bounds(for: box)
        WatermarkPage.pdfBounds = pageBounds
        context.translateBy(x: 0.0, y: pageBounds.size.height)
        context.scaleBy(x: 1.0, y: -1.0)
        context.rotate(by: CGFloat.pi / 4.0)

        let fontName = UIFont.systemFont(ofSize: 20).familyName

        // WatermarkPage.watermark.draw(at: CGPoint(x:0, y:0), withAttributes: WatermarkPage.attributes)

        let watermarkSize = CGSize(width: 200, height: 50)
        let titleAttributes = [
            NSFontAttributeName: UIFont(
                named: fontName,
                fitting: WatermarkPage.watermark,
                into: watermarkSize,
                with: [
                    NSFontAttributeName: UIFont.boldSystemFont(ofSize: 50),
                ],
                options: NSStringDrawingOptions.usesLineFragmentOrigin
                )!,
            NSForegroundColorAttributeName:UIColor(red: 0.5, green: 0.5, blue: 0.5, alpha: 0.2)
        ]

        var wX = pageBounds.width * -1.5
        var wY = pageBounds.height * -1.5

        while(wY < pageBounds.height) {
            while (wX < pageBounds.width) {

                (WatermarkPage.watermark as NSString).draw(with: CGRect(x: wX, y: wY, width: watermarkSize.width, height: watermarkSize.height),
                                                           options: .usesLineFragmentOrigin,
                                                           attributes: titleAttributes,
                                                           context: nil)

                wX += watermarkSize.width + 20
            }
            wX = 0
            wY += watermarkSize.height + 20
        }


        context.restoreGState()
        UIGraphicsPopContext()

    }
}

extension UIFont {
    convenience init?(named fontName: String, fitting text: String, into targetSize: CGSize, with attributes: [String: Any], options: NSStringDrawingOptions) {
        var attributes = attributes
        let fontSize = targetSize.height

        attributes[NSFontAttributeName] = UIFont(name: fontName, size: fontSize)
        let size = text.boundingRect(with: CGSize(width: .greatestFiniteMagnitude, height: fontSize),
                                     options: options,
                                     attributes: attributes,
                                     context: nil).size

        let heightSize = targetSize.height / (size.height / fontSize)
        let widthSize = targetSize.width / (size.width / fontSize)

        self.init(name: fontName, size: min(heightSize, widthSize))
    }
}
