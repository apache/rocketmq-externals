#include "RocketMQClient.h"
#include "AtomicValue.h"
#include "FileUtil.h"

volatile int RocketMQUtil::_logFd = -1;
int RocketMQUtil::_logLevel = 0;
std::string RocketMQUtil::_logPath = "";

pid_t RocketMQUtil::getPid()
{
    static __thread pid_t pid = 0;
    if (!pid)
    {
        pid = getpid();
    }
    return pid;
}

pid_t RocketMQUtil::getTid()
{
    static __thread pid_t pid = 0;
    static __thread pid_t tid = 0;
    if (!pid || !tid || pid != getpid())
    {
        pid = getpid();
        tid = syscall(__NR_gettid);
    }
    return tid;
}

int RocketMQUtil::getDiffDays(time_t tmFirst, time_t tmSecond)
{
    static struct timeb g_tb;
    static bool g_tbInit = false;

    if(!g_tbInit)
    {
        ftime(&g_tb);
        g_tbInit = true;
    }

    return (tmSecond - g_tb.timezone*60)/86400 - (tmFirst - g_tb.timezone*60)/86400;
};


std::string RocketMQUtil::tm2str(const time_t& t, const std::string& sFormat)
{
    struct tm stTm;
    localtime_r(&t, &stTm);

    char sTimeString[255] = "\0";
    strftime(sTimeString, sizeof(sTimeString), sFormat.c_str(), &stTm);

    return std::string(sTimeString);
}

std::string RocketMQUtil::now2str(const std::string& sFormat)
{
    time_t t = time(NULL);
    return tm2str(t, sFormat.c_str());
}

std::string RocketMQUtil::now2str()
{
    return now2str("%Y-%m-%d %H:%M:%S");
}

int64_t RocketMQUtil::getNowMs()
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * (int64_t)1000 + tv.tv_usec / 1000;
}


std::string RocketMQUtil::str2fmt(const char* format, ...)
{
    int dataLen = 0;
    va_list args;
    char buffer[8092];
    buffer[0] = buffer[sizeof(buffer) - 1] = '\0';

    va_start( args, format );
    dataLen = ::vsnprintf(buffer, sizeof(buffer), format, args);
    va_end(args);

    return std::string(buffer);
};


int RocketMQUtil::initLog(const std::string& sLogPath)
{
	if (sLogPath.empty())
	{
		return 0;
	}

	const char *pLogLevel = getenv("ROCKETMQ_LOGLEVEL");
	if (pLogLevel != NULL)
	{
		int logLevel = atoi(pLogLevel);
    	_logLevel = logLevel;
		_logPath = sLogPath;
	}
	else
	{
		_logLevel = WARN_LOG;
		_logPath = sLogPath;
	}

	std::string logDir = kpr::FileUtil::extractFilePath(_logPath);
	if (!kpr::FileUtil::isFileExist(logDir, S_IFDIR))
	{
		kpr::FileUtil::makeDirRecursive(logDir);
	}

    return 0;
}

void RocketMQUtil::setLogLevel(int logLevel)
{
	_logLevel = logLevel;
}


void RocketMQUtil::writeLog(const char* fmt, ...)
{
    if (_logPath.empty())
    {
        return;
    }

    static volatile time_t last_time = 0;
	static std::string last_time_str = "";
	time_t old = last_time;
	time_t now = time(NULL);

	if (now - last_time >= 5)
	{
		if (__sync_bool_compare_and_swap(&last_time, old, now))
		{
			std::string time_str = tm2str(now, "%Y%m%d");
	    	if (_logFd < 0 || time_str != last_time_str)
	    	{
	    		int oldFd = _logFd;
				std::string logFullPath = _logPath + "." + time_str;
	    		_logFd = open(logFullPath.c_str(), O_CREAT | O_RDWR | O_APPEND, 0666);
	    		if (_logFd > 0)
	    		{
					last_time_str = time_str;
				}

				if (oldFd > 0)
	    		{
	    			close(oldFd);
	    		}
	    	}
		}
	}

    char buf[1024*128];
    buf[0] = buf[sizeof(buf) - 1] = '\0';

    va_list ap;
    va_start(ap, fmt);
    int size = vsnprintf(buf, sizeof(buf), fmt, ap);
    va_end(ap);

	int logFd = _logFd;
    if (logFd > 0 && (size > 0 && size < (int)sizeof(buf)))
    {
        int ret = write(logFd, buf, size);
        if (ret < 0)
        {
        	if (errno == EBADF)
        	{
        		write(_logFd, buf, size);
        	}
        }
    }

    return;
}



