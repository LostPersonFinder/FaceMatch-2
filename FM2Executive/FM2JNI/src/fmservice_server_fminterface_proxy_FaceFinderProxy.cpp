 /*
 * Informational Notice:
 * This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
 * an agency of the Department of Health and Human Services, United States Government.
 *
 * The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.
 *
 * The license does not supersede any applicable United States law.
 *
 * The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.
 *
 * Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
 * (FAR) 48 C.F.R. Part52.227-14, Rights in Dataï¿½General.
 * The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
 * non-commercial products.
 *
 * LICENSE:
 *
 * Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
 * as provided by Federal law.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * -	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.
 *
 * -	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
 * in the documentation and/or other materials provided with the distribution.
 *
 * -	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
 * of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 
/*Implementation for native class fmservice_server_fminterface_proxy_FaceFinder */
/* Author: 
 * Date: June 3, 2015
 */

#include "common.h"
#include "fmservice_server_fminterface_proxy_FaceFinderProxy.h"
#include "FaceFinder.h"
#include "stdafx.h"
#include <string.h>
#include <sys/timeb.h>

using namespace FaceMatch;
using namespace cv;
using namespace std;

std::string ffErrorMsg;
int  ffVerbLevel = 0;

static int getMilliCount(){
	timeb tb;
	ftime(&tb);
	int nCount = tb.millitm + (tb.time & 0xfffff) * 1000;
	return nCount;
}

/*
 * Class:     fmservice_server_fminterface_proxy_FaceFinder
 * Method:    n_FaceFinderCreate
 * Signature: (JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;I)J
 */
JNIEXPORT jlong JNICALL Java_fmservice_server_fminterface_proxy_FaceFinderProxy_n_1FaceFinderCreate (
		JNIEnv *env,  jclass callingClass, jlong jfrdHandle, 
		jstring jImageFileDir, jstring jImageFileName, jstring jInputRegions, 
		jint jFlags)
{
	ffErrorMsg.clear();
	try
	{
		if (ffVerbLevel > 1)
			cout << "- JNI: n_FaceFinderCreate: " << "FRDHandle=" << jfrdHandle << " flags: " << jFlags << endl ;

		int start = getMilliCount();
			
		// allocate Strings locally  --  NULL => boolean false: no copy
		const char* ImageFileDir  = env->GetStringUTFChars(jImageFileDir, NULL);		
		const char* ImageFileName  = env->GetStringUTFChars(jImageFileName, NULL);
		const char* InputRegions  = (jInputRegions == NULL) ? 
					NULL : env->GetStringUTFChars(jInputRegions, NULL);

		long faceFinderHandle = (long)NULL;	

		if (ffVerbLevel > 0)
                                    {
			cout << "- JNI: Image File:" << ImageFileName << ", Input Regions " << (InputRegions == NULL ? "NULL" : InputRegions) << endl;
                                                      cout  << "-JNI:<ImageDir : " <<  ImageFileDir << endl;
                                    }
		FaceRegionDetector* FRDObj = (FaceRegionDetector*) jfrdHandle;

		FaceRegionDetector& FRD = (*FRDObj);
		if (ffVerbLevel > 1)
			cout << "- JNI: FRD parameters: " << FRDObj->FaceAspectLimit << ",FaceDiameterMax = " << FRDObj->FaceDiameterMax
				<<", usingGPU: " << FRDObj->usingGPU() << endl;
		FaceFinder* FaceFinderObj;
		if (InputRegions == NULL)
                                    {
                                            FaceFinderObj = new FaceFinder(FRD, ImageFileDir, ImageFileName, jFlags);
                                    }
		else
                                    {
                                            FaceFinderObj = new FaceFinder(FRD, ImageFileDir, ImageFileName, InputRegions, jFlags);
                                    }
		if (ffVerbLevel > 1)
			cout << "-JNI: n_FaceFinderCreate: returned from FaceFinder constructor , Handle=" << (long) FaceFinderObj << endl;

		jobject storedObj = env->NewGlobalRef((jobject)FaceFinderObj);
		faceFinderHandle = (long)FaceFinderObj;
		if (ffVerbLevel > 1)
			cout << "-JNI: n_FaceFinderCreate: " << "FaceFinder object Handle=" << faceFinderHandle << endl ;

		// free up the allocated Strings
		env->ReleaseStringUTFChars(jImageFileDir, ImageFileDir);
		env->ReleaseStringUTFChars(jImageFileName, ImageFileName);
		if (InputRegions != NULL)
			env->ReleaseStringUTFChars(jInputRegions, InputRegions);

		int finish = getMilliCount();
		if (ffVerbLevel > 1)
			cout << "-JNI: FaceFinder object (for input file) creation time " << (finish-start) << " millisec" << endl;

		return faceFinderHandle;		// return the pointer (long handle)
	}

	// error processing
	catch(const exception & e)
	{
		ostringstream errstr;
		errstr.clear();
		errstr <<__FUNCTION__<<" exception: "<<e.what()<<endl;
		ffErrorMsg = errstr.str();
		if (ffVerbLevel > 1)
			cout << ffErrorMsg;
	}
	
	catch(...)			// Access violation and other types of errors
	{
		ostringstream errstr;
		errstr.clear();
		errstr << "Exiting due to unknown exception" << endl;
		ffErrorMsg = errstr.str();
		if (ffVerbLevel > 1)
			cout << ffErrorMsg;
	}
	return (long)NULL;
}
/*-------------------------------------------------------------------------------------------------*/
/*
 * Class:     fmservice_server_fminterface_proxy_FaceFinder
 * Method:    n_gotFaces
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_fmservice_server_fminterface_proxy_FaceFinderProxy_n_1gotFaces (
		JNIEnv *env,  jclass callingClass, jlong jffHandle , jboolean isLax)
{
	//int start = getMilliCount();
	FaceFinder* FFPtr = (FaceFinder*) jffHandle;
	if (ffVerbLevel > 1)
		cout << "- JNI: FaceFinder object handle: " << jffHandle << endl;
	bool hasFaces =  FFPtr->gotFaces();
	//int finish = getMilliCount();
	//	cout << "-JNI: FaceFinder gotFaces time " << (finish-start) << " millisec" << endl;
	return hasFaces;
}

/*-------------------------------------------------------------------------------------------------*/
/*
 * Class:     fmservice_server_fminterface_proxy_FaceFinder
 * Method:    n_getFaces
 * Signature: (J)[Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_fmservice_server_fminterface_proxy_FaceFinderProxy_n_1getFaces (
	JNIEnv *env,  jclass callingClass, jlong jffHandle)
{
	//int start = getMilliCount();

	ffErrorMsg.clear();

	FaceFinder* FFPtr = (FaceFinder*) jffHandle;
		
	const FaceRegions& FRegions = (*FFPtr).getFaces();
	const unsigned faceCount = FRegions.size();
	if (ffVerbLevel > 0)
		cout << "- JNI: Image File: " << FFPtr->getImageFN() << ", primary face:  " <<FFPtr->getPrimaryFaceRegion() << endl;
		
	//default: all faces returned
	ostringstream oss;
	oss.clear();
	if(faceCount > 0)
	{
		if (ffVerbLevel > 1)
			cout << "- JNI: Number of faces found by FaceFinder = " <<  faceCount << endl;
		const FaceMatch::FaceRegions& fregs((*FFPtr).getFaces());
			
		// output face region coordinates to the stream
		oss << fregs << endl;
		if(oss.str().length() > 0)
		{
			string s = oss.str();			// get the String
			if (ffVerbLevel > 0)
				cout << "-JNI: Face Regions = " << s << endl;
				
			//return face regions to caller
			const char* faces = &(s[0]);	
			jstring faceRegions = env->NewStringUTF(faces);
			
			//int finish = getMilliCount();
			//cout << "-JNI: FaceFinder getFaces time " << (finish-start) << " millisec" << endl;
			return faceRegions;
		}
	}
	else // no faces exist - save eror message
	{
		oss << "- JNI: No faces found in the image: " << FFPtr->getImageFN() << endl;
		ffErrorMsg = oss.str();
		if (ffVerbLevel > 0)
			cout << ffErrorMsg;
	}
	return NULL;
}
/*-------------------------------------------------------------------------------------------------*/
/*
 * Class:     fmservice_server_fminterface_proxy_FaceFinderProxy
 * Method:    n_getErrorMessage
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_fmservice_server_fminterface_proxy_FaceFinderProxy_n_1getErrorMessage
  (JNIEnv *env, jclass callingClass, jlong jffHandle)
{
	jstring errorString  = env->NewStringUTF(&ffErrorMsg[0]);
	return errorString;
}

/*-------------------------------------------------------------------------------------------------*/
/*
 * Class:     fmservice_server_fminterface_proxy_FaceFinderProxy
 * Method:    n_setVerboseLevel
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_fmservice_server_fminterface_proxy_FaceFinderProxy_n_1setVerboseLevel
  (JNIEnv *env, jclass callingClass, jint vlevel)
{
	ffVerbLevel = vlevel;
}



	
