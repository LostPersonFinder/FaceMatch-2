ABOUT
-----
FaceMatch2 (FM2) system (comprising FM2WebServices, FM2WebServer, FM2Executive and FM2Workbench) are products of the Lost Person Finder project (http://lpf.nlm.nih.gov) (LPF) at the Lister Hill National Center for Biomedical Communications, which is an intramural R&D division of the U.S. National Library of Medicine, part of the U.S. National Institutes of Health. 

FaceMatch2 was developed as a set of Web services to provide face detection and face query capabilities to a known user by using the LPF-developed FaceMatch Library. FM2 Web Server is a Java-based system which runs on a Linux platform under Tomcat and uses HTTP-REST based communications protocol. The design and implementation details of the FM2 system are provided in the Documents section. 
If an NVIDIA GPU is available, FM2 uses it for faster facematching performance. Otherwise, all operations are conducted using the CPU only.

FM2 was implemented and tested as a more flexible and standardized replacement for the Microsoft-based operational FaceMatch system used by PeopleLocator. However, FM2 has no explicit link with, or operational dependency on, the latter - which simply acts as a client of FM2.

For a full description of the FaceMatch2 system, see the Archive 2018 Conference Proceedings article:
"FaceMatch: A System for Dynamic Search and Retrieval of Faces." (https://lhncbc.nlm.nih.gov/system/files/pub9805.pdf)
(Note: FaceMatch in this article referes to the FaceMatch2 system, the replacement for the original Microsoft-based system)

LICENCE
-------
This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, an agency of the Department of Health and Human Services, United States Government.

The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.

The license does not supersede any applicable United States law.

The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.

Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation (FAR) 48 C.F.R. Part52.227-14, Rights in Data?General.
The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and non-commercial products.

LICENSE:

Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership, as provided by Federal law.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

?	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.

?	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

?	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes of Health, and the names of any of the software developers shall not be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITEDTO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


FaceMatch2 WebServices
--------------------
These are HTTP/REST based web services which may be used to by a FM2 client to search on single or multiple faces 
in a photographic image. They are described in the file web.xml included in the Documents directory.

The face matching capability is provided by the underlying C++ facematch library
developed at NLM, which is invoked by the FM2-specific Java Native Interface module. This module, called FM2JNI,
is the only non-Java software component of FM2. 

Capabilities:
- Face localization, single and multiple regions.
- Face ingest, single and multiple regions.
- Face query, single and multiple regions.
- Suppot of multiple clients (users) by the Web server
- Support of multiple search domains by a client for faster, localized search
- Database recording of all user transactions, with multiple time stamps
- No local saving of user ingested images for face matching, only their extracted features

FaceMatch2 Web Server
----------------------
This front-end component runs under the Apachie/Tomcat Server, and is supported by a MySQL database. 
All service requests from a client application are received as HTTP/REST based requests, and the results are sent
using the similar protocols, as encoded in the Web.xml file and described in the FM2 User Interface Document.
The FM2 WebServer is linked to the lower level executive, called the FM2Executive, 
to carry out all requested operations and return the results.

FM2 Executive
-------------
This is the back-end component is responsible for interpreteting and routing all requests 
to the appropriate FM2 modules - using a configuration file indicated in the Web.xml file. It consists of two 
components: The FaceMatch Server and the FaceMatch JNI (Java Native Interface). The later consists of 
a set of C++ modules which are compiled and installed as a shared library under Tomcat to interface with 
the Lost Person Finder's FaceMatch Library, which is also installed as a Tomcat shared library.

The "installedConfig" directory contains all configuration and options files used by the FM2Server during operations.
It may be moved to a different location for operations if desired. The parameter "fm2server.configfile" 
in the webfm2server.cfg file must point to the full pathname of this file.

Note that FM2 Server's interface with the FaceMatch library is highly modular, and can be easily replaced 
to use any other library with similar functionality, with appropriarte changes to the JNI modules as necessary.

FM2Workbench
------------
This is a GUI-based client application, provided for the ease of development and testing at a client facility in 
creating facematch service requests, sending them to the FM2 Web Server, receiving the results and visually displaying 
them on the screen. It may also be used internally at the server facility to perform both single-request and 
batch-based testing, debugging, performance monitoring etc.

Documents
-----------
FaceMatch2UISpec.docx - FM2 Web Interface document
FaceMatchSystemReq.docx - High level, functional requirements for FM2
FM2Overview.pptx - High level system components
FM2Implementation.pptx - FaceMatch2 Web Server Design, Implementation and Test
web.xml - Web interface file used by the FM2WebServer

Configuration files
-----------------
webfm2server.cfg - Sample configuration file used by FM2WebServer. Must be customized at the server facility.
FM2LocalServer.cfg - Sample configuration file used by the FM2 Executive. Must be customized at the server facility.
facematchDBschema.sql - File defining FaceMatch2 database schema and initializing the static tables
FM2InstallationNotes.txt - Instructions for creating/using various files for building the FM2 system

Build and Install
------------------ 
The procedure for building and installing the FM2 system are provided in the document "FM2InstallationNotes.txt"
included in this directory.

It may be noted that the FM2 Web server may also be run under Tomcat on a Windows-based machine with appropriate library support, 
including the OpenCV libraries, and a Facematch library rebuilt on the Windows system. 


Prerequisites
-------------
Apache-tomcat 8.0 or compatible version
MySQL 5.7 or compatible version
CUDA 5.5 or later SDK from NVIDIA
OpenCV 2.4.13 or newer version from opencv.org
NetBeans 8.0 or higher from netbeans.org
Linux-RedHat operating system
Java compiler: Version V7 or highter
