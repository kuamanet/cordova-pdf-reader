import UIKit
import PDFKit
import SnapKit

// TODOS pdf pages (maybe when thumbnails are disabled) with
// TODOS parametrize colors
// TODOS parametrize labels
@available(iOS 11.0, *)
class PdfViewController: UIViewController, PDFDocumentDelegate {
    
    var backgroundColor = #colorLiteral(red: 0.2549019754, green: 0.2745098174, blue: 0.3019607961, alpha: 1)
    
    var accentColor = #colorLiteral(red: 0.462745098, green: 0.7215686275, blue: 0.1568627451, alpha: 1)
    
    var iconInHeaderColor = UIColor.white
    
    var pdfNameColor = UIColor.white
    
    var noResultsString = "Nessun risultato"
    
    var betweenCurrentIndexAndTotalString = "di"

    var pdfDocument: PDFDocument?

    var pdfView: NonSelectablePDFView?

    var thumbnailsView: PDFThumbnailView?
    
    let searchView = UIView()
    let searchInput = UITextField()
    
    let pdfNameLabel = UILabel()
    
    var pdfName: String? {
        didSet {
            pdfNameLabel.text = pdfName
            pdfNameLabel.sizeToFit()
        }
    }

    var pdfBase64: String? {
        didSet {
            if let base64 = pdfBase64{
                showPdf(fromBase46String: base64)
            }
        }
    }

    var watermark: String? {
        didSet {
            if let w = watermark {
                WatermarkPage.watermark = w
            }
        }
    }
    
    var currentSearchResultLabel = UILabel()
    
    var currentSearchResultIndex = 0 {
        didSet {
            fillTextualSearchInfos()
            goToCurrentSelection()
        }
    }
    
    var totalSearchResultsLabel = UILabel()
    
    var currentSearchSelection = [PDFSelection]()  {
        didSet {
            fillTextualSearchInfos()
        }
    }
    
    let textualSearchInfos = UILabel()

    var buildThumbnails: Bool? {
        didSet {
            buildThumbnailsIfRequired()
        }
    }
    
    var goToPrevSearchResultBtn = UIButton()
    
    struct CurrentPDFAnnotation {
        let annotation:PDFAnnotation
        let page:PDFPage
    }
    
    var currentPdfAnnotation:CurrentPDFAnnotation?
    
    func fillTextualSearchInfos() {
        if(currentSearchSelection.count == 0) {
            textualSearchInfos.text = noResultsString
        } else {
            textualSearchInfos.text = "\(currentSearchResultIndex + 1) \(betweenCurrentIndexAndTotalString) \(currentSearchSelection.count)"
        }
        
        textualSearchInfos.sizeToFit()
    
        textualSearchInfos.snp.updateConstraints { make in
            make.width.equalTo(textualSearchInfos.frame.size.width)
            make.centerY.equalToSuperview()
            make.trailing.equalTo(goToPrevSearchResultBtn.snp.leading).offset(-10)
        }
        
        searchInput.snp.updateConstraints { make in
            make.trailing.equalTo(textualSearchInfos.snp.leading).offset(-10)
        }
        
        searchView.layoutSubviews()
    }

    /// Shows the pdf
    /// - Parameters:
    ///     - base64Data: the *base64* rapresentation of a pdf
    func showPdf(fromBase46String base64Data:String) -> Void {

        if let res = Data(base64Encoded: base64Data) {

            if let pdfDocument = PDFDocument(data: res) {
                let pdfView = NonSelectablePDFView(frame: self.view.frame)
                pdfView.displayMode = .singlePageContinuous
                pdfView.autoScales = true
                pdfView.displayDirection = .horizontal
                pdfView.usePageViewController(true, withViewOptions: [:])
                pdfView.displayMode = .singlePageContinuous
                pdfDocument.delegate = self
                pdfView.document = pdfDocument

                self.pdfView = pdfView
                self.pdfView?.backgroundColor = backgroundColor

                self.view.addSubview(pdfView)
                self.pdfDocument = pdfDocument

                buildHeader()
            } else {
                print("Could not create pdf document from data")
            }

        } else {
            print("Could not parse data")
        }

        self.view.backgroundColor = backgroundColor
    }

    func classForPage() -> AnyClass {
        return WatermarkPage.self
    }

    
    /// If not build yet (and not disabled by the user configs), shows a list of thumbnails at the bottom of the view
    func buildThumbnailsIfRequired() {
        guard let doBuildThumbnails = buildThumbnails else {
            return
        }

        if doBuildThumbnails == false {
            return
        }

        if thumbnailsView != nil {
            return
        }

        if pdfView == nil {
            return
        }



        self.pdfView!.translatesAutoresizingMaskIntoConstraints = false

        self.pdfView!.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor).isActive = true
        self.pdfView!.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor).isActive = true
        // self.pdfView!.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor).isActive = true


        let thumbnailView = PDFThumbnailView()
        thumbnailView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(thumbnailView)

        thumbnailView.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor).isActive = true
        thumbnailView.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor).isActive = true
        thumbnailView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor).isActive = true

        pdfView!.bottomAnchor.constraint(equalTo: thumbnailView.topAnchor).isActive = true
        thumbnailView.heightAnchor.constraint(equalToConstant: 120).isActive = true

        thumbnailView.thumbnailSize = CGSize(width: 100, height: 100)
        thumbnailView.layoutMode = .horizontal

        thumbnailView.pdfView = pdfView!

    }
    
}

///
/// Header builder
///
@available(iOS 11.0, *)
extension PdfViewController {
    
    /// Builds the view header.
    /// It contains the name of the rendered pdf file, and a search field
    ///
    /// When search is not active is rendered like this:
    /// ```
    ///  --------------------------------------------------
    /// |< | The provided name of the pdf            | 🔍 |
    ///  --------------------------------------------------
    /// ```
    ///
    /// When the user taps on the search icon:
    /// ```
    ///  --------------------------------------------------
    /// |< | Search somenthing..                          |
    ///  --------------------------------------------------
    /// ```
    ///
    /// When the user types and his search matches something
    /// ```
    ///  --------------------------------------------------
    /// |< | An awesome search             | 1 of X | < > |
    ///  --------------------------------------------------
    /// ```
    ///
    /// When the user types and his search matches nothing
    /// ```
    ///  --------------------------------------------------
    /// |< | An awesome search             | no results   |
    ///  --------------------------------------------------
    /// ```
    func buildHeader() {
        
        let headerView = UIView()
        
        headerView.translatesAutoresizingMaskIntoConstraints = false
        self.view.addSubview(headerView)
        
        headerView.snp.makeConstraints { (make) -> Void in
            make.height.equalTo(60)
            make.top.equalTo(self.view.safeAreaLayoutGuide.snp.top)
            make.width.equalTo(self.view)
        }
        
        headerView.backgroundColor = backgroundColor
        
        // back button, center left of the header, when tapped finishes the controller
        if let image = UIImage(named: "baseline_arrow_back_black_24pt.png") {
            let button = UIButton(type: .custom)
            button.setImage(image.withRenderingMode(.alwaysTemplate), for: .normal)
            button.tintColor = iconInHeaderColor
            button.imageView?.tintColor = iconInHeaderColor
            
            headerView.addSubview(button)
            
            button.snp.makeConstraints{ make in
                
                make.centerY.equalTo(headerView)
                make.leading.equalTo(headerView).offset(20)
                make.width.equalTo(40)
            }
            
            button.addTarget(self, action: #selector(onBackBtnTap), for: .touchUpInside)
            
            headerView.addSubview(pdfNameLabel)
            
            pdfNameLabel.snp.makeConstraints { make in
                
                make.centerY.equalTo(headerView)
                make.leading.equalTo(button.snp.trailing).offset(20)
                make.height.equalTo(20)
            }
            
            pdfNameLabel.textColor = pdfNameColor
            pdfNameLabel.font = pdfNameLabel.font?.withSize(20)
            pdfNameLabel.backgroundColor = UIColor.clear
            
        }
        
        // search btn, center right of the header, when tapped shows the search bar
        if let image = UIImage(named: "baseline_search_white_24pt") {
            let button = UIButton(type: .custom)
            button.setImage(image.withRenderingMode(.alwaysTemplate), for: .normal)
            button.tintColor = UIColor.white
            button.imageView?.tintColor = UIColor.white
            
            headerView.addSubview(button)
            
            button.snp.makeConstraints{ make in
                
                make.centerY.equalTo(headerView)
                make.trailing.equalTo(headerView).offset(-20)
                make.width.equalTo(40)
            }
            
            button.addTarget(self, action: #selector(onSearchBtnTap), for: .touchUpInside)
            pdfNameLabel.snp.makeConstraints { make in
                
                make.centerY.equalTo(headerView)
                make.trailing.equalTo(button.snp.leading).offset(20)
                
            }
        }
        
        // Search View Structure:
        //  --------------------------------------------------
        // |< | Search somenthing..           | 1 of X | < > |
        //  --------------------------------------------------
        
        
        // Or...
        //  --------------------------------------------------
        // |< | Searching something!          | no results ! |
        //  --------------------------------------------------
        searchView.backgroundColor = #colorLiteral(red: 1, green: 1, blue: 1, alpha: 1)
        
        headerView.addSubview(searchView)
        
        // same width as parent, but translated of the 100% on the right
        searchView.snp.makeConstraints { make in
            make.top.equalTo(headerView)
            make.left.equalTo(headerView)
            make.bottom.equalTo(headerView)
            make.right.equalTo(headerView)
        }
        
        searchView.alpha = 0
        searchView.transform = CGAffineTransform(translationX: headerView.frame.width, y: 0)
        
        // search btn, center right of the header, when tapped shows the search bar
        if let image = UIImage(named: "baseline_arrow_back_black_24pt.png") {
            let button = UIButton(type: .custom)
            button.setImage(image.withRenderingMode(.alwaysTemplate), for: .normal)
            button.tintColor = accentColor
            button.imageView?.tintColor = accentColor
            
            searchView.addSubview(button)
            
            button.snp.makeConstraints{ make in
                
                make.centerY.equalTo(headerView)
                make.leading.equalTo(headerView).offset(20)
                make.width.equalTo(40)
            }
            
            button.addTarget(self, action: #selector(onCloseSearchBtnTap), for: .touchUpInside)
            
            searchView.addSubview(searchInput)
            searchInput.snp.makeConstraints { make in
                make.leading.equalTo(button.snp.trailing).offset(10)
                make.top.equalTo(searchView).offset(5)
                make.bottom.equalTo(searchView).offset(5)
            }
            searchInput.placeholder = "Ricerca..."
            searchInput.addTarget(self, action: #selector(onUserSearch), for: .editingChanged)
            
            // Search controls
            let image = UIImage(named: "baseline_keyboard_arrow_right_black_24pt")!
            
            let goToNextSearchResultBtn = UIButton(type: .custom)
            goToNextSearchResultBtn.setImage(image.withRenderingMode(.alwaysTemplate), for: .normal)
            goToNextSearchResultBtn.tintColor = backgroundColor
            goToNextSearchResultBtn.imageView?.tintColor = backgroundColor
            
            searchView.addSubview(goToNextSearchResultBtn)
            
            goToNextSearchResultBtn.snp.makeConstraints{ make in
                
                make.centerY.equalToSuperview()
                make.trailing.equalToSuperview().offset(-20)
                make.width.equalTo(40)
            }
            
            goToNextSearchResultBtn.addTarget(self, action: #selector(onGoToNextSelectionTap), for: .touchUpInside)
            
            let prevArrowImage = UIImage(named: "baseline_keyboard_arrow_left_black_24pt")!
            
            goToPrevSearchResultBtn = UIButton(type: .custom)
            goToPrevSearchResultBtn.setImage(prevArrowImage.withRenderingMode(.alwaysTemplate), for: .normal)
            goToPrevSearchResultBtn.tintColor = backgroundColor
            goToPrevSearchResultBtn.imageView?.tintColor = backgroundColor
            
            searchView.addSubview(goToPrevSearchResultBtn)
            
            goToPrevSearchResultBtn.snp.makeConstraints{ make in
                
                make.centerY.equalToSuperview()
                make.trailing.equalTo(goToNextSearchResultBtn.snp.leading).offset(-5)
                make.width.equalTo(40)
            }
            
            goToPrevSearchResultBtn.addTarget(self, action: #selector(onGoToPrevSelectionTap), for: .touchUpInside)
            
            searchView.addSubview(textualSearchInfos)
            
            textualSearchInfos.snp.makeConstraints { make in
                print(textualSearchInfos.frame.size.width)
                make.width.equalTo(textualSearchInfos.frame.size.width)
                make.centerY.equalToSuperview()
                make.trailing.equalTo(goToPrevSearchResultBtn.snp.leading).offset(-10)
            }
            
            searchInput.snp.makeConstraints { make in
                make.trailing.equalTo(textualSearchInfos.snp.leading).offset(-10)
            }
            
        }
        
        // give the header some elevation
        headerView.elevate(elevation: 5.0)
        
        
        // keep the pdf view under the header
        self.pdfView!.snp.makeConstraints { make in
            make.top.equalTo(headerView.snp.bottom)
        }
        
    }
}


///
/// User interaction handlers
///
@available(iOS 11.0, *)
extension PdfViewController {
    
    //
    // When the back arrow on the top left gets tapped, dismiss
    //
    @objc func onBackBtnTap() {
        dismiss(animated: true, completion: nil)
    }
    
    ///
    /// When the search icon on the top right gets tapped, show the search bar
    ///
    @objc func onSearchBtnTap() {
        UIView.animate(withDuration: 0.3, animations: {
            self.searchView.alpha = 1
            self.searchView.transform = CGAffineTransform.identity;
            self.searchInput.becomeFirstResponder()
        });
    }
    
    @objc func onCloseSearchBtnTap() {
        searchInput.resignFirstResponder()
        searchInput.text = ""
        clearSelection()
        UIView.animate(withDuration: 0.3, animations: {
            self.searchView.alpha = 0
            self.searchView.transform = CGAffineTransform(translationX: self.searchView.frame.width, y: 0);
        });
    }
    
    @objc func onUserSearch() {
        clearSelection()
        if let term = searchInput.text, let pdfDoc = pdfDocument {
            currentSearchSelection = pdfDoc.findString(term, withOptions: [.caseInsensitive])
            currentSearchSelection.forEach { selection in
                selection.pages.forEach { page in
                    
                    let highlight = PDFAnnotation(bounds: selection.bounds(for: page), forType: .highlight, withProperties: nil)
                    highlight.endLineStyle = .square
                    highlight.color = UIColor.orange.withAlphaComponent(0.5)
                    page.addAnnotation(highlight)
                }
            }
            
            goToCurrentSelection()
        }
    }
    
    @objc func onGoToPrevSelectionTap() {
        if currentSearchResultIndex > 0 {
            currentSearchResultIndex -= 1
        }
    }
    
    @objc func onGoToNextSelectionTap() {
        if currentSearchResultIndex < currentSearchSelection.count - 1 {
            currentSearchResultIndex += 1
        }
    }
    
    func goToCurrentSelection() {
        if currentSearchSelection.count == 0 {
            return
        }
        
        if let currentHighlight = currentPdfAnnotation {
            currentHighlight.page.removeAnnotation(currentHighlight.annotation)
        }
        
        let selection = currentSearchSelection[currentSearchResultIndex]
        // FWI tryed to print it during development, its pages count is always 1
        pdfView?.go(to: selection)
        
        if let page = selection.pages.first {
         
            let highlight = PDFAnnotation(bounds: selection.bounds(for: page), forType: .highlight, withProperties: nil)
            highlight.endLineStyle = .square
            highlight.color = UIColor.orange.withAlphaComponent(0.9)
            page.addAnnotation(highlight)
            
            currentPdfAnnotation = CurrentPDFAnnotation(annotation: highlight, page: page)
            
        }
    }
    
    /// Resets the search related parameters
    func clearSelection() {
        currentSearchResultIndex = 0
        
        if let pdfDoc = pdfDocument {
            var index = 0
            while (index < pdfDoc.pageCount) {
                pdfDoc.page(at: index)?.annotations.forEach{pdfDoc.page(at: index)?.removeAnnotation($0)}
                index += 1
            }
        }
    }
}
