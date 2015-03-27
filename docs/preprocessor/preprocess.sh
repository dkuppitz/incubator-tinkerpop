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

pushd "$(dirname $0)/../.." > /dev/null

if [ ! -f bin/gremlin.sh ]; then
  echo "Gremlin REPL is not available. Cannot preprocess AsciiDoc files."
  popd > /dev/null
  exit 1
fi

mkdir -p docs/target

for input in $(find docs/src/ -name "*.asciidoc")
do
  name=`basename $input`
  output="docs/target/${name}"
  echo "${input} > ${output}"
  if [ $(grep -c '^\[gremlin' $input) -gt 0 ]; then
    bin/gremlin.sh -e docs/preprocessor/processor.groovy $input > $output
    ec=$?
    if [ $ec -ne 0 ]; then
      popd > /dev/null
      exit $ec
    fi
  else
    cp $input $output
  fi
done

popd > /dev/null
