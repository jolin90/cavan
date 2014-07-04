#pragma once

/*
 * Author: Fuang.Cao
 * Email: cavan.cfa@gmail.com
 * Date: Sat Jan 14 12:59:51 CST 2012
 */

#include <cavan.h>
#include <cavan/network.h>
#include <cavan/service.h>
#include <cavan/alarm.h>

#define TCP_DD_DEFAULT_PORT		8888
#define TCP_DD_DEFAULT_SOCKET	CAVAN_NETWORK_TEMP_PATH "/socket-service"
#define TCP_DD_DAEMON_COUNT		10
#define TCP_DD_TIMEOUT			5000

enum tcp_dd_package_type
{
	TCP_DD_WRITE,
	TCP_DD_READ,
	TCP_DD_EXEC,
	TCP_ALARM_ADD,
	TCP_ALARM_REMOVE,
	TCP_ALARM_LIST,
	TCP_DD_RESPONSE,
	TCP_DD_PACKAGE_COUNT
};

struct tcp_dd_response_package
{
	u32 code;
	u32 number;
	char message[1024];
};

struct tcp_dd_file_request
{
	u32 offset;
	u32 size;
	u32 mode;
	char filename[1024];
};

struct tcp_dd_exec_request
{
	u16 lines;
	u16 columns;
	char command[1024];
};

struct tcp_alarm_add_request
{
	u32 time;
	u32 repeat;
	char command[1024];
};

struct tcp_alarm_query_request
{
	int index;
};

struct tcp_dd_package
{
	u32 type;

	union
	{
		struct tcp_dd_response_package res_pkg;
		struct tcp_dd_file_request file_req;
		struct tcp_dd_exec_request exec_req;
		struct tcp_alarm_add_request alarm_add;
		struct tcp_alarm_query_request alarm_query;
	};
};

struct cavan_tcp_dd_service
{
	u16 port;
	const char *url;
	const char *sun_path;
	network_connect_type_t type;
	struct network_service service;
	struct cavan_alarm_thread alarm;
};

int tcp_dd_service_run(struct cavan_dynamic_service *service);
int tcp_dd_send_file(struct network_file_request *file_req, const char *url);
int tcp_dd_receive_file(struct network_file_request *file_req, const char *url);
int tcp_dd_exec_command(const char *command, const char *url);

int tcp_alarm_add(const char *url, const char *command, time_t time, time_t repeat);
int tcp_alarm_remove(const char *url, int index);
int tcp_alarm_list(const char *url, int index);

int tcp_dd_pipe_open_connect(const char *hostname, u16 port);
void tcp_dd_pipe_close_connect(int sockfd);
