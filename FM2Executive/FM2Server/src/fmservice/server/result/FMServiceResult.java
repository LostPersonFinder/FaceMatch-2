/*
 * /*
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
package fmservice.server.result;

import org.json.simple.JSONObject;
import fmservice.httputils.common.ServiceConstants;
import java.util.Calendar;
import java.util.Date;

/**
 * A class to record/return the result of a FaceMatch service operations. 
 * Derived classes report results of different known operations.
 * Top level class is used for reporting invalid operation request types only
 *
 *
 */
public  class  FMServiceResult implements ServiceConstants
{
    public  long requestId;
    public int client_id;
    public int serviceType;
    public int operation;
    public Status serviceStatus;
    public  Date  serviceDate;          // Time when the service was done
    public boolean gpuUsed;           // was GPU used by FaceMatchLib for this operation
    public float serviceTime;             // total  time to perform  the service by the FM server, to be filled later
    
    
    /*----- constructors  ---*/
     public FMServiceResult( int svcType, int operType)
    {
        this(svcType, operType, 0, "");
     }
    
     public FMServiceResult( int svcType, int operType, int statusCode, String statusMsg)
    {
        this(svcType, operType, new Status(statusCode, statusMsg));  
     } 
     
    public FMServiceResult( int svcType, int oper, Status inStatus)
    {
        requestId = -1;                     // unique ID set for each service request by the FaceMatch server
        serviceType = svcType;
        operation = oper;
        serviceStatus = inStatus;
        serviceTime = (float) ( 1.0);
        serviceDate = Calendar.getInstance().getTime();
     }
   
    
    /*---- methods ---*/
    public  boolean isValidRequest()
    {
        return (serviceType != ServiceConstants.INVALID_OPERATION);
    }
    
   public void setStatus(int statusCode, String statusMsg)
   {
      serviceStatus = new Status(statusCode, statusMsg);
   }  
   
    public void setStatus(Status inStatus)
   {
      serviceStatus = inStatus;
   }     

    public  int getServiceType()
    {
        return serviceType;
    }
     public  int getOperationType()
    {
        return operation;
    }
    //------------------------------------
    public Status getStatus()
    {
        return serviceStatus;
    }
    public int  getStatusCode()
    {
        return serviceStatus.statusCode;
    }
    public String  getStatusMessage()
    {
        return serviceStatus.statusMessage;
    }
    public boolean isSuccess()
    {
       return (serviceStatus.isSuccess());        
    }
     
     
     /*-------------------------------------------------------------------*/
         public  String convertToJSONString()
         {
              JSONObject  resultObj = new JSONObject();
               // Fill-in the general information
              fillStandardInfo(resultObj);
              return resultObj.toJSONString();
         }
         
         protected void fillStandardInfo(JSONObject resObj)
         {
            resObj.put(SERVICE, serviceType);
            resObj.put(OPERATION, operation);
            resObj.put(STATUS_CODE, serviceStatus.statusCode);
            resObj.put(STATUS_MESSAGE,  serviceStatus.statusMessage);
            resObj.put(SERVICE_TIME, serviceTime);
            serviceDate = Calendar.getInstance().getTime();         // reset to "Now"
            resObj.put(SERVICE_DATE, serviceDate.toString());
         }
}
