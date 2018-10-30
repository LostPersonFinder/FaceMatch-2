REM create the Java Native Interface JNI files for FaceMatch Library interface 

set outDir=<TopDir>\FM-JNI\include
cd <TopDir>\build\classes

javah -v  -d %outDir% facematch.server.fminterface.proxy.FaceRegionDetector
javah -v  -d %outDir% facematch.server.fminterface.proxy.FaceFinderProxy
javah -v  -d %outDir% facematch.server.fminterface.proxy.FaceRegionMatcherProxy

pause
