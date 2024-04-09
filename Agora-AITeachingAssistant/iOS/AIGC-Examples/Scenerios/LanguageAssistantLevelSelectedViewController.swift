//
//  LanguageAssistantLevelSelectedViewController.swift
//  AIGC-Examples
//
//  Created by ZYP on 2024/2/29.
//

import UIKit

class LanguageAssistantLevelSelectedViewController: UIViewController {
    struct Section {
        let title: String
        let rows: [Row]
    }
    
    struct Row {
        let title: String
    }
    
    struct LevelItem: Codable {
        let level: String
        let prompt: String
        let preloadTipMessage: [String]
        let userName: String
        let teacherName: String
        let promptToken: UInt
        let llmExtraInfoJson: String
        let topic: String
    }
    
    let tableview = UITableView(frame: .zero, style: .grouped)
    var list = [Section]()
    
    /// 提示部分
    var promptTipSlic: String!
    /// 润色部分
    var promptPolishSlic: String!
    /// 翻译部分
    var promptTranslateSlic: String!
    /// 引导对话结束
    var promptDialogEnd: String!
    
    var levelItems = [LevelItem]()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        createData()
        setupUI()
        commonInit()
    }
    
    func setupUI() {
        title = "分级选择"
        view.addSubview(tableview)
        tableview.translatesAutoresizingMaskIntoConstraints = false
        
        tableview.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        tableview.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        tableview.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        tableview.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
    }
    
    func commonInit() {
        tableview.register(UITableViewCell.self, forCellReuseIdentifier: "cell")
        tableview.dataSource = self
        tableview.delegate = self
        tableview.reloadData()
    }
    
    func createData() {
        list = [Section(title: "GPT3.5", rows: [.init(title: "A"),
                                                .init(title: "B"),
                                                .init(title: "C"),
                                                .init(title: "D")]),
                Section(title: "GPT4", rows: [.init(title: "A"),
                                              .init(title: "B"),
                                              .init(title: "C"),
                                              .init(title: "D")])]
        
        
        var path = Bundle.main.path(forResource: "teacher_level.json", ofType: nil)
        var data = try! Data(contentsOf: URL(fileURLWithPath: path!))
        
        if let eKey = Config.key {
            var secretString = try! String(contentsOf: URL(fileURLWithPath: path!))
            secretString.removeLast()
            let originalString = secretString.aesDecrypt(key: eKey)
            data = originalString.data(using: .utf8)!
        }
        
        levelItems = try! JSONDecoder().decode([LevelItem].self, from: data)
        
        
        path = Bundle.main.path(forResource: "tip_prompt.json", ofType: nil)
        promptTipSlic = try! String(contentsOf: URL(fileURLWithPath: path!))
        if let eKey = Config.key {
            promptTipSlic.removeLast()
            promptTipSlic = promptTipSlic.aesDecrypt(key: eKey)
        }
        
        path = Bundle.main.path(forResource: "polish_prompt.json", ofType: nil)
        promptPolishSlic = try! String(contentsOf: URL(fileURLWithPath: path!))
        if let eKey = Config.key {
            promptPolishSlic.removeLast()
            promptPolishSlic = promptPolishSlic.aesDecrypt(key: eKey)
        }
        
        path = Bundle.main.path(forResource: "translate_prompt.json", ofType: nil)
        promptTranslateSlic = try! String(contentsOf: URL(fileURLWithPath: path!))
        if let eKey = Config.key {
            promptTranslateSlic.removeLast()
            promptTranslateSlic = promptTranslateSlic.aesDecrypt(key: eKey)
        }
        
        path = Bundle.main.path(forResource: "end_prompt.json", ofType: nil)
        promptDialogEnd = try! String(contentsOf: URL(fileURLWithPath: path!))
        if let eKey = Config.key {
            promptDialogEnd.removeLast()
            promptDialogEnd = promptDialogEnd.aesDecrypt(key: eKey)
        }
    }
    
}

extension LanguageAssistantLevelSelectedViewController: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        list[section].title
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        list.count
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return list[section].rows.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableview.dequeueReusableCell(withIdentifier: "cell", for: indexPath)
        let item = list[indexPath.section].rows[indexPath.row]
        cell.textLabel?.text = item.title
        cell.accessoryType = .disclosureIndicator
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        
        let config = AIGCManager.Configurate(sttProviderName: "microsoft",
                                             llmProviderName: indexPath.section == 0 ? "azureOpenai-gpt-35-turbo-16k" : "azureOpenai-gpt-4",
                                             ttsProviderName: "microsoft",
                                             roleId: "ai_teaching_assistant-zh-CN",
                                             inputLang: .ZH_CN,
                                             outputLang: .ZH_CN,
                                             userName: "XiaoLi",
                                             customPrompt: nil,
                                             enableMultiTurnShortTermMemory: true,
                                             speechRecognitionFiltersLength:3,
                                             enableSTT: false,
                                             enableTTS: false,
                                             noiseEnv: .noise,
                                             speechRecCompLevel: .normal,
                                             continueToHandleLLM: true,
                                             continueToHandleTTS: true)
        let promptGenerator = PromptGenerator(teacherName: levelItems[indexPath.row].teacherName,
                                              userName: levelItems[indexPath.row].userName,
                                              promptDialog: levelItems[indexPath.row].prompt,
                                              promptTipSlic: promptTipSlic,
                                              promptPolishSlic: promptPolishSlic,
                                              promptTranslateSlic: promptTranslateSlic,
                                              promptDialogEnd: promptDialogEnd,
                                              promptToken: levelItems[indexPath.row].promptToken,
                                              extraInfoJson: levelItems[indexPath.row].llmExtraInfoJson)
        
        let vc = LanguageAssistantViewController(config: config,
                                                 promptGenerator: promptGenerator,
                                                 preloadTipMessage: levelItems[indexPath.row].preloadTipMessage)
        
        let llmName = indexPath.section == 0 ? "(GPT3.5)" : "(GPT4)"
        let topic = "(\(levelItems[indexPath.row].topic))"
        vc.title = "级别" + levelItems[indexPath.row].level + llmName + topic
        navigationController?.pushViewController(vc, animated: true)
    }
}
