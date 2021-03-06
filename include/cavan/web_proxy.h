#pragma once

/*
 * File:		web_proxy.h
 * Author:		Fuang.Cao <cavan.cfa@gmail.com>
 * Created:		2013-10-04 12:45:41
 *
 * Copyright (c) 2013 Fuang.Cao <cavan.cfa@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */

#include <cavan.h>
#include <cavan/service.h>

#define CAVAN_WEB_PROXY_PORT	9090

struct web_proxy_service {
	struct network_service service;
	struct network_url url;
	struct network_url url_proxy;
	size_t proxy_hostlen;
};

char *web_proxy_find_prop(const char *req, const char *req_end, const char *name, size_t namelen);
char *web_proxy_set_prop(char *req, char *req_end, const char *name, size_t namelen, const char *value, size_t valuelen);
ssize_t web_proxy_read_request(struct network_client *client, char *buff, size_t size);

int web_proxy_service_run(struct cavan_dynamic_service *service);
