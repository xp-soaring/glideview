@echo off
if ".%1"=="." C:\ian_lewis\bin\j2sdk\bin\javac -target 1.1 igcview.java
if not ".%1"=="." C:\ian_lewis\bin\j2sdk\bin\javac -target 1.1 %1
