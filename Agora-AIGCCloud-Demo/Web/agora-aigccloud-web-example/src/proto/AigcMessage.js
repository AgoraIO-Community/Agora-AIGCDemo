/*eslint-disable block-scoped-var, id-length, no-control-regex, no-magic-numbers, no-prototype-builtins, no-redeclare, no-shadow, no-var, sort-vars*/
import * as $protobuf from "protobufjs/minimal";

// Common aliases
const $Reader = $protobuf.Reader, $Writer = $protobuf.Writer, $util = $protobuf.util;

// Exported root namespace
const $root = $protobuf.roots["default"] || ($protobuf.roots["default"] = {});

export const AigcMessage = $root.AigcMessage = (() => {

    /**
     * Properties of an AigcMessage.
     * @exports IAigcMessage
     * @interface IAigcMessage
     * @property {number|null} [magicnum] AigcMessage magicnum
     * @property {number|null} [type] AigcMessage type
     * @property {number|null} [version] AigcMessage version
     * @property {number|null} [userid] AigcMessage userid
     * @property {number|null} [roundid] AigcMessage roundid
     * @property {number|null} [flag] AigcMessage flag
     * @property {string|null} [content] AigcMessage content
     * @property {number|Long|null} [timestamp] AigcMessage timestamp
     */

    /**
     * Constructs a new AigcMessage.
     * @exports AigcMessage
     * @classdesc Represents an AigcMessage.
     * @implements IAigcMessage
     * @constructor
     * @param {IAigcMessage=} [properties] Properties to set
     */
    function AigcMessage(properties) {
        if (properties)
            for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                if (properties[keys[i]] != null)
                    this[keys[i]] = properties[keys[i]];
    }

    /**
     * AigcMessage magicnum.
     * @member {number} magicnum
     * @memberof AigcMessage
     * @instance
     */
    AigcMessage.prototype.magicnum = 0;

    /**
     * AigcMessage type.
     * @member {number} type
     * @memberof AigcMessage
     * @instance
     */
    AigcMessage.prototype.type = 0;

    /**
     * AigcMessage version.
     * @member {number} version
     * @memberof AigcMessage
     * @instance
     */
    AigcMessage.prototype.version = 0;

    /**
     * AigcMessage userid.
     * @member {number} userid
     * @memberof AigcMessage
     * @instance
     */
    AigcMessage.prototype.userid = 0;

    /**
     * AigcMessage roundid.
     * @member {number} roundid
     * @memberof AigcMessage
     * @instance
     */
    AigcMessage.prototype.roundid = 0;

    /**
     * AigcMessage flag.
     * @member {number} flag
     * @memberof AigcMessage
     * @instance
     */
    AigcMessage.prototype.flag = 0;

    /**
     * AigcMessage content.
     * @member {string} content
     * @memberof AigcMessage
     * @instance
     */
    AigcMessage.prototype.content = "";

    /**
     * AigcMessage timestamp.
     * @member {number|Long} timestamp
     * @memberof AigcMessage
     * @instance
     */
    AigcMessage.prototype.timestamp = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

    /**
     * Creates a new AigcMessage instance using the specified properties.
     * @function create
     * @memberof AigcMessage
     * @static
     * @param {IAigcMessage=} [properties] Properties to set
     * @returns {AigcMessage} AigcMessage instance
     */
    AigcMessage.create = function create(properties) {
        return new AigcMessage(properties);
    };

    /**
     * Encodes the specified AigcMessage message. Does not implicitly {@link AigcMessage.verify|verify} messages.
     * @function encode
     * @memberof AigcMessage
     * @static
     * @param {IAigcMessage} message AigcMessage message or plain object to encode
     * @param {$protobuf.Writer} [writer] Writer to encode to
     * @returns {$protobuf.Writer} Writer
     */
    AigcMessage.encode = function encode(message, writer) {
        if (!writer)
            writer = $Writer.create();
        if (message.magicnum != null && Object.hasOwnProperty.call(message, "magicnum"))
            writer.uint32(/* id 1, wireType 0 =*/8).int32(message.magicnum);
        if (message.type != null && Object.hasOwnProperty.call(message, "type"))
            writer.uint32(/* id 2, wireType 0 =*/16).int32(message.type);
        if (message.version != null && Object.hasOwnProperty.call(message, "version"))
            writer.uint32(/* id 3, wireType 0 =*/24).int32(message.version);
        if (message.userid != null && Object.hasOwnProperty.call(message, "userid"))
            writer.uint32(/* id 4, wireType 0 =*/32).int32(message.userid);
        if (message.roundid != null && Object.hasOwnProperty.call(message, "roundid"))
            writer.uint32(/* id 5, wireType 0 =*/40).int32(message.roundid);
        if (message.flag != null && Object.hasOwnProperty.call(message, "flag"))
            writer.uint32(/* id 6, wireType 0 =*/48).int32(message.flag);
        if (message.content != null && Object.hasOwnProperty.call(message, "content"))
            writer.uint32(/* id 7, wireType 2 =*/58).string(message.content);
        if (message.timestamp != null && Object.hasOwnProperty.call(message, "timestamp"))
            writer.uint32(/* id 8, wireType 0 =*/64).int64(message.timestamp);
        return writer;
    };

    /**
     * Encodes the specified AigcMessage message, length delimited. Does not implicitly {@link AigcMessage.verify|verify} messages.
     * @function encodeDelimited
     * @memberof AigcMessage
     * @static
     * @param {IAigcMessage} message AigcMessage message or plain object to encode
     * @param {$protobuf.Writer} [writer] Writer to encode to
     * @returns {$protobuf.Writer} Writer
     */
    AigcMessage.encodeDelimited = function encodeDelimited(message, writer) {
        return this.encode(message, writer).ldelim();
    };

    /**
     * Decodes an AigcMessage message from the specified reader or buffer.
     * @function decode
     * @memberof AigcMessage
     * @static
     * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
     * @param {number} [length] Message length if known beforehand
     * @returns {AigcMessage} AigcMessage
     * @throws {Error} If the payload is not a reader or valid buffer
     * @throws {$protobuf.util.ProtocolError} If required fields are missing
     */
    AigcMessage.decode = function decode(reader, length) {
        if (!(reader instanceof $Reader))
            reader = $Reader.create(reader);
        let end = length === undefined ? reader.len : reader.pos + length, message = new $root.AigcMessage();
        while (reader.pos < end) {
            let tag = reader.uint32();
            switch (tag >>> 3) {
            case 1: {
                    message.magicnum = reader.int32();
                    break;
                }
            case 2: {
                    message.type = reader.int32();
                    break;
                }
            case 3: {
                    message.version = reader.int32();
                    break;
                }
            case 4: {
                    message.userid = reader.int32();
                    break;
                }
            case 5: {
                    message.roundid = reader.int32();
                    break;
                }
            case 6: {
                    message.flag = reader.int32();
                    break;
                }
            case 7: {
                    message.content = reader.string();
                    break;
                }
            case 8: {
                    message.timestamp = reader.int64();
                    break;
                }
            default:
                reader.skipType(tag & 7);
                break;
            }
        }
        return message;
    };

    /**
     * Decodes an AigcMessage message from the specified reader or buffer, length delimited.
     * @function decodeDelimited
     * @memberof AigcMessage
     * @static
     * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
     * @returns {AigcMessage} AigcMessage
     * @throws {Error} If the payload is not a reader or valid buffer
     * @throws {$protobuf.util.ProtocolError} If required fields are missing
     */
    AigcMessage.decodeDelimited = function decodeDelimited(reader) {
        if (!(reader instanceof $Reader))
            reader = new $Reader(reader);
        return this.decode(reader, reader.uint32());
    };

    /**
     * Creates an AigcMessage message from a plain object. Also converts values to their respective internal types.
     * @function fromObject
     * @memberof AigcMessage
     * @static
     * @param {Object.<string,*>} object Plain object
     * @returns {AigcMessage} AigcMessage
     */
    AigcMessage.fromObject = function fromObject(object) {
        if (object instanceof $root.AigcMessage)
            return object;
        let message = new $root.AigcMessage();
        if (object.magicnum != null)
            message.magicnum = object.magicnum | 0;
        if (object.type != null)
            message.type = object.type | 0;
        if (object.version != null)
            message.version = object.version | 0;
        if (object.userid != null)
            message.userid = object.userid | 0;
        if (object.roundid != null)
            message.roundid = object.roundid | 0;
        if (object.flag != null)
            message.flag = object.flag | 0;
        if (object.content != null)
            message.content = String(object.content);
        if (object.timestamp != null)
            if ($util.Long)
                (message.timestamp = $util.Long.fromValue(object.timestamp)).unsigned = false;
            else if (typeof object.timestamp === "string")
                message.timestamp = parseInt(object.timestamp, 10);
            else if (typeof object.timestamp === "number")
                message.timestamp = object.timestamp;
            else if (typeof object.timestamp === "object")
                message.timestamp = new $util.LongBits(object.timestamp.low >>> 0, object.timestamp.high >>> 0).toNumber();
        return message;
    };

    /**
     * Creates a plain object from an AigcMessage message. Also converts values to other types if specified.
     * @function toObject
     * @memberof AigcMessage
     * @static
     * @param {AigcMessage} message AigcMessage
     * @param {$protobuf.IConversionOptions} [options] Conversion options
     * @returns {Object.<string,*>} Plain object
     */
    AigcMessage.toObject = function toObject(message, options) {
        if (!options)
            options = {};
        let object = {};
        if (options.defaults) {
            object.magicnum = 0;
            object.type = 0;
            object.version = 0;
            object.userid = 0;
            object.roundid = 0;
            object.flag = 0;
            object.content = "";
            if ($util.Long) {
                let long = new $util.Long(0, 0, false);
                object.timestamp = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
            } else
                object.timestamp = options.longs === String ? "0" : 0;
        }
        if (message.magicnum != null && message.hasOwnProperty("magicnum"))
            object.magicnum = message.magicnum;
        if (message.type != null && message.hasOwnProperty("type"))
            object.type = message.type;
        if (message.version != null && message.hasOwnProperty("version"))
            object.version = message.version;
        if (message.userid != null && message.hasOwnProperty("userid"))
            object.userid = message.userid;
        if (message.roundid != null && message.hasOwnProperty("roundid"))
            object.roundid = message.roundid;
        if (message.flag != null && message.hasOwnProperty("flag"))
            object.flag = message.flag;
        if (message.content != null && message.hasOwnProperty("content"))
            object.content = message.content;
        if (message.timestamp != null && message.hasOwnProperty("timestamp"))
            if (typeof message.timestamp === "number")
                object.timestamp = options.longs === String ? String(message.timestamp) : message.timestamp;
            else
                object.timestamp = options.longs === String ? $util.Long.prototype.toString.call(message.timestamp) : options.longs === Number ? new $util.LongBits(message.timestamp.low >>> 0, message.timestamp.high >>> 0).toNumber() : message.timestamp;
        return object;
    };

    /**
     * Converts this AigcMessage to JSON.
     * @function toJSON
     * @memberof AigcMessage
     * @instance
     * @returns {Object.<string,*>} JSON object
     */
    AigcMessage.prototype.toJSON = function toJSON() {
        return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
    };

    /**
     * Gets the default type url for AigcMessage
     * @function getTypeUrl
     * @memberof AigcMessage
     * @static
     * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
     * @returns {string} The default type url
     */
    AigcMessage.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
        if (typeUrlPrefix === undefined) {
            typeUrlPrefix = "type.googleapis.com";
        }
        return typeUrlPrefix + "/AigcMessage";
    };

    return AigcMessage;
})();

export { $root as default };
