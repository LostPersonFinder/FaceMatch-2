echo *** Create jar file with the class which loads the FM-Native shared library. 
Echo *** Copy it to Tomcat's shared/lib directory, which should be included in the java.lib.path ***

set LOCAL_LIB=<FM2InstallDir>/JNILoader/dist
set TOMCAT_SHARED_LIB=<TomcatDir>/shared/lib
set CPP_LIB=<FaceMatchLibDir>/bin

echo *** copy it to the Tomcat's shared lib library **

cp %LOCAL_LIB%/JNILoader.jar %TOMCAT_SHARED_LIB%
cp %CPP_LIB%/libcommon.so %TOMCAT_SHARED_LIB%
cp %CPP_LIB%/libFaceMatchLibJni.so %TOMCAT_SHARED_LIB%

pause
