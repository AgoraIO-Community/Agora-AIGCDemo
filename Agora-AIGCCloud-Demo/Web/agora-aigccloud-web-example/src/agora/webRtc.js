
let localTracks = {
  // videoTrack: null,
  audioTrack: null
};

let remoteUsers = {};

// Agora 客户端选项
let options = {
  appid: null,
  channel: null,
  uid: null,
  token: null,
  role: "audience",
  audienceLatency: 2
};

let callbacks = {
  onStreamMessage: null,
  onJoinChannelSuccess: null,
  onLeaveChannel: null,
  onVolumeLevelChange: null
};

let volumeAnimation;

export function setCallbacks({ streamMsgCallback, joinChannelSuccessCallback, leaveChannelCallback, volumeLevelChangeCallback }) {
  callbacks.onStreamMessage = streamMsgCallback;
  callbacks.onJoinChannelSuccess = joinChannelSuccessCallback;
  callbacks.onLeaveChannel = leaveChannelCallback;
  callbacks.onVolumeLevelChange = volumeLevelChangeCallback;
}

let isJoinChannel = false;

let client = null;

async function initAgoraRTC() {
  if (!client) {
    client = AgoraRTC.createClient({
      mode: "live",
      codec: "vp8",
      role: "host",
    })
    handleStreamMsg(client);
  }
}

export async function joinChannel(isLiver, channel, uid, appid, token) {
  console.log("joinChannel isLiver = " + isLiver + " channel = " + channel + " uid = " + uid + " appid = " + appid + " token = " + token)
  await initAgoraRTC();
  options.role = isLiver ? "host" : "audience";
  options.channel = channel;
  options.uid = uid;
  options.appid = appid;
  options.token = token

  await join();
}
async function join() {
  // create Agora client

  if (options.role === "audience") {
    client.setClientRole(options.role, {
      level: options.audienceLatency
    });
    // add event listener to play remote tracks when remote user publishs.
    client.on("user-published", handleUserPublished);
    client.on("user-unpublished", handleUserUnpublished);
  } else {
    client.setClientRole(options.role);
    // add event listener to play remote tracks when remote user publishs.
    client.on("user-published", handleUserPublished);
    client.on("user-unpublished", handleUserUnpublished);
  }

  // join the channel
  options.uid = await client.join(options.appid, options.channel, options.token || null, options.uid || null);
  if (options.role === "host") {
    // create local audio and video tracks
    if (!localTracks.audioTrack) {
      localTracks.audioTrack = await AgoraRTC.createMicrophoneAudioTrack({
        encoderConfig: "music_standard"
      });
    }
    // if (!localTracks.videoTrack) {
    //   localTracks.videoTrack = await AgoraRTC.createCameraVideoTrack();
    // }
    // play local video track
    // localTracks.videoTrack.play("local-player");
    // $("#local-player-name").text(`localTrack(${options.uid})`);
    // $("#joined-setup").css("display", "flex");
    // publish local tracks to channel
    await client.publish(Object.values(localTracks));
    isJoinChannel = true;
    console.log("joinChannel success");
  }
}
export async function leaveChannel() {
  if (!isJoinChannel) {
    console.log("not join channel and return");
    return
  }
  cancelAnimationFrame(volumeAnimation);

  for (const trackName in localTracks) {
    const track = localTracks[trackName];
    if (track) {
      track.stop();
      track.close();
      localTracks[trackName] = undefined;
    }
  }
  remoteUsers = {};

  // leave the channel
  await client.leave();

  isJoinChannel = false;

  if (callbacks.onLeaveChannel) {
    callbacks.onLeaveChannel();
  }
  console.log("leaveChannel success");
}

async function subscribe(user, mediaType) {
  const uid = user.uid;
  // subscribe to a remote user
  await client.subscribe(user, mediaType);
  console.log("subscribe success");
  if (mediaType === 'video') {
    // const player = $(`
    //     <div id="player-${uid}" class="player"></div>
    // `);
    // $("#remote-playerlist").append(player);
    // user.videoTrack.play(`player-${uid}`, {
    //   fit: "contain"
    // });
    getRemoteVideoStats(uid);
  }
  if (mediaType === 'audio') {
    user.audioTrack.play();
  }
  volumeAnimation = requestAnimationFrame(setVolumeWave);

  if (callbacks.onJoinChannelSuccess) {
    callbacks.onJoinChannelSuccess();
  }
}
function handleUserPublished(user, mediaType) {
  //print in the console log for debugging 
  console.log('"user-published" event for remote users is triggered.');
  const id = user.uid;
  remoteUsers[id] = user;
  subscribe(user, mediaType);
}
function handleUserUnpublished(user, mediaType) {
  //print in the console log for debugging 
  console.log('"user-unpublished" event for remote users is triggered.');
  if (mediaType === 'video') {
    const id = user.uid;
    delete remoteUsers[id];
    // $(`#player-wrapper-${id}`).remove();
  }
}

// eslint-disable-next-line no-unused-vars
function sendDataStream(dataBase64String) {
  if (!isJoinChannel) {
    console.log("not join channel and return");
    return -1;
  }
  console.log("sendDataStream dataBase64String = " + dataBase64String)
  let ret = client.sendStreamMessage({ payload: base64ToByteArray(dataBase64String), syncWithAudio: true });
  console.log("sendStreamMessage success ret = " + ret)
  return 0;
}

function base64ToByteArray(base64String) {
  var binaryString = window.atob(base64String);
  var byteArray = new Uint8Array(binaryString.length);
  for (var i = 0; i < binaryString.length; i++) {
    byteArray[i] = binaryString.charCodeAt(i);
  }
  return byteArray;
}

function getRemoteVideoStats(uid) {
  var width = 0;
  var height = 0;
  const interval = setInterval(() => {
    if (!isJoinChannel) {
      clearInterval(interval);
      return;
    }
    let remoteVideoStats = client.getRemoteVideoStats()[uid];
    let videoHeight = remoteVideoStats.receiveResolutionHeight;
    let videoWidth = remoteVideoStats.receiveResolutionWidth;
    if (0 != videoHeight && 0 != videoWidth) {
      if (width != videoWidth || height != videoHeight) {
        width = videoWidth;
        height = videoHeight;
        console.log("onVideoSizeChange width = " + width + " height = " + height)
      }
    }
  }, 1000);
}
function handleStreamMsg(client) {
  client.on("stream-message", async (uid, data) => {
    if (callbacks.onStreamMessage) {
      callbacks.onStreamMessage(uid, data);
    }
  });
}

// show real-time volume while adjusting device. 
function setVolumeWave() {
  volumeAnimation = requestAnimationFrame(setVolumeWave);
  if (callbacks.onVolumeLevelChange && localTracks.audioTrack != null) {
    callbacks.onVolumeLevelChange(localTracks.audioTrack.getVolumeLevel());
  }
}

export async function mute(enable) {
  if (!localTracks.audioTrack) {
    return;
  }
  if (enable) {
    await localTracks.audioTrack.setEnabled(false);
  } else {
    await localTracks.audioTrack.setEnabled(true);
  }
}