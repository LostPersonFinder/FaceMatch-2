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
 
/*Implementation for native class fmservice_server_fminterface_proxy_FRDProxy */
/* Author:
 * Date: June 2, 2015
 */


#include "common.h"
#include "fmservice_server_fminterface_proxy_FRDProxy.h"
#include "RegionDetector.h"
#include "stdafx.h"
#include <string.h>

using namespace std;
using namespace FaceMatch;
using namespace cv;

std::string frdErrorMsg;
int  frdVerbLevel = 0;

/*
 * Class:     fmservice_server_fminterface_proxy_FRDProxy
 * Method:    n_createSimpleFRD
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIDDDZ)J
 */
JNIEXPORT jlong JNICALL Java_fmservice_server_fminterface_proxy_FRDProxy_n_1createSimpleFRD	
(JNIEnv *env, jclass callingClass, 
		jstring jXMLModelPath, jstring jFaceModelFN, jstring jProfileModelFN, 
		jstring jSkinColorMapperKind, jstring jSkinColorParmFN, 
		jint jFaceDiameterMin, jint jFaceDiameterMax, jdouble jSkinMassT, jdouble jSkinLikelihoodT, 
		jdouble jMinAspect, jboolean jPreferGpu)
{
	// allocate Strings locally
	const char* cXMLModelPath  = env->GetStringUTFChars(jXMLModelPath, NULL);
	const char* cFaceModelFN  = env->GetStringUTFChars(jFaceModelFN, NULL);
	const char* cProfileModelFN  = env->GetStringUTFChars(jProfileModelFN, NULL);
	const char* cSkinColorMapperKind  = env->GetStringUTFChars(jSkinColorMapperKind, NULL);
	const char* cSkinColorParmFN  = env->GetStringUTFChars(jSkinColorParmFN, NULL);

		
	try
	{	
		// Note: char* arguments are auto converted to string
		FaceRegionDetector* FRDObj = new FaceRegionDetector (
				cXMLModelPath, cFaceModelFN, cProfileModelFN,
				cSkinColorMapperKind, cSkinColorParmFN,
				(unsigned)jFaceDiameterMin, (unsigned)jFaceDiameterMax,
				jSkinMassT, jSkinLikelihoodT,
				jMinAspect, jPreferGpu);
			
		if (frdVerbLevel > 0)
			cout<<"-JNI: n_createSimpleFRD - FRDHandle: "<< (long)FRDObj <<endl;
			
		if (frdVerbLevel > 1)
		cout << "-JNI: LocalFRD parameters: FaceAspectLimit = " << FRDObj->FaceAspectLimit << ",FaceDiameterMax = " << 
		FRDObj->FaceDiameterMax <<", usingGPU" << FRDObj->usingGPU() << endl;

					
		// Free the allocated Strings	
		env->ReleaseStringUTFChars(jXMLModelPath, cXMLModelPath);
		env->ReleaseStringUTFChars(jFaceModelFN,  cFaceModelFN);
		env->ReleaseStringUTFChars(jProfileModelFN, cProfileModelFN);
		env->ReleaseStringUTFChars(jSkinColorMapperKind, cSkinColorMapperKind);
		env->ReleaseStringUTFChars(jSkinColorParmFN, cSkinColorParmFN);
			

		jobject storedObj = env->NewGlobalRef((jobject)FRDObj);
		long localHandle = (long)FRDObj;

		// save the handle to avoid deallocation, and return the allocated handle
		FaceRegionDetector* globalFRD = (FaceRegionDetector*) storedObj;
		if (frdVerbLevel > 1)
		cout << "-JNI: Local FRD handle: " << localHandle << endl;
		return localHandle;		// return the pointer (long handle)
	}

	// error processing
	catch(const exception & e)
	{
		ostringstream errstr;
		errstr.clear();
		errstr <<__FUNCTION__<<"JNI: Exception in creating Simple FRD: " <<e.what()<<endl;
		frdErrorMsg = errstr.str();
		if (frdVerbLevel > 1)
			cout << frdErrorMsg;
	}
	
	catch(...)			// Access violation and other types of errors
	{
		ostringstream errstr;
		errstr.clear();
		errstr << "JNI: unknown exception in creating Simple FRD" << endl;
		frdErrorMsg = errstr.str();
		if (frdVerbLevel > 1)
			cout << frdErrorMsg;
	}
	return (long)NULL;
}



/*
 * Class:     fmservice_server_fminterface_proxy_FRDProxy
 * Method:    n_createFRDWithLock
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIDDDZ)J
 */
JNIEXPORT jlong JNICALL Java_fmservice_server_fminterface_proxy_FRDProxy_n_1createFRDWithLock
 (JNIEnv *env, jclass callingClass, 
		jstring jXMLModelPath, jstring jFaceModelFN, jstring jProfileModelFN, 
		jstring jSkinColorMapperKind, jstring jSkinColorParmFN, 
		jint jFaceDiameterMin, jint jFaceDiameterMax, jdouble jSkinMassT, jdouble jSkinLikelihoodT, 
		jdouble jMinAspect, jboolean jPreferGpu)
{
// allocate Strings locally
	const char* cXMLModelPath  = env->GetStringUTFChars(jXMLModelPath, NULL);
	const char* cFaceModelFN  = env->GetStringUTFChars(jFaceModelFN, NULL);
	const char* cProfileModelFN  = env->GetStringUTFChars(jProfileModelFN, NULL);
	const char* cSkinColorMapperKind  = env->GetStringUTFChars(jSkinColorMapperKind, NULL);
	const char* cSkinColorParmFN  = env->GetStringUTFChars(jSkinColorParmFN, NULL);

	try
	{
		OMPAutoLockSimple static FaceRegionDetector*  FRDObj = NULL;
			
		// Note: char* arguments are auto converted to string
		FRDObj = new FaceRegionDetector (
				cXMLModelPath, cFaceModelFN, cProfileModelFN,
				cSkinColorMapperKind, cSkinColorParmFN,
				(unsigned)jFaceDiameterMin, (unsigned)jFaceDiameterMax,
				jSkinMassT, jSkinLikelihoodT,
				jMinAspect, jPreferGpu);
		if (frdVerbLevel > 0)
			cout<<"-JNI:n_createFRDWithLock - FRDHandle: "<< (long)FRDObj <<endl;
					
		
		// Free the allocated Strings	
		env->ReleaseStringUTFChars(jXMLModelPath, cXMLModelPath);
		env->ReleaseStringUTFChars(jFaceModelFN,  cFaceModelFN);
		env->ReleaseStringUTFChars(jProfileModelFN, cProfileModelFN);
		env->ReleaseStringUTFChars(jSkinColorMapperKind, cSkinColorMapperKind);
		env->ReleaseStringUTFChars(jSkinColorParmFN, cSkinColorParmFN);
			

		jobject storedObj = env->NewGlobalRef((jobject)FRDObj);
		jlong localHandle = (jlong)FRDObj;
		jlong handle = (jlong)storedObj;
		//cout<<"Returning from createFRD - FRDHandle: "<< handle <<endl;
		if (frdVerbLevel > 1)
		cout << "-JNI: LocalFRD parameters: FaceAspectLimit = " << FRDObj->FaceAspectLimit << ",FaceDiameterMax = " << FRDObj->FaceDiameterMax
			<<", usingGPU" << FRDObj->usingGPU() << endl;

		// save the handle to avoid deallocation, and return the allocated handle
		FaceRegionDetector* globalFRD = (FaceRegionDetector*) storedObj;
		if (frdVerbLevel > 0)
			cout << "--JNI:Local FRD handle: " << localHandle << endl;
		return localHandle;		// return the pointer (long handle)
	}

	// error processing
	catch(const exception & e)
	{
		ostringstream errstr;
		errstr.clear();
		errstr <<__FUNCTION__<<"JNI: Exception in creating FRD with Lock: "<<e.what()<<endl;
		frdErrorMsg = errstr.str();
		if (frdVerbLevel > 1)
			cout << frdErrorMsg;
	}
	
	catch(...)			// Access violation and other types of errors
	{
		ostringstream errstr;
		errstr.clear();
		errstr << "JNI: Unknown error in creating FRD with Lock" << endl;
		frdErrorMsg = errstr.str();
		if (frdVerbLevel > 1)
			cout << frdErrorMsg;
	}
	return (long)NULL;
}

/*
 * Class:     fmservice_server_fminterface_proxy_FRDProxy
 * Method:    n_usingGPU
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_fmservice_server_fminterface_proxy_FRDProxy_n_1usingGPU
  (JNIEnv *env, jclass caller, jlong jfrdHandle)
{
	FaceRegionDetector* frdPtr = (FaceRegionDetector*) jfrdHandle;
	if (frdPtr == NULL)
		return false;
	return frdPtr->usingGPU(); 
}

/*
 * Class:     fmservice_server_fminterface_proxy_FRDProxy
 * Method:    n_deallocate
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_fmservice_server_fminterface_proxy_FRDProxy_n_1deallocate
  (JNIEnv *env, jclass caller, jlong jfrdandle)
{
	return true;
}

/*-------------------------------------------------------------------------------------------------*/
/*
 * Class:     fmservice_server_fminterface_proxy_FRDProxy
 * Method:    n_getErrorMessage
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_fmservice_server_fminterface_proxy_FRDProxy_n_1getErrorMessage
  (JNIEnv *env, jclass callingClass, jlong jffHandle)
{
	jstring errorString  = env->NewStringUTF(&frdErrorMsg[0]);
	return errorString;
}

/*-------------------------------------------------------------------------------------------------*/
/*
 * Class:     fmservice_server_fminterface_proxy_FRDProxy
 * Method:    n_setVerboseLevel
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_fmservice_server_fminterface_proxy_FRDProxy_n_1setVerboseLevel
  (JNIEnv *env, jclass callingClass, jint vlevel)
{
	frdVerbLevel = vlevel;
}
/*-------------------------------------------------------------------------------------------------*/