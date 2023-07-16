@echo off
REM Written by combustiblemon and Wisp
REM get working dir and then replace backslashes with forward slashes.
set "PROJECT_DIR=%~dp0"
set "PROJECT_DIR=%PROJECT_DIR:\=/%"
set "PROJECT_DIR=%PROJECT_DIR:dokka/=%"


Set "DOKKA_DIR=%PROJECT_DIR%dokka/"
Set "DOKKA-CONFIG=%DOKKA_DIR%dokka-configuration.json"

echo Project dir: %PROJECT_DIR%
echo Dokka dir: %DOKKA_DIR%
echo Dokka config: %DOKKA-CONFIG%

echo { > %DOKKA-CONFIG%
echo   "outputDir": "./docs", >> %DOKKA-CONFIG%
echo   "sourceSets": [ >> %DOKKA-CONFIG%
echo     { >> %DOKKA-CONFIG%
echo       "sourceSetID": { >> %DOKKA-CONFIG%
echo         "scopeId": "moduleName", >> %DOKKA-CONFIG%
echo         "sourceSetName": "main" >> %DOKKA-CONFIG%
echo       }, >> %DOKKA-CONFIG%
echo       "sourceRoots": [ >> %DOKKA-CONFIG%
echo         "%PROJECT_DIR%src/org/magiclib", >> %DOKKA-CONFIG%
echo         "%PROJECT_DIR%MagicLib-Kotlin" >> %DOKKA-CONFIG%
echo       ] >> %DOKKA-CONFIG%
echo     } >> %DOKKA-CONFIG%
echo   ], >> %DOKKA-CONFIG%
echo   "pluginsClasspath": [ >> %DOKKA-CONFIG%
echo     "./dokka/dokka-base-1.8.10.jar", >> %DOKKA-CONFIG%
echo     "./dokka/kotlinx-html-jvm-0.8.0.jar", >> %DOKKA-CONFIG%
echo     "./dokka/dokka-analysis-1.8.10.jar", >> %DOKKA-CONFIG%
echo     "./dokka/kotlin-analysis-intellij-1.8.10.jar", >> %DOKKA-CONFIG%
echo     "./dokka/kotlin-analysis-compiler-1.8.10.jar", >> %DOKKA-CONFIG%
echo     "./dokka/freemarker-2.3.31.jar" >> %DOKKA-CONFIG%
echo   ] >> %DOKKA-CONFIG%
echo } >> %DOKKA-CONFIG%

REM It hates being run from here, just update the config file and then run it through IntelliJ.

REM @echo on
REM cd %DOKKA_DIR%
REM java -jar dokka-cli-1.8.10.jar %DOKKA_DIR%dokka-configuration.json

