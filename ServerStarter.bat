@ECHO OFF
TITLE ~ServerStarter~ Made by Rigged of RaGEZONE Forums
ECHO Press enter to begin the process of opening your server...
pause >nul
ECHO Starting launch_world.bat
ECHO Starting launch_login.bat
ECHO Starting launch_channel.bat
start launch_world.bat
ping localhost -w 10>nul
start launch_login.bat
ping localhost -w 10>nul
start launch_channel.bat