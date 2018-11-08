#/*
#* Licensed to the Apache Software Foundation (ASF) under one or more
#* contributor license agreements.  See the NOTICE file distributed with
#* this work for additional information regarding copyright ownership.
#* The ASF licenses this file to You under the Apache License, Version 2.0
#* (the "License"); you may not use this file except in compliance with
#* the License.  You may obtain a copy of the License at
#*
#*     http://www.apache.org/licenses/LICENSE-2.0
#*
#* Unless required by applicable law or agreed to in writing, software
#* distributed under the License is distributed on an "AS IS" BASIS,
#* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#* See the License for the specific language governing permissions and
#* limitations under the License.
#*/

LIBS_ORIG := $(patsubst %/,%, $(dir $(wildcard libs/*/Makefile)))
LIBS_CLEAN := $(addsuffix -clean,$(LIBS_ORIG))

.PHONY: $(LIBS_CLEAN)

all: build-shared

build-libs: $(LIBS_ORIG)

$(LIBS_ORIG):
	$(MAKE) -C $@

build-shared:
	$(MAKE) -C project

test:
	@echo $(LIBS_ORIG)

# clean:$(LIBS_CLEAN)
clean:
	$(MAKE) -C project clean
	$(MAKE) -C bin clean
	$(RM) -rf  logs/*.log
	$(RM) -rf  tmp

cleanall:$(LIBS_CLEAN) clean

install:
	$(MAKE) -C project install

$(LIBS_CLEAN):
	$(MAKE) -C $(@:-clean=) clean
