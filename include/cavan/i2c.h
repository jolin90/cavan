#pragma once

/*
 * File:		i2c.h
 * Author:		Fuang.Cao <cavan.cfa@gmail.com>
 * Created:		2015-09-28 17:48:13
 *
 * Copyright (c) 2015 Fuang.Cao <cavan.cfa@gmail.com>
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
#include <linux/i2c.h>
#include <linux/i2c-dev.h>

#define CAVAN_I2C_RATE(k)		((k) * 1000)
#define CAVAN_I2C_RATE_100K		CAVAN_I2C_RATE(100)
#define CAVAN_I2C_RATE_400K		CAVAN_I2C_RATE(100)

struct cavan_i2c_msg {
	__u16 addr;	/* slave address			*/
	__u16 flags;
	__u16 length;		/* msg length				*/
	__u8 *buff;		/* pointer to msg data			*/
#ifdef CONFIG_I2C_ROCKCHIP_COMPAT
	__u32 scl_rate;
#endif
};

struct cavan_i2c_client {
	int fd;
	u16 addr;
	void *private_data;
};

int cavan_i2c_init(struct cavan_i2c_client *client, int index, void *data);
void cavan_i2c_deinit(struct cavan_i2c_client *client);
int cavan_i2c_transfer(struct cavan_i2c_client *client, struct cavan_i2c_msg *msgs, size_t count);
void cavan_i2c_detect(struct cavan_i2c_client *client);
int cavan_i2c_write_data(struct cavan_i2c_client *client, u8 addr, const void *data, size_t size);
int cavan_i2c_read_data(struct cavan_i2c_client *client, u8 addr, void *data, size_t size);
int cavan_i2c_update_bits8(struct cavan_i2c_client *client, u8 addr, u8 value, u8 mask);
int cavan_i2c_update_bits16(struct cavan_i2c_client *client, u8 addr, u16 value, u16 mask);
int cavan_i2c_update_bits32(struct cavan_i2c_client *client, u8 addr, u32 value, u32 mask);

static inline int cavan_i2c_set_address(struct cavan_i2c_client *client, u16 addr)
{
	int ret = ioctl(client->fd, I2C_SLAVE_FORCE, addr);
	if (ret < 0) {
		return ret;
	}

	client->addr = addr;

	return 0;
}

static inline int cavan_i2c_set_tenbit(struct cavan_i2c_client *client, bool enable)
{
	return ioctl(client->fd, I2C_TENBIT, enable);
}

static inline int cavan_i2c_master_send(struct cavan_i2c_client *client, const void *buff, size_t size)
{
	return write(client->fd, buff, size);
}

static inline int cavan_i2c_master_recv(struct cavan_i2c_client *client, void *buff, size_t size)
{
	return read(client->fd, buff, size);
}

static inline int cavan_i2c_read_register8(struct cavan_i2c_client *client, u8 addr, u8 *value)
{
	return cavan_i2c_read_data(client, addr, value, sizeof(*value));
}

static inline int cavan_i2c_write_register8(struct cavan_i2c_client *client, u8 addr, const u8 value)
{
	return cavan_i2c_write_data(client, addr, &value, sizeof(value));
}

static inline int cavan_i2c_read_register16(struct cavan_i2c_client *client, u8 addr, u16 *value)
{
	return cavan_i2c_read_data(client, addr, value, sizeof(*value));
}

static inline int cavan_i2c_write_register16(struct cavan_i2c_client *client, u8 addr, const u16 value)
{
	return cavan_i2c_write_data(client, addr, &value, sizeof(value));
}

static inline int cavan_i2c_read_register32(struct cavan_i2c_client *client, u8 addr, u32 *value)
{
	return cavan_i2c_read_data(client, addr, value, sizeof(*value));
}

static inline int cavan_i2c_write_register32(struct cavan_i2c_client *client, u8 addr, const u32 value)
{
	return cavan_i2c_write_data(client, addr, &value, sizeof(value));
}
