# 原生WebSocket客户端使用指南

## 🔌 **连接配置**

### **连接地址**
```javascript
// 开发环境
const WS_URL = "ws://localhost:8080/api/ws";

// 生产环境（HTTPS）
const WS_URL = "wss://your-domain.com/api/ws";
```

## 📋 **订阅路径清单**

### **1. 批次相关订阅**
| 路径 | 用途 | 消息类型 |
|------|------|----------|
| `/topic/batch/{batchId}` | 特定批次消息 | 状态变更、进度更新 |
| `/topic/batches/all` | 所有批次状态 | 批次列表更新 |

### **2. 运行相关订阅**
| 路径 | 用途 | 消息类型 |
|------|------|----------|
| `/topic/run/{runId}` | 特定运行消息 | 任务状态、问题完成 |
| `/topic/progress/run/{runId}` | 运行进度消息 | 进度百分比更新 |

### **3. 全局订阅**
| 路径 | 用途 | 消息类型 |
|------|------|----------|
| `/topic/global` | 全局广播消息 | 系统通知、公告 |
| `/topic/errors` | 全局错误消息 | 系统级错误 |

### **4. 状态和错误订阅**
| 路径 | 用途 | 消息类型 |
|------|------|----------|
| `/topic/status/{entityId}` | 实体状态变更 | 状态更新 |
| `/topic/error/{entityId}` | 实体错误消息 | 错误通知 |

### **5. 用户专有订阅**
| 路径 | 用途 | 消息类型 |
|------|------|----------|
| `/user/{userId}/queue/messages` | 用户专有消息 | 个人通知 |
| `/user/queue/errors` | 用户错误消息 | 个人错误通知 |

## 🛠️ **完整实现示例**

### **WebSocket管理器类**
```javascript
class NativeWebSocketManager {
    constructor(url) {
        this.url = url;
        this.ws = null;
        this.stompClient = null;
        this.connected = false;
        this.subscriptions = new Map();
        this.messageHandlers = new Map();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectInterval = 3000;
    }

    /**
     * 连接到WebSocket服务器
     */
    async connect() {
        return new Promise((resolve, reject) => {
            try {
                // 创建原生WebSocket连接
                this.ws = new WebSocket(this.url);
                
                // 创建STOMP客户端
                this.stompClient = Stomp.over(this.ws);
                
                // 启用调试日志
                this.stompClient.debug = (str) => {
                    console.log(`STOMP: ${str}`);
                };

                // 连接到服务器
                this.stompClient.connect(
                    {}, // 连接头信息
                    (frame) => {
                        console.log('WebSocket连接成功:', frame);
                        this.connected = true;
                        this.reconnectAttempts = 0;
                        resolve(frame);
                    },
                    (error) => {
                        console.error('WebSocket连接失败:', error);
                        this.connected = false;
                        this.handleReconnect();
                        reject(error);
                    }
                );

                // 监听WebSocket事件
                this.ws.onclose = (event) => {
                    console.log('WebSocket连接关闭:', event);
                    this.connected = false;
                    this.handleReconnect();
                };

                this.ws.onerror = (error) => {
                    console.error('WebSocket错误:', error);
                };

            } catch (error) {
                console.error('创建WebSocket连接失败:', error);
                reject(error);
            }
        });
    }

    /**
     * 断开连接
     */
    disconnect() {
        if (this.stompClient && this.connected) {
            // 取消所有订阅
            this.subscriptions.forEach((subscription) => {
                subscription.unsubscribe();
            });
            this.subscriptions.clear();

            // 断开STOMP连接
            this.stompClient.disconnect(() => {
                console.log('WebSocket连接已断开');
                this.connected = false;
            });
        }
    }

    /**
     * 处理重连逻辑
     */
    handleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            
            setTimeout(() => {
                this.connect().catch(error => {
                    console.error('重连失败:', error);
                });
            }, this.reconnectInterval);
        } else {
            console.error('达到最大重连次数，停止重连');
        }
    }

    /**
     * 订阅消息
     */
    subscribe(destination, callback) {
        if (!this.connected || !this.stompClient) {
            throw new Error('WebSocket未连接');
        }

        const subscription = this.stompClient.subscribe(destination, (message) => {
            try {
                const data = JSON.parse(message.body);
                callback(data);
            } catch (error) {
                console.error('解析消息失败:', error, message.body);
            }
        });

        this.subscriptions.set(destination, subscription);
        console.log(`已订阅: ${destination}`);
        
        return subscription;
    }

    /**
     * 取消订阅
     */
    unsubscribe(destination) {
        const subscription = this.subscriptions.get(destination);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(destination);
            console.log(`已取消订阅: ${destination}`);
        }
    }

    /**
     * 发送消息
     */
    send(destination, message = {}) {
        if (!this.connected || !this.stompClient) {
            throw new Error('WebSocket未连接');
        }

        this.stompClient.send(destination, {}, JSON.stringify(message));
    }

    // ==================== 业务订阅方法 ====================

    /**
     * 订阅批次消息
     */
    subscribeBatch(batchId, callback) {
        const destination = `/topic/batch/${batchId}`;
        const subscription = this.subscribe(destination, callback);
        
        // 发送订阅确认
        this.send(`/app/batch/${batchId}/subscribe`, {
            batchId: batchId,
            timestamp: new Date().toISOString()
        });
        
        return subscription;
    }

    /**
     * 订阅运行消息
     */
    subscribeRun(runId, callback) {
        const destination = `/topic/run/${runId}`;
        const subscription = this.subscribe(destination, callback);
        
        // 发送订阅确认
        this.send(`/app/run/${runId}/subscribe`, {
            runId: runId,
            timestamp: new Date().toISOString()
        });
        
        return subscription;
    }

    /**
     * 订阅运行进度
     */
    subscribeRunProgress(runId, callback) {
        const destination = `/topic/progress/run/${runId}`;
        return this.subscribe(destination, callback);
    }

    /**
     * 订阅全局消息
     */
    subscribeGlobal(callback) {
        const destination = '/topic/global';
        const subscription = this.subscribe(destination, callback);
        
        // 发送订阅确认
        this.send('/app/global/subscribe', {
            timestamp: new Date().toISOString()
        });
        
        return subscription;
    }

    /**
     * 订阅所有批次状态
     */
    subscribeAllBatches(callback) {
        const destination = '/topic/batches/all';
        const subscription = this.subscribe(destination, callback);
        
        // 发送订阅确认
        this.send('/app/batches/all/subscribe', {
            timestamp: new Date().toISOString()
        });
        
        return subscription;
    }

    /**
     * 订阅状态变更
     */
    subscribeStatus(entityId, callback) {
        const destination = `/topic/status/${entityId}`;
        return this.subscribe(destination, callback);
    }

    /**
     * 订阅错误消息
     */
    subscribeErrors(entityId, callback) {
        const destination = `/topic/error/${entityId}`;
        return this.subscribe(destination, callback);
    }

    /**
     * 订阅全局错误
     */
    subscribeGlobalErrors(callback) {
        const destination = '/topic/errors';
        return this.subscribe(destination, callback);
    }

    /**
     * 订阅用户专有消息
     */
    subscribeUserMessages(userId, callback) {
        const destination = `/user/${userId}/queue/messages`;
        return this.subscribe(destination, callback);
    }

    /**
     * 订阅用户错误消息
     */
    subscribeUserErrors(callback) {
        const destination = '/user/queue/errors';
        return this.subscribe(destination, callback);
    }
}
```

### **使用示例**
```javascript
// 创建WebSocket管理器
const wsManager = new NativeWebSocketManager('ws://localhost:8080/api/ws');

// 连接到服务器
wsManager.connect()
    .then(() => {
        console.log('WebSocket连接成功');
        
        // 订阅批次消息
        wsManager.subscribeBatch(123, (message) => {
            console.log('收到批次消息:', message);
            handleBatchMessage(message);
        });
        
        // 订阅运行消息
        wsManager.subscribeRun(456, (message) => {
            console.log('收到运行消息:', message);
            handleRunMessage(message);
        });
        
        // 订阅运行进度
        wsManager.subscribeRunProgress(456, (message) => {
            console.log('收到进度更新:', message);
            updateProgressBar(message.payload.progress);
        });
        
        // 订阅全局消息
        wsManager.subscribeGlobal((message) => {
            console.log('收到全局消息:', message);
            showGlobalNotification(message);
        });
        
        // 订阅所有批次状态
        wsManager.subscribeAllBatches((message) => {
            console.log('收到批次状态更新:', message);
            updateBatchList(message.payload.batches);
        });
        
        // 订阅错误消息
        wsManager.subscribeGlobalErrors((message) => {
            console.error('收到错误消息:', message);
            showErrorNotification(message);
        });
        
    })
    .catch(error => {
        console.error('WebSocket连接失败:', error);
    });

// 消息处理函数
function handleBatchMessage(message) {
    switch (message.type) {
        case 'STATUS_CHANGE':
            updateBatchStatus(message.payload);
            break;
        case 'PROGRESS_UPDATE':
            updateBatchProgress(message.payload);
            break;
        case 'TASK_COMPLETED':
            showTaskCompletedNotification(message.payload);
            break;
        default:
            console.log('未知消息类型:', message.type);
    }
}

function handleRunMessage(message) {
    switch (message.type) {
        case 'QUESTION_STARTED':
            updateQuestionStatus(message.payload, 'started');
            break;
        case 'QUESTION_COMPLETED':
            updateQuestionStatus(message.payload, 'completed');
            break;
        case 'TASK_FAILED':
            showTaskFailedNotification(message.payload);
            break;
        default:
            console.log('未知运行消息类型:', message.type);
    }
}

// 页面卸载时断开连接
window.addEventListener('beforeunload', () => {
    wsManager.disconnect();
});
```

### **Vue.js组件示例**
```javascript
// WebSocket组合式API
import { ref, onMounted, onUnmounted } from 'vue';

export function useWebSocket(url) {
    const wsManager = ref(null);
    const connected = ref(false);
    const messages = ref([]);

    onMounted(async () => {
        wsManager.value = new NativeWebSocketManager(url);
        
        try {
            await wsManager.value.connect();
            connected.value = true;
        } catch (error) {
            console.error('WebSocket连接失败:', error);
        }
    });

    onUnmounted(() => {
        if (wsManager.value) {
            wsManager.value.disconnect();
        }
    });

    const subscribeBatch = (batchId, callback) => {
        if (wsManager.value && connected.value) {
            return wsManager.value.subscribeBatch(batchId, callback);
        }
    };

    const subscribeRun = (runId, callback) => {
        if (wsManager.value && connected.value) {
            return wsManager.value.subscribeRun(runId, callback);
        }
    };

    return {
        wsManager,
        connected,
        messages,
        subscribeBatch,
        subscribeRun
    };
}
```

## 📝 **消息格式说明**

### **标准消息格式**
```javascript
{
    "type": "PROGRESS_UPDATE",  // 消息类型
    "payload": {                // 消息内容
        "batchId": 123,
        "progress": 75.5,
        "message": "处理进度更新"
    },
    "timestamp": "2024-01-15T10:30:00"  // 时间戳
}
```

### **消息类型枚举**
- `PROGRESS_UPDATE` - 进度更新
- `STATUS_CHANGE` - 状态变更
- `TASK_STARTED` - 任务开始
- `TASK_COMPLETED` - 任务完成
- `TASK_PAUSED` - 任务暂停
- `TASK_RESUMED` - 任务恢复
- `TASK_FAILED` - 任务失败
- `QUESTION_STARTED` - 问题开始处理
- `QUESTION_COMPLETED` - 问题处理完成
- `QUESTION_FAILED` - 问题处理失败
- `ERROR` - 错误消息
- `NOTIFICATION` - 系统通知

## 🔧 **最佳实践**

### **1. 连接管理**
- 实现自动重连机制
- 监听连接状态变化
- 处理网络异常情况

### **2. 订阅管理**
- 及时取消不需要的订阅
- 避免重复订阅同一路径
- 在组件销毁时清理订阅

### **3. 错误处理**
- 捕获并处理连接错误
- 处理消息解析错误
- 提供用户友好的错误提示

### **4. 性能优化**
- 合理控制订阅数量
- 避免频繁的连接/断开操作
- 使用消息缓存机制

## 🚨 **注意事项**

1. **路径前缀**: 所有订阅路径都需要包含正确的前缀（`/topic`、`/queue`、`/user`）
2. **消息确认**: 某些订阅需要发送确认消息到对应的`/app`路径
3. **用户认证**: 用户专有消息需要正确的用户身份验证
4. **跨域配置**: 确保服务器端已正确配置CORS
5. **连接超时**: 注意WebSocket连接的超时设置 