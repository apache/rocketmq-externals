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
#ifndef _TASK_QUEUE_I_
#define _TASK_QUEUE_I_

#include <boost/atomic.hpp>
#include <boost/thread/mutex.hpp>
#include <list>
#include <vector>
using namespace std;

namespace rocketmq {

//<!***************************************************************************
typedef void (*taskfunc)(void*);

//<! 数据加操作的集合
class ITask_impl {
 public:
  virtual ~ITask_impl() {}
  virtual void run() = 0;
  virtual ITask_impl* fork() = 0;
};

//<!***************************************************************************
class Task_impl : public ITask_impl {
 public:
  Task_impl(taskfunc func, void* arg_) : m_func(func), m_arg(arg_) {}
  virtual ~Task_impl() {
    m_func = 0;
    m_arg = 0;
  }
  virtual void run() {
    if (m_func != 0) m_func(m_arg);
  }
  virtual ITask_impl* fork() { return new Task_impl(m_func, m_arg); }

 protected:
  taskfunc m_func;
  void* m_arg;
};

//<!***************************************************************************
//<! 构造ITask_impl的子类对象时，为其赋予不同的数据和操作即可。
//<! 这里使用了组合的方式实现了接口和实现的分离;
//<!***************************************************************************
struct Task {
  static void dumy(void*) {}

  Task(taskfunc f_, void* d_) { m_pTaskImpl = new Task_impl(f_, d_); }
  Task(ITask_impl* task_imp_) : m_pTaskImpl(task_imp_) {}
  Task(const Task& src_) : m_pTaskImpl(src_.m_pTaskImpl->fork()) {}
  Task() { m_pTaskImpl = new Task_impl(&Task::dumy, 0); }
  virtual ~Task() { delete m_pTaskImpl; }
  Task& operator=(const Task& src_) {
    delete m_pTaskImpl;
    m_pTaskImpl = src_.m_pTaskImpl->fork();
    return *this;
  }
  void run() {
    if (m_pTaskImpl) m_pTaskImpl->run();
  }

 private:
  ITask_impl* m_pTaskImpl;
};

//<!***************************************************************************
class ITaskQueue {
 public:
  typedef list<Task> TaskList;

 public:
  virtual ~ITaskQueue() {}
  virtual void close() = 0;
  virtual void produce(const Task& task) = 0;
  // virtual void multi_produce(const TaskList& tasks) = 0;
  // virtual int  consume(Task& task)                  = 0;
  // virtual int  consume_all(TaskList& tasks)         = 0;
  virtual int run() = 0;
  // virtual int  batch_run()                          = 0;
  virtual bool bTaskQueueStatusOK() = 0;
};

//<!***************************************************************************
//<! 由于不同的操作和数据可能需要构造不同ITask_impl子类，
//<!
//我们需要提供一些泛型函数，能够将用户的所有操作和数据都能轻易的转换成Task对象。
//<! TaskBinder 提供一系列的gen函数，能够转换用户的普通函数和数据为Task对象;
//<!***************************************************************************
struct TaskBinder {
  static Task gen(void (*func)(void*), void* p_) { return Task(func, p_); }

  template <typename RET>
  static Task gen(RET (*func)(void)) {
    struct lambda {
      static void taskfunc(void* p_) { (*(RET(*)(void))p_)(); };
    };
    return Task(lambda::taskfunc, (void*)func);
  }

  template <typename FUNCT, typename ARG1>
  static Task gen(FUNCT func, ARG1 arg1) {
    struct lambda : public ITask_impl {
      FUNCT dest_func;
      ARG1 arg1;
      lambda(FUNCT func, const ARG1& arg1) : dest_func(func), arg1(arg1) {}
      virtual void run() { (*dest_func)(arg1); }
      virtual ITask_impl* fork() { return new lambda(dest_func, arg1); }
    };
    return Task(new lambda(func, arg1));
  }

  template <typename FUNCT, typename ARG1, typename ARG2>
  static Task gen(FUNCT func, ARG1 arg1, ARG2 arg2) {
    struct lambda : public ITask_impl {
      FUNCT dest_func;
      ARG1 arg1;
      ARG2 arg2;
      lambda(FUNCT func, const ARG1& arg1, const ARG2& arg2)
          : dest_func(func), arg1(arg1), arg2(arg2) {}
      virtual void run() { (*dest_func)(arg1, arg2); }
      virtual ITask_impl* fork() { return new lambda(dest_func, arg1, arg2); }
    };
    return Task(new lambda(func, arg1, arg2));
  }

  template <typename FUNCT, typename ARG1, typename ARG2, typename ARG3>
  static Task gen(FUNCT func, ARG1 arg1, ARG2 arg2, ARG3 arg3) {
    struct lambda : public ITask_impl {
      FUNCT dest_func;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      lambda(FUNCT func, const ARG1& arg1, const ARG2& arg2, const ARG3& arg3)
          : dest_func(func), arg1(arg1), arg2(arg2), arg3(arg3) {}
      virtual void run() { (*dest_func)(arg1, arg2, arg3); }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, arg1, arg2, arg3);
      }
    };
    return Task(new lambda(func, arg1, arg2, arg3));
  }

  template <typename FUNCT, typename ARG1, typename ARG2, typename ARG3,
            typename ARG4>
  static Task gen(FUNCT func, ARG1 arg1, ARG2 arg2, ARG3 arg3, ARG4 arg4) {
    struct lambda : public ITask_impl {
      FUNCT dest_func;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      lambda(FUNCT func, const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4)
          : dest_func(func), arg1(arg1), arg2(arg2), arg3(arg3), arg4(arg4) {}
      virtual void run() { (*dest_func)(arg1, arg2, arg3, arg4); }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, arg1, arg2, arg3, arg4);
      }
    };
    return Task(new lambda(func, arg1, arg2, arg3, arg4));
  }

  template <typename FUNCT, typename ARG1, typename ARG2, typename ARG3,
            typename ARG4, typename ARG5>
  static Task gen(FUNCT func, ARG1 arg1, ARG2 arg2, ARG3 arg3, ARG4 arg4,
                  ARG5 arg5) {
    struct lambda : public ITask_impl {
      FUNCT dest_func;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      ARG5 arg5;
      lambda(FUNCT func, const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4, const ARG5& arg5)
          : dest_func(func),
            arg1(arg1),
            arg2(arg2),
            arg3(arg3),
            arg4(arg4),
            arg5(arg5) {}
      virtual void run() { (*dest_func)(arg1, arg2, arg3, arg4, arg5); }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, arg1, arg2, arg3, arg4, arg5);
      }
    };
    return Task(new lambda(func, arg1, arg2, arg3, arg4, arg5));
  }

  template <typename FUNCT, typename ARG1, typename ARG2, typename ARG3,
            typename ARG4, typename ARG5, typename ARG6>
  static Task gen(FUNCT func, ARG1 arg1, ARG2 arg2, ARG3 arg3, ARG4 arg4,
                  ARG5 arg5, ARG6 arg6) {
    struct lambda : public ITask_impl {
      FUNCT dest_func;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      ARG5 arg5;
      ARG6 arg6;
      lambda(FUNCT func, const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4, const ARG5& arg5, const ARG6& arg6)
          : dest_func(func),
            arg1(arg1),
            arg2(arg2),
            arg3(arg3),
            arg4(arg4),
            arg5(arg5),
            arg6(arg6) {}
      virtual void run() { (*dest_func)(arg1, arg2, arg3, arg4, arg5, arg6); }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, arg1, arg2, arg3, arg4, arg5, arg6);
      }
    };
    return Task(new lambda(func, arg1, arg2, arg3, arg4, arg5, arg6));
  }

  template <typename FUNCT, typename ARG1, typename ARG2, typename ARG3,
            typename ARG4, typename ARG5, typename ARG6, typename ARG7>
  static Task gen(FUNCT func, ARG1 arg1, ARG2 arg2, ARG3 arg3, ARG4 arg4,
                  ARG5 arg5, ARG6 arg6, ARG7 arg7) {
    struct lambda : public ITask_impl {
      FUNCT dest_func;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      ARG5 arg5;
      ARG6 arg6;
      ARG7 arg7;
      lambda(FUNCT func, const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4, const ARG5& arg5, const ARG6& arg6,
             const ARG7& arg7)
          : dest_func(func),
            arg1(arg1),
            arg2(arg2),
            arg3(arg3),
            arg4(arg4),
            arg5(arg5),
            arg6(arg6),
            arg7(arg7) {}
      virtual void run() {
        (*dest_func)(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      }
    };
    return Task(new lambda(func, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
  }

  template <typename FUNCT, typename ARG1, typename ARG2, typename ARG3,
            typename ARG4, typename ARG5, typename ARG6, typename ARG7,
            typename ARG8>
  static Task gen(FUNCT func, ARG1 arg1, ARG2 arg2, ARG3 arg3, ARG4 arg4,
                  ARG5 arg5, ARG6 arg6, ARG7 arg7, ARG8 arg8) {
    struct lambda : public ITask_impl {
      FUNCT dest_func;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      ARG5 arg5;
      ARG6 arg6;
      ARG7 arg7;
      ARG8 arg8;
      lambda(FUNCT func, const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4, const ARG5& arg5, const ARG6& arg6,
             const ARG7& arg7, const ARG8& arg8)
          : dest_func(func),
            arg1(arg1),
            arg2(arg2),
            arg3(arg3),
            arg4(arg4),
            arg5(arg5),
            arg6(arg6),
            arg7(arg7),
            arg8(arg8) {}
      virtual void run() {
        (*dest_func)(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                          arg8);
      }
    };
    return Task(
        new lambda(func, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8));
  }

  template <typename FUNCT, typename ARG1, typename ARG2, typename ARG3,
            typename ARG4, typename ARG5, typename ARG6, typename ARG7,
            typename ARG8, typename ARG9>
  static Task gen(FUNCT func, ARG1 arg1, ARG2 arg2, ARG3 arg3, ARG4 arg4,
                  ARG5 arg5, ARG6 arg6, ARG7 arg7, ARG8 arg8, ARG9 arg9) {
    struct lambda : public ITask_impl {
      FUNCT dest_func;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      ARG5 arg5;
      ARG6 arg6;
      ARG7 arg7;
      ARG8 arg8;
      ARG9 arg9;
      lambda(FUNCT func, const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4, const ARG5& arg5, const ARG6& arg6,
             const ARG7& arg7, const ARG8& arg8, const ARG9& arg9)
          : dest_func(func),
            arg1(arg1),
            arg2(arg2),
            arg3(arg3),
            arg4(arg4),
            arg5(arg5),
            arg6(arg6),
            arg7(arg7),
            arg8(arg8),
            arg9(arg9) {}
      virtual void run() {
        (*dest_func)(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
      }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                          arg8, arg9);
      }
    };
    return Task(
        new lambda(func, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9));
  }

  //<!***************************************************************************
  //<! class fuctions;;
  //<!***************************************************************************
  template <typename T, typename RET>
  static Task gen(RET (T::*func)(void), T* obj) {
    struct lambda : public ITask_impl {
      RET (T::*dest_func)(void);
      T* obj;
      lambda(RET (T::*func)(void), T* obj) : dest_func(func), obj(obj) {}
      virtual void run() { (obj->*dest_func)(); }
      virtual ITask_impl* fork() { return new lambda(dest_func, obj); }
    };
    return Task(new lambda(func, obj));
  }

  template <typename T, typename RET, typename FARG1, typename ARG1>
  static Task gen(RET (T::*func)(FARG1), T* obj, ARG1 arg1) {
    struct lambda : public ITask_impl {
      RET (T::*dest_func)(FARG1);
      T* obj;
      ARG1 arg1;
      lambda(RET (T::*pfunc)(FARG1), T* obj, const ARG1& arg1)
          : dest_func(pfunc), obj(obj), arg1(arg1) {}
      virtual void run() { (obj->*dest_func)(arg1); }
      virtual ITask_impl* fork() { return new lambda(dest_func, obj, arg1); }
    };
    return Task(new lambda(func, obj, arg1));
  }

  template <typename T, typename RET, typename FARG1, typename FARG2,
            typename ARG1, typename ARG2>
  static Task gen(RET (T::*func)(FARG1, FARG2), T* obj, ARG1 arg1, ARG2 arg2) {
    struct lambda : public ITask_impl {
      RET (T::*dest_func)(FARG1, FARG2);
      T* obj;
      ARG1 arg1;
      ARG2 arg2;
      lambda(RET (T::*func)(FARG1, FARG2), T* obj, const ARG1& arg1,
             const ARG2& arg2)
          : dest_func(func), obj(obj), arg1(arg1), arg2(arg2) {}
      virtual void run() { (obj->*dest_func)(arg1, arg2); }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, obj, arg1, arg2);
      }
    };
    return Task(new lambda(func, obj, arg1, arg2));
  }

  template <typename T, typename RET, typename FARG1, typename FARG2,
            typename FARG3, typename ARG1, typename ARG2, typename ARG3>
  static Task gen(RET (T::*func)(FARG1, FARG2, FARG3), T* obj, ARG1 arg1,
                  ARG2 arg2, ARG3 arg3) {
    struct lambda : public ITask_impl {
      RET (T::*dest_func)(FARG1, FARG2, FARG3);
      T* obj;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      lambda(RET (T::*func)(FARG1, FARG2, FARG3), T* obj, const ARG1& arg1,
             const ARG2& arg2, const ARG3& arg3)
          : dest_func(func), obj(obj), arg1(arg1), arg2(arg2), arg3(arg3) {}
      virtual void run() { (obj->*dest_func)(arg1, arg2, arg3); }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, obj, arg1, arg2, arg3);
      }
    };
    return Task(new lambda(func, obj, arg1, arg2, arg3));
  }

  template <typename T, typename RET, typename FARG1, typename FARG2,
            typename FARG3, typename FARG4, typename ARG1, typename ARG2,
            typename ARG3, typename ARG4>
  static Task gen(RET (T::*func)(FARG1, FARG2, FARG3, FARG4), T* obj, ARG1 arg1,
                  ARG2 arg2, ARG3 arg3, ARG4 arg4) {
    struct lambda : public ITask_impl {
      RET (T::*dest_func)(FARG1, FARG2, FARG3, FARG4);
      T* obj;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      lambda(RET (T::*func)(FARG1, FARG2, FARG3, FARG4), T* obj,
             const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4)
          : dest_func(func),
            obj(obj),
            arg1(arg1),
            arg2(arg2),
            arg3(arg3),
            arg4(arg4) {}
      virtual void run() { (obj->*dest_func)(arg1, arg2, arg3, arg4); }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, obj, arg1, arg2, arg3, arg4);
      }
    };
    return Task(new lambda(func, obj, arg1, arg2, arg3, arg4));
  }

  template <typename T, typename RET, typename FARG1, typename FARG2,
            typename FARG3, typename FARG4, typename FARG5, typename ARG1,
            typename ARG2, typename ARG3, typename ARG4, typename ARG5>
  static Task gen(RET (T::*func)(FARG1, FARG2, FARG3, FARG4, FARG5), T* obj,
                  ARG1 arg1, ARG2 arg2, ARG3 arg3, ARG4 arg4, ARG5 arg5) {
    struct lambda : public ITask_impl {
      RET (T::*dest_func)(FARG1, FARG2, FARG3, FARG4, FARG5);
      T* obj;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      ARG5 arg5;
      lambda(RET (T::*func)(FARG1, FARG2, FARG3, FARG4, FARG5), T* obj,
             const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4, const ARG5& arg5)
          : dest_func(func),
            obj(obj),
            arg1(arg1),
            arg2(arg2),
            arg3(arg3),
            arg4(arg4),
            arg5(arg5) {}
      virtual void run() { (obj->*dest_func)(arg1, arg2, arg3, arg4, arg5); }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, obj, arg1, arg2, arg3, arg4, arg5);
      }
    };
    return Task(new lambda(func, obj, arg1, arg2, arg3, arg4, arg5));
  }

  template <typename T, typename RET, typename FARG1, typename FARG2,
            typename FARG3, typename FARG4, typename FARG5, typename FARG6,
            typename ARG1, typename ARG2, typename ARG3, typename ARG4,
            typename ARG5, typename ARG6>
  static Task gen(RET (T::*func)(FARG1, FARG2, FARG3, FARG4, FARG5, FARG6),
                  T* obj, ARG1 arg1, ARG2 arg2, ARG3 arg3, ARG4 arg4, ARG5 arg5,
                  ARG6 arg6) {
    struct lambda : public ITask_impl {
      RET (T::*dest_func)(FARG1, FARG2, FARG3, FARG4, FARG5, FARG6);
      T* obj;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      ARG5 arg5;
      ARG6 arg6;
      lambda(RET (T::*func)(FARG1, FARG2, FARG3, FARG4, FARG5, FARG6), T* obj,
             const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4, const ARG5& arg5, const ARG6& arg6)
          : dest_func(func),
            obj(obj),
            arg1(arg1),
            arg2(arg2),
            arg3(arg3),
            arg4(arg4),
            arg5(arg5),
            arg6(arg6) {}
      virtual void run() {
        (obj->*dest_func)(arg1, arg2, arg3, arg4, arg5, arg6);
      }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, obj, arg1, arg2, arg3, arg4, arg5, arg6);
      }
    };
    return Task(new lambda(func, obj, arg1, arg2, arg3, arg4, arg5, arg6));
  }

  template <typename T, typename RET, typename FARG1, typename FARG2,
            typename FARG3, typename FARG4, typename FARG5, typename FARG6,
            typename FARG7, typename ARG1, typename ARG2, typename ARG3,
            typename ARG4, typename ARG5, typename ARG6, typename ARG7>
  static Task gen(RET (T::*func)(FARG1, FARG2, FARG3, FARG4, FARG5, FARG6,
                                 FARG7),
                  T* obj, ARG1 arg1, ARG2 arg2, ARG3 arg3, ARG4 arg4, ARG5 arg5,
                  ARG6 arg6, ARG7 arg7) {
    struct lambda : public ITask_impl {
      RET (T::*dest_func)(FARG1, FARG2, FARG3, FARG4, FARG5, FARG6, FARG7);
      T* obj;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      ARG5 arg5;
      ARG6 arg6;
      ARG7 arg7;
      lambda(RET (T::*func)(FARG1, FARG2, FARG3, FARG4, FARG5, FARG6, FARG7),
             T* obj, const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4, const ARG5& arg5, const ARG6& arg6,
             const ARG7& arg7)
          : dest_func(func),
            obj(obj),
            arg1(arg1),
            arg2(arg2),
            arg3(arg3),
            arg4(arg4),
            arg5(arg5),
            arg6(arg6),
            arg7(arg7) {}
      virtual void run() {
        (obj->*dest_func)(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
      }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, obj, arg1, arg2, arg3, arg4, arg5, arg6,
                          arg7);
      }
    };
    return Task(
        new lambda(func, obj, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
  }

  template <typename T, typename RET, typename FARG1, typename FARG2,
            typename FARG3, typename FARG4, typename FARG5, typename FARG6,
            typename FARG7, typename FARG8, typename ARG1, typename ARG2,
            typename ARG3, typename ARG4, typename ARG5, typename ARG6,
            typename ARG7, typename ARG8>
  static Task gen(RET (T::*func)(FARG1, FARG2, FARG3, FARG4, FARG5, FARG6,
                                 FARG7, FARG8),
                  T* obj, ARG1 arg1, ARG2 arg2, ARG3 arg3, ARG4 arg4, ARG5 arg5,
                  ARG6 arg6, ARG7 arg7, ARG8 arg8) {
    struct lambda : public ITask_impl {
      RET (T::*dest_func)
      (FARG1, FARG2, FARG3, FARG4, FARG5, FARG6, FARG7, FARG8);
      T* obj;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      ARG5 arg5;
      ARG6 arg6;
      ARG7 arg7;
      ARG8 arg8;
      lambda(RET (T::*func)(FARG1, FARG2, FARG3, FARG4, FARG5, FARG6, FARG7,
                            FARG8),
             T* obj, const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4, const ARG5& arg5, const ARG6& arg6,
             const ARG7& arg7, const ARG8& arg8)
          : dest_func(func),
            obj(obj),
            arg1(arg1),
            arg2(arg2),
            arg3(arg3),
            arg4(arg4),
            arg5(arg5),
            arg6(arg6),
            arg7(arg7),
            arg8(arg8) {}
      virtual void run() {
        (obj->*dest_func)(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
      }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, obj, arg1, arg2, arg3, arg4, arg5, arg6,
                          arg7, arg8);
      }
    };
    return Task(
        new lambda(func, obj, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8));
  }

  template <typename T, typename RET, typename FARG1, typename FARG2,
            typename FARG3, typename FARG4, typename FARG5, typename FARG6,
            typename FARG7, typename FARG8, typename FARG9, typename ARG1,
            typename ARG2, typename ARG3, typename ARG4, typename ARG5,
            typename ARG6, typename ARG7, typename ARG8, typename ARG9>
  static Task gen(RET (T::*func)(FARG1, FARG2, FARG3, FARG4, FARG5, FARG6,
                                 FARG7, FARG8, FARG9),
                  T* obj, ARG1 arg1, ARG2 arg2, ARG3 arg3, ARG4 arg4, ARG5 arg5,
                  ARG6 arg6, ARG7 arg7, ARG8 arg8, ARG9 arg9) {
    struct lambda : public ITask_impl {
      RET (T::*dest_func)
      (FARG1, FARG2, FARG3, FARG4, FARG5, FARG6, FARG7, FARG8, FARG9);
      T* obj;
      ARG1 arg1;
      ARG2 arg2;
      ARG3 arg3;
      ARG4 arg4;
      ARG5 arg5;
      ARG6 arg6;
      ARG7 arg7;
      ARG8 arg8;
      ARG9 arg9;
      lambda(RET (T::*func)(FARG1, FARG2, FARG3, FARG4, FARG5, FARG6, FARG7,
                            FARG8, FARG9),
             T* obj, const ARG1& arg1, const ARG2& arg2, const ARG3& arg3,
             const ARG4& arg4, const ARG5& arg5, const ARG6& arg6,
             const ARG7& arg7, const ARG8& arg8, const ARG9& arg9)
          : dest_func(func),
            obj(obj),
            arg1(arg1),
            arg2(arg2),
            arg3(arg3),
            arg4(arg4),
            arg5(arg5),
            arg6(arg6),
            arg7(arg7),
            arg8(arg8),
            arg9(arg9) {}
      virtual void run() {
        (obj->*dest_func)(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
      }
      virtual ITask_impl* fork() {
        return new lambda(dest_func, obj, arg1, arg2, arg3, arg4, arg5, arg6,
                          arg7, arg8, arg9);
      }
    };
    return Task(new lambda(func, obj, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                           arg8, arg9));
  }
};

//<!***************************************************************************
class disruptorLFQ;
class TaskQueue : public ITaskQueue {
 public:
  TaskQueue(int threadCount);
  virtual ~TaskQueue();
  virtual void close();
  virtual void produce(const Task& task);
  virtual int run();
  virtual bool bTaskQueueStatusOK();

 private:
  boost::atomic<bool> m_flag;
  disruptorLFQ* m_disruptorLFQ;
  boost::mutex m_publishLock;
};

//<!***************************************************************************
}  //<! end namespace;

#endif
