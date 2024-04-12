Pod::Spec.new do |spec|
  spec.name         = "AgoraAIGCService"
  spec.version      = "1.2.0-alpha-5"
  spec.summary      = "AgoraAIGCService"
  spec.description  = "AgoraAIGCService"
  
  spec.homepage     = "https://github.com/AgoraIO-Community"
    spec.license      = "MIT"
  spec.author       = { "ZYP" => "zhuyuping@agora.io" }
  spec.source       = { :http => "https://download.agora.io/sdk/release/AgoraAIGCService-v1.2.0-alpha-5.zip"}
  spec.ios.deployment_target = '12.0'
  spec.vendored_frameworks = ["*.framework"]
  spec.dependency 'SocketRocket', '0.7.0'
  spec.dependency 'MicrosoftCognitiveServicesSpeech-iOS', '~> 1.25'
  spec.dependency 'AgoraComponetLog'
  spec.pod_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64', 'DEFINES_MODULE' => 'YES' }
  spec.user_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64', 'DEFINES_MODULE' => 'YES' }
end
