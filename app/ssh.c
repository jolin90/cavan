/*
 * File:		ssh.c
 * Author:		Fuang.Cao <cavan.cfa@gmail.com>
 * Created:		2016-03-11 11:00:47
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
#include <cavan/command.h>

#define FILE_CREATE_DATE "2016-03-11 11:00:47"

static void show_usage(const char *command)
{
	println("Usage: %s [option]", command);
	println("-h, -H, --help\t\tshow this help");
	println("-v, -V, --version\tshow version");
}

int main(int argc, char *argv[])
{
	int c;
	int option_index;
	struct option long_option[] = {
		{
			.name = "help",
			.has_arg = no_argument,
			.flag = NULL,
			.val = CAVAN_COMMAND_OPTION_HELP,
		}, {
			.name = "version",
			.has_arg = no_argument,
			.flag = NULL,
			.val = CAVAN_COMMAND_OPTION_VERSION,
		}, {
			0, 0, 0, 0
		},
	};

	while ((c = getopt_long(argc, argv, "vVhH", long_option, &option_index)) != EOF) {
		switch (c) {
		case 'v':
		case 'V':
		case CAVAN_COMMAND_OPTION_VERSION:
			show_author_info();
			println(FILE_CREATE_DATE);
			return 0;

		case 'h':
		case 'H':
		case CAVAN_COMMAND_OPTION_HELP:
			show_usage(argv[0]);
			return 0;

		default:
			show_usage(argv[0]);
			return -EINVAL;
		}
	}

	return 0;
}
