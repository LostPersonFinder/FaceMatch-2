/*
Informational Notice:
This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
an agency of the Department of Health and Human Services, United States Government.

- The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.

- The license does not supersede any applicable United States law.

- The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.

Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
(FAR) 48 C.F.R. Part52.227-14, Rights in Data—General.
The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
non-commercial products.

LICENSE:

Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
as provided by Federal law.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
•	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.

•	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.

•	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITEDTO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/** BatchInputSelectionDialog.java
 * 
 *  Allows FaceMatch2 user to provide inputs to conduct a test/operation or analyze results of a
 * previously stored  test operation performed in "batch mode"
 */

package workbench.reqproc.input;

    
import java.util.HashMap;

import javax.swing.JFrame;

public class BatchInputSelectionDialog extends InputSelectionDialog
{

    // Note: Must be in same order as in Menu commands 
     private static String[] batchOperationCommands = {
          INGEST_BATCH_RT, REMOVE_BATCH_RT, 
         QUERY_BATCH_RT,  FF_BATCH_RT, SCENARIO_TEST};
    
    private static String[] OperationTypeRBTexts = batchOperationCommands;
    private static String myPanelTitle =  "Select options for Batch operations";
     

     /*--------------------------------------------------------------------------------------------------------*/
     // constructor
    public BatchInputSelectionDialog( JFrame frame, HashMap<String, Object>defaultParams)
    {
        super(frame, defaultParams);
        setPanelTitle(myPanelTitle);
        setOperationTypeInfo(batchOperationCommands, OperationTypeRBTexts);
         buildInputSelectionPanel();
    }

    /*------------------------------------------------------------------------------------*/
    // set the initial choices as indicated in the input parameter map
    /*------------------------------------------------------------------------------------*/
    public  void setInitialChoices( String command, HashMap<String, Object> inputParams)
    {  
        super.setInitialChoices(command,  inputParams);
    }
    ////////////////////////////////////////////////////////////////////////
}
