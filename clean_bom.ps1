# Clean BOM characters script
# Clean BOM characters from all Java files under src directory

$javaFiles = Get-ChildItem -Path ".\src" -Filter "*.java" -Recurse

foreach ($file in $javaFiles) {
    Write-Host "Processing file: $($file.FullName)"
    
    # Read file content
    $content = [System.IO.File]::ReadAllBytes($file.FullName)
    
    # Check if the file starts with BOM (UTF-8 BOM is EF BB BF)
    if ($content.Length -ge 3 -and $content[0] -eq 0xEF -and $content[1] -eq 0xBB -and $content[2] -eq 0xBF) {
        Write-Host "  Found BOM, removing..."
        
        # Create new content (without BOM)
        $newContent = New-Object byte[] ($content.Length - 3)
        [System.Array]::Copy($content, 3, $newContent, 0, $content.Length - 3)
        
        # Write back to file
        [System.IO.File]::WriteAllBytes($file.FullName, $newContent)
        Write-Host "  BOM removed"
    } else {
        Write-Host "  No BOM found, skipping"
    }
}

Write-Host "All Java files processed!" 