# File Encoding Conversion Script
# Convert all Java files to UTF-8 encoding without BOM

# Get all Java files
$javaFiles = Get-ChildItem -Path ".\src" -Filter "*.java" -Recurse

foreach ($file in $javaFiles) {
    Write-Host "Processing file: $($file.FullName)"
    
    try {
        # Read file content
        $content = Get-Content -Path $file.FullName -Raw
        
        # Write content back with UTF-8 encoding (no BOM)
        [System.IO.File]::WriteAllText($file.FullName, $content, [System.Text.UTF8Encoding]::new($false))
        
        Write-Host "  Converted to UTF-8 (no BOM)"
    }
    catch {
        Write-Host "  Error processing file: $_"
    }
}

Write-Host "All Java files processed!" 