@echo off
set DIR=%~dp0
if "%DIR%"=="" set DIR=%CD%\
set APP_HOME=%DIR%
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%CLASSPATH%" (
  echo Gradle wrapper jar not found. Please run the Gradle wrapper bootstrap or open the project in Android Studio.
  exit /b 1
)
java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
