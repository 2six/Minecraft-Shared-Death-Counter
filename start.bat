@echo off
title Minecraft Auto-Reset Server

:start_server
echo -----------------------------------------
echo [System] Starting Server...
echo -----------------------------------------

REM 1. World Folder Check
if not exist "world" mkdir "world"

REM 2. Restore Statistics (if backup exists)
if exist "saved_stats" (
    echo [System] Restoring player stats...
    if not exist "world\stats" mkdir "world\stats"
    xcopy /E /Y "saved_stats" "world\stats\" >nul
)

echo [System] Launching Java...
echo -----------------------------------------

REM 3. Server Start
REM -Xms4G -Xmx4G: Set initial and maximum heap size to 4GB
REM Adjust memory settings as needed
java -Xms4G -Xmx4G -jar server.jar nogui

REM --- Server Stopped ---
echo.
echo [System] Server Stopped.

REM ===============================
REM CHECK 1: HARD RESET (WIPE ALL)
REM ===============================
if exist "hard_reset.flag" (
    echo.
    echo [!!! HARD RESET DETECTED !!!]
    echo [System] Wiping ALL data...
    timeout /t 3
    
    rmdir /s /q world
    rmdir /s /q world_nether
    rmdir /s /q world_the_end
    rmdir /s /q saved_stats
    
    del hard_reset.flag
    goto start_server
)

REM ===============================
REM CHECK 2: NORMAL RESET (GAME OVER)
REM ===============================
if exist "reset.flag" (
    echo.
    echo [!!! GAME OVER DETECTED !!!]
    echo [System] Resetting world...
    timeout /t 3

    REM Backup Stats
    if not exist "saved_stats" mkdir "saved_stats"
    if exist "world\stats" (
        xcopy /E /Y "world\stats" "saved_stats\" >nul
    )

    REM Delete Worlds
    rmdir /s /q world
    rmdir /s /q world_nether
    rmdir /s /q world_the_end

    del reset.flag
    goto start_server
)

REM ===============================
REM NO FLAG (MANUAL STOP)
REM ===============================
echo [System] Manual stop detected. Restarting in 5 seconds...
timeout /t 5
goto start_server