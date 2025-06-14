# WebSocket测试脚本
Write-Host "开始测试WebSocket消息发送..." -ForegroundColor Green

# 测试1: 发送问题完成消息
Write-Host "`n1. 测试问题完成消息..." -ForegroundColor Yellow
try {
    $response1 = Invoke-RestMethod -Uri "http://localhost:8080/api/websocket-test/question-completed/8?questionId=999&completedCount=10" -Method POST
    Write-Host "响应: $($response1 | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
}

# 测试2: 发送运行状态消息
Write-Host "`n2. 测试运行状态消息..." -ForegroundColor Yellow
try {
    $response2 = Invoke-RestMethod -Uri "http://localhost:8080/api/websocket-test/run-message/8?messageType=QUESTION_COMPLETED" -Method POST
    Write-Host "响应: $($response2 | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
}

# 测试3: 发送全局消息
Write-Host "`n3. 测试全局消息..." -ForegroundColor Yellow
try {
    $response3 = Invoke-RestMethod -Uri "http://localhost:8080/api/websocket-test/global-message?messageType=NOTIFICATION&message=TestMessage" -Method POST
    Write-Host "响应: $($response3 | ConvertTo-Json)" -ForegroundColor Cyan
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nTest completed!" -ForegroundColor Green
Write-Host "Please check if frontend received WebSocket messages." -ForegroundColor Magenta 