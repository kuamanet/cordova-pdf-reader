//
//  MaterialView.swift
//  testPdfReader
//
//  Created by Daniele De Matteo on 21/03/19.
//

import UIKit

// MaterialView Credits go to https://medium.com/material-design-for-ios/part-1-elevation-e48ff795c693
protocol MaterialView {
    func elevate(elevation: Double)
}

extension UIView: MaterialView {
    func elevate(elevation: Double) {
        self.layer.masksToBounds = false
        self.layer.shadowColor = UIColor.black.cgColor
        self.layer.shadowOffset = CGSize(width: 0, height: elevation)
        self.layer.shadowRadius = abs(CGFloat(elevation))
        self.layer.shadowOpacity = 0.24
    }
}

