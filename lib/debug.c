/*
 * File:			debug.c
 * Author:		Fuang.Cao <cavan.cfa@gmail.com>
 * Created:		2014-04-10 09:28:26
 *
 * Copyright (c) 2014 Fuang.Cao <cavan.cfa@gmail.com>
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
#include <time.h>

static const char *build_time_string = __DATE__ " " __TIME__;

#ifndef CONFIG_ANDROID
#include <execinfo.h>

char *dump_backtrace(char *buff, size_t size)
{
	int i, nptrs;
	void *ptrs[100];
	char **strings;
	char *buff_end = buff + size;

	nptrs = backtrace(ptrs, NELEM(ptrs));
	strings = backtrace_symbols(ptrs, nptrs);
	if (strings == NULL) {
		pr_error_info("backtrace_symbols");
		return NULL;
	}

	buff += snprintf(buff, buff_end - buff, "backtrace() returned %d addresses\n", nptrs);

	for (i = 0; i < nptrs && buff < buff_end; i++) {
		buff += snprintf(buff, buff_end - buff, "%s\n", strings[i]);
	}

	free(strings);

	return buff;
}

char *address_to_symbol(const void *addr, char *buff, size_t size)
{
	char **strings;
	void *ptrs[] = { (void *) addr };

	strings = backtrace_symbols(ptrs, 1);
	if (strings == NULL) {
		pr_error_info("backtrace_symbols");
		return NULL;
	}

	strncpy(buff, strings[0], size);

	free(strings);

	return buff;
}
#endif

int dump_stack(void)
{
	char buff[4096];

	if (dump_backtrace(buff, sizeof(buff))== NULL) {
		return -EFAULT;
	}

	pd_info("%s", buff);

	return 0;
}

static void sigsegv_handler(int signum, siginfo_t *info, void *ptr)
{
	static const char *si_codes[] = { "", "SEGV_MAPERR", "SEGV_ACCERR" };

	pr_info("Segmentation Fault Trace:");
	pr_info("info.si_signo = %d", signum);
	pr_info("info.si_errno = %d", info->si_errno);
	pr_info("info.si_code  = %d (%s)", info->si_code, si_codes[info->si_code]);
	pr_info("info.si_addr  = %p", info->si_addr);

	dump_stack();

	exit(-1);
}

int catch_sigsegv(void)
{
	struct sigaction action;

	memset(&action, 0, sizeof(action));

	action.sa_sigaction = sigsegv_handler;
	action.sa_flags = SA_SIGINFO;

	return sigaction(SIGSEGV, &action, NULL);
}

int cavan_get_build_time(struct tm *time)
{
	return strptime(build_time_string, "%h %d %Y %T", time) ? 0 : -EFAULT;
}

const char *cavan_get_build_time_string(void)
{
	struct tm time;
	static char buff[24];

	if (cavan_get_build_time(&time) < 0) {
		return build_time_string;
	}

	snprintf(buff, sizeof(buff), "%04d-%02d-%02d %02d:%02d:%02d",
		time.tm_year + 1900, time.tm_mon + 1, time.tm_mday, time.tm_hour, time.tm_min, time.tm_sec);

	return buff;
}
