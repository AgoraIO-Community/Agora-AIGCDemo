// DO NOT EDIT.
// swift-format-ignore-file
//
// Generated by the Swift generator plugin for the protocol buffer compiler.
// Source: AigcMessage.proto
//
// For information on using the generated types, please see the documentation:
//   https://github.com/apple/swift-protobuf/

import Foundation
import SwiftProtobuf

// If the compiler emits an error on this type, it is because this file
// was generated by a version of the `protoc` Swift plug-in that is
// incompatible with the version of SwiftProtobuf to which you are linking.
// Please ensure that you are building against the same version of the API
// that was used to generate this file.
fileprivate struct _GeneratedWithProtocGenSwiftVersion: SwiftProtobuf.ProtobufAPIVersionCheck {
  struct _2: SwiftProtobuf.ProtobufAPIVersion_2 {}
  typealias Version = _2
}

struct AigcMessage {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  /// proto3 does not support [default] option
  var magicnum: Int32 = 0

  /// 100-stt, 120-llm, 130-tts, 140-session status
  var type: Int32 = 0

  /// default should be manually set to 1 in the code
  var version: Int32 = 0

  var userid: Int32 = 0

  var roundid: Int32 = 0

  var flag: Int32 = 0

  var content: String = String()

  var unknownFields = SwiftProtobuf.UnknownStorage()

  init() {}
}

#if swift(>=5.5) && canImport(_Concurrency)
extension AigcMessage: @unchecked Sendable {}
#endif  // swift(>=5.5) && canImport(_Concurrency)

// MARK: - Code below here is support for the SwiftProtobuf runtime.

extension AigcMessage: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  static let protoMessageName: String = "AigcMessage"
  static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "magicnum"),
    2: .same(proto: "type"),
    3: .same(proto: "version"),
    4: .same(proto: "userid"),
    5: .same(proto: "roundid"),
    6: .same(proto: "flag"),
    7: .same(proto: "content"),
  ]

  mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    while let fieldNumber = try decoder.nextFieldNumber() {
      // The use of inline closures is to circumvent an issue where the compiler
      // allocates stack space for every case branch when no optimizations are
      // enabled. https://github.com/apple/swift-protobuf/issues/1034
      switch fieldNumber {
      case 1: try { try decoder.decodeSingularInt32Field(value: &self.magicnum) }()
      case 2: try { try decoder.decodeSingularInt32Field(value: &self.type) }()
      case 3: try { try decoder.decodeSingularInt32Field(value: &self.version) }()
      case 4: try { try decoder.decodeSingularInt32Field(value: &self.userid) }()
      case 5: try { try decoder.decodeSingularInt32Field(value: &self.roundid) }()
      case 6: try { try decoder.decodeSingularInt32Field(value: &self.flag) }()
      case 7: try { try decoder.decodeSingularStringField(value: &self.content) }()
      default: break
      }
    }
  }

  func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    if self.magicnum != 0 {
      try visitor.visitSingularInt32Field(value: self.magicnum, fieldNumber: 1)
    }
    if self.type != 0 {
      try visitor.visitSingularInt32Field(value: self.type, fieldNumber: 2)
    }
    if self.version != 0 {
      try visitor.visitSingularInt32Field(value: self.version, fieldNumber: 3)
    }
    if self.userid != 0 {
      try visitor.visitSingularInt32Field(value: self.userid, fieldNumber: 4)
    }
    if self.roundid != 0 {
      try visitor.visitSingularInt32Field(value: self.roundid, fieldNumber: 5)
    }
    if self.flag != 0 {
      try visitor.visitSingularInt32Field(value: self.flag, fieldNumber: 6)
    }
    if !self.content.isEmpty {
      try visitor.visitSingularStringField(value: self.content, fieldNumber: 7)
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  static func ==(lhs: AigcMessage, rhs: AigcMessage) -> Bool {
    if lhs.magicnum != rhs.magicnum {return false}
    if lhs.type != rhs.type {return false}
    if lhs.version != rhs.version {return false}
    if lhs.userid != rhs.userid {return false}
    if lhs.roundid != rhs.roundid {return false}
    if lhs.flag != rhs.flag {return false}
    if lhs.content != rhs.content {return false}
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}
