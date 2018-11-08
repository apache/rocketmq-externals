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

define BUILD_LIBRARY
$(if $(wildcard $@),@$(RM) $@)
$(if $(wildcard ar.mac),@$(RM) ar.mac)
$(if $(filter %.a, $^),
@echo CREATE $@ > ar.mac
@echo SAVE >> ar.mac
@echo END >> ar.mac
@$(AR) -M < ar.mac
)
$(if $(filter %.o,$^),@$(AR) -q $@ $(filter %.o, $^))
$(if $(filter %.a, $^),
@echo OPEN $@ > ar.mac
$(foreach LIB, $(filter %.a, $^),
@echo ADDLIB $(LIB) >> ar.mac
)
@echo SAVE >> ar.mac
@echo END >> ar.mac
@$(AR) -M < ar.mac
@$(RM) ar.mac
)
endef
