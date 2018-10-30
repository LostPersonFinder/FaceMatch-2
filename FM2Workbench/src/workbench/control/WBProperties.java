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

/*
 * standard configured properties/values for WB operations
 */

package workbench.control;

import java.awt.Color;
import java.util.Properties;

import javax.swing.JFrame;


public class  WBProperties
{
    // constants
     public static Color Grey1 = new Color (242, 242, 242);
    public static Color Grey2 = new Color (234, 234, 234);
    public static Color Grey3 = new Color (212, 212, 212);
    
    public static Color BlueGrey1 = new Color (212, 220, 228);
    public static Color BlueGrey2 = new Color (203, 213, 223);
    public static Color BlueGrey3 = new Color (168, 184, 200);
    public static Color NAVY =  new Color(0x0, 0x20, 0x60);           // RGB: 0, 32, 96 - Navy

    public static String fmServerURL ;                                                // url to connect to for FM2 Web services
    public static String  regionWebServiceURL ;                              // for  face region related operations
    public static String imageWebServiceURL;                                // for whole image related operations
    public static Color defaultBgColor = BlueGrey1;                        // new Color (80, 100, 40);           
    public static Color defaultFontColor = NAVY;
    
    //Proprties set at startup time. needed by other modules
    public static Properties configProperties;
    public static boolean isAdminClient;                                            // is client running by FM2 administrator
    //public static String serverLogFile;                                                 // to extract info from server log file
    public static String clientLogFile;                                                   // to extract info from client's log file
    public static JFrame mainFrame;                                                 // main display frame
}


    
