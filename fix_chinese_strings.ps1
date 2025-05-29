# Fix Chinese String Script
# This script will search for Java files with Chinese string problems and attempt to fix them

# Get all controller files first (where most problems appear)
$controllerFiles = Get-ChildItem -Path ".\src\main\java\com\example\demo\controller" -Filter "*.java"

foreach ($file in $controllerFiles) {
    Write-Host "Processing file: $($file.FullName)"
    
    try {
        # Read file content
        $content = Get-Content -Path $file.FullName -Raw
        
        # Replace problematic log statements with English placeholders
        $content = $content -replace 'logger\.info\("[^"]*[\u4e00-\u9fa5][^"]*"\);', 'logger.info("Operation completed");'
        $content = $content -replace 'logger\.debug\("[^"]*[\u4e00-\u9fa5][^"]*"\);', 'logger.debug("Debug info");'
        $content = $content -replace 'logger\.warn\("[^"]*[\u4e00-\u9fa5][^"]*"\);', 'logger.warn("Warning");'
        $content = $content -replace 'logger\.error\("[^"]*[\u4e00-\u9fa5][^"]*"\);', 'logger.error("Error occurred");'
        
        # Replace problematic response messages with English placeholders
        $content = $content -replace 'response\.put\("message", "[^"]*[\u4e00-\u9fa5][^"]*"\);', 'response.put("message", "Operation completed");'
        
        # Fix broken string literals
        $content = $content -replace '".+[\u4e00-\u9fa5].+\?\);', '"Operation completed");'
        
        # Write content back
        [System.IO.File]::WriteAllText($file.FullName, $content, [System.Text.UTF8Encoding]::new($false))
        
        Write-Host "  Fixed potential Chinese string issues"
    }
    catch {
        Write-Host "  Error processing file: $_"
    }
}

# Process other common locations for potential issues
$otherFiles = @(
    ".\src\main\java\com\example\demo\config\RestTemplateConfig.java"
)

foreach ($filePath in $otherFiles) {
    if (Test-Path $filePath) {
        Write-Host "Processing file: $filePath"
        
        try {
            # Read file content
            $content = Get-Content -Path $filePath -Raw
            
            # Replace problematic log statements with English placeholders
            $content = $content -replace 'logger\.info\("[^"]*[\u4e00-\u9fa5][^"]*"\);', 'logger.info("Operation completed");'
            
            # Write content back
            [System.IO.File]::WriteAllText($filePath, $content, [System.Text.UTF8Encoding]::new($false))
            
            Write-Host "  Fixed potential Chinese string issues"
        }
        catch {
            Write-Host "  Error processing file: $_"
        }
    }
}

Write-Host "All targeted files processed!" 