#!/usr/bin/env bash
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

if [ -d "./target" ]; then
  if [ -d "./target/lib" ]; then
     rm -rf "./target/lib"
  fi
  mkdir "target/lib"
else
  mkdir -p "target/lib"
fi

mvn generate-sources

podman build -f src/main/docker/Dockerfile-centos -t checkleak-native-builder .

if [[ $OSTYPE == 'darwin'* ]]; then
  # for some reason the :Z is not working on mac
  podman run --rm -v $PWD/target/lib:/work/target/lib checkleak-native-builder "$@"
else
  podman run --rm -v $PWD/target/lib:/work/target/lib:Z checkleak-native-builder "$@"
fi

# to debug the image
#podman run -it --rm -v $PWD/target/lib:/work/target/lib:Z checkleak-native-builder "$@" bash
chown -Rv $USER:$GID ./target/lib
ls -liat ./target/lib

