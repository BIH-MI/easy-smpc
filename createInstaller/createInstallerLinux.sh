#!/bin/bash
#==================================
#Create an EasySMPC installer for Linux
#Usually only the version variable and the type (RPM or DEB) needs to be adapted
#==================================
VERSION="1.0.0"
TYPE="deb"
BUILD_PATH="../target"
MAIN_JAR="easysmpc.jar"
MAIN_CLASS="org.bihealth.mi.easysmpc.App"
APPLICATION_NAME="easySMPC"
DESCRIPTION_TEXT="Tool to add sums in a secure manner"
VENDOR="Medical Informatics Group@Berlin Institute of Health and University of Technical University of Darmstadt"
COPYRIGHT="Licensed under the Apache License, Version 2.0 (the License); you may not use this file except in compliance with the License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0"
LICENSE_FILE="../LICENSE"
# Todo resize png
# ICONPATH="../src/main/resources/org/bihealth/mi/easysmpc/resources/icon.png"
#add file associations?

PACKAGE_COMMAND="jpackage 	--input $BUILD_PATH\
							--main-jar $MAIN_JAR\
							--main-class $MAIN_CLASS\
							--type $TYPE\
							--dest $BUILD_PATH\
							--name $APPLICATION_NAME\
							--app-version $VERSION\
							--description \"$DESCRIPTION_TEXT\"\
							--vendor \"$VENDOR\"\
							--copyright \"$COPYRIGHT\"\
							--license-file $LICENSE_FILE\
							--linux-menu-group\
							--linux-shortcut\"

eval $PACKAGE_COMMAND