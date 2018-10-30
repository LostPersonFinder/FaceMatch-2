echo create the Java Native Interface JNI files for FaceMatch Library interface 
#
#
outDir=<TopDir>/FM2JNI/include
#
echo $pwd
#
javah -v  -d $outDir fmservice.server.fminterface.proxy.FRDProxy
javah -v  -d $outDir fmservice.server.fminterface.proxy.InfoProxy
javah -v  -d $outDir fmservice.server.fminterface.proxy.FaceFinderProxy
javah -v  -d $outDir fmservice.server.fminterface.proxy.FaceRegionMatcherProxy
#
#popd
#----------------------------------------------------------------------------------
