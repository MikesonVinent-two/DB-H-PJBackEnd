/**
 * WebSocket客户端示例
 * 用于连接到服务器并接收实时通知
 */
class WebSocketClient {
    /**
     * 构造函数
     * @param {string} serverUrl WebSocket服务器URL，例如：'http://localhost:8080'
     */
    constructor(serverUrl) {
        this.serverUrl = serverUrl;
        this.stompClient = null;
        this.connected = false;
        this.subscriptions = new Map();
        this.handlers = {
            connect: [],
            disconnect: [],
            error: []
        };
    }

    /**
     * 连接到WebSocket服务器
     * @returns {Promise} 连接成功或失败的Promise
     */
    connect() {
        return new Promise((resolve, reject) => {
            // 如果已经连接，直接返回
            if (this.connected && this.stompClient) {
                resolve();
                return;
            }

            // 创建SockJS对象
            // 使用/ws路径
            const socket = new SockJS(`${this.serverUrl}/ws`);
            
            // 启用调试日志，帮助排查问题
            console.log(`尝试连接WebSocket: ${this.serverUrl}/ws`);
            
            // 创建STOMP客户端
            this.stompClient = Stomp.over(socket);
            
            // 启用STOMP客户端调试日志
            this.stompClient.debug = function(str) {
                console.log(`STOMP: ${str}`);
            };
            
            // 连接到服务器
            this.stompClient.connect(
                {}, // 头信息，可以为空
                frame => {
                    console.log('WebSocket连接成功');
                    this.connected = true;
                    
                    // 触发连接成功回调
                    this._triggerHandlers('connect', frame);
                    
                    resolve(frame);
                },
                error => {
                    console.error('WebSocket连接失败:', error);
                    this.connected = false;
                    this.stompClient = null;
                    
                    // 触发错误回调
                    this._triggerHandlers('error', error);
                    
                    reject(error);
                }
            );
        });
    }

    /**
     * 断开与WebSocket服务器的连接
     */
    disconnect() {
        if (this.stompClient && this.connected) {
            this.stompClient.disconnect(() => {
                console.log('WebSocket连接已断开');
                this.connected = false;
                
                // 触发断开连接回调
                this._triggerHandlers('disconnect');
            });
        }
    }

    /**
     * 订阅批次消息
     * @param {number} batchId 批次ID
     * @param {function} callback 接收消息的回调函数
     * @returns {string} 订阅ID
     */
    subscribeBatch(batchId, callback) {
        if (!this.connected) {
            throw new Error('WebSocket未连接');
        }
        
        const destination = `/topic/batch/${batchId}`;
        const subscription = this.stompClient.subscribe(destination, message => {
            const data = JSON.parse(message.body);
            callback(data);
        });
        
        // 发送订阅确认消息
        this.stompClient.send(`/app/batch/${batchId}/subscribe`, {}, JSON.stringify({
            batchId: batchId,
            timestamp: new Date()
        }));
        
        // 保存订阅
        this.subscriptions.set(subscription.id, subscription);
        
        return subscription.id;
    }

    /**
     * 订阅运行消息
     * @param {number} runId 运行ID
     * @param {function} callback 接收消息的回调函数
     * @returns {string} 订阅ID
     */
    subscribeRun(runId, callback) {
        if (!this.connected) {
            throw new Error('WebSocket未连接');
        }
        
        const destination = `/topic/run/${runId}`;
        const subscription = this.stompClient.subscribe(destination, message => {
            const data = JSON.parse(message.body);
            callback(data);
        });
        
        // 发送订阅确认消息
        this.stompClient.send(`/app/run/${runId}/subscribe`, {}, JSON.stringify({
            runId: runId,
            timestamp: new Date()
        }));
        
        // 保存订阅
        this.subscriptions.set(subscription.id, subscription);
        
        return subscription.id;
    }

    /**
     * 订阅全局消息
     * @param {function} callback 接收消息的回调函数
     * @returns {string} 订阅ID
     */
    subscribeGlobal(callback) {
        if (!this.connected) {
            throw new Error('WebSocket未连接');
        }
        
        const destination = '/topic/global';
        const subscription = this.stompClient.subscribe(destination, message => {
            const data = JSON.parse(message.body);
            callback(data);
        });
        
        // 发送订阅确认消息
        this.stompClient.send('/app/global/subscribe', {}, JSON.stringify({
            timestamp: new Date()
        }));
        
        // 保存订阅
        this.subscriptions.set(subscription.id, subscription);
        
        return subscription.id;
    }

    /**
     * 取消订阅
     * @param {string} subscriptionId 订阅ID
     */
    unsubscribe(subscriptionId) {
        if (!this.connected) {
            return;
        }
        
        const subscription = this.subscriptions.get(subscriptionId);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(subscriptionId);
        }
    }

    /**
     * 添加事件处理器
     * @param {string} event 事件类型：'connect', 'disconnect', 'error'
     * @param {function} handler 处理函数
     */
    on(event, handler) {
        if (this.handlers[event]) {
            this.handlers[event].push(handler);
        }
    }

    /**
     * 移除事件处理器
     * @param {string} event 事件类型
     * @param {function} handler 处理函数
     */
    off(event, handler) {
        if (this.handlers[event]) {
            this.handlers[event] = this.handlers[event].filter(h => h !== handler);
        }
    }

    /**
     * 触发事件处理器
     * @param {string} event 事件类型
     * @param {any} data 事件数据
     * @private
     */
    _triggerHandlers(event, data) {
        if (this.handlers[event]) {
            this.handlers[event].forEach(handler => {
                try {
                    handler(data);
                } catch (error) {
                    console.error(`处理${event}事件时出错:`, error);
                }
            });
        }
    }
}

// 使用示例
/*
const wsClient = new WebSocketClient('http://localhost:8080');

// 连接到服务器
wsClient.connect()
    .then(() => {
        console.log('已连接到WebSocket服务器');
        
        // 订阅批次消息
        const batchSubscriptionId = wsClient.subscribeBatch(123, data => {
            console.log('收到批次消息:', data);
            
            // 根据消息类型进行处理
            switch (data.type) {
                case 'PROGRESS_UPDATE':
                    updateProgressBar(data.payload.progress);
                    break;
                case 'STATUS_CHANGE':
                    updateStatus(data.payload.status);
                    break;
                case 'ERROR':
                    showError(data.payload.error);
                    break;
            }
        });
        
        // 订阅运行消息
        const runSubscriptionId = wsClient.subscribeRun(456, data => {
            console.log('收到运行消息:', data);
        });
        
        // 订阅全局消息
        const globalSubscriptionId = wsClient.subscribeGlobal(data => {
            console.log('收到全局消息:', data);
        });
        
        // 一段时间后取消订阅
        setTimeout(() => {
            wsClient.unsubscribe(batchSubscriptionId);
            wsClient.unsubscribe(runSubscriptionId);
            wsClient.unsubscribe(globalSubscriptionId);
            
            // 断开连接
            wsClient.disconnect();
        }, 60000);
    })
    .catch(error => {
        console.error('连接WebSocket服务器失败:', error);
    });
*/ 