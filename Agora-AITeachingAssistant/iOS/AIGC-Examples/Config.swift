//
//  Config.swift
//  Demo
//
//  Created by ZYP on 2022/12/23.
//

import Foundation

struct Config {
    static let channelId = "gpt001"
    static let hostUid: UInt = 0
    static let appId = <#appId#>
    static let certificate = <#certificate#>
    /// 解密密钥，如果为空则表示用明文.需要替换json文件里面的内容
    static let key: String? = nil
}
