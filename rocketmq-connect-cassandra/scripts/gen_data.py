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


from sqlalchemy import *
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, String
from sqlalchemy.orm import sessionmaker
import argparse
import random
import string
import datetime
import time
import uuid



Base = declarative_base()
class JdbcCassandra(Base):
    __tablename__ = 'jdbc_cassandra'
    int_type = Column(INT, primary_key=True, autoincrement=True)
    ascii_type = Column(VARCHAR(50))
    boolean_type = Column(BOOLEAN)
    date_type = Column(DATE)
    decimal_type = Column(DECIMAL)
    double_type = Column(FLOAT)
    float_type = Column(FLOAT)
    inet_type = Column(VARCHAR(50))
    smallint_type = Column(SMALLINT)
    time_type  = Column(TIME)
    text_type = Column(TEXT)
    timestamp_type  = Column(TIMESTAMP)
    #timeuuid_type  = Column(CHAR(36))
    tinyint_type  = Column(SMALLINT)
    #uuid_type  = Column(CHAR(36))
    varchar_type  = Column(VARCHAR(50))
    varint_type  = Column(BIGINT)



def random_string(strlen=8):
    letters = string.ascii_lowercase
    return bytes(''.join(random.choice(letters) for i in range(strlen)), 'utf8') 


def str_time_prop(start, end, format, prop):
    """Get a time at a proportion of a range of two formatted times.

    start and end should be strings specifying times formated in the
    given format (strftime-style), giving an interval [start, end].
    prop specifies how a proportion of the interval to be taken after
    start.  The returned time will be in the specified format.
    """

    stime = time.mktime(time.strptime(start, format))
    etime = time.mktime(time.strptime(end, format))

    ptime = stime + prop * (etime - stime)

    return time.strftime(format, time.localtime(ptime))


def random_date(start, end, prop):
    return bytes(str_time_prop(start, end, '%m/%d/%Y %I:%M %p', prop), 'utf8')

def random_time(start, end, prop):
    return bytes(str_time_prop(start, end, '%Y-%m-%d %H:%M:%S', prop), 'utf8')

def format_time():
    t = datetime.datetime.now()
    s = t.strftime('%Y-%m-%d %H:%M:%S.%f')
    return s[:-3]

def main():
    
    # define parser
    parser = argparse.ArgumentParser()
    parser.add_argument('hostname', metavar='HOSTNAME', type=str,
                        help='hostname of target mysql database ')
    parser.add_argument('port', metavar='PORT', type=str,
                        help='port of target mysql database')
    parser.add_argument('username', metavar='USERNAME', type=str,
                        help='username of the user connecting to mysql')
    parser.add_argument('password', metavar='PASSWORD', type=str,
                        help='password of specified user')   
    parser.add_argument('database', metavar='DATABASE', type=str,
                        help='which database to connect to') 
    parser.add_argument('count', metavar='COUNT', type=int,
                        help='how many random records to insert into the database')                                                           
    args = parser.parse_args()


    # get variabless from command line
    hostname = args.hostname
    port = args.port
    username = args.username
    password = args.password
    database = args.database
    count = args.count


    # create db connection
    # print("----------------ERROR-------------")
    # print("mysql+pymysql://{}:{}@{}:{}/{}".format(username, password, hostname, port, database))
    engine = create_engine("mysql+pymysql://{}:{}@{}:{}/{}".format(username, password, hostname, port, database), echo=True)


    # create table if not exist
    Base.metadata.create_all(engine)    

    # create a session
    Session = sessionmaker(bind=engine)
    session = Session()

    for i in range(0, count):
        random_record = JdbcCassandra(
            ascii_type = random_string(30),
            boolean_type = (i % 2 == 0),
            date_type = datetime.datetime.now().date(),
            decimal_type = 10.5,
            double_type = 1.5,
            float_type = 8.3,
            inet_type = "127.0.0.1",
            smallint_type = 1,
            time_type = datetime.datetime.now().time(),
            text_type = random_string(30),
            timestamp_type = datetime.datetime.now(),
            #timeuuid_type = str(uuid.uuid1()),
            tinyint_type = 1,
            #uuid_type = str(uuid.uuid1()),
            varchar_type = random_string(30),
            varint_type = random.getrandbits(63),
        )
        session.add(random_record)

    session.commit()


if __name__ == "__main__":
    main()