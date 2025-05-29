# 删除不可映射字符脚本
# 此脚本将删除所有文本文件中的不可映射字符

# 定义要处理的文件扩展名列表
$fileExtensions = @("*.java", "*.xml", "*.properties", "*.txt", "*.sql", "*.yml", "*.yaml", "*.json", "*.js", "*.html", "*.css", "*.md")

# 获取所有匹配的文件
$files = @()
foreach ($ext in $fileExtensions) {
    $files += Get-ChildItem -Path "." -Filter $ext -Recurse -File | Where-Object { 
        -not ($_.FullName.Contains(".git") -or $_.FullName.Contains(".gradle") -or $_.FullName.Contains("build"))
    }
}

Write-Host "发现 $($files.Count) 个文件需要处理"
$processedCount = 0
$modifiedCount = 0

# 定义用于检测BOM的函数
function Get-FileEncoding {
    [CmdletBinding()]
    Param (
        [Parameter(Mandatory = $True, ValueFromPipelineByPropertyName = $True)]
        [string]$Path
    )

    $bytes = [System.IO.File]::ReadAllBytes($Path)

    # 检测BOM标记
    if ($bytes.Length -ge 4 -and $bytes[0] -eq 0x00 -and $bytes[1] -eq 0x00 -and $bytes[2] -eq 0xFE -and $bytes[3] -eq 0xFF) { 
        return [System.Text.Encoding]::GetEncoding("utf-32BE") 
    }
    elseif ($bytes.Length -ge 4 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE -and $bytes[2] -eq 0x00 -and $bytes[3] -eq 0x00) { 
        return [System.Text.Encoding]::UTF32
    }
    elseif ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFE -and $bytes[1] -eq 0xFF) { 
        return [System.Text.Encoding]::BigEndianUnicode 
    }
    elseif ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE) { 
        return [System.Text.Encoding]::Unicode 
    }
    elseif ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) { 
        return [System.Text.Encoding]::UTF8 
    }
    else { 
        # 尝试检测编码
        try {
            $utf8Content = [System.Text.Encoding]::UTF8.GetString($bytes)
            return [System.Text.Encoding]::UTF8
        }
        catch {
            # 如果UTF-8解码失败，使用默认编码
            return [System.Text.Encoding]::Default
        }
    }
}

foreach ($file in $files) {
    try {
        $processedCount++
        Write-Host "[$processedCount/$($files.Count)] 处理文件: $($file.FullName)"
        
        # 检测文件编码
        $encoding = Get-FileEncoding -Path $file.FullName
        Write-Host "  检测到编码: $($encoding.WebName)" -ForegroundColor Cyan
        
        # 读取文件内容
        $content = [System.IO.File]::ReadAllText($file.FullName, $encoding)
        
        # 保存原始长度
        $originalLength = $content.Length
        
        # 移除不可映射字符
        # 控制字符 (C0和C1控制字符集)
        $newContent = [regex]::Replace($content, '[\x00-\x08\x0B\x0C\x0E-\x1F\x7F-\x9F]', '')
        
        # 特定不可映射字符处理
        $problematicChars = @(
            [char]0xE688,  # 用户特别指定的字符
            [char]0xFFFD,  # Unicode替换字符
            [char]0xFFFF,  # Unicode未定义字符
            [char]0xFFFE   # Unicode字节序标记（逆序）
        )
        
        foreach ($char in $problematicChars) {
            $newContent = $newContent.Replace($char, '')
        }
        
        # 检查文件内容是否已修改
        if ($newContent.Length -ne $originalLength) {
            # 写回文件，保持原始编码
            [System.IO.File]::WriteAllText($file.FullName, $newContent, $encoding)
            $modifiedCount++
            Write-Host "  已移除不可映射字符，文件已更新" -ForegroundColor Green
        }
        else {
            Write-Host "  未发现不可映射字符" -ForegroundColor Gray
        }
    }
    catch {
        Write-Host "  处理文件时出错: $_" -ForegroundColor Red
    }
}

Write-Host "处理完成! 共处理 $processedCount 个文件，修改了 $modifiedCount 个文件。" 