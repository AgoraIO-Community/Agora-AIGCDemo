//
//  MainView.swift
//  RTEAIGCServiceDemo
//
//  Created by ZhouRui on 2024/6/19.
//

import UIKit

protocol MainViewDelegate: NSObjectProtocol {
    func mainViewDidShouldSendText(text: String)
    func mainViewDidTapAction(action: MainView.Action)
}

class MainView: UIView, UITableViewDataSource {
    var dataList = [Info]()
    private var textField = UITextField()
    private let sendBtn = UIButton()
    private let interruptBtn = UIButton()
    private let tableView = UITableView(frame: .zero, style: .grouped)
    weak var delegate: MainViewDelegate?
    private let startBtn = UIButton()
    private let stopBtn = UIButton()
    private let sendMsgBtn = UIButton()
    public let taskIdLabel = UILabel()
    public let modeLabel = UILabel()
    public let ttsSelLabel = UILabel()
    public let modeBtn = UIButton()
    public let ttsSelBtn = UIButton()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        commonInit()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = .white
        tableView.backgroundColor = .red
        addSubview(tableView)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.leftAnchor.constraint(equalTo: leftAnchor).isActive = true
        tableView.rightAnchor.constraint(equalTo: rightAnchor).isActive = true
        tableView.heightAnchor.constraint(equalToConstant: 350).isActive = true
        tableView.topAnchor.constraint(equalTo: safeAreaLayoutGuide.topAnchor).isActive = true
        
        addSubview(textField)
        textField.translatesAutoresizingMaskIntoConstraints = false
        addSubview(sendBtn)
        sendBtn.translatesAutoresizingMaskIntoConstraints = false
        addSubview(interruptBtn)
        interruptBtn.translatesAutoresizingMaskIntoConstraints = false
        addSubview(startBtn)
        startBtn.translatesAutoresizingMaskIntoConstraints = false
        addSubview(stopBtn)
        stopBtn.translatesAutoresizingMaskIntoConstraints = false
        addSubview(sendMsgBtn)
        sendMsgBtn.translatesAutoresizingMaskIntoConstraints = false
        addSubview(taskIdLabel)
        taskIdLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(modeBtn)
        modeBtn.translatesAutoresizingMaskIntoConstraints = false
        addSubview(ttsSelBtn)
        ttsSelBtn.translatesAutoresizingMaskIntoConstraints = false
        addSubview(modeLabel)
        modeLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(ttsSelLabel)
        ttsSelLabel.translatesAutoresizingMaskIntoConstraints = false
        
        textField.borderStyle = .roundedRect
        textField.placeholder = "请输入内容"
        
        interruptBtn.setTitle("打断", for: .normal)
        interruptBtn.setTitleColor(.blue, for: .normal)
        interruptBtn.addTarget(self, action: #selector(interruptButtonTap(_:)), for: .touchUpInside)
        interruptBtn.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        interruptBtn.topAnchor.constraint(equalTo: textField.bottomAnchor, constant: 10).isActive = true
        
        sendMsgBtn.setTitle("发送自定义message", for: .normal)
        sendMsgBtn.setTitleColor(.blue, for: .normal)
        sendMsgBtn.addTarget(self, action: #selector(sendMsgButtonTap(_:)), for: .touchUpInside)
        sendMsgBtn.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        sendMsgBtn.topAnchor.constraint(equalTo: textField.bottomAnchor, constant: 80).isActive = true
        
        startBtn.setTitle("开始", for: .normal)
        startBtn.setTitleColor(.blue, for: .normal)
        startBtn.addTarget(self, action: #selector(startButtonTap(_:)), for: .touchUpInside)
        startBtn.topAnchor.constraint(equalTo: textField.bottomAnchor, constant: 10).isActive = true
        startBtn.leftAnchor.constraint(equalTo: tableView.leftAnchor, constant: 20).isActive = true
        
        stopBtn.setTitle("停止", for: .normal)
        stopBtn.setTitleColor(.blue, for: .normal)
        stopBtn.addTarget(self, action: #selector(stopButtonTap(_:)), for: .touchUpInside)
        stopBtn.topAnchor.constraint(equalTo: textField.bottomAnchor, constant: 80).isActive = true
        stopBtn.leftAnchor.constraint(equalTo: tableView.leftAnchor, constant: 20).isActive = true
        
        textField.leftAnchor.constraint(equalTo: leftAnchor, constant: 10).isActive = true
        textField.rightAnchor.constraint(equalTo: sendBtn.leftAnchor).isActive = true
        textField.topAnchor.constraint(equalTo: tableView.bottomAnchor).isActive = true
        
        sendBtn.setTitle("发送", for: .normal)
        sendBtn.setTitleColor(.blue, for: .normal)
        sendBtn.backgroundColor = .green
        
        sendBtn.rightAnchor.constraint(equalTo: rightAnchor, constant: -10).isActive = true
        sendBtn.widthAnchor.constraint(equalToConstant: 80).isActive = true
        sendBtn.centerYAnchor.constraint(equalTo: textField.centerYAnchor).isActive = true
        sendBtn.addTarget(self, action: #selector(buttonTap(_:)), for: .touchUpInside)
        
        taskIdLabel.topAnchor.constraint(equalTo: stopBtn.bottomAnchor, constant: 50).isActive = true
        taskIdLabel.leftAnchor.constraint(equalTo: tableView.leftAnchor, constant: 20).isActive = true
        taskIdLabel.text = "TaskId: "
        
//        textField.isHidden = true
//        sendBtn.isHidden = true
        
        modeLabel.text = "aliYun_mode: "
        modeLabel.topAnchor.constraint(equalTo: taskIdLabel.bottomAnchor, constant: 20).isActive = true
        modeLabel.leftAnchor.constraint(equalTo: textField.leftAnchor).isActive = true
        
        modeBtn.setTitle(AliYunMode.multiEngineNoContext.description, for: .normal)
        modeBtn.setTitleColor(.blue, for: .normal)
        modeBtn.addTarget(self, action: #selector(modeButtonTap(_:)), for: .touchUpInside)
        modeBtn.topAnchor.constraint(equalTo: taskIdLabel.bottomAnchor, constant: 15).isActive = true
        modeBtn.leftAnchor.constraint(equalTo: modeLabel.rightAnchor, constant: 10).isActive = true
        
        ttsSelLabel.text = "tts_select: "
        ttsSelLabel.topAnchor.constraint(equalTo: modeLabel.bottomAnchor, constant: 20).isActive = true
        ttsSelLabel.leftAnchor.constraint(equalTo: textField.leftAnchor).isActive = true
        
        ttsSelBtn.setTitle(TTSSelect.ali_tts.description, for: .normal)
        ttsSelBtn.setTitleColor(.blue, for: .normal)
        ttsSelBtn.addTarget(self, action: #selector(ttsSelButtonTap(_:)), for: .touchUpInside)
        ttsSelBtn.topAnchor.constraint(equalTo: modeLabel.bottomAnchor, constant: 15).isActive = true
        ttsSelBtn.leftAnchor.constraint(equalTo: ttsSelLabel.rightAnchor, constant: 10).isActive = true
    }
    
    private func commonInit() {
        tableView.dataSource = self
        tableView.reloadData()
    }
    
    func addOrUpdateInfo(info: Info) {
        if dataList.isEmpty {
            dataList.append(info)
            tableView.reloadData()
            return
        }
        
        for (index, obj) in dataList.enumerated().reversed() {
            if obj.uuid == info.uuid {
                if info.uuid.contains("llm") {
                    dataList[index].content = obj.content + info.content
                }
                else { /** stt */
                    dataList[index].content = info.content
                }
                let indexPath = IndexPath(row: index, section: 0)
                tableView.reloadData()
                tableView.scrollToRow(at: indexPath, at: .middle, animated: false)
                return
            }
        }
        
        dataList.append(info)
        tableView.reloadData()
        let indexPath = IndexPath(row: dataList.count - 1, section: 0)
        tableView.scrollToRow(at: indexPath, at: .middle, animated: false)
    }
    
    @objc func buttonTap(_ sender: UIButton) {
        delegate?.mainViewDidShouldSendText(text: textField.text!)
        textField.text = ""
    }
    
    func showTextField(show: Bool) {
        textField.isHidden = !show
        sendBtn.isHidden = !show
    }
    
    func changeModeBtnTitle(_ title: String) {
        modeBtn.setTitle(title, for: .normal)
    }
    
    func changeTTSBtnTitle(_ title: String) {
        ttsSelBtn.setTitle(title, for: .normal)
    }
    
    @objc func modeButtonTap(_ sender: UIButton) {
        delegate?.mainViewDidTapAction(action: .modeChange)
    }
    
    @objc func ttsSelButtonTap(_ sender: UIButton) {
        delegate?.mainViewDidTapAction(action: .ttsChange)
    }
    
    /// UITableViewDataSource
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let info = dataList[indexPath.row]
        let cell = UITableViewCell(style: .value1, reuseIdentifier: "cell")
        cell.textLabel?.text = info.prefix + info.content
        cell.textLabel?.textColor = .blue
        cell.textLabel?.numberOfLines = 0
        return cell
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        dataList.count
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    @objc func interruptButtonTap(_ sender: UIButton) {
        delegate?.mainViewDidTapAction(action: .interrupt)
    }
    
    @objc func sendMsgButtonTap(_ sender: UIButton) {
        delegate?.mainViewDidTapAction(action: .sendMsg)
    }
    
    @objc func startButtonTap(_ sender: UIButton) {
        delegate?.mainViewDidTapAction(action: .start)
    }
    
    @objc func stopButtonTap(_ sender: UIButton) {
        delegate?.mainViewDidTapAction(action: .stop)
    }
}

extension MainView {
    class Info {
        let uuid: String
        var content: String
        
        init(uuid: String, content: String) {
            self.uuid = uuid
            self.content = content
        }
        
        var prefix: String {
            if uuid.contains("llm") {
                return "[llm]:"
            } else if uuid.contains("You") {
                return "[You]:"
            }
            return ""
        }
    }
}

extension MainView {
    enum Action {
        case interrupt
        case sendMsg
        case start
        case stop
        case modeChange
        case ttsChange
    }
}
