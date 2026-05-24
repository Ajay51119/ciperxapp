@if "%DEBUG%"=="" @echo off
if "%OS%"=="Windows_NT" setlocal

@rem Use Public folder for cache to avoid space issues in 'Ajay Kumar'
set GRADLE_USER_HOME=C:\Users\Public\.gradle

@rem Force use of Java 11 - Update this path if your Java 11 is installed elsewhere
if exist "C:\Program Files\Java\jdk-11.0.2" set JAVA_HOME=C:\Program Files\Java\jdk-11.0.2
if exist "C:\Program Files\Java\jdk-11" set JAVA_HOME=C:\Program Files\Java\jdk-11
if exist "C:\Program Files\Java\openjdk-11" set JAVA_HOME=C:\Program Files\Java\openjdk-11

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute
echo ERROR: JAVA_HOME is not set
goto fail
:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
if exist "%JAVA_EXE%" goto execute
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
goto fail
:execute
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
:end
if %ERRORLEVEL% equ 0 goto mainEnd
:fail
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
exit /b %EXIT_CODE%
:mainEnd
if "%OS%"=="Windows_NT" endlocal
