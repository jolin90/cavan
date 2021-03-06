// Fuang.Cao <cavan.cfa@gmail.com> Fri May 13 17:54:01 CST 2011

#include <cavan.h>
#include <cavan/uevent.h>
#include <cavan/text.h>
#include <cavan/parser.h>
#include <cavan/network.h>

int uevent_init(struct uevent_desc *desc)
{
	int sockfd = network_create_socket_uevent();
	if (sockfd < 0) {
		pr_red_info("network_create_socket_uevent: %d", sockfd);
		return sockfd;
	}

	desc->sockfd = sockfd;

	return 0;
}

void uevent_deinit(struct uevent_desc *desc)
{
	close(desc->sockfd);
}

size_t uevent_split_base(const char *event, size_t event_len, char *props[], size_t size)
{
	size_t count = 0;
	const char *event_end = event + event_len;

	while (event < event_end && count < size) {
		if (IS_LETTER(*event)) {
			props[count++] = (char *) event;
		}

		while (*event++);
	}

	return count;
}

char *uevent_get_property_base(char *props[], int prop_count, const char *prefix, char *buff)
{
	for (prop_count--; prop_count >= 0; prop_count--) {
		if (text_lhcmp(prefix, props[prop_count]) == 0) {
			text_copy(buff, props[prop_count] + text_len(prefix));

			return buff;
		}
	}

	return NULL;
}

int uevent_match_base(char *props[], int prop_count, struct uevent_filter *filter)
{
	int count;

	for (prop_count--, count = filter->count; prop_count >= 0; prop_count--) {
		if (text_array_find(props[prop_count], filter->props, filter->count) >= 0) {
			if (--count == 0) {
				return 0;
			}
		}
	}

	return -1;
}

int get_device_uevent(struct uevent_desc *desc, struct uevent_filter *filters, size_t size)
{
	int ret;
	int sockfd = desc->sockfd;
	struct uevent_filter *p, *p_end;

	p_end = filters + size;

	while (1) {
		ret = recv(sockfd, desc->buff, sizeof(desc->buff), 0);
		if (ret < 0) {
			pr_err_info("recv");
			return ret;
		}

		desc->buff[ret] = 0;
		desc->prop_count = uevent_split_base(desc->buff, ret, desc->props, NELEM(desc->props));

		for (p = filters; p < p_end; p++) {
			if (uevent_match_base(desc->props, desc->prop_count, p) == 0) {
				return 0;
			}
		}
	}

	return -1;
}

int get_disk_add_uevent(struct uevent_desc *desc)
{
	struct uevent_filter filter = {
		.props = {
			"ACTION=add",
			"DEVTYPE=disk",
		},
		.count = 2,
	};

	return get_device_uevent(desc, &filter, 1);
}

int get_partition_add_uevent(struct uevent_desc *desc)
{
	struct uevent_filter filter = {
		.props = {
			"ACTION=add",
			"DEVTYPE=partition",
		},
		.count = 2,
	};

	return get_device_uevent(desc, &filter, 1);
}

int get_block_device_remove_uevent(struct uevent_desc *desc)
{
	struct uevent_filter filter = {
		.props = {
			"ACTION=remove",
			"SUBSYSTEM=block",
		},
		.count = 2,
	};

	return get_device_uevent(desc, &filter, 1);
}
