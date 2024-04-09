//
//  AppDelegate.swift
//  GPT-Demo
//
//  Created by ZYP on 2023/7/27.
//

import UIKit
import CryptoSwift
@main
class AppDelegate: UIResponder, UIApplicationDelegate {

//    private var timer: GCDTimer? = GCDTimer()

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        Log.setLoggers(loggers: [ConsoleLogger(), FileLogger()])
        return true
    }
    
    // MARK: UISceneSession Lifecycle

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }
    func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
    }
}
