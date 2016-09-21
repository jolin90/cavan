/*
 * File:		tcp_bridge.c
 * Author:		Fuang.Cao <cavan.cfa@gmail.com>
 * Created:		2016-09-21 17:48:56
 *
 * Copyright (c) 2016 Fuang.Cao <cavan.cfa@gmail.com>
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
#include <cavan/tcp_bridge.h>

int main(int argc, char *argv[])
{
	assert(argc > 2);

	return cavan_tcp_bridge_run(argv[1], argv[2]);
}
