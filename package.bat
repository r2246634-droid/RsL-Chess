@echo off
setlocal
rem JavaFX SDK konumu: JAVAFX_HOME ortam degiskeni varsa o kullanilir.
if defined JAVAFX_HOME (
    set "FX=%JAVAFX_HOME%"
) else (
    set "FX=C:\Users\Ruslan\Desktop\javafx-sdk-26.0.1"
)
set "MP=%FX%\lib"
if not exist "%MP%" (
    echo JavaFX SDK bulunamadi: %MP%
    echo JAVAFX_HOME ortam degiskenini JavaFX SDK klasorune ayarlayin. Ornek:
    echo     setx JAVAFX_HOME "C:\javafx-sdk-26.0.1"
    pause
    exit /b 1
)

rem jar/jpackage PATH'te olmayabilir (Oracle kurulumu yalnizca java/javac kisayolu ekler):
rem JDK klasoru JAVA_HOME'dan, yoksa java'nin kendi bildirdigi java.home'dan bulunur.
set "JDK="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JDK=%JAVA_HOME%"
if not defined JDK (
    for /f "tokens=2 delims==" %%H in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.home"') do set "JDK=%%H"
)
if defined JDK if "%JDK:~0,1%"==" " set "JDK=%JDK:~1%"
if not exist "%JDK%\bin\jpackage.exe" (
    echo JDK bulunamadi ^(jpackage.exe yok^). JAVA_HOME'u JDK klasorune ayarlayin.
    pause
    exit /b 1
)
set "JAR_TOOL=%JDK%\bin\jar.exe"
set "JPACKAGE=%JDK%\bin\jpackage.exe"

set JAVA_OPTS=--module-path "%MP%" --add-modules javafx.controls,javafx.graphics
set VERSION=1.0
set JAR=RsLChess.jar
set BUNDLE=bundle_temp
set OUT=dist

echo [1/5] Derleniyor...
if exist bin rmdir /s /q bin
javac %JAVA_OPTS% -encoding UTF-8 -d bin ^
  src\main\java\com\chess\core\*.java ^
  src\main\java\com\chess\pieces\*.java ^
  src\main\java\com\chess\game\*.java ^
  src\main\java\com\chess\ai\*.java ^
  src\main\java\com\chess\i18n\*.java ^
  src\main\java\com\chess\sound\*.java ^
  src\main\java\com\chess\theme\*.java ^
  src\main\java\com\chess\network\*.java ^
  src\main\java\com\chess\ui\*.java
if errorlevel 1 ( echo HATA: Derleme basarisiz! & pause & exit /b 1 )

echo [2/5] JAR olusturuluyor...
"%JAR_TOOL%" --create --file "%JAR%" --main-class com.chess.ui.Main -C bin .
if errorlevel 1 ( echo HATA: JAR yapilamadi! & pause & exit /b 1 )

echo [3/5] Bundle klasoru hazirlaniyor...
if exist "%BUNDLE%" rmdir /s /q "%BUNDLE%"
mkdir "%BUNDLE%\lib"
mkdir "%BUNDLE%\bin"
copy /Y "%JAR%" "%BUNDLE%\" >nul
rem Yalnizca kullanilan JavaFX modulleri
for %%M in (base controls graphics) do copy /Y "%MP%\javafx.%%M.jar" "%BUNDLE%\lib\" >nul
rem JavaFX yerel kutuphaneleri: jar'lar bunlari ../bin altinda arar (lib ile birlikte gitmezse oyun acilmaz)
copy /Y "%FX%\bin\*.dll" "%BUNDLE%\bin\" >nul
rem Web/medya modulleri kullanilmiyor - buyuk DLL'leri disarida birak
for %%D in (jfxwebkit gstreamer-lite glib-lite jfxmedia fxplugins) do if exist "%BUNDLE%\bin\%%D.dll" del "%BUNDLE%\bin\%%D.dll"
if exist resources xcopy /E /I /Y /Q resources "%BUNDLE%\resources\" >nul

set ICON_ARG=
if exist resources\icon.ico set ICON_ARG=--icon resources\icon.ico
set FX_JAVA_OPTS=--java-options "--module-path=$APPDIR\lib" --java-options "--add-modules=javafx.controls,javafx.graphics"

if exist "%OUT%" rmdir /s /q "%OUT%"

echo [4/5] Tasinabilir surum (app-image) olusturuluyor...
"%JPACKAGE%" ^
  --type app-image ^
  --name "RsL Chess" ^
  --app-version %VERSION% ^
  --vendor "RsL" ^
  --description "RsL Chess - Java Satranc Oyunu" ^
  --input "%BUNDLE%" ^
  --main-jar "%JAR%" ^
  %FX_JAVA_OPTS% ^
  --dest "%OUT%" ^
  %ICON_ARG%
if errorlevel 1 ( echo HATA: jpackage app-image basarisiz! & goto cleanup )
powershell -NoProfile -Command "Compress-Archive -Path '%OUT%\RsL Chess' -DestinationPath '%OUT%\RsL-Chess-%VERSION%-windows.zip' -Force"
echo    Tasinabilir: %OUT%\RsL Chess\RsL Chess.exe
echo    Zip:         %OUT%\RsL-Chess-%VERSION%-windows.zip

echo [5/5] Kurulum sihirbazi (.exe installer)...
rem Installer icin WiX Toolset gerekir; yoksa bu adim atlanir.
where candle >nul 2>nul || where wix >nul 2>nul
if errorlevel 1 (
    echo    WiX Toolset bulunamadi - installer atlandi. ^(https://wixtoolset.org/^)
    goto cleanup
)
"%JPACKAGE%" ^
  --type exe ^
  --name "RsL Chess" ^
  --app-version %VERSION% ^
  --vendor "RsL" ^
  --description "RsL Chess - Java Satranc Oyunu" ^
  --input "%BUNDLE%" ^
  --main-jar "%JAR%" ^
  %FX_JAVA_OPTS% ^
  --dest "%OUT%" ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  %ICON_ARG%
if errorlevel 1 ( echo    HATA: installer olusturulamadi. ) else ( echo    Installer: %OUT%\RsL Chess-%VERSION%.exe )

:cleanup
if exist "%BUNDLE%" rmdir /s /q "%BUNDLE%"
if exist "%JAR%" del "%JAR%"
echo.
echo === Bitti ===
if not defined NO_PAUSE pause
