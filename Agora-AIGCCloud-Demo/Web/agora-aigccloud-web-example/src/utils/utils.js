export function generateRandomChannelId() {
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    const charactersLength = characters.length;
    for (let i = 0; i < 10; i++) {
      result += characters.charAt(Math.floor(Math.random() * charactersLength));
    }
    return result;
  }
  
  export function generateRandomUid() {
    return Math.floor(Math.random() * 10000);
  }

  export function formatDateWithMilliseconds() {
    const date = new Date();
    
    // 使用 toLocaleString 获取年月日和时分秒
    const dateTimeString = date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
  
    // 获取毫秒并补齐到三位
    const milliseconds = date.getMilliseconds().toString().padStart(3, '0');
  
    // 组合日期时间和毫秒
    return `${dateTimeString}.${milliseconds}`;
  }
  