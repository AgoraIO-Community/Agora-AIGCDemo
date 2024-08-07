<template>
  <div class="container">
    <div class="settings">
      <h3>设置</h3>
      <div class="setting-group" v-if="isMainPage">
        STT Mode
        <select v-model="sttMode">
          <option value="0">Quick</option>
          <option value="1">Normal</option>
        </select>

      </div>

      <div class="setting-group" v-if="isMainPage">
        TTS Mode
        <select v-model="ttsMode">
          <option value="ali_cosy">阿里ali_cosy</option>
          <option value="ali_tts">阿里ali_tts</option>
        </select>
      </div>

      <div class="setting-group" v-if="isMainPage">
        LLM Mode
        <select v-model="llmMode">
          <option value="qwen">通义千问</option>
          <option value="tiangong">天工</option>
        </select>
      </div>

      <div class="setting-group">
        Channel ID:<input v-model="channelId" type="text" placeholder="请输入频道号">
      </div>

      <div class="setting-group">
        <button v-if="!isJoinChannel" @click="startCloudService" :disabled="isLoading" class="join_button">加入</button>
        <button v-else @click="stopCloudService" :disabled="isLoading" class="join_button">离开</button>
      </div>
      <br><br>

      <!-- 新增的麦克风音量和静音控制 -->
      <div class="setting-group">
        <h4>麦克风</h4>
        <div class="microphone-control">
          <button @click="toggleMute" class="mute-button">
            <img :src="microphoneIcon" :alt="isMuted ? 'Microphone Off' : 'Microphone On'">
          </button>
          <div class="volume-wave">
            <div class="volume-bar" v-for="n in 20" :key="n" :class="{ active: n <= Math.ceil(currentVolume / 5) }">
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="message-list" ref="messageList">
      <div v-for="item in messageList" :key="item.sid" class="message-item">
        <template v-if="item.startTimestamp !== 0">
          <span class="startTimestamp">{{ item.startTimestamp }}</span>&nbsp;
        </template>
        <template v-if="item.endTimestamp !== 0">
          ~&nbsp;<span class="endTimestamp">{{ item.endTimestamp }}</span>&nbsp;
        </template>
        <span class="title">{{ item.title }}</span>&nbsp;
        <span class="message">{{ item.message }}</span>
      </div>
    </div>
  </div>
</template>


<script>
import { joinChannel, leaveChannel, setCallbacks, mute } from '../agora/webRtc.js';
import { generateRandomChannelId, generateRandomUid, formatDateWithMilliseconds, formatTimestamp } from '../utils/utils.js';
import NetworkService from '../utils/NetworkService.js';
import { AigcMessage } from '../proto/AigcMessage';

import microOnIcon from '../../img/micro_on.png'
import microOffIcon from '../../img/micro_off.png'

export default {
  name: 'MainUI',
  setup() {
  },
  data() {
    return {
      appId: import.meta.env.VITE_AGORA_APP_ID,
      cloudHost: import.meta.env.VITE_AGORA_CLOUD_HOST,
      channelId: '',
      uid: 0,
      isJoinChannel: false,
      isLoading: false,
      ttsMode: 'ali_tts',
      sttMode: 0,
      llmMode: 'qwen',
      taskId: '',
      regionCode: 'CN',
      messageList: [],
      mConversationIndex: 0,
      isMuted: false,
      currentVolume: 0,
    }
  },
  computed: {
    microphoneIcon() {
      return this.isMuted ? microOffIcon : microOnIcon
    },
    isMainPage() {
      return this.$route.path === '/'
    },
    isTianGongPage() {
      return this.$route.path === '/tiangong'
    },
    isTongYiPage() {
      return this.$route.path === '/tongyi'
    }
  },
  created() {
    if (this.isTianGongPage) {
      this.llmMode = 'tiangong'
    }
  },
  async mounted() {
    setCallbacks({
      streamMsgCallback: this.handleStreamMessage, joinChannelSuccessCallback: this.handleJoinChannelSuccess, leaveChannelCallback: this.handleLeaveChannel, volumeLevelChangeCallback: this.handleVolumeLevelChange
    });
    // try {
    //   this.protoRoot = await ProtobufUtil.loadProto('/src/proto/AigcMessage.proto');
    //   console.log('Proto root loaded:', this.protoRoot);
    // } catch (error) {
    //   console.error('Failed to load proto file:', error);
    // }
  },
  updated() {
    this.$nextTick(this.scrollToBottom);
  },
  methods: {
    startCloudService() {
      this.isLoading = true;
      if (!this.channelId) {
        this.channelId = generateRandomChannelId();
      }
      if (!this.uid) {
        this.uid = generateRandomUid();
      }
      NetworkService.post(this.cloudHost + '/' + this.regionCode + '/v1/projects/' + this.appId + '/aigc-workers/local/start',  // url
        {
          channel_name: this.channelId,
          user_conf: [{
            speak_uid: this.uid,
            rtc_uid: this.uid + 1,
            inLanguages: ["zh-CN"],
            outLanguages: ["zh-CN"]
          }],
          aliYun_mode: this.sttMode,
          tts_select: this.ttsMode,
          llm_select: this.llmMode
        },  // request body
        { 'Content-Type': 'application/json' }  // headers
      )
        .then(response => {
          console.log('startCloudService Success:', response);
          if (response.code == 0) {
            this.taskId = response.data.id;
            this.mConversationIndex++;
            this.joinChannel()
          } else {
            this.isLoading = false;
            alert(response.message)
          }
        })
        .catch(error => {
          console.error('startCloudService Error:', error);
          this.isLoading = false;
        });
    },
    stopCloudService() {
      this.isLoading = true;
      NetworkService.delete(this.cloudHost + '/' + this.regionCode + '/v1/projects/' + this.appId + '/aigc-workers/' + this.taskId + '/local', {}, { 'Content-Type': 'application/json' })
        .then(response => {
          console.log('stopCloudService Success:', response);
          if (response.code == 0) {
            this.leaveChannel()
          } else {
            this.isLoading = false;
            alert(response.message)
          }
        })
        .catch(error => {
          console.error('stopCloudService Error:', error);
          this.isLoading = false;
        });
    },
    async handleStreamMessage(uid, data) {
      try {
        const aigcMessage = AigcMessage.decode(data);
        console.log('aigcMessage message:', aigcMessage);
        if (aigcMessage.type == 100) {
          //stt message
          if (!aigcMessage.content) return;
          const message = aigcMessage.content + (aigcMessage.flag === 1 ? '[FIN]' : '');
          const sid = `${this.mConversationIndex}${aigcMessage.roundid}stt`;
          const title = `用户[${aigcMessage.userid}]说：`;

          const endTimestamp = aigcMessage.flag === 1 ? formatTimestamp(aigcMessage.timestamp) : 0;
          this.updateMessage({
            sid,
            startTimestamp: formatTimestamp(aigcMessage.timestamp),
            endTimestamp,
            title,
            message,
            isAppend: false
          });
        } else if (aigcMessage.type == 120) {
          //llm message
          const message = aigcMessage.content + (aigcMessage.flag === 1 ? '[FIN]' : '');
          if (!message) return;

          const endTimestamp = aigcMessage.flag === 1 ? formatTimestamp(aigcMessage.timestamp) : 0;
          const sid = `${this.mConversationIndex}${aigcMessage.roundid}llm`;
          const title = `AI说：`;

          this.updateMessage({
            sid,
            startTimestamp: formatTimestamp(aigcMessage.timestamp),
            endTimestamp,
            title,
            message,
            isAppend: true
          });
        } else if (aigcMessage.type == 130) {
          //tts message
          const sid = `${this.mConversationIndex}${aigcMessage.roundid}tts${aigcMessage.flag}`;
          const title = aigcMessage.flag === 0 ? '开始播放语音' : aigcMessage.flag === 1 ? '结束播放语音' : '播放语音中';
          this.updateMessage({ sid: sid, startTimestamp: formatTimestamp(aigcMessage.timestamp), endTimestamp: 0, title: title, message: "", isAppend: false });
        } else if (aigcMessage.type == 140) {
          //conversation message
          const sid = `${this.mConversationIndex}${aigcMessage.roundid}conversation${aigcMessage.flag}`;
          const title = aigcMessage.flag === 0 ? '会话开始' : aigcMessage.flag === 1 ? '会话结束' : '会话中';
          this.updateMessage({ sid: sid, startTimestamp: formatTimestamp(aigcMessage.timestamp), endTimestamp: 0, title: title, message: "", isAppend: false });
        } else {
          console.log('Unknown message type:', aigcMessage.type);
        }

      } catch (error) {
        console.error('Error handling protobuf:', error);
      }
    },
    async handleJoinChannelSuccess() {
      this.updateMessage({ sid: this.mConversationIndex + "join", startTimestamp: 0, endTimestamp: 0, title: 'Join channel(' + this.channelId + ')', message: '', isAppend: false });
      //this.updateMessage({ sid: this.mConversationIndex + "join", date: formatDateWithMilliseconds(), title: 'Join channel(' + this.channelId + ')', message: '', isAppend: false });
      this.isJoinChannel = true;
      this.isLoading = false;
    },
    async handleLeaveChannel() {
      this.updateMessage({ sid: this.mConversationIndex + "leave", startTimestamp: 0, endTimestamp: 0, title: 'Leave channel(' + this.channelId + ')', message: '', isAppend: false });
      //this.updateMessage({ sid: this.mConversationIndex + "leave", date: formatDateWithMilliseconds(), title: 'Leave channel(' + this.channelId + ')', message: '', isAppend: false });

      this.resetData();
      this.stopRequest();
    },
    async handleVolumeLevelChange(volumeLevel) {
      this.currentVolume = volumeLevel * 100;
    },
    async joinChannel() {
      if (!this.channelId) {
        this.channelId = generateRandomChannelId();
      }
      if (!this.uid) {
        this.uid = generateRandomUid();
      }
      const isLiver = true;
      const channel = this.channelId;
      const uid = this.uid;
      const appid = this.appId;
      const token = '';

      await joinChannel(isLiver, channel, uid, appid, token);

    },
    async leaveChannel() {
      await leaveChannel();
    },
    resetData() {
      this.isJoinChannel = false;
      this.channelId = '';
      this.uid = 0;
      this.isLoading = false;
      this.currentVolume = 0;
    },
    stopRequest() {
      NetworkService.stop();
    },

    updateMessage({ sid, startTimestamp, endTimestamp, title, message, isAppend }) {
      const existingMessageIndex = this.messageList.findIndex(item => item.sid === sid);

      if (existingMessageIndex !== -1) {
        const processStartTimestamp = this.messageList[existingMessageIndex].startTimestamp !== 0 ? this.messageList[existingMessageIndex].startTimestamp : startTimestamp;
        // 更新现有消息
        this.messageList[existingMessageIndex] = {
          ...this.messageList[existingMessageIndex],
          title,
          message: isAppend
            ? this.messageList[existingMessageIndex].message + message
            : message,
          processStartTimestamp,
          endTimestamp
        };
      } else {
        // 添加新消息
        this.messageList.push({ sid, startTimestamp, endTimestamp, title, message });
      }
    },
    scrollToBottom() {
      const messageList = this.$refs.messageList;
      if (messageList) {
        messageList.scrollTop = messageList.scrollHeight;
      }
    },
    toggleMute() {
      if (this.isJoinChannel) {
        this.isMuted = !this.isMuted;
        // 实现静音/取消静音的逻辑
        if (this.isMuted) {
          // 执行静音操作
          mute(true);
        } else {
          // 执行取消静音操作
          mute(false);
        }
      }
    },
  }
}
</script>

<style scoped>
.container {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.settings {
  width: 25%;
  padding: 20px;
  background-color: #f0f0f0;
  overflow-y: auto;
}

.setting-group {
  margin-bottom: 20px;
}

.setting-group h4 {
  margin-bottom: 10px;
}

input[type="text"] {
  width: 40%;
  padding: 5px;
  margin-top: 5px;
}

.join_button {
  width: 100%;
  padding: 10px;
  margin-top: 10px;
}

.message-list {
  width: 60%;
  padding: 20px;
  /* 设置一个最大高度，以启用滚动 */
  height: 95%;
  /* 设置适当的高度 */
  overflow-y: auto;
  /* 启用垂直滚动 */
}

.message-item {
  margin-bottom: 10px;
  padding: 10px;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  text-align: left;
  /* 确保内容左对齐 */
}

.message-item div {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  /* 确保子元素左对齐 */
}

.message-item .date {
  font-weight: bold;
  margin-bottom: 5px;
}

.message-item .title {
  font-weight: bold;
  margin-bottom: 5px;
}

.message-item .message {
  word-break: break-word;
  /* 确保长文本会换行 */
}

.microphone-control {
  display: flex;
  align-items: center;
}

.mute-button {
  background: none;
  border: 1px solid transparent;
  /* 默认无边框 */
  cursor: pointer;
  padding: 5px;
  margin-right: 10px;
  flex-shrink: 0;
  transition: border-color 0.3s ease;
}

.mute-button:active {
  border-color: #e9ecef;
  /* 点击时边框颜色 */
}

.mute-button img {
  width: 24px;
  height: 24px;
  vertical-align: middle;
}

.volume-wave {
  display: flex;
  align-items: center;
  width: 100%;
  height: 24px;
  background-color: #e9ecef;
  border-radius: 0.25rem;
  overflow: hidden;
}

.volume-bar {
  flex-grow: 1;
  height: 100%;
  background-color: #007bff;
  margin: 0 1px;
  opacity: 0.3;
  transition: opacity 0.3s ease;
}

.volume-bar.active {
  opacity: 1;
}
</style>