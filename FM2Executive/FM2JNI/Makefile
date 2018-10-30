#
# Informational Notice:
# This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
# an agency of the Department of Health and Human Services, United States Government.
#
# The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.
#
# The license does not supersede any applicable United States law.
#
# The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.
#
# Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
# (FAR) 48 C.F.R. Part52.227-14, Rights in Dataï¿½General.
# The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
# non-commercial products.
#
# LICENSE:
#
# Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
# as provided by Federal law.
#
# Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
# -	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.
#
# -	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
# in the documentation and/or other materials provided with the distribution.
#
# -	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
# of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
# specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
# THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
# OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
# PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
# WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
# EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#-------------------------------------------------------------------

#Make file for FaceMatch 2.0 Java Interface

#-------------------------------------------------------------------
# define FMLIB g++ compilation rules as in common.mk
GCCVERSION := $(shell gcc -dumpversion)

#$(info GCCVERSION = $(GCCVERSION))

GCCVER := $(subst .,0,$(GCCVERSION))
GCCVER := $(shell expr `echo $(GCCVER)` | cut -b1-3)

#$(info GCCVER = $(GCCVER))

PREC11 := $(shell echo $(GCCVER)\<407 | bc)

ifeq ($(PREC11), 1) # pre C++11 compatible
	CXX11 := -std=c++0x -Doverride=
else # assume 4.7 or higher
	CXX11 := -std=c++11 -g
endif

CFLAGS := $(CXX11) `pkg-config opencv --cflags` -DEBUG -fopenmp
LFLAGS := `pkg-config opencv --libs` -fopenmp

#-------------------------------------------------------------------

FMLIB_DIR := <FMLibInstallDir>
BIN_DIR := $(FMLIB_DIR)/bin
COMMON := $(FMLIB_DIR)/common
SHARED_DIR := $(BIN_DIR)
# SHARED_DIR := /opt/fm2/lib/shared             

FM2JNI_PATH := /home/dmisra/FaceMatch2/FM2JNI
#-------------------------------------------------------------------

#---------------- FaceMatch 2.0 Java Interface ---------------------

JNI_SRC := fmservice_server_fminterface_proxy_FaceFinderProxy.cpp \
fmservice_server_fminterface_proxy_FaceRegionMatcherProxy.cpp \
fmservice_server_fminterface_proxy_FRDProxy.cpp \
fmservice_server_fminterface_proxy_InfoProxy.cpp


FM2JNI_SRC := $(addprefix $(FM2JNI_PATH)/src/, $(JNI_SRC))
FM2JNI_OBJ := $(FM2JNI_SRC:.cpp=.o)  

$(FM2JNI_PATH)/%.o: $(FM2JNI_PATH)/%.cpp
	$(CXX) $(CFLAGS)  -I$(FM2JNI_PATH)/include -I$(COMMON) -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -fpic -c $< -o $@

libFaceMatchLibJni.so: $(SHARED_DIR)/libcommon.so $(FM2JNI_OBJ)
	$(CXX) -shared -o $@ $(FM2JNI_OBJ) -L/usr/local/lib -L$(BIN_DIR) -lcommon $(LFLAGS)
	cp libFaceMatchLibJni.so $(SHARED_DIR)
	cp libFaceMatchLibJni.so $(BIN_DIR)
	
#-------------------------------------------------------------------


all: FM2JNI_LIB 
	# done $@: $^


FM2JNI_LIB : libFaceMatchLibJni.so


clean:
	#echo Removing $(FMJNI_LIB), $(FM2JNI_OBJ) 
	rm -vf $(FMJNI_LIB) $(FM2JNI_OBJ)
