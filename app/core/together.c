// This File Is Automatically Generated By FuangCao
// Fuang.Cao <cavan.cfa@gmail.com>
// Create date: 2011-09-24 03:01:54

#include <cavan.h>
#include <cavan/command.h>

#ifndef CONFIG_CAVAN_MAIN_NAME
#define CONFIG_CAVAN_MAIN_NAME	"cavan-main"
#endif

#ifndef CONFIG_CAVAN_MAP_H
#define CONFIG_CAVAN_MAP_H		"cavan_map.h"
#endif

#ifndef CONFIG_CAVAN_MAP_C
#define CONFIG_CAVAN_MAP_C		"cavan_map.c"
#endif

#include CONFIG_CAVAN_MAP_H

static int cavan_main(int argc, char *argv[]);

const struct cavan_command_map cmd_map_table[] =
{
	{CONFIG_CAVAN_MAIN_NAME, cavan_main},

	#include CONFIG_CAVAN_MAP_C
};

int main(int argc, char *argv[])
{
#if 0
	int i;

	for (i = 0; i < argc; i++)
	{
		println("argv[%d] = %s", i, argv[i]);
	}
#endif

	return FIND_EXEC_COMMAND(cmd_map_table);
}

static int cavan_main(int argc, char *argv[])
{
	if (argc > 1)
	{
		return main(argc - 1, argv + 1);
	}

	print_command_table(cmd_map_table + 1, ARRAY_SIZE(cmd_map_table) - 1);

	return -EINVAL;
}
