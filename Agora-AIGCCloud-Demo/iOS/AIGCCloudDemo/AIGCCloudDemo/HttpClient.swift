//
//  HttpClient.swift
//  iOSRtcAppTemplate
//
//  Created by ZhouRui on 2024/6/18.
//

import Foundation

enum HttpSerializerType: Int {
    case json = 0
    case raw = 1
}

enum HttpMethodType: Int {
    case get = 0
    case post = 1
    case delete = 2
    case put = 3
}

class HttpRequest: NSObject {
    // 请求URL
    var requestUrl: String?
    
    // 上传参数形式，默认值 HttpSerializerTypeJSON
    var requestSerializerType: HttpSerializerType = .json
    
    // 返回数据类型，默认值 HttpSerializerTypeJSON
    var responseSerializerType: HttpSerializerType = .json
    
    // 请求方法，默认POST
    var httpMethod: HttpMethodType = .post
    
    // 请求超时时间，默认10s
    var timeoutInterval: TimeInterval = 10.0
    
    // 设置http请求信息头(HTTPHeaderField)，默认为nil
    var headers: [String: String]?
    
    // 上传参数，类型为NSDictionary或NSData,默认nil
    var requestParameters: Any?
    
    // 接收http响应数据ContentTypes
    var responseAcceptableContentTypes: Set<String>?
}

class HttpResponse: NSObject {
    // 请求状态码
    var statusCode: Int = 0
    
    // 接口返回失败后的错误信息，如果成功则为nil
    var error: NSError?
    
    // 调用成功后的返回数据，接口返回或读取缓存的数据，类型为NSDictionary或NSData
    var responseObject: Any?
}

class HttpClient: NSObject {
    private var session: URLSession
    
    override init() {
        self.session = URLSession.shared
        super.init()
    }

    func sendRequest(_ request: HttpRequest) async -> HttpResponse {
        guard let url = URL(string: request.requestUrl ?? "") else {
            let response = HttpResponse()
            response.error = NSError(domain: "Invalid URL", code: -1, userInfo: nil)
            return response
        }
        
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = httpMethodString(request.httpMethod)
        urlRequest.timeoutInterval = request.timeoutInterval
        urlRequest.allHTTPHeaderFields = request.headers
        urlRequest.httpBody = request.requestParameters as? Data
        
        do {
            let (data, response) = try await session.data(for: urlRequest)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                let response = self._createResponse(httpResponse: nil, responseObject: nil, error: nil)
                return response
            }
            
            if httpResponse.statusCode != 200 {
                let response = self._createResponse(httpResponse: httpResponse, responseObject: nil, error: nil)
                return response
            }
            
            if let responseObject = try? JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] {
                let response = self._createResponse(httpResponse: httpResponse, responseObject: responseObject, error: nil)
                return response
            } else {
                let response = self._createResponse(httpResponse: httpResponse, responseObject: nil, error: nil)
                return response
            }
        } catch {
            let response = self._createResponse(httpResponse: nil, responseObject: nil, error: error as NSError)
            return response
        }
    }
    
    // MARK: - Private
    
    private func httpMethodString(_ method: HttpMethodType) -> String {
        switch method {
        case .get:
            return "GET"
        case .post:
            return "POST"
        case .put:
            return "PUT"
        case .delete:
            return "DELETE"
        }
    }
    
    private func _createResponse(httpResponse: HTTPURLResponse?, responseObject: Any?, error: Error?) -> HttpResponse {
        let response = HttpResponse()
        response.statusCode = httpResponse?.statusCode ?? 0
        response.error = error as NSError?
        response.responseObject = responseObject
        return response
    }
}
