<template>
  <div class="container">
    <div class="left-side">
      <el-card class="settings">
        <h3>设置</h3>
        <el-form label-position="top" label-width="100px">
          <el-form-item label="STT Mode" v-if="isMainPage">
            <el-select v-model="sttMode" placeholder="请选择">
              <el-option label="Quick" :value="0"></el-option>
              <el-option label="Normal" :value="1"></el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="TTS Mode" v-if="isMainPage">
            <el-select v-model="ttsMode" placeholder="请选择">
              <el-option label="阿里ali_cosy" value="ali_cosy"></el-option>
              <el-option label="阿里ali_tts" value="ali_tts"></el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="LLM Mode" v-if="isMainPage">
            <el-select v-model="llmMode" placeholder="请选择">
              <el-option label="通义千问" value="qwen"></el-option>
              <el-option label="天工" value="tiangong"></el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="Channel ID">
            <el-input v-model="channelId" placeholder="请输入频道号"></el-input>
          </el-form-item>

          <el-form-item class="button-group">
            <el-button v-if="!isJoinChannel" @click="startCloudService" :loading="isLoading"
              type="primary">加入</el-button>
            <el-button v-else @click="stopCloudService" :loading="isLoading" type="danger">离开</el-button>
          </el-form-item>

          <h3>麦克风</h3>
          <!-- 新增的麦克风音量和静音控制 -->
          <div class="microphone-header">

            <div class="microphone-control">
              <el-button @click="toggleMute" circle>
                <img :src="microphoneIcon" :alt="isMuted ? 'Microphone Off' : 'Microphone On'" class="microphone-icon">
              </el-button>
              <div class="volume-wave">
                <div class="volume-bar" v-for="n in 20" :key="n" :class="{ active: n <= Math.ceil(currentVolume / 5) }">
                </div>
              </div>
            </div>

          </div>

        </el-form>
      </el-card>
    </div>
    <div class="right-side">
      <el-card class="message-list" ref="messageList">
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
      </el-card>
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
      conversationDataMap: new Map(),
      enableAins: true,
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
    //this.$nextTick(this.scrollToBottom);
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

          let startTimestamp = 0;
          const conversationData = this.getConversationData(aigcMessage.roundid);
          if (null == conversationData) {
            this.initConversationData(aigcMessage.roundid);
            startTimestamp = aigcMessage.timestamp;
          } else {
            if (conversationData.sttStartTimestamp == null || conversationData.sttStartTimestamp == 0) {
              startTimestamp = aigcMessage.timestamp;
            } else {
              startTimestamp = conversationData.sttStartTimestamp;
            }
          }

          if (startTimestamp != 0) {
            this.updateConversationData(aigcMessage.roundid, 'sttStartTimestamp', startTimestamp);
          }

          const endTimestamp = aigcMessage.flag === 1 ? aigcMessage.timestamp : 0;
          if (endTimestamp != 0) {
            this.updateConversationData(aigcMessage.roundid, 'sttEndTimestamp', endTimestamp);
          }

          this.updateMessage({
            sid,
            startTimestamp: formatTimestamp(startTimestamp),
            endTimestamp: formatTimestamp(endTimestamp),
            title,
            message,
            isAppend: false
          });
        } else if (aigcMessage.type == 120) {
          //llm message
          const message = aigcMessage.content + (aigcMessage.flag === 1 ? '[FIN]' : '');
          if (!message) return;
          const sid = `${this.mConversationIndex}${aigcMessage.roundid}llm`;
          const title = `AI说：`;

          let startTimestamp = 0;
          const conversationData = this.getConversationData(aigcMessage.roundid);
          if (null == conversationData) {
            this.initConversationData(aigcMessage.roundid);
            startTimestamp = aigcMessage.timestamp;
          } else {
            if (conversationData.llmStartTimestamp == null || conversationData.llmStartTimestamp == 0) {
              startTimestamp = aigcMessage.timestamp;
            } else {
              startTimestamp = conversationData.llmStartTimestamp;
            }
          }

          if (startTimestamp != 0) {
            this.updateConversationData(aigcMessage.roundid, 'llmStartTimestamp', startTimestamp);
          }

          const endTimestamp = aigcMessage.flag === 1 ? aigcMessage.timestamp : 0;
          if (endTimestamp != 0) {
            this.updateConversationData(aigcMessage.roundid, 'llmEndTimestamp', endTimestamp);
          }

          this.updateMessage({
            sid,
            startTimestamp: formatTimestamp(startTimestamp),
            endTimestamp: formatTimestamp(endTimestamp),
            title,
            message,
            isAppend: true
          });
        } else if (aigcMessage.type == 130) {
          //tts message
          const sid = `${this.mConversationIndex}${aigcMessage.roundid}tts${aigcMessage.flag}`;
          const title = aigcMessage.flag === 0 ? '开始播放语音' : aigcMessage.flag === 1 ? '结束播放语音' : '播放语音中';
          const conversationData = this.getConversationData(aigcMessage.roundid);
          let startTimestamp = 0;
          let endTimestamp = 0;
          if (null == conversationData) {
            this.initConversationData(aigcMessage.roundid);
            if (aigcMessage.flag === 0) {
              startTimestamp = aigcMessage.timestamp;
            } else if (aigcMessage.flag === 1) {
              endTimestamp = aigcMessage.timestamp;
            }
          } else {
            if (aigcMessage.flag === 0) {
              if (conversationData.ttsStartTimestamp == null || conversationData.ttsStartTimestamp == 0) {
                startTimestamp = aigcMessage.timestamp;
              } else {
                startTimestamp = conversationData.ttsStartTimestamp;
              }
              endTimestamp = conversationData.ttsEndTimestamp;
            } else if (aigcMessage.flag === 1) {
              if (conversationData.ttsEndTimestamp == null || conversationData.ttsEndTimestamp == 0) {
                endTimestamp = aigcMessage.timestamp;
              } else {
                endTimestamp = conversationData.ttsEndTimestamp;
              }
              startTimestamp = conversationData.ttsStartTimestamp;
            }
          }

          if (startTimestamp != 0) {
            this.updateConversationData(aigcMessage.roundid, 'ttsStartTimestamp', startTimestamp);
          }

          if (endTimestamp != 0) {
            this.updateConversationData(aigcMessage.roundid, 'ttsEndTimestamp', endTimestamp);
          }

          let message = '';
          if (aigcMessage.flag === 0) {
            message = `(STT耗时：${conversationData.sttEndTimestamp - conversationData.conversationStartTimestamp}ms；
                          LLM第一个返回耗时：${conversationData.llmStartTimestamp - conversationData.sttEndTimestamp}ms；
                          TTS耗时：${startTimestamp - conversationData.llmStartTimestamp}ms；
                          agent总体耗时：${startTimestamp - conversationData.conversationStartTimestamp}ms)`;
          }

          this.updateMessage({ sid: sid, startTimestamp: formatTimestamp(aigcMessage.timestamp), endTimestamp: 0, title: title, message: message, isAppend: false });
        } else if (aigcMessage.type == 140) {
          //conversation message
          const sid = `${this.mConversationIndex}${aigcMessage.roundid}conversation${aigcMessage.flag}`;
          const title = aigcMessage.flag === 0 ? '会话开始' : aigcMessage.flag === 1 ? '会话结束' : '会话中';
          const conversationData = this.getConversationData(aigcMessage.roundid);
          let startTimestamp = 0;
          let endTimestamp = 0;
          if (null == conversationData) {
            this.initConversationData(aigcMessage.roundid);
            if (aigcMessage.flag === 0) {
              startTimestamp = aigcMessage.timestamp;
            } else if (aigcMessage.flag === 1) {
              endTimestamp = aigcMessage.timestamp;
            }
          } else {
            if (aigcMessage.flag === 0) {
              if (conversationData.conversationStartTimestamp == null || conversationData.conversationStartTimestamp == 0) {
                startTimestamp = aigcMessage.timestamp;
              } else {
                startTimestamp = conversationData.conversationStartTimestamp;
              }
              endTimestamp = conversationData.conversationEndTimestamp;
            } else if (aigcMessage.flag === 1) {
              if (conversationData.conversationEndTimestamp == null || conversationData.conversationEndTimestamp == 0) {
                endTimestamp = aigcMessage.timestamp;
              } else {
                endTimestamp = conversationData.conversationEndTimestamp;
              }
              startTimestamp = conversationData.conversationStartTimestamp;
            }
          }

          if (startTimestamp != 0) {
            this.updateConversationData(aigcMessage.roundid, 'conversationStartTimestamp', startTimestamp);
          }

          if (endTimestamp != 0) {
            this.updateConversationData(aigcMessage.roundid, 'conversationEndTimestamp', endTimestamp);
          }

          this.updateMessage({ sid: sid, startTimestamp: formatTimestamp(startTimestamp), endTimestamp: 0, title: title, message: "", isAppend: false });
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
      this.conversationDataMap.clear();
    },
    stopRequest() {
      NetworkService.stop();
    },

    updateMessage({ sid, startTimestamp, endTimestamp, title, message, isAppend }) {
      const existingMessageIndex = this.messageList.findIndex(item => item.sid === sid);

      if (existingMessageIndex !== -1) {
        // 更新现有消息
        this.messageList[existingMessageIndex] = {
          ...this.messageList[existingMessageIndex],
          title,
          message: isAppend
            ? this.messageList[existingMessageIndex].message + message
            : message,
          startTimestamp,
          endTimestamp
        };
      } else {
        // 添加新消息
        this.messageList.push({ sid, startTimestamp, endTimestamp, title, message });
      }
      this.scrollToBottom();
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const messageListEl = this.$refs.messageList.$el;
        messageListEl.scrollTop = messageListEl.scrollHeight;
      });

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
    initConversationData(index) {
      this.conversationDataMap.set(index, {
        vadStartTimestamp: null,
        vadEndTimestamp: null,
        sttStartTimestamp: null,
        sttEndTimestamp: null,
        llmStartTimestamp: null,
        llmEndTimestamp: null,
        ttsStartTimestamp: null,
        ttsEndTimestamp: null,
        conversationStartTimestamp: null,
        conversationEndTimestamp: null
      });
    },
    updateConversationData(index, field, value) {
      if (!this.conversationDataMap.has(index)) {
        this.initConversationData(index);
      }

      const data = this.conversationDataMap.get(index);
      data[field] = value;
      this.conversationDataMap.set(index, data);
    },
    getConversationData(index) {
      return this.conversationDataMap.get(index);
    }
  }
}
</script>

<style scoped>
.container {
  display: flex;
}

.left-side {
  position: fixed;
  width: 25%;
  height: 100vh;
  top: 0;
  left: 0;
  overflow-y: auto;
  padding: 20px;
  box-sizing: border-box;
}

.right-side {
  position: fixed;
  width: 75%;
  height: 100vh;
  top: 0;
  right: 0;
  overflow-y: auto;
  padding: 20px;
  box-sizing: border-box;
}

.settings,
.message-list {
  margin-bottom: 20px;
}

.microphone-control {
  display: flex;
  align-items: center;
}

.volume-wave {
  display: flex;
  margin-left: 10px;
  flex-grow: 1; /* 使 volume-wave 占据剩余空间 */
}

.volume-bar {
  flex-grow: 1; /* 使每个 volume-bar 占据相同的剩余空间 */
  height: 20px;
  background-color: #ccc;
  margin-right: 2px;
}

.volume-bar.active {
  background-color: #409EFF;
}

.message-list {
  margin-top: 0px;
  /* 顶部留出距离 */
  margin-bottom: 0px;
  /* 底部留出距离 */
  max-height: calc(100vh);
  /* 设置最大高度为整个屏幕减去上下的距离 */
  overflow-y: auto;
  /* 内容超出时滚动 */
}

.message-item {
  margin-bottom: 10px;
  text-align: left;
  /* 确保文本内容靠左对齐 */
}

.button-group {
  display: flex;
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */
}

.button-group .el-button {
  width: 100%;
  /* 设置按钮宽度 */
  margin: 0 10px;
  /* 设置按钮间距 */
}

.microphone-icon {
  width: 24px;
  /* 调整图片宽度 */
  height: 24px;
  /* 调整图片高度 */
}

.startTimestamp,
.endTimestamp,
.title {
  font-weight: bold; /* 设置字体加粗 */
}

.message-item:nth-child(odd) {
  background-color: #f9f9f9; /* 奇数项背景颜色 */
}

.message-item:nth-child(even) {
  background-color: #e9e9e9; /* 偶数项背景颜色 */
}

</style>