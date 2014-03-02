#!/bin/bash

set -e

curdir="$(pwd)"

cd /usr/share/tomcat-6
cp "$(find -name el-api.jar 2>> /dev/null | head -1)" "$curdir"/setup/i2p-browser/i2p-patch/apps/jetty/apache-tomcat-deployer/lib/
cp "$(find -name jasper-el.jar 2>> /dev/null | head -1)" "$curdir"/setup/i2p-browser/i2p-patch/apps/jetty/apache-tomcat-deployer/lib/
cp "$(find -name jasper.jar 2>> /dev/null | head -1)" "$curdir"/setup/i2p-browser/i2p-patch/apps/jetty/apache-tomcat-deployer/lib/
cp "$(find -name jsp-api.jar 2>> /dev/null | head -1)" "$curdir"/setup/i2p-browser/i2p-patch/apps/jetty/apache-tomcat-deployer/lib/
cp "$(find -name servlet-api.jar 2>> /dev/null | head -1)" "$curdir"/setup/i2p-browser/i2p-patch/apps/jetty/apache-tomcat-deployer/lib/
cp "$(find -name tomcat-juli.jar 2>> /dev/null | head -1)" "$curdir"/setup/i2p-browser/i2p-patch/apps/jetty/apache-tomcat-deployer/lib/

cd "$curdir"

cp -r setup ../
