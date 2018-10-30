echo --- Create jar file with common HTTP communication request/response protocols between the client and the server ---
BUILD_DIR=<FM2InstallDir>/FM2Server
CLIB=<FM2InstallDir>/FM2JavaClient/lib

cd  $BUILD_DIR/build/classes
jar cvf  $BUILD_DIR/dist/httpcommon.jar ./fmservice/httputils
cp $BUILD_DIR/dist/httpcommon.jar $CLIB/httpcommon.jar
ls -ls $CLIB/httpcommon.jar

sleep 5

