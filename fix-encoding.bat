@echo off
chcp 65001
for /r %%i in (*.java) do (
  echo Converting %%i to UTF-8
  powershell -Command "$content = Get-Content -Path '%%i' -Encoding Default; $content | Set-Content -Path '%%i' -Encoding UTF8"
)
