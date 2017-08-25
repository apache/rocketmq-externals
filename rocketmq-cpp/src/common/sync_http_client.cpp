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
#include <boost/asio.hpp>
#include <boost/bind.hpp>
#include <boost/lambda/bind.hpp>
#include <boost/lambda/lambda.hpp>
#include <iostream>
#include <istream>
#include <ostream>
#include <string>

#include "Logging.h"
#include "url.h"

using boost::lambda::var;
using boost::asio::ip::tcp;
using boost::asio::deadline_timer;

namespace {
void check_deadline(deadline_timer* deadline, tcp::socket* socket,
                    const boost::system::error_code& ec) {
  // Check whether the deadline has passed. We compare the deadline against
  // the current time since a new asynchronous operation may have moved the
  // deadline before this actor had a chance to run.
  if (deadline->expires_at() <= deadline_timer::traits_type::now()) {
    // The deadline has passed. The socket is closed so that any outstanding
    // asynchronous operations are cancelled. This allows the blocked
    // connect(), read_line() or write_line() functions to return.
    boost::system::error_code ignored_ec;
    socket->close(ignored_ec);

    // There is no longer an active deadline. The expiry is set to positive
    // infinity so that the actor takes no action until a new deadline is set.
    deadline->expires_at(boost::posix_time::pos_infin);
  }

  // Put the actor back to sleep.
  deadline->async_wait(boost::bind(&check_deadline, deadline, socket,
                                   boost::asio::placeholders::error));
}
}  // namespace

namespace rocketmq {
bool SyncfetchNsAddr(const Url& url_s, std::string& body) {
  bool ret = true;
  try {
    boost::asio::io_service io_service;
    // Get a list of endpoints corresponding to the server name.
    tcp::resolver resolver(io_service);
    tcp::resolver::query query(url_s.host_, url_s.port_);
    tcp::resolver::iterator endpoint_iterator = resolver.resolve(query);
    boost::system::error_code ec = boost::asio::error::would_block;
    deadline_timer deadline(io_service);
    // TODO hardcode
    boost::posix_time::seconds timeout(3);
    deadline.expires_from_now(timeout);
    // Try each endpoint until we successfully establish a connection.
    tcp::socket socket(io_service);
    boost::system::error_code deadline_ec;
    check_deadline(&deadline, &socket, deadline_ec);

    boost::asio::async_connect(socket, endpoint_iterator,
                               boost::lambda::var(ec) = boost::lambda::_1);

    do {
      io_service.run_one();
    } while (ec == boost::asio::error::would_block);

    if (ec || !socket.is_open()) {
      LOG_ERROR("socket connect failure, connect timeout or connect failure");
      return false;
    }

    // Form the request. We specify the "Connection: close" header so that the
    // server will close the socket after transmitting the response. This will
    // allow us to treat all data up until the EOF as the content.
    boost::asio::streambuf request;
    std::ostream request_stream(&request);
    request_stream << "GET " << url_s.path_ << " HTTP/1.0\r\n";
    request_stream << "Host: " << url_s.host_ << "\r\n";
    request_stream << "Accept: */*\r\n";
    request_stream << "Connection: close\r\n\r\n";

    // Send the request.
    boost::asio::write(socket, request);

    // Read the response status line. The response streambuf will automatically
    // grow to accommodate the entire line. The growth may be limited by passing
    // a maximum size to the streambuf constructor.
    boost::asio::streambuf response;
    boost::asio::read_until(socket, response, "\r\n");

    // Check that response is OK.
    std::istream response_stream(&response);
    std::string http_version;
    response_stream >> http_version;
    unsigned int status_code;
    response_stream >> status_code;
    std::string status_message;
    std::getline(response_stream, status_message);
    if (!response_stream || http_version.substr(0, 5) != "HTTP/") {
      LOG_INFO("Invalid response %s\n", status_message.c_str());
      return false;
    }

    if (status_code != 200) {
      LOG_INFO("Response returned with status code %d ", status_code);
      return false;
    }

    // Read the response headers, which are terminated by a blank line.
    boost::asio::read_until(socket, response, "\r\n\r\n");

    // Process the response headers.
    std::string header;
    while (std::getline(response_stream, header) && header != "\r")
      ;

    // Write whatever content we already have to output.
    if (response.size() > 0) {
      boost::asio::streambuf::const_buffers_type cbt = response.data();
      body.clear();
      body.insert(body.begin(), boost::asio::buffers_begin(cbt),
                  boost::asio::buffers_end(cbt));
    }

    // Read until EOF, writing data to output as we go.
    boost::system::error_code error;
    while (boost::asio::read(socket, response,
                             boost::asio::transfer_at_least(1), error))
      std::cout << &response;
    if (error != boost::asio::error::eof)
      throw boost::system::system_error(error);

  } catch (std::exception& e) {
    LOG_ERROR("Exception:  %s", e.what());
    ret = false;
  }

  return ret;
}
}  // end of namespace ons
