/*
 * Author: Fuang.Cao
 * Email: cavan.cfa@gmail.com
 * Date: Thu Jul 19 10:31:41 CST 2012
 */

#include <cavan.h>
#include <cavan/adb.h>

#define CAVAN_ADB_DEBUG			0
#define CAVAN_ADB_POLL_DELAY	200

bool adb_is_client(void)
{
	static int retval = -1;

	if (retval < 0) {
		retval = file_access_e("/dev/usb-ffs/adb") || file_access_e("/dev/android_adb");
	}

	return retval;
}

bool adb_is_host(void)
{
	return !adb_is_client();
}

int adb_read_status(int sockfd, char *buff, size_t size)
{
	int ret;
	ssize_t recvlen;

	recvlen = inet_recv(sockfd, buff, size > 8 ? 8 : size - 1);
	if (recvlen < 0) {
		text_copy(buff, "protocol fault (no status)");
		return recvlen;
	}

	buff[recvlen] = 0;
	ret = text_lhcmp("OKAY", buff) ? -EFAULT : 0;

	if (recvlen == 8) {
		size_t length;

		length = text2value_unsigned(buff + 4, NULL, 16);
		if (length >= size) {
			length = size - 1;
		}

		recvlen = inet_recv(sockfd, buff, length);
		if (recvlen < 0) {
			return recvlen;
		}

		buff[recvlen] = 0;
	}

	return ret;
}

int adb_send_text(int sockfd, const char *text)
{
	int ret;
	ssize_t sendlen;
	size_t length;
	char buff[16];
	char status[256];

	length = text_len(text);

#if __WORDSIZE == 64
	sprintf(buff, "%04lx", length);
#else
	sprintf(buff, "%04x", length);
#endif

#if CAVAN_ADB_DEBUG
	println("text = %s%s", buff, text);
#endif

	sendlen = inet_send(sockfd, buff, 4);
	if (sendlen < 0) {
		pr_red_info("inet_send");
		return sendlen;
	}

	sendlen = inet_send(sockfd, text, length);
	if (sendlen < 0) {
		return sendlen;
	}

	ret = adb_read_status(sockfd, status, sizeof(status));
	if (ret < 0) {
#if CAVAN_ADB_DEBUG
		pr_red_info("status = %s", status);
#endif
		return ret;
	}

#if CAVAN_ADB_DEBUG
	println("status = %s", status);
#endif

	return 0;
}

int adb_connect_service_base(const char *ip, u16 port, int retry)
{
	int sockfd;
	unsigned int i;
	u16 ports[] = { port, ADB_PORT1, ADB_PORT2 };
	struct sockaddr_in addr;

	sockfd = inet_socket(SOCK_STREAM);
	if (sockfd < 0) {
		pr_err_info("socket");
		return sockfd;
	}

	if (ip == NULL) {
		ip = LOCAL_HOST_IP;
	}

#if CAVAN_ADB_DEBUG
	println("IP = %s", ip);
#endif

	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = inet_addr(ip);

	while (1) {
		for (i = 0; i < NELEM(ports); i++) {
			if (ports[i] == 0) {
				continue;
			}

#if CAVAN_ADB_DEBUG
			pr_info("Try port %04d", ports[i]);
#endif

			addr.sin_port = htons(ports[i]);

			if (inet_connect(sockfd, &addr) >= 0) {
				return sockfd;
			}
		}

		if (--retry < 0) {
			break;
		}

		if (adb_is_host() && addr.sin_addr.s_addr == 0x0100007F) {
			if (cavan_system("adb start-server", 0, NULL)) {
				pr_error_info("adb start-server");
				break;
			}
		}
	}

	close(sockfd);

	return -ENOENT;
}

int adb_connect_service(const char *ip, u16 port, const char *service)
{
	int ret;
	int sockfd;

	sockfd = adb_connect_service_base(ip, port, 1);
	if (sockfd < 0) {
		pr_red_info("adb_create_link");
		return sockfd;
	}

	if (adb_is_host() && (ret = adb_send_command(sockfd, "host:transport-any")) < 0) {
#if CAVAN_ADB_DEBUG
		pr_red_info("adb_send_command");
#endif
		goto out_close_sockfd;
	}

	ret = adb_send_command(sockfd, service);
	if (ret < 0) {
		pr_red_info("adb_connect_service");
		goto out_close_sockfd;
	}

	return sockfd;

out_close_sockfd:
	close(sockfd);
	return ret;
}

bool adb_wait_for_device_once(const char *ip, u16 port)
{
	int sockfd = adb_connect_service(ip, port, "host:wait-for-any-device");
	if (sockfd < 0) {
		return false;
	}

	close(sockfd);

	return true;
}

int adb_wait_for_device(const char *ip, u16 port, u32 msec)
{
	if (!adb_is_host()) {
		return 0;
	}

	if (msec > 0) {
		while (!adb_wait_for_device_once(ip, port)) {
			if (msec < CAVAN_ADB_POLL_DELAY) {
				return -ETIMEDOUT;
			}

			msleep(CAVAN_ADB_POLL_DELAY);
			msec -= CAVAN_ADB_POLL_DELAY;
		}
	} else {
		while (!adb_wait_for_device_once(ip, port)) {
			msleep(500);
		}
	}

	return 0;
}

int adb_create_tcp_link(const char *ip, u16 port, u16 tcp_port, bool wait_device)
{
	char service[32];

	if (wait_device) {
		int ret;

		print("waiting for adb connection ... ");

		ret = adb_wait_for_device(ip, port, 0);
		if (ret < 0) {
			return ret;
		}

		println("OK");
	}

	sprintf(service, "tcp:%04d", tcp_port);

	return adb_connect_service(ip, port, service);
}

char *adb_parse_sms_single(const char *buff, const char *end, char *segments[], size_t size)
{
	unsigned int i;
	char *p;

	end = text_find_lf(buff, end);
	if (end == NULL) {
		return NULL;
	}

	for (i = 0, size--; i < size; i++) {
		for (p = segments[i]; buff < end; p++, buff++) {
			if (*buff == ',') {
				break;
			}

			*p = *buff;
		}

		buff++;
		*p = 0;
	}

	for (p = segments[i]; buff < end; buff++, p++) {
		*p = *buff;
	}

	*p = 0;

	return (char *) end;
}

char *adb_parse_sms_multi(const char *buff, const char *end)
{
	char mobile[32];
	char time[32];
	char content[1024];
	char *segments[] = {
		mobile, time, content
	};

	while (1) {
		const char *temp = adb_parse_sms_single(buff, end, segments, NELEM(segments));
		if (temp == NULL) {
			break;
		}

		println("mobile = %s", mobile);
		println("time = %s", time);
		println("content = %s", content);

		for (buff = temp; buff < end && (*buff == '\r' || *buff == '\n'); buff++);
	}

	return (char *) buff;
}

char *adb_parse_sms_main(char *buff, char *end)
{
	char *p = adb_parse_sms_multi(buff, end);

	while (p < end) {
		*buff++ = *p++;
	}

	return buff;
}

int frecv_text_and_write(int sockfd, int fd)
{
	char buff[4096], *p, *p_end;
	ssize_t recvlen;
	ssize_t writelen;

	p = buff;
	p_end = buff + sizeof(buff);

	while (1) {
		recvlen = recv(sockfd, p, p_end - p, 0);
		if (recvlen <= 0) {
			pr_red_info("inet_recv");
			return recvlen;
		}

		// println("recvlen = %d", recvlen);
		p = adb_parse_sms_main(buff, p + recvlen);

		writelen = ffile_write(fd, buff, p - buff);
		if (writelen < 0) {
			pr_red_info("ffile_write");
			return writelen;
		}
	}

	return 0;
}

int recv_text_and_write(int sockfd, const char *filename)
{
	int ret;
	int fd;

	fd = open(filename, O_WRONLY | O_CREAT | O_TRUNC | O_SYNC, 0777);
	if (fd < 0) {
		pr_err_info("Failed to open file `%s'", filename);
		return fd;
	}

	ret = frecv_text_and_write(sockfd, fd);

	close(fd);

	return ret;
}

ssize_t sms_receive_value(int sockfd, void *value, size_t size)
{
	return (size_t) recv(sockfd, value, size, 0) == size ? (ssize_t) size : -1;
}

ssize_t sms_receive_text(int sockfd, char *buff)
{
	u32 length;

	if (sms_receive_value(sockfd, &length, sizeof(length)) < 0 || sms_receive_value(sockfd, buff, length) < 0) {
		return -1;
	}

	buff[length] = 0;

	return length;
}

ssize_t sms_send_response(int sockfd, u8 type)
{
	return send(sockfd, &type, 1, 0) == 1 ? 1 : 0;
}

int sms_receive_message(int sockfd, struct eavoo_short_message *message)
{
	u8 type;

	message->date = 0;
	message->address[0] = 0;
	message->body[0] = 0;

	while (1) {
		if (sms_receive_value(sockfd, &type, sizeof(type)) < 0) {
			pr_err_info("recv");
			return -1;
		}

		switch (type) {
		case SMS_TYPE_DATE:
			if (sms_receive_value(sockfd, &message->date, sizeof(message->date)) < 0) {
				return -1;
			}
			break;

		case SMS_TYPE_ADDRESS:
			if (sms_receive_text(sockfd, message->address) < 0) {
				return -1;
			}
			break;

		case SMS_TYPE_BODY:
			if (sms_receive_text(sockfd, message->body) < 0) {
				return -1;
			}
			break;

		case SMS_TYPE_END:
			return sms_send_response(sockfd, SMS_TYPE_ACK);

		case SMS_TYPE_TEST:
#if ADB_DEBUG
			println("SMS_TYPE_TEST");
#endif
			if (sms_send_response(sockfd, SMS_TYPE_ACK) < 0) {
				pr_err_info("send");
				return -1;
			}
			continue;

		default:
			pr_red_info("unknown sms type");
			sms_send_response(sockfd, SMS_TYPE_FAILED);
			return -EINVAL;
		}
	}

	return 0;
}

void show_eavoo_short_message(struct eavoo_short_message *message)
{
	print_sep(60);
	println("Address = %s", message->address);
	println("Body = %s", message->body);
	println("Date = 0x%08x", message->date);
	println("Date = %s", asctime(localtime((time_t *) &message->date)));
}

int fsms_receive_and_write(int sockfd, int fd)
{
	int ret;
	struct eavoo_short_message message;

	while (1) {
		ret = sms_receive_message(sockfd, &message);
		if (ret < 0) {
			return ret;
		}

		show_eavoo_short_message(&message);
	}

	return 0;
}

int sms_receive_and_write(int sockfd, const char *filename)
{
	int ret;
	int fd;

	fd = open(filename, O_WRONLY | O_CREAT | O_TRUNC | O_SYNC, 0777);
	if (fd < 0) {
		pr_err_info("Failed to open file `%s'", filename);
		return fd;
	}

	ret = fsms_receive_and_write(sockfd, fd);

	close(fd);

	return ret;
}
