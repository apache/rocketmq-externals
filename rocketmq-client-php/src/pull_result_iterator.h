/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#ifndef ROCKETMQ_CLIENT_PHP_PULL_RESULT_ITERATOR_H_
#define ROCKETMQ_CLIENT_PHP_PULL_RESULT_ITERATOR_H_

#include <phpcpp.h>
#include "pull_result.h"

class PullResultIterator : public Php::Iterator
{
    private:
        std::vector<rocketmq::MQMessageExt> &_vec;
        std::vector<rocketmq::MQMessageExt>::const_iterator _iter;
    public :
        PullResultIterator(PullResult *obj, std::vector<rocketmq::MQMessageExt> &vec)
            : Php::Iterator(obj), _vec(vec), _iter(vec.begin()) { }

        virtual ~PullResultIterator(){}

        virtual bool valid(){
            return this->_iter != this->_vec.end();
        }

        Php::Value key(){
            return this->_iter - this->_vec.begin();
        }

        void rewind(){
            this-> _iter = this->_vec.begin();
        }

        void next(){
            this->_iter ++;
        }

        Php::Value current(){
            return Php::Object(MESSAGE_EXT_CLASS_NAME, new MessageExt(*_iter));
        }
};

#endif
