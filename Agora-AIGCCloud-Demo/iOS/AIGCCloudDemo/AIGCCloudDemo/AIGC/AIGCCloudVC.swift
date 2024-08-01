//
//  AIGCCloudVC.swift
//  iOSRtcAppTemplate
//
//  Created by ZhouRui on 2024/6/18.
//

import UIKit
import AgoraRtcKit

struct AIGCCloudConfig {
    var regionName: String
    var regionCode: String
    var domain: String
}

enum AliYunMode: Int {
    case multiEngineNoContext = 0   // stt是多引擎并发执行，开启vad检测，llm请求不携带上下文
    case singleEngineWithContext = 1 // stt是单引擎，不开启vad，llm请求携带上下文

    var description: String {
        switch self {
        case .multiEngineNoContext:
            return "多引擎、开vad、llm不带上下文"
        case .singleEngineWithContext:
            return "单引擎、不开vad、llm带上下文"
        }
    }
}

enum TTSSelect: Int {
    case ali_cosy = 0
    case ali_tts = 1
    
    var description: String {
        switch self {
        case .ali_cosy:
            return "ali_cosy"
        case .ali_tts:
            return "ali_tts"
        }
    }
}

class AIGCCloudVC: UIViewController {
    private let mainView = MainView()
    private var config: AIGCCloudConfig!
    private let httpClient = HttpClient()
    private let rtcManager = RtcManager()
    private var taskId = ""
    
    var currentMode: AliYunMode = .multiEngineNoContext {
        didSet {
            updateModeButtonTitle()
        }
    }
    
    var currentTTSSelect: TTSSelect = .ali_tts {
        didSet {
            updateTTSButtonTitle()
        }
    }
    
    init(config: AIGCCloudConfig) {
        super.init(nibName: nil, bundle: nil)
        self.config = config
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        title = "AIGC Cloud"
        
        view.backgroundColor = .white
        view.addSubview(mainView)
        mainView.frame = view.bounds
        mainView.delegate = self
        
        rtcManager.delegate = self
        rtcManager.initEngine()
        
//        rtcManager.joinChannel()
        
//        startCloudService()
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        stopCloudService()
    }
    
    private func updateModeButtonTitle() {
        mainView.changeModeBtnTitle(currentMode.description)
    }
    
    private func updateTTSButtonTitle() {
        mainView.changeTTSBtnTitle(currentTTSSelect.description)
    }
    
    func startCloudService() {
        let url = "\(config.domain)/\(config.regionCode)/v1/projects/\(Config.aigcCloud_appId)/aigc-workers/local/start"

        let headers = ["Content-Type": "application/json"]

        var bodyJson = [String: Any]()
        bodyJson["channel_name"] = Config.aigcCloud_channelId

        var userConfJsonArray = [[String: Any]]()
        var userConfJson = [String: Any]()
        userConfJson["speak_uid"] = Config.aigcCloud_userId
        userConfJson["rtc_uid"] = Config.aigcCloud_userId + 1

        let mInLanguage = ["zh-CN", "en-US"]
        let mOutLanguage = ["zh-CN", "en-US"]

        var inLanguagesJSONArray = [String]()
        for language in mInLanguage {
            inLanguagesJSONArray.append(language)
        }

        var outLanguagesJSONArray = [String]()
        for language in mOutLanguage {
            outLanguagesJSONArray.append(language)
        }

        userConfJson["inLanguages"] = inLanguagesJSONArray
        userConfJson["outLanguages"] = outLanguagesJSONArray
        userConfJsonArray.append(userConfJson)

        bodyJson["user_conf"] = userConfJsonArray
        
        bodyJson["aliYun_mode"] = currentMode.rawValue
        bodyJson["tts_select"] = currentTTSSelect.description

        let bodyData = try? JSONSerialization.data(withJSONObject: bodyJson, options: [])
        let bodyString = String(data: bodyData!, encoding: .utf8)
        print("startCloudService body: \(bodyString!)")
        let request = HttpRequest()
        request.requestUrl = url
        request.httpMethod = .post
        request.headers = headers
        request.requestParameters = bodyData
        Task {
            let response = await httpClient.sendRequest(request)
            if let error = response.error {
                print("startCloudService error: \(error)")
            } else {
                print("startCloudService success")
                if response.responseObject != nil {
                    print("startCloudService responseObject: \(response.responseObject!)")
                    let code = (response.responseObject as! [String: Any])["code"] as! Int
                    let data = (response.responseObject as! [String: Any])["data"]
                    taskId = (data as! [String: String])["id"] ?? ""
                    print("current taskId: \(taskId)")
                    mainView.taskIdLabel.text = "TaskId: \(taskId)"
                    if code == 0 {
                        rtcManager.joinChannel()
                        rtcManager.startRecord()
                    }
                }
            }
        }
    }
    
    func stopCloudService() {
        let url = "\(config.domain)/\(config.regionCode)/v1/projects/\(Config.aigcCloud_appId)/aigc-workers/\(taskId)/local"
        let headers = ["Content-Type": "application/json"]
        
        let request = HttpRequest()
        request.requestUrl = url
        request.httpMethod = .delete
        request.headers = headers
        Task {
            let response = await httpClient.sendRequest(request)
            if let error = response.error {
                print("stopCloudService error: \(error)")
            } else {
                print("stopCloudService success")
                if response.responseObject != nil {
                    print("stopCloudService responseObject: \(response.responseObject!)")
                    let code = (response.responseObject as! [String: Any])["code"] as! Int
                    if code == 0 {
                        rtcManager.leaveChannel()
                        rtcManager.stopRecord()
                    }
                }
            }
        }
    }
    
    func handleStreamMessage(userId: UInt, streamId: Int, data: Data) {
        do {
            let streamMsg = try AigcMessage(serializedData: data)
            let timestamp = Date().timeIntervalSince1970 * 1000
            switch streamMsg.type {
                case 100:
                    let sttContent = streamMsg.content
                    let sttFlag = streamMsg.flag
                    let roundId = String(streamMsg.roundid)
                    print("streamMsg type: \(streamMsg.type), content: \(sttContent), flag: \(sttFlag), roundId: \(roundId), timestamp: \(timestamp)")
                    DispatchQueue.main.async {
                        let result = self.handleSTTMessage(sttContent) ?? ""
                        let info = MainView.Info(uuid: "You" + roundId, content: result)
                        self.mainView.addOrUpdateInfo(info: info)
                    }
                    break
                case 120:
                    let llmContent = streamMsg.content
                    let llmFlag = streamMsg.flag
                    let roundId = String(streamMsg.roundid)
                    print("streamMsg type: \(streamMsg.type), content: \(llmContent), flag: \(llmFlag), roundId: \(roundId), timestamp: \(timestamp)")
                    DispatchQueue.main.async {
                        let info = MainView.Info(uuid: "llm" + roundId, content: llmContent)
                        self.mainView.addOrUpdateInfo(info: info)
                    }
                    break
                case 130:
                    let ttsFlag = streamMsg.flag
                    print("streamMsg type: \(streamMsg.type), flag: \(ttsFlag), timestamp: \(timestamp)")
                    break
                case 140:
                    let sessionFlag = streamMsg.flag
                    print("streamMsg type: \(streamMsg.type), flag: \(sessionFlag), timestamp: \(timestamp)")
                    break
                default:
                    break
            }
            
        } catch {
            
        }
    }
    
    func handleSTTMessage(_ content: String) -> String? {
        /*{"header":{"namespace":"SpeechTranscriber","name":"TranscriptionResultChanged","status":20000000,"message_id":"410a5608c4404402acaf08c5377745c8","task_id":"52c72e3feec94cbabcbddf5c93c818f9","status_text":"Gateway:SUCCESS:Success."},"payload":{"index":1,"time":360,"result":"他，","confidence":0.603,"words":[],"status":0,"fixed_result":"","unfixed_result":""}}, flag: 0
         */
        // 解析上面的字符串
        let sttContent = content
        // 去掉字符串最后的 , flag: 0
        let handleSttString = sttContent.replacingOccurrences(of: ", flag: 0", with: "")
        // 解析json字符串
        let jsonData = handleSttString.data(using: .utf8)
        do {
            let sttJson = try JSONSerialization.jsonObject(with: jsonData!, options: .mutableContainers) as! [String: Any]
            let payload = sttJson["payload"] as! [String: Any]
            let confidence = payload["confidence"] as! Double
            if confidence < 0.8 {
                return nil
            }
            let result = payload["result"] as! String
            return result
        } catch {
            
        }
        return handleSttString
    }
}

extension AIGCCloudVC: RtcManagerDelegate {
    func rtcManagerOnCreatedRenderView(view: UIView) {
        
    }
    
    func rtcManagerOnCaptureAudioFrame(frame: AgoraAudioFrame) {
        
    }
    
    func rtcManagerOnDebug(text: String) {
        
    }
    
    func rtcManagerOnReceiveStreamMessage(userId: UInt, streamId: Int, data: Data) {
        handleStreamMessage(userId: userId, streamId: streamId, data: data)
    }
}

// MARK: - MainViewDelegate
extension AIGCCloudVC: MainViewDelegate {
    func mainViewDidShouldSendText(text: String) {
        let info = MainView.Info(uuid: "\(UInt8.random(in: 0...200))", content: text)
        mainView.addOrUpdateInfo(info: info)
    }
    
    func mainViewDidTapAction(action: MainView.Action) {
        switch action {
        case .interrupt: break
            
        case .sendMsg: break
            
        case .start:
            startCloudService()
            break
            
        case .stop:
            stopCloudService()
            break
            
        case .modeChange:
            if currentMode == .multiEngineNoContext {
                currentMode = .singleEngineWithContext
            } else {
                currentMode = .multiEngineNoContext
            }
            break
        case .ttsChange:
            if currentTTSSelect == .ali_cosy {
                currentTTSSelect = .ali_tts
            } else {
                currentTTSSelect = .ali_cosy
            }
            break
        }
            
    }
}
