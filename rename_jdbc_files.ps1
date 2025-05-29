$jdbcPath = "src\main\java\com\example\demo\repository\jdbc"
$files = Get-ChildItem -Path $jdbcPath -Filter "Jdbc*.java"

# 创建一个映射表，用于替换类名引用
$classMap = @{}

# 首先构建映射表
foreach ($file in $files) {
    $oldClassName = $file.BaseName  # 例如：JdbcTagRepository
    $newClassName = $oldClassName -replace "^Jdbc", ""  # 例如：TagRepository
    $classMap[$oldClassName] = $newClassName
}

# 然后处理每个文件
foreach ($file in $files) {
    $oldClassName = $file.BaseName  # 例如：JdbcTagRepository
    $newClassName = $oldClassName -replace "^Jdbc", ""  # 例如：TagRepository
    $newFileName = "$($newClassName).java"  # 例如：TagRepository.java
    $content = Get-Content -Path $file.FullName -Raw

    # 替换类声明
    $content = $content -replace "public class $oldClassName", "public class $newClassName"
    
    # 替换构造函数
    $content = $content -replace "public $oldClassName\(", "public $newClassName("
    
    # 替换对其他JDBC类的引用
    foreach ($oldClass in $classMap.Keys) {
        if ($oldClass -ne $oldClassName) {  # 不替换当前类的引用
            $content = $content -replace $oldClass, $classMap[$oldClass]
        }
    }

    # 保存到新文件
    Set-Content -Path "$jdbcPath\$newFileName" -Value $content
    
    # 删除旧文件
    Remove-Item -Path $file.FullName
    
    Write-Host "重命名并修改: $($file.Name) -> $newFileName"
}

Write-Host "所有文件处理完成!" 