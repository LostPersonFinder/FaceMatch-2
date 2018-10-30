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
 
 /*Implementation for native class fmservice_server_fminterface_proxy_FaceRegionMatcherProxy */
/* Author: 
 * Date: June 2, 2015
 */


#include "common.h"
#include "fmservice_server_fminterface_proxy_FaceRegionMatcherProxy.h"
#include "RegionDetector.h"
#include "FaceFinder.h"
#include "ImageMatcherFaceRegions.h"
#include "ImageDescriptorIndex.h"
#include "stdafx.h"
#include <iostream>
#include <string.h>


using namespace std;
using namespace FaceMatch;
using namespace cv;

std::string fmErrorMsg;
int fmVerbLevel = 0;
//bool singleIndexFileMany = true;

bool createTextFiles = false; // create .yml files for testing only
bool checkFileOpenOverhead = false; // Open/close time overhead for a set of files

float findFileOpenCloseTime(string fn[], int fc);


//-------------------------------------------------------------------------------


/**
// Template FMNdx => ImageMatcherIndexBase<NdxType> : ImageMatcher of specified NdxType
template<class T>
static T & singletonFaceMatcher(const string& indexFilename, 
                                           FaceRegionDetector& FRD, 
                                           unsigned FaceFinderFlags, 
                                           unsigned ImgNormDim, unsigned fmFlags)
{
        StaticLkdCtor T matcher(indexFilename , FRD, FaceFinderFlags, ImgNormDim, fmFlags);
        return matcher;
}
 **/

/*-----------------------------------------------------------------------------------------------*/


/// default image matcher for face/profile regions
typedef ImageMatcherFaceRegionsBase<SigIndexSIFT> ImageMatcherSIFT;
typedef ImageMatcherFaceRegionsBase<SigIndexMany> ImageMatcherMany;
typedef ImageMatcherFaceRegionsBase<SigIndexManyDist> ImageMatcherDIST;
typedef ImageMatcherFaceRegionsBase<SigIndexHaarFace> ImageMatcherHAAR;
typedef ImageMatcherFaceRegionsBase<SigIndexSURF> ImageMatcherSURF;
typedef ImageMatcherFaceRegionsBase<SigIndexORB> ImageMatcherORB;
typedef ImageMatcherFaceRegionsBase<SigIndexLBPH> ImageMatcherLBPH;
typedef ImageMatcherFaceRegionsBase<SigIndexRSILC> ImageMatcherRSILC;

ImageMatcher* getFaceMatcher(FaceRegionDetector& FRD,
        const string & IndexFilename,
        const string & indexType,
        unsigned FaceFinderFlags,
        unsigned ImgNormDim,
        unsigned fmFlags)

{

    if (indexType == "SIFT")
    {
        ImageMatcherSIFT* matcher = new ImageMatcherSIFT(IndexFilename, FRD, FaceFinderFlags, ImgNormDim, fmFlags);
        return matcher;
    }
    else if (indexType == "MANY")
    {
        ImageMatcherMany* matcher =
                new ImageMatcherMany(IndexFilename, FRD, FaceFinderFlags, ImgNormDim, fmFlags);
        return matcher;
    }
    else if (indexType == "DIST")
    {
        ImageMatcherDIST* matcher =
                new ImageMatcherDIST(IndexFilename, FRD, FaceFinderFlags, ImgNormDim, fmFlags);
        return matcher;
    }
    else if (indexType == "HAAR")
    {
        ImageMatcherHAAR* matcher =
                new ImageMatcherHAAR(IndexFilename, FRD, FaceFinderFlags, ImgNormDim, fmFlags);
        return matcher;
    }
    else if (indexType == "SURF")
    {
        ImageMatcherSURF* matcher =
                new ImageMatcherSURF(IndexFilename, FRD, FaceFinderFlags, ImgNormDim, fmFlags);
        return matcher;
    }
    else if (indexType == "ORB")
    {
        ImageMatcherORB* matcher =
                new ImageMatcherORB(IndexFilename, FRD, FaceFinderFlags, ImgNormDim, fmFlags);
        return matcher;
    }
    else if (indexType == "LBPH")
    {
        ImageMatcherLBPH* matcher =
                new ImageMatcherLBPH(IndexFilename, FRD, FaceFinderFlags, ImgNormDim, fmFlags);
        return matcher;
    }
    else if (indexType == "RSILC")
    {
        ImageMatcherRSILC* matcher =
                new ImageMatcherRSILC(IndexFilename, FRD, FaceFinderFlags, ImgNormDim, fmFlags);
        return matcher;
    }


    else
        throw FaceMatch::Exception("Unknown face match \"TEST\" mode " + indexType);
}



/*------------------------------------------------------------------------------------*/

/*
 * Class:     fmservice_server_fminterface_proxy_FaceRegionMatcherProxy
 * Method:    n_createImageMatcher
 * Signature: (JLjava/lang/String;III)J
 */
JNIEXPORT jlong JNICALL Java_fmservice_server_fminterface_proxy_FaceRegionMatcherProxy_n_1createImageMatcher
(JNIEnv *env, jclass callingClass, jlong jfrdHandle, jstring jindexType, jint jffFlags, jint jimageDim, jint jfmFlags)
{
    // allocate Strings locally
    const char* cIndexType = env->GetStringUTFChars(jindexType, NULL);

    // give empty indexFile name, otherwise, it assumes an existing file to which data is to be added
    string IndexFilename = "";

    FaceRegionDetector* pFRD = (FaceRegionDetector*) jfrdHandle;
    FaceRegionDetector& FRD = (*pFRD);

    unsigned FaceFinderFlags = jffFlags;
    unsigned ImgNormDim = jimageDim;
    unsigned fmFlags = jfmFlags;



    try
    {
        ImageMatcher* faceMatcher
                = getFaceMatcher(FRD, IndexFilename, cIndexType, FaceFinderFlags, ImgNormDim, fmFlags);

        /*
                ImageMatcher* faceMatcher =
                getSingletonFaceMatcher(FRD, IndexFilename, cIndexType, FaceFinderFlags, ImgNormDim,  fmFlags); 
         */

        // free the allocated string, save a global reference and return the handle
        env->ReleaseStringUTFChars(jindexType, cIndexType);

        ImageMatcher* imHandle = faceMatcher;
        long long matcherHandle = (long long) imHandle;
        jobject storedObj = env->NewGlobalRef((jobject) imHandle);

        if (fmVerbLevel >= 2)
            cout << "-JNI: Number of ALREADY loaded entries: " << faceMatcher-> count() << endl;

        return matcherHandle;
    }

    // error processing
    catch (const exception & e)
    {
        ostringstream errstr;
        errstr.clear();
        errstr << __FUNCTION__ << " exception: " << e.what() << endl;
        fmErrorMsg = errstr.str();
        if (fmVerbLevel > 0)
            cout << fmErrorMsg;
        return 0;
    }
}
/*------------------------------------------------------------------------------------*/

/*
 * Class:     fmservice_server_fminterface_proxy_FaceRegionMatcherProxy
 * Method:    n_ingestRegion
 * Signature: (JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_fmservice_server_fminterface_proxy_FaceRegionMatcherProxy_n_1ingestRegion
(JNIEnv *env, jclass callingClass, jlong imHandle, jstring jindexFileName, jstring jimageFileName,
        jstring jimageTag, jstring jfaceAttrib)
{

    const char* cIndexFileName = env->GetStringUTFChars(jindexFileName, NULL);
    const char* cImageFileName = env->GetStringUTFChars(jimageFileName, NULL);
    const char* cImageTag = env->GetStringUTFChars(jimageTag, NULL);

    const char* cFaceAttrib = (jfaceAttrib == NULL) ? NULL : env->GetStringUTFChars(jfaceAttrib, NULL);
    if (fmVerbLevel > 1)
    {
        if (cFaceAttrib != NULL)
            cout << "-JNI: ingestRegion: ImageMatcherHandle " << imHandle << ", FaceAttributes: " << cFaceAttrib << endl;
        else
            cout << "-JNI: ingestRegion: ImageMatcherHandle " << imHandle << ", ImageTag: " << cImageTag << endl;
    }
    long long imPtr = (long long) imHandle;


    ImageMatcher* pFaceMatcher = (ImageMatcher *) imPtr;
    std::string ImageFileName(cImageFileName);
    std::string IndexFileName(cIndexFileName);
    std::string ImageTag(cImageTag);
    std::string FaceAttrib(cFaceAttrib);

    // Convert the image face regions to their full IDs
    // IDs are in the form: ImageID\t\FaceRegion
    // output face region coordinates to the stream
    ostringstream oss;
    oss.clear();
    oss << ImageTag;
    if (!FaceAttrib.empty())
        oss << "\t" << FaceAttrib; // region with all attributes
    string regionIDstr = oss.str();
    if (fmVerbLevel > 1)
        cout << "JNI: Attributes  of face to be ingested in " << ImageFileName << ": " << regionIDstr << endl;

    int num = 0;
    try
    {
        int numIndexed = pFaceMatcher->ingest(ImageFileName, regionIDstr, 0); // regionIDstr	
        if (fmVerbLevel > 1)
            cout << "-JNI: number of regions indexed: " + numIndexed << endl;
        num = numIndexed;
        if (num < 0)
        {
            if (fmVerbLevel > 0)
                cout << "-JNI: error in ingesting regions in image " + ImageFileName << endl;
        }
        else if (num == 0) // region not indexed due to invalid face data etc
        {
            if (fmVerbLevel > 0)
                cout << "-JNI: did not find valid face in given region" << endl;
        }
        else
        {
            pFaceMatcher->save(IndexFileName);
            if (fmVerbLevel > 0)
                cout << "-JNI: Stored " << num << " IndexDescriptors in binary file: " << IndexFileName << endl;

            if (createTextFiles) //also store in text format
            {
                string textIndexFileName = IndexFileName + ".txt";
                pFaceMatcher->save(textIndexFileName);

                if (fmVerbLevel > 1)
                    cout << " JNI: Also stored IndexDescriptors in text file: " << textIndexFileName << endl;
            }
        }
    }

    // Error processing
    catch (const exception & e)
    {
        ostringstream errstr;
        errstr.clear();
        errstr << __FUNCTION__ << " exception: " << e.what() << endl;
        fmErrorMsg = errstr.str();
        if (fmVerbLevel > 0)
            cout << fmErrorMsg;
    }
    catch (...) // Access violation and other types of errors
    {
        ostringstream errstr;
        errstr.clear();
        errstr << "Exiting due to unknown exception" << endl;
        fmErrorMsg = errstr.str();
        if (fmVerbLevel > 0)
            cout << fmErrorMsg;
    }
    // free up the allocated Strings
    env->ReleaseStringUTFChars(jindexFileName, cIndexFileName);
    env->ReleaseStringUTFChars(jimageFileName, cImageFileName);
    env->ReleaseStringUTFChars(jimageTag, cImageTag);
    if (cFaceAttrib != NULL)
        env->ReleaseStringUTFChars(jfaceAttrib, cFaceAttrib);
    return num;
}
/*------------------------------------------------------------------------------------*/

/*
 * Class:     fmservice_server_fminterface_proxy_FaceRegionMatcherProxy
 * Method:    n_clearIndex
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_fmservice_server_fminterface_proxy_FaceRegionMatcherProxy_n_1clearIndex
(JNIEnv *, jclass, jlong imHandle)
{
    // Clear the indexed data held by the ImageMatcher
    long long imPtr = (long long) imHandle;
    ImageMatcher* pFaceMatcher = (ImageMatcher *) imPtr;
    try
    {
        int count = pFaceMatcher->count();
        if (count > 0)
            pFaceMatcher->clear();
        return 1;
    }

    // error processing
    catch (const exception & e)
    {
        ostringstream errstr;
        errstr << __FUNCTION__ << " exception: " << e.what() << endl;
        fmErrorMsg.append(errstr.str());
        if (fmVerbLevel > 0)
            cout << fmErrorMsg;
    }

    catch (...) // Access violation and other types of errors
    {
        ostringstream errstr;
        errstr << "Unknown error in ingesting index data" << endl;
        fmErrorMsg.append(errstr.str());
        if (fmVerbLevel > 0)
            cout << fmErrorMsg;
    }
    return 0;
}
/*------------------------------------------------------------------------------------*/

/*
 * Class:     fmservice_server_fminterface_proxy_FaceRegionMatcherProxy
 * Method:    n_loadIndexData
 * Signature: (J[Ljava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_fmservice_server_fminterface_proxy_FaceRegionMatcherProxy_n_1loadIndexData
(JNIEnv *env, jclass callingClass, jlong imHandle, jobjectArray bbufArray)
{
    // Retrieve the set of byte[] data from the index buffers and load it in the ImageMatcher
    // Reference: https://docs.oracle.com/javase/8/docs/technotes/guides/jni/jni-14.html#GetDirectBufferAddress

    long long imPtr = (long long) imHandle;
    ImageMatcher* pFaceMatcher = (ImageMatcher *) imPtr;

    // Get the ByteBuffer object in the array

    jsize length = env->GetArrayLength(bbufArray);
    if (fmVerbLevel > 0)
        cout << "JNI: Number of imageDescriptors: " << length << endl;

    for (int i = 0; i < length; i++)
    {
        jobject bbuf = env->GetObjectArrayElement(bbufArray, i);
        if (bbuf == NULL)
        {
            fmErrorMsg.append("Invalid byteBuffer provided in position: ").append(std::to_string(i));
            return 0;
        }
        void *addr = env->GetDirectBufferAddress(bbuf);
        byte* indexBuf = (byte *) addr;

        jlong bufSize = env->GetDirectBufferCapacity(bbuf);
        if (fmVerbLevel > 1)
        {
            cout << "Index data length: " << bufSize << ", first 32 characters: " << endl;
            for (int i = 0; i < 64; i++)
                cout << indexBuf[i];
            cout << endl;
        }
    }

    // The following capability does not currently exist in the FaceMatch Lib
    /*	try
            {
                    //Load this data to the imageMatcher
                    pFaceMatcher.load(dataBuf, bufSize);
            }
            catch(exception& ex)
            {
                    cout << "Exception in loading image descriptor data in ImageMatcher" << endl;
                    return 0;
            }
            catch (...)
            {
                    cout << " Received unknown error in loading  image descriptor data in ImageMatcher" << endl;
                    return 0;
            }*/

    return length; // Number of files loaded
}


/*-------------------------------------------------------------------------------------------*/

/*
 * Class:     fmservice_server_fminterface_proxy_FaceRegionMatcherProxy
 * Method:    n_loadIndexFiles
 * Signature: (J[Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_fmservice_server_fminterface_proxy_FaceRegionMatcherProxy_n_1loadIndexFiles
(JNIEnv *env, jclass callingClass, jlong imHandle, jobjectArray jfilenameArray)
{
    // Retrieve the set of IndexFiles and load the data to the ImageMatcher
    long long imPtr = (long long) imHandle;
    ImageMatcher* pFaceMatcher = (ImageMatcher *) imPtr;


    int fileCount = env->GetArrayLength(jfilenameArray);
    if (fmVerbLevel > 0)
        cout << "-JNI: Number of imageDescriptor files to load: " << fileCount << endl;


    //---------------------------------------------------------------------------------------------
    // Get the IndexFileNames in the array and ingest each one, one at a time
    // Note: proceed if one or more files found to be corrupt

    fmErrorMsg.clear();

    auto t3 = std::chrono::high_resolution_clock::now();
    int indexCount = 0;


    string* IndexFileNames = new string [fileCount];
    jstring* jindexFilenames = new jstring[fileCount];
    const char** cIndexFileNames = new const char*[fileCount];

    for (int i = 0; i < fileCount; i++)
    {
        jstring jindexFilename = (jstring) env->GetObjectArrayElement(jfilenameArray, i);
        jindexFilenames[i] = jindexFilename;
        cIndexFileNames[i] = env->GetStringUTFChars(jindexFilename, NULL);

        if (cIndexFileNames[i] == NULL)
        {
            fmErrorMsg.append("Invalid Index file name provided in position: ").append(std::to_string(i));
            return 0;
        }

        IndexFileNames[i] = string(cIndexFileNames[i]);



    }// end filename  conversion
   // cout << "-JNI: retrieved  " << fileCount << " files names." << endl;

    // if want to check file open/load times -- for testing only
    int fcnt = fileCount;
    float openCloseTime = 0;

    if (checkFileOpenOverhead)
        openCloseTime = findFileOpenCloseTime(IndexFileNames, fcnt);


    // now load these files  for the ImageMatcher
    // Concatenate all files names to a single string for batch loading by FMLib, without incurring the
    // high overhead of vertical indexing after each file load
    try
    {
        ostringstream fileList;
         fileList << IndexFileNames[0];
    
        for (int i = 1; i < fileCount; i++)
        {
            fileList << "\n" << IndexFileNames[i];
        }
         string fileNameList = fileList.str();
         int size = pFaceMatcher->load(fileNameList);
         if (fmVerbLevel > 1)
            cout << "-JNI: Loaded  " << fileCount << "  imageDescriptor files." << endl;
    }

    // error processing
    catch (const exception & e)
    {
        ostringstream errstr;
        errstr << __FUNCTION__ << " exception: " << e.what() << endl;
        fmErrorMsg.append(errstr.str());
        cout << fmErrorMsg;
    }

    catch (...) // Access violation and other types of errors
    {
        ostringstream errstr;
        errstr << "Unknown error in loading index data" << endl;
        fmErrorMsg.append(errstr.str());
        cout << fmErrorMsg;
    }

    indexCount = pFaceMatcher->count();
    auto t4 = std::chrono::high_resolution_clock::now();
    float elapsedTime = std::chrono::duration_cast<std::chrono::milliseconds>(t4 - t3).count();
    float dataLoadTime = elapsedTime - openCloseTime;
    float averageTime = dataLoadTime / fileCount;

    if (fmVerbLevel > 0)
    std::cout << "-JNI: loaded " << indexCount << " sets of Indexes from " << fcnt << " files by ImageMatcher in: " << dataLoadTime
            << " millisec, average time per file: " << averageTime << " millisec " << endl;


    // free up the allocated Strings
    for (int i = 0; i < fileCount; i++)
        env->ReleaseStringUTFChars(jindexFilenames[i], cIndexFileNames[i]);

    delete[] jindexFilenames;
    return fcnt; // Number of files loaded
}

//---------------------------------------------------------------------------------------------
// Check the time to open/close a large set of index (or any binary) files in milliseconds
//---------------------------------------------------------------------------------------------

float findFileOpenCloseTime(string fileNames[], int fileCount)
{
    float openCloseTime = 0;
    int datalen = 0;

    auto t1 = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < fileCount; i++)
    {
        string cFileName = fileNames[i];
        try
        {
            std::ifstream is(cFileName, ios::binary | ios::in);
            if (is)
            {
                // get length of file:
                is.seekg(0, is.end);
                int length = is.tellg();
                datalen += length;
                is.close();
            }
        }
        catch (...) // Access violation and other types of errors
        {
            ostringstream errstr;
            errstr << "Unknown error in reading index data" << endl;
            fmErrorMsg.append(errstr.str());
            if (fmVerbLevel > 0)
                cout << fmErrorMsg;
        }
    }
    auto t2 = std::chrono::high_resolution_clock::now();

    openCloseTime = std::chrono::duration_cast<std::chrono::microseconds>(t2 - t1).count();
    if (fmVerbLevel > 1)
    std::cout << "-JNI: time to open close " << fileCount << " files " <<
            (openCloseTime / 1000) << " milliseconds " << endl;
    return (openCloseTime / 1000);
}

//>>> new method
/*-------------------------------------------------------------------------------------------*/

/*
 * Class:     fmservice_server_fminterface_proxy_FaceRegionMatcherProxy
 * Method:    n_loadMasterIndexFile

 * Signature: (JLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_fmservice_server_fminterface_proxy_FaceRegionMatcherProxy_n_1loadMasterIndexFile
(JNIEnv *env, jclass callingClass, jlong imHandle, jstring jmindexFileName)
{

    fmErrorMsg.clear();

    // Load the two-tier master index file (contatning a list of .mdx files to be loaded for query
    const char* cIndexFileName = env->GetStringUTFChars(jmindexFileName, NULL);

    if (cIndexFileName == NULL)
    {
        fmErrorMsg.append("Invalid Master Index file name provided: ");
        return 0;
    }

    long long imPtr = (long long) imHandle;
    ImageMatcher* pFaceMatcher = (ImageMatcher *) imPtr;

    int indexCount = 0;
    auto t1 = std::chrono::high_resolution_clock::now();
    try
    {
        //Load this data to the imageMatcher 
        pFaceMatcher->load(cIndexFileName);
    }

    // error processing
    catch (const exception & e)
    {
        ostringstream errstr;
        errstr << __FUNCTION__ << " exception: " << e.what() << endl;
        fmErrorMsg.append(errstr.str());
        if (fmVerbLevel > 0)
            cout << fmErrorMsg;
    }

    catch (...) // Access violation and other types of errors
    {
        ostringstream errstr;
        errstr << "Unknown error in loading index data" << endl;
        fmErrorMsg.append(errstr.str());
        if (fmVerbLevel > 0)
            cout << fmErrorMsg;
    }

    indexCount = pFaceMatcher->count();
    auto t2 = std::chrono::high_resolution_clock::now();
    float elapsedTime = std::chrono::duration_cast<std::chrono::milliseconds>(t2 - t1).count();
    float averageTime = elapsedTime / indexCount;

    std::cout << "-JNI: loaded " << indexCount << " sets of Indexes from " << cIndexFileName << " file by ImageMatcher in: "
            << elapsedTime << " millisec, average time per index: " << averageTime << " millisec " << endl;

    return indexCount; // Number of files loaded
}
//<<<<

/*----------------------------------------------------------------------------------------------------*/

/*
 * Class:     fmservice_server_fminterface_proxy_FaceRegionMatcherProxy
 * Method:    n_saveIndexData
 * Signature: (JLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_fmservice_server_fminterface_proxy_FaceRegionMatcherProxy_n_1saveIndexData
(JNIEnv *env, jclass callingClass, jlong imHandle, jstring jindexFileName)
{

    // get the imageMatcher
    long long imPtr = (long long) imHandle;
    ImageMatcher* pFaceMatcher = (ImageMatcher *) imPtr;

    const char* cIndexFileName = env->GetStringUTFChars(jindexFileName, NULL);
    fmErrorMsg.clear();

    if (cIndexFileName == NULL)
    {
        fmErrorMsg.append("Invalid Index file name provided: ");
        return 0;
    }
    int indexCount = pFaceMatcher->count();
    // write the indexData help by the image macher
    pFaceMatcher->save(cIndexFileName);

    // for testing, open the file again and find its size
    int length = 0;
    std::ifstream is(cIndexFileName, ios::binary | ios::in);
    if (is)
    {
        // get length of file:
        is.seekg(0, is.end);
        length = is.tellg();
        std::cout << "-JNI: saved " << cIndexFileName << ", file size" << length << endl;
    }
    env->ReleaseStringUTFChars(jindexFileName, cIndexFileName);
    return length;
}





/*------------------------------------------------------------------------------------*/

/*
 * Class:     fmservice_server_fminterface_proxy_FaceRegionMatcherProxy
 * Method:    n_removeRegions
 * Signature: (J[Ljava/lang/String;)I
 */

JNIEXPORT jint JNICALL Java_fmservice_server_fminterface_proxy_FaceRegionMatcherProxy_n_1removeRegions
(JNIEnv *env, jclass callingClass, jlong imHandle, jobjectArray jregionArray)
{
    // Remove the FaceRegion index data loaded to the ImageMatcher
    long long imPtr = (long long) imHandle;
    ImageMatcher* pFaceMatcher = (ImageMatcher *) imPtr;

    fmErrorMsg.clear();

    // Get the IndexFileNames in the array
    int regionCount = env->GetArrayLength(jregionArray);

    string regions = "";
    for (int i = 0; i < regionCount; i++)
    {
        jstring jregionId = (jstring) env->GetObjectArrayElement(jregionArray, i);
        const char* cRegionId = env->GetStringUTFChars(jregionId, NULL);

        if (cRegionId == NULL)
        {
            fmErrorMsg.append("NULL region ID provided: ");
            cout << "NULL region ID provided in position: " << i << endl;
            continue;
        }

        // Regions are concatenated with new line separator
        if (i == 0)
            regions = std::string(cRegionId);
        else
            regions = regions.append("\n").append(std::string(cRegionId));

        // release the allocated string
        env->ReleaseStringUTFChars(jregionId, cRegionId);
    }
    /*if (fmVerbLevel >= 1)     //for initial debugging only 
            {
                    cout << "-JNI: Region to remove: \"" << regions << "\"" << endl;
                    // list all regions currently in the image matcher
                    string res;
                    pFaceMatcher->list(res, regions);
                    cout << "Regions exiting: " <<  res  << endl;
            }
     */
    int nr = 0; // number of regions removed
    try
    {
        //Remove this data from the imageMatcher
        nr = pFaceMatcher->remove(regions);
        int count = pFaceMatcher->count();
        if (fmVerbLevel > 0)
            cout << "-JNI: Removed " << nr << " face regions. Remaining regions for face matching = " << count << endl;
        if (nr > 0)
        {
            //ImageMatcherDIST*  matcher = (ImageMatcherDIST *)pFaceMatcher;
            ostringstream listing;
            string lstr = listing.str();
            pFaceMatcher->list(lstr);
            cout << "List: " << lstr;
        }
    }

    // Error processing
    catch (const exception & e)
    {
        ostringstream errstr;
        errstr << __FUNCTION__ << " exception: " << e.what() << endl;
        fmErrorMsg.append(errstr.str());
        if (fmVerbLevel > 0)
            cout << fmErrorMsg;
    }

    catch (...) // Access violation and other types of errors
    {
        ostringstream errstr;
        errstr << "Unknown error in removing index data" << endl;
        fmErrorMsg.append(errstr.str());
        if (fmVerbLevel > 0)
            cout << fmErrorMsg;
    }
    return nr; // Number of regions removed
}

/*------------------------------------------------------------------------------------*/

/*
 * Class:     fmservice_server_fminterface_proxy_FaceRegionMatcherProxy
 * Method:    n_queryMatches
 * Signature: (JLjava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_fmservice_server_fminterface_proxy_FaceRegionMatcherProxy_n_1queryMatches
(JNIEnv *env, jclass claaingClass, jlong imageMatcherHandle,
        jstring jqueryImageName, jobjectArray jqueryRegionArray, jfloat jtolerance)
{
    // Retrieve the ImageMatcher 
    long long imPtr = (long long) imageMatcherHandle;
    ImageMatcher* pFaceMatcher = (ImageMatcher *) imPtr;
    const char* cQueryImageName = env->GetStringUTFChars(jqueryImageName, NULL);

    // Get the queryFileName and Face Regions, assumed to be not null 
    int faceCount = env->GetArrayLength(jqueryRegionArray);
    if (fmVerbLevel > 1)
        cout << "-JNI: Number of Face regions given in query image: " << faceCount << endl;

    string queryImage = std::string(cQueryImageName);
    for (int i = 0; i < faceCount; i++)
    {
        jstring jqueryRegion = (jstring) env->GetObjectArrayElement(jqueryRegionArray, i);
        const char* cQueryRegion = env->GetStringUTFChars(jqueryRegion, NULL);
        string region = std::string(cQueryRegion);
        if (fmVerbLevel > 1)
            cout << "Region " << i << ": " <<  region << endl;
        queryImage += ("\t" + region);
        env->ReleaseStringUTFChars(jqueryRegion, cQueryRegion);
    }
    if (fmVerbLevel > 1)
    {
        int flen = queryImage.length();
        cout << "-JNI: Query image sent to FMLib  ImageMatcher: " << queryImage  << endl;
    }
    float tolerance = jtolerance;
    if (tolerance == 1.0) // Note: 1.0 returns only one, see FM corrected document
        tolerance = 0.999; // get all matches 

    string QueryResult = "";
    int status = 1;
    try
    {
        int n = pFaceMatcher->query(QueryResult, queryImage, tolerance, 0);
        // print the result
        if (fmVerbLevel > 1)
        {
            cout << "-JNI: Tolerance: " << tolerance << ", Number of matches found: " << n << endl;
            cout << QueryResult << endl;
        }
    }
    catch (const exception & e)
    {
        ostringstream errstr;
        errstr << __FUNCTION__ << " exception: " << e.what() << endl;
        fmErrorMsg.append(errstr.str());
        if (fmVerbLevel > 0)
            cout << fmErrorMsg << endl;
        status = 0;
    }

    // release allocated strings
    env->ReleaseStringUTFChars(jqueryImageName, cQueryImageName);

    // return results
    if (status == 1)
    {
        //if (fmVerbLevel > 1)
        //	cout <<"-JNI: returning query result: " << QueryResult << endl;

        const char* queryResult = &(QueryResult[0]); // convert to char*
        jstring jqueryResult = env->NewStringUTF(queryResult);
        return jqueryResult;
    }
    else
        return NULL;
}
/*-------------------------------------------------------------------------------------------------*/

/*
 * Class:     fmservice_server_fminterface_proxy_FaceRegionMatcherProxy
 * Method:    n_getErrorMessage
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_fmservice_server_fminterface_proxy_FaceRegionMatcherProxy_n_1getErrorMessage
(JNIEnv *env, jclass callingClass, jlong jffHandle)
{
    jstring errorString = env->NewStringUTF(&fmErrorMsg[0]);
    return errorString;
}

/*-------------------------------------------------------------------------------------------------*/

/*
 * Class:     fmservice_server_fminterface_proxy_FaceRegionMatcherProxy
 * Method:    n_setVerboseLevel
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_fmservice_server_fminterface_proxy_FaceRegionMatcherProxy_n_1setVerboseLevel
(JNIEnv *env, jclass callingClass, jint vlevel)
{
    fmVerbLevel = vlevel;
}




