@echo off
rem Copies the PHP API and the photos into XAMPP, then (re)creates the database.
rem Run it from this folder after starting Apache and MySQL in the XAMPP Control Panel.
set TARGET=C:\xampp\htdocs\sennara
if not exist "%TARGET%" mkdir "%TARGET%"
xcopy /E /I /Y "%~dp0api" "%TARGET%" >nul
xcopy /E /I /Y "%~dp0img" "%TARGET%\img" >nul
C:\xampp\php\php.exe "%~dp0install.php"
echo.
echo API ready at http://localhost/sennara/  (the emulator reaches it at http://10.0.2.2/sennara/)
pause
