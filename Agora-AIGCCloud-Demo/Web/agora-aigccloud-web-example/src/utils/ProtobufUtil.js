import * as protobuf from 'protobufjs';

export default {
  async loadProto(protoPath) {
    try {
      return await protobuf.load(protoPath);
    } catch (error) {
      console.error('Error loading proto file:', error);
      throw error;
    }
  },

  encode(root, messageName, payload) {
    const MessageType = root.lookupType(messageName);
    const message = MessageType.create(payload);
    return MessageType.encode(message).finish();
  },

  decode(root, messageName, buffer) {
    const MessageType = root.lookupType(messageName);
    const decoded = MessageType.decode(new Uint8Array(buffer));
    return MessageType.toObject(decoded,{
      longs: String,  // 将 long 值转换为字符串
      enums: String,  // 将枚举值转换为字符串
      bytes: String,  // 将字节转换为 base64 编码的字符串
      defaults: true, // 包含默认值
      arrays: true,   // 总是将重复字段作为数组
      objects: true,  // 总是将消息字段作为对象
      oneofs: true    // 包含 oneof 字段
    });
  }
};