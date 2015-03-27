#!/bin/bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

USERNAME=$1

if [ "${USERNAME}" == "" ]; then
  echo "Please provide a SVN username."
  echo -e "\nUsage:\n\t$0 <username>\n"
  exit 1
fi

SVN_CMD="svn --no-auth-cache --username=${USERNAME}"
VERSION=$(cat pom.xml | grep -A1 '<artifactId>tinkerpop</artifactId>' | grep '<version>' | awk -F '>' '{print $2}' | awk -F '<' '{print $1}')

rm -rf target
mkdir -p target/svn
${SVN_CMD} co https://svn.apache.org/repos/asf/incubator/tinkerpop/site/ target/svn

pushd target/svn
${SVN_CMD} rm "docs/${VERSION}"
${SVN_CMD} rm "javadocs/${VERSION}"
${SVN_CMD} commit . -m "Docs for TinkerPop ${VERSION} are being replaced."
popd

docs/preprocessor/preprocess.sh && mvn process-resources -Dasciidoc && rm -rf docs/target
mvn process-resources -Djavadoc

mkdir -p "target/svn/docs/${VERSION}"
mkdir -p "target/svn/javadocs/${VERSION}/core"
mkdir -p "target/svn/javadocs/${VERSION}/full"

cp -R target/docs/htmlsingle/.   "target/svn/docs/${VERSION}"
cp -R target/site/apidocs/core/. "target/svn/javadocs/${VERSION}/core"
cp -R target/site/apidocs/full/. "target/svn/javadocs/${VERSION}/full"

pushd target/svn
${SVN_CMD} add * --force
${SVN_CMD} commit -m "Deploy docs for TinkerPop ${VERSION}"
popd
