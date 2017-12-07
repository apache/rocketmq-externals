/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.redis.replicator.rdb.module;

public class ModuleKey {
    private final String moduleName;
    private final int moduleVersion;

    private ModuleKey(String moduleName, int moduleVersion) {
        this.moduleName = moduleName;
        this.moduleVersion = moduleVersion;
    }

    public static ModuleKey key(String moduleName, int moduleVersion) {
        return new ModuleKey(moduleName, moduleVersion);
    }

    public String getModuleName() {
        return moduleName;
    }

    public int getModuleVersion() {
        return moduleVersion;
    }

    @Override
    public String toString() {
        return "ModuleKey{" +
            "moduleName='" + moduleName + '\'' +
            ", moduleVersion=" + moduleVersion +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ModuleKey moduleKey = (ModuleKey) o;

        if (moduleVersion != moduleKey.moduleVersion)
            return false;
        return moduleName.equals(moduleKey.moduleName);
    }

    @Override
    public int hashCode() {
        int result = moduleName.hashCode();
        result = 31 * result + moduleVersion;
        return result;
    }
}
