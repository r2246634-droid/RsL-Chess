@echo off
setlocal
set MP=C:\Users\Ruslan\Desktop\javafx-sdk-26.0.1\lib
set JAVA_OPTS=--module-path "%MP%" --add-modules javafx.controls,javafx.graphics
set JAR=RsLChess.jar
set BUNDLE=bundle_temp
set OUT=dist

echo [1/4] Derleniyor...
javac %JAVA_OPTS% -d bin ^
  src\main\java\com\chess\core\*.java ^
  src\main\java\com\chess\pieces\*.java ^
  src\main\java\com\chess\game\*.java ^
  src\main\java\com\chess\ai\*.java ^
  src\main\java\com\chess\i18n\*.java ^
  src\main\java\com\chess\sound\*.java ^
  src\main\java\com\chess\theme\*.java ^
  src\main\java\com\chess\network\*.java ^
  src\main\java\com\chess\ui\*.java
if %errorlevel% neq 0 ( echo HATA: Derleme basarisiz! & pause & exit /b 1 )

echo [2/4] JAR olusturuluyor...
jar --create --file "%JAR%" --main-class com.chess.ui.Main -C bin .
if %errorlevel% neq 0 ( echo HATA: JAR yapilamadi! & pause & exit /b 1 )

echo [3/4] Bundle klasoru hazirlanıyor...
if exist "%BUNDLE%" rmdir /s /q "%BUNDLE%"
mkdir "%BUNDLE%"
mkdir "%BUNDLE%\lib"
copy "%JAR%" "%BUNDLE%\"
xcopy /E /I /Y "%MP%" "%BUNDLE%\lib\"
if exist resources xcopy /E /I /Y resources "%BUNDLE%\resources\"

echo [4/4] EXE paketleniyor...
if exist "%OUT%" rmdir /s /q "%OUT%"

set ICON_ARG=
if exist resources\icon.ico set ICON_ARG=--icon resources\icon.ico

jpackage ^
  --type exe ^
  --name "RsL Chess" ^
  --app-version 1.0 ^
  --vendor "RsL" ^
  --description "RsL Chess - Java Satranc Motoru" ^
  --input "%BUNDLE%" ^
  --main-jar "%JAR%" ^
  --java-options "--module-path lib --add-modules javafx.controls,javafx.graphics" ^
  --dest "%OUT%" ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  %ICON_ARG%

if %errorlevel% equ 0 (
    echo.
    echo === BASARILI! ===
    echo EXE: %OUT%\RsL Chess-1.0.exe
    rmdir /s /q "%BUNDLE%"
    del "%JAR%"
) else (
    echo.
    echo === jpackage hatasi. JAR hazir: %JAR% ===
    echo Calistirmak icin: run.bat
    rmdir /s /q "%BUNDLE%"
)
echo.
pause
