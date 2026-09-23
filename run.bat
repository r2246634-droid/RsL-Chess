@echo off
set MODULE_PATH=C:\Users\Ruslan\Desktop\javafx-sdk-26.0.1\lib
set JAVA_OPTS=--module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.graphics

javac %JAVA_OPTS% -d bin ^
  src\main\java\com\chess\core\*.java ^
  src\main\java\com\chess\pieces\*.java ^
  src\main\java\com\chess\game\*.java ^
  src\main\java\com\chess\ai\*.java ^
  src\main\java\com\chess\sound\*.java ^
  src\main\java\com\chess\theme\*.java ^
  src\main\java\com\chess\network\*.java ^
  src\main\java\com\chess\ui\*.java

if %errorlevel% neq 0 (
    echo.
    echo === Derleme basarisiz! ===
    pause
    exit /b 1
)

echo Derleme basarili. Oyun baslatiliyor...
java %JAVA_OPTS% -cp bin com.chess.ui.Main
