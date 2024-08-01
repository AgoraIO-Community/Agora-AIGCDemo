//
//  RtcManager.swift
//  GPT-Demo
//
//  Created by ZYP on 2023/7/28.
//

import AgoraRtcKit
import RTMTokenBuilder

protocol RtcManagerDelegate: NSObjectProtocol {
    func rtcManagerOnCreatedRenderView(view: UIView)
    func rtcManagerOnCaptureAudioFrame(frame: AgoraAudioFrame)
    func rtcManagerOnDebug(text: String)
    func rtcManagerOnReceiveStreamMessage(userId: UInt, streamId: Int, data: Data)
}

class RtcManager: NSObject {
    fileprivate var agoraKit: AgoraRtcEngineKit!
    weak var delegate: RtcManagerDelegate?
    fileprivate var isRecord = false
    private var soundQueue = Queue<Data>()
    fileprivate let logTag = "RtcManager"
    
    deinit {
        agoraKit.leaveChannel()
        print("RtcManager deinit")
    }
    
    func initEngine() {
        let config = AgoraRtcEngineConfig()
        config.appId = Config.aigcCloud_appId
        agoraKit = AgoraRtcEngineKit.sharedEngine(with: config, delegate: self)
        
        agoraKit.setVideoFrameDelegate(self)
        agoraKit.setChannelProfile(.liveBroadcasting)
        agoraKit.setClientRole(.broadcaster)
        agoraKit.enableAudioVolumeIndication(50, smooth: 3, reportVad: true)
        agoraKit.setAudioScenario(.chorus)
                
//        agoraKit.setParameters("{\"rtc.debug.enable\":true}")
//        // 开启AI降噪soft模式
//        agoraKit.setParameters("{\"che.audio.enable.nsng\": true}")
//        agoraKit.setParameters("{\"che.audio.ains_mode\": 2}")
//        agoraKit.setParameters("{\"che.audio.ns_mode\": 2}")
//        agoraKit.setParameters("{\"che.audio.nsng.lowerBound\": 80}")
//        agoraKit.setParameters("{\"che.audio.nsng.lowerMask\": 50}")
//        agoraKit.setParameters("{\"che.audio.nsng.statisitcalbound\": 5}")
//        agoraKit.setParameters("{\"che.audio.nsng.finallowermask\": 30}")
//
//        agoraKit.setParameters("{\"che.audio.apm_dump\": true}")
        
//        agoraKit.setParameters("{\"che.audio.nsng.enhfactorstastical\": 200}")
        
//        let path = NSHomeDirectory() + "/Documents/"
//        let dic = ["che.audio.dump_path": path]
//        let jsonData = try! JSONSerialization.data(withJSONObject: dic)
//        let jsonStr = String(data: jsonData, encoding: .utf8)
//        agoraKit.setParameters("{\"che.audio.dump_path\": \(path)}")
//        agoraKit.setParameters(jsonStr!)
        
        
//        agoraKit.setParameters("{\"che.audio.frame_dump\":{\"location\":\"all\",\"action\":\"start\",\"max_size_bytes\":\"100000000\",\"uuid\":\"123456789\", \"duration\": \"150000\"}}")
    }
    
    func joinChannel() {
//        let dateFormatter = DateFormatter()
//        dateFormatter.dateFormat = "yyyyMMddHHmmss"
//        let currentDateString = dateFormatter.string(from: Date())
        let channelId = Config.aigcCloud_channelId // + currentDateString + String(Int.random(in: 10...100))
//        let token = TokenBuilder.rtcToken2(Config.appId,
//                                           appCertificate: Config.certificate,
//                                           uid: Int32(Config.hostUid),
//                                           channelName: channelId)
        let option = AgoraRtcChannelMediaOptions()
        option.clientRoleType = .broadcaster
        agoraKit.setAudioFrameDelegate(self)
        agoraKit.enableAudio()
        let ret = agoraKit.joinChannel(byToken: "",
                                       channelId: channelId,
                                       uid: Config.aigcCloud_userId,
                                       mediaOptions: option) { String, UInt, Int in
            print("joinChannel success.")
        }
        if ret != 0 {
            let text = "joinChannel ret \(ret)"
            Log.errorText(text: text, tag: logTag)
        }
    }
    
    func leaveChannel() {
        let ret = agoraKit.leaveChannel()
        if ret != 0 {
            let text = "leaveChannel ret \(ret)"
            Log.errorText(text: text, tag: logTag)
        }
    }
    
    func startRecord() {
        isRecord = true
    }
    
    func stopRecord() {
        isRecord = false
    }
    
    func setPlayData(data: Data) {
        soundQueue.enqueue(data)
    }
}

extension RtcManager: AgoraRtcEngineDelegate {
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        Log.errorText(text: "didOccurError \(errorCode)", tag: logTag)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed: Int) {
        let text = "didJoinedOfUid \(uid)"
        Log.info(text: text, tag: logTag)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinChannel channel: String, withUid uid: UInt, elapsed: Int) {
        let text = "didJoinChannel withUid \(uid)"
        Log.info(text: text, tag: logTag)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, receiveStreamMessageFromUid uid: UInt, streamId: Int, data: Data) {
        let text = "receiveStreamMessageFromUid \(uid), streamId \(streamId)"
        Log.info(text: text, tag: logTag)
        self.delegate?.rtcManagerOnReceiveStreamMessage(userId: uid, streamId: streamId, data: data)
    }
}

extension RtcManager: AgoraVideoFrameDelegate {
    // MARK: - AgoraVideoFrameDelegate
}

extension RtcManager: AgoraAudioFrameDelegate {
    func onEarMonitoringAudioFrame(_ frame: AgoraAudioFrame) -> Bool {
        true
    }
    
    func getEarMonitoringAudioParams() -> AgoraAudioParams {
        let params = AgoraAudioParams ()
        params.sampleRate = 16000
        params.channel = 1
        params.mode = .readWrite
        params.samplesPerCall = 640
        return params
    }
    
    func getRecordAudioParams() -> AgoraAudioParams {
        let params = AgoraAudioParams()
        params.sampleRate = 16000
        params.channel = 1
        params.mode = .readWrite
        params.samplesPerCall = 640
        return params
    }
    
    func onRecordAudioFrame(_ frame: AgoraAudioFrame, channelId: String) -> Bool {
        if self.isRecord {
            self.delegate?.rtcManagerOnCaptureAudioFrame(frame: frame)
        }
        return true
    }
    
    func onRecord(_ frame: AgoraAudioFrame, channelId: String) -> Bool {
        return true
    }
    
    func getPlaybackAudioParams() -> AgoraAudioParams {
        let params = AgoraAudioParams()
        params.sampleRate = 16000
        params.channel = 1
        params.mode = .readWrite
        params.samplesPerCall = 640
        return params
    }
    
    func getObservedAudioFramePosition() -> AgoraAudioFramePosition {
        return [.record, .playback]
    }
    
    func onPlaybackAudioFrame(_ frame: AgoraAudioFrame, channelId: String) -> Bool {
        if let data = soundQueue.dequeue() {
            data.withUnsafeBytes { rawBufferPointer in
                let rawPtr = rawBufferPointer.baseAddress!
                let bufferPtr = UnsafeMutableRawPointer(frame.buffer)
                bufferPtr?.copyMemory(from: rawPtr, byteCount: data.count)
            }
        }
        return true
    }
    
    func onMixedAudioFrame(_ frame: AgoraAudioFrame, channelId: String) -> Bool {
        return true
    }
    
    func getMixedAudioParams() -> AgoraAudioParams {
        let params = AgoraAudioParams()
        params.sampleRate = 16000
        params.channel = 1
        params.mode = .readWrite
        params.samplesPerCall = 640
        return params
    }
    
    func onPlaybackAudioFrame(beforeMixing frame: AgoraAudioFrame, channelId: String, uid: UInt) -> Bool {
        return true
    }
}

struct Queue<T> {
    private var elements: [T] = []
    private let semaphore = DispatchSemaphore(value: 1)
    private let logTag = "Queue"
    
    mutating func enqueue(_ element: T) {
        semaphore.wait()
        elements.append(element)
        semaphore.signal()
    }
    
    mutating func reset() {
        semaphore.wait()
        elements.removeAll()
        semaphore.signal()
    }
    
    mutating func dequeue() -> T? {
        semaphore.wait()
        defer { semaphore.signal() }
        let t = elements.isEmpty ? nil : elements.removeFirst()
        return t
    }
    
    func peek() -> T? {
        semaphore.wait()
        defer { semaphore.signal() }
        return elements.first
    }
    
    func isEmpty() -> Bool {
        semaphore.wait()
        defer { semaphore.signal() }
        return elements.isEmpty
    }
    
    func count() -> Int {
        semaphore.wait()
        defer { semaphore.signal() }
        return elements.count
    }
}

extension RtcManager {
    func invokeRtcManagerOnDebug(text: String) {
        if Thread.isMainThread {
            self.delegate?.rtcManagerOnDebug(text: text)
            return
        }
        
        DispatchQueue.main.async { [weak self] in
            self?.delegate?.rtcManagerOnDebug(text: text)
        }
    }
}
