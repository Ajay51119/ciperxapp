// ios/CipherKeyboardExtension/KeyboardViewController.swift
import UIKit

class KeyboardViewController: UIInputViewController {

    var cipherMap: [String: String] = [:]
    var reverseMap: [String: String] = [:]

    override func viewDidLoad() {
        super.viewDidLoad()
        loadCipher()
        setupUI()
    }

    func loadCipher() {
        // Read from App Group shared UserDefaults (set up App Group in Xcode)
        let defaults = UserDefaults(suiteName: "group.com.cipherkeyapp")
        guard let mapData = defaults?.data(forKey: "cipher_map"),
              let map = try? JSONSerialization.jsonObject(with: mapData) as? [String: String]
        else { return }

        cipherMap = map
        reverseMap = [:]
        map.forEach { key, value in
            reverseMap[value.lowercased()] = key.uppercased()
        }
    }

    func encode(_ text: String) -> String {
        return text.unicodeScalars
            .map { ch -> String in
                let up = String(ch).uppercased()
                if let coded = cipherMap[up] { return coded }
                if ch == " " { return "|" }
                return String(ch)
            }
            .joined(separator: "·")
    }

    func decode(_ text: String) -> String {
        let tokens = text.split(separator: "·", omittingEmptySubsequences: false)
        return tokens.map { tok -> String in
            if tok == "|" { return " " }
            if let letter = reverseMap[tok.lowercased()] { return letter }
            return String(tok)
        }.joined()
    }

    func setupUI() {
        view.backgroundColor = UIColor(red: 0.11, green: 0.11, blue: 0.11, alpha: 1)

        // Logo bar
        let logoBar = UIView()
        logoBar.backgroundColor = UIColor(red: 0.15, green: 0.15, blue: 0.15, alpha: 1)
        logoBar.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(logoBar)

        let logoBtn = UIButton(type: .system)
        logoBtn.setTitle("Cφ", for: .normal)
        logoBtn.backgroundColor = UIColor(red: 0.11, green: 0.73, blue: 0.33, alpha: 1)
        logoBtn.setTitleColor(.black, for: .normal)
        logoBtn.titleLabel?.font = .boldSystemFont(ofSize: 14)
        logoBtn.layer.cornerRadius = 8
        logoBtn.translatesAutoresizingMaskIntoConstraints = false
        logoBtn.addTarget(self, action: #selector(tappedLogo), for: .touchUpInside)
        logoBar.addSubview(logoBtn)

        let labelView = UILabel()
        labelView.text = "Tap Cφ to encode selected text"
        labelView.textColor = .lightGray
        labelView.font = .systemFont(ofSize: 12)
        labelView.translatesAutoresizingMaskIntoConstraints = false
        logoBar.addSubview(labelView)

        // Input field
        let inputField = UITextField()
        inputField.backgroundColor = UIColor(red: 0.08, green: 0.08, blue: 0.08, alpha: 1)
        inputField.textColor = .white
        inputField.attributedPlaceholder = NSAttributedString(
            string: "Type to encode...",
            attributes: [.foregroundColor: UIColor.darkGray]
        )
        inputField.layer.cornerRadius = 8
        inputField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: 0))
        inputField.leftViewMode = .always
        inputField.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(inputField)

        NSLayoutConstraint.activate([
            logoBar.topAnchor.constraint(equalTo: view.topAnchor),
            logoBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            logoBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            logoBar.heightAnchor.constraint(equalToConstant: 44),

            logoBtn.leadingAnchor.constraint(equalTo: logoBar.leadingAnchor, constant: 10),
            logoBtn.centerYAnchor.constraint(equalTo: logoBar.centerYAnchor),
            logoBtn.widthAnchor.constraint(equalToConstant: 36),
            logoBtn.heightAnchor.constraint(equalToConstant: 36),

            labelView.leadingAnchor.constraint(equalTo: logoBtn.trailingAnchor, constant: 10),
            labelView.centerYAnchor.constraint(equalTo: logoBar.centerYAnchor),

            inputField.topAnchor.constraint(equalTo: logoBar.bottomAnchor, constant: 8),
            inputField.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 10),
            inputField.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -10),
            inputField.heightAnchor.constraint(equalToConstant: 40),
        ])
    }

    @objc func tappedLogo() {
        guard let proxy = textDocumentProxy as? UITextDocumentProxy else { return }
        // Get text before cursor
        let before = proxy.documentContextBeforeInput ?? ""
        let after = proxy.documentContextAfterInput ?? ""
        let full = before + after
        if full.isEmpty { return }
        let encoded = encode(full)
        // Delete all and replace
        for _ in full { proxy.deleteBackward() }
        proxy.insertText(encoded)
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
    }
}
