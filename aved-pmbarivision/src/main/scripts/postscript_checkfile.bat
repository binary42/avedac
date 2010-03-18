@echo off
REM This script checks if processing was completed by checking if the
REM xml file exists and is non-empty
REM returns 0 if ok, otherwise 1
REM This is inteded to be run as a post-script in the AVED Condor DAG 

echo %~z1 
if %~z1==0 goto bad
goto good

:bad
echo "File is bad" 
exit /b 1

:good
echo "File is good" 
exit /b 0


