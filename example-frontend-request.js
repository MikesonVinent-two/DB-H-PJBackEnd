// 使用fetch API向后端发送请求示例
async function sendChatRequest(message) {
  try {
    const response = await fetch('http://localhost:8080/api/llm/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // 如果需要身份验证，这里可以添加Authorization头
      },
      // 由于我们设置了allowCredentials: false，这里也不应该发送凭证
      credentials: 'omit',
      body: JSON.stringify({
        api: "https://api.openai.com/v1",  // OpenAI API基础URL
        apiKey: "your-api-key-here",      // 你的API密钥
        model: "gpt-3.5-turbo",           // 模型名称
        message: message,                 // 用户消息
        temperature: 0.7,                 // 可选参数
        maxTokens: 1000,                  // 可选参数
        systemPrompts: [                  // 可选的系统提示
          {
            "role": "system", 
            "content": "你是一个有用的AI助手。"
          }
        ]
      })
    });

    if (!response.ok) {
      console.error(`HTTP错误! 状态: ${response.status}`);
      // 尝试获取错误详情
      const errorData = await response.json();
      console.error('错误详情:', errorData);
      throw new Error(`HTTP错误! 状态: ${response.status}`);
    }

    const data = await response.json();
    console.log('API响应:', data);
    return data;
  } catch (error) {
    console.error('请求失败:', error);
    throw error;
  }
}

// 使用示例
// sendChatRequest("你好，请告诉我今天的天气如何？")
//   .then(response => {
//     console.log("AI回复:", response.content);
//   })
//   .catch(error => {
//     console.error("错误:", error);
//   }); 