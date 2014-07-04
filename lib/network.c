// Fuang.Cao <cavan.cfa@gmail.com> Thu Apr 21 10:08:25 CST 2011

#include <cavan.h>
#include <cavan/file.h>
#include <cavan/command.h>
#include <cavan/network.h>
#include <cavan/progress.h>

static struct network_protocol protocols[] =
{
	[NETWORK_PROTOCOL_HTTP] = {"http", 80, NETWORK_PROTOCOL_HTTP},
	[NETWORK_PROTOCOL_HTTPS] = {"https", 445, NETWORK_PROTOCOL_HTTPS},
	[NETWORK_PROTOCOL_FTP] = {"ftp", 21, NETWORK_PROTOCOL_FTP}
};

ssize_t sendto_select(int sockfd, int retry, const void *buff, size_t len, const struct sockaddr_in *remote_addr)
{
	while (retry--)
	{
		ssize_t sendlen;

		sendlen = inet_sendto(sockfd, buff, len, remote_addr);
		if (sendlen < 0)
		{
			print_error("send data failed");
			return sendlen;
		}

		if (file_poll_input(sockfd, NETWORK_TIMEOUT_VALUE))
		{
			return sendlen;
		}

		println("timeout retry = %d", retry);
	}

	return -ETIMEDOUT;
}

ssize_t sendto_receive(int sockfd, long timeout, int retry, const void *send_buff, ssize_t sendlen, void *recv_buff, ssize_t recvlen, struct sockaddr_in *remote_addr, socklen_t *addr_len)
{
	sendlen = sendto_select(sockfd, retry, send_buff, sendlen, remote_addr);
	if (sendlen < 0)
	{
		error_msg("send data timeout");
		return sendlen;
	}

	return inet_recvfrom(sockfd, recv_buff, recvlen, remote_addr, addr_len);
}

const char *mac_protocol_type_tostring(int type)
{
	switch (type)
	{
	case ETH_P_LOOP:
		return "ETH_P_LOOP";
	case ETH_P_PUP:
		return "ETH_P_PUP";
	case ETH_P_PUPAT:
		return "ETH_P_PUPAT";
	case ETH_P_IP:
		return "ETH_P_IP";
	case ETH_P_X25:
		return "ETH_P_X25";
	case ETH_P_ARP:
		return "ETH_P_ARP";
	case ETH_P_BPQ:
		return "ETH_P_BPQ";
	case ETH_P_IEEEPUP:
		return "ETH_P_IEEEPUP";
	case ETH_P_IEEEPUPAT:
		return "ETH_P_IEEEPUPAT";
	case ETH_P_DEC:
		return "ETH_P_DEC";
	case ETH_P_DNA_DL:
		return "ETH_P_DNA_DL";
	case ETH_P_DNA_RC:
		return "ETH_P_DNA_RC";
	case ETH_P_DNA_RT:
		return "ETH_P_DNA_RT";
	case ETH_P_LAT:
		return "ETH_P_LAT";
	case ETH_P_DIAG:
		return "ETH_P_DIAG";
	case ETH_P_CUST:
		return "ETH_P_CUST";
	case ETH_P_SCA:
		return "ETH_P_SCA";
	case ETH_P_RARP:
		return "ETH_P_RARP";
	case ETH_P_ATALK:
		return "ETH_P_ATALK";
	case ETH_P_AARP:
		return "ETH_P_AARP";
	case ETH_P_8021Q:
		return "ETH_P_8021Q";
	case ETH_P_IPX:
		return "ETH_P_IPX";
	case ETH_P_IPV6:
		return "ETH_P_IPV6";
	case ETH_P_SLOW:
		return "ETH_P_SLOW";
	case ETH_P_WCCP:
		return "ETH_P_WCCP";
	case ETH_P_PPP_DISC:
		return "ETH_P_PPP_DISC";
	case ETH_P_PPP_SES:
		return "ETH_P_PPP_SES";
	case ETH_P_MPLS_UC:
		return "ETH_P_MPLS_UC";
	case ETH_P_MPLS_MC:
		return "ETH_P_MPLS_MC";
	case ETH_P_ATMMPOA:
		return "ETH_P_ATMMPOA";
	case ETH_P_ATMFATE:
		return "ETH_P_ATMFATE";
	case ETH_P_AOE:
		return "ETH_P_AOE";
	case ETH_P_TIPC:
		return "ETH_P_TIPC";
#if 0
	case ETH_P_TEB:
		return "ETH_P_TEB";
	case ETH_P_PAUSE:
		return "ETH_P_PAUSE";
	case ETH_P_LINK_CTL:
		return "ETH_P_LINK_CTL";
	case ETH_P_EDSA:
		return "ETH_P_EDSA";
	case ETH_P_FIP:
		return "ETH_P_FIP";
	case ETH_P_1588:
		return "ETH_P_1588";
	case ETH_P_FCOE:
		return "ETH_P_FCOE";
	case ETH_P_PAE:
		return "ETH_P_PAE";
#endif
	default:
		return "unknown";
	}
}

const char *ip_protocol_type_tostring(int type)
{
	switch (type)
	{
	case IPPROTO_IP:
		return "IPPROTO_IP";
	case IPPROTO_ICMP:
		return "IPPROTO_ICMP";
	case IPPROTO_IGMP:
		return "IPPROTO_IGMP";
	case IPPROTO_IPIP:
		return "IPPROTO_IPIP";
	case IPPROTO_TCP:
		return "IPPROTO_TCP";
	case IPPROTO_EGP:
		return "IPPROTO_EGP";
	case IPPROTO_PUP:
		return "IPPROTO_PUP";
	case IPPROTO_UDP:
		return "IPPROTO_UDP";
	case IPPROTO_IDP:
		return "IPPROTO_IDP";
	case IPPROTO_RSVP:
		return "IPPROTO_RSVP";
	case IPPROTO_GRE:
		return "IPPROTO_GRE";
	case IPPROTO_IPV6:
		return "IPPROTO_IPV6";
	case IPPROTO_ESP:
		return "IPPROTO_ESP";
	case IPPROTO_AH:
		return "IPPROTO_AH";
	case IPPROTO_COMP:
		return "IPPROTO_COMP";
	case IPPROTO_SCTP:
		return "IPPROTO_SCTP";
	case IPPROTO_RAW:
		return "IPPROTO_RAW";
	case IPPROTO_PIM:
		return "IPPROTO_PIM";
#if 0
	case IPPROTO_DCCP:
		return "IPPROTO_DCCP";
	case IPPROTO_BEETPH:
		return "IPPROTO_BEETPH";
	case IPPROTO_UDPLITE:
		return "IPPROTO_UDPLITE";
#endif
	default:
		return "unknown";
	}
}

void show_mac_header(struct mac_header *hdr)
{
	pr_bold_info("MAC Header:");
	println("dest_mac = %s", mac_address_tostring((char *)hdr->dest_mac, sizeof(hdr->dest_mac)));
	println("src_mac = %s", mac_address_tostring((char *)hdr->src_mac, sizeof(hdr->src_mac)));
	println("protocol_type = %s", mac_protocol_type_tostring(ntohs(hdr->protocol_type)));
}

void show_ip_header(struct ip_header *hdr, int simple)
{
	pr_bold_info("IP Header:");

	if (simple == 0)
	{
		println("version = %d", hdr->version);
		println("header_length = %d", hdr->header_length);
		println("service_type = %d", hdr->service_type);
		println("total_length = %d", ntohs(hdr->total_length));
		println("flags = %d", hdr->flags);
		println("piece_offset = %d", hdr->piece_offset);
		println("ttl = %d", hdr->ttl);
		println("header_checksum = 0x%04x", ntohs(hdr->header_checksum));
	}

	println("protocol_type = %s", ip_protocol_type_tostring(hdr->protocol_type));
	println("src_ip = %s", inet_ntoa(*(struct in_addr *)&hdr->src_ip));
	println("dest_ip = %s", inet_ntoa(*(struct in_addr *)&hdr->dest_ip));
}

void show_tcp_header(struct tcp_header *hdr)
{
	pr_bold_info("TCP Header:");
	println("src_port = %d", ntohs(hdr->src_port));
	println("dest_port = %d", ntohs(hdr->dest_port));
	println("sequence = %d", ntohl(hdr->sequence));
	println("sck_sequence = %d", ntohl(hdr->sck_sequence));
	println("window_size = %d", ntohs(hdr->window_size));
	println("reserve = %d", hdr->reserve);
	println("TCP_URG = %d", hdr->TCP_URG);
	println("TCP_ACK = %d", hdr->TCP_ACK);
	println("TCP_PSH = %d", hdr->TCP_PSH);
	println("TCP_RST = %d", hdr->TCP_RST);
	println("TCP_SYN = %d", hdr->TCP_SYN);
	println("TCP_FIN = %d", hdr->TCP_FIN);
	println("checksum = 0x%04x", ntohs(hdr->checksum));
	println("urgent_pointer = %d", ntohs(hdr->urgent_pointer));
}

void show_udp_header(struct udp_header *hdr)
{
	pr_bold_info("UDP Header:");
	println("src_port = %d", ntohs(hdr->src_port));
	println("dest_port = %d", ntohs(hdr->dest_port));
	println("udp_length = %d", ntohs(hdr->udp_length));
	println("udp_checksum = 0x%04x", hdr->udp_checksum);
}

void show_arp_header(struct arp_header *hdr, int simple)
{
	pr_bold_info("ARP Header:");

	if (simple == 0)
	{
		println("hardware_type = %d", ntohs(hdr->hardware_type));
		println("protocol_type = %d", ntohs(hdr->protocol_type));
		println("hardware_addrlen = %d", hdr->hardware_addrlen);
		println("protocol_addrlen = %d", hdr->protocol_addrlen);
		println("op_code = %d", ntohs(hdr->op_code));
	}

	println("src_mac = %s", mac_address_tostring((char *)hdr->src_mac, sizeof(hdr->src_mac)));
	println("src_ip = %s", inet_ntoa(*(struct in_addr *)&hdr->src_ip));
	println("dest_mac = %s", mac_address_tostring((char *)hdr->dest_mac, sizeof(hdr->dest_mac)));
	println("dest_ip = %s", inet_ntoa(*(struct in_addr *)&hdr->dest_ip));
}

void show_dhcp_header(struct dhcp_header *hdr)
{
	pr_bold_info("DHCP Header:");

	println("opcode = %d", hdr->opcode);
	println("htype = %d", hdr->htype);
	println("hlen = %d", hdr->hlen);
	println("hops = %d", hdr->hops);
	println("transction_id = 0x%08x", ntohl(hdr->transction_id));
	println("seconds = %d", ntohs(hdr->seconds));
	println("flags = 0x%04x", ntohs(hdr->flags));
	println("ciaddr = %s", inet_ntoa(*(struct in_addr *)&hdr->ciaddr));
	println("yiaddr = %s", inet_ntoa(*(struct in_addr *)&hdr->yiaddr));
	println("siaddr = %s", inet_ntoa(*(struct in_addr *)&hdr->siaddr));
	println("giaddr = %s", inet_ntoa(*(struct in_addr *)&hdr->giaddr));
	println("chaddr = %s", mac_address_tostring((char *)hdr->chaddr, MAC_ADDRESS_LEN));
	println("sname = %s", hdr->sname);
}

void show_icmp_header(struct icmp_header *hdr)
{
	pr_bold_info("ICMP Header:");

	println("type = %d", hdr->type);
	println("code = %d", hdr->code);
	println("checksum = 0x%04x", hdr->checksum);
}

int cavan_route_table_init(struct cavan_route_table *table, size_t table_size)
{
	struct cavan_route_node **pp, **pp_end;

	pp = malloc(table_size * sizeof(void *));
	if (pp == NULL)
	{
		return -ENOMEM;
	}

	table->route_table = pp;

	for (pp_end = pp + table_size; pp < pp_end; pp++)
	{
		*pp = NULL;
	}

	table->table_size = table_size;

	return 0;
}

void cavan_route_table_deinit(struct cavan_route_table *table)
{
	if (table)
	{
		free(table->route_table);
		table->table_size = 0;
	}
}

int cavan_route_table_insert_node(struct cavan_route_table *table, struct cavan_route_node *node)
{
	struct cavan_route_node **pp, **pp_end;

	for (pp = table->route_table, pp_end = pp + table->table_size; pp < pp_end && *pp; pp++);

	if (pp < pp_end)
	{
		*pp = node;
		return 0;
	}

	return -ENOMEM;
}

struct cavan_route_node **cavan_find_route_by_mac(struct cavan_route_table *table, u8 *mac)
{
	struct cavan_route_node *p, **pp, **pp_end;

	for (pp = table->route_table, pp_end = pp + table->table_size; pp < pp_end; pp++)
	{
		p = *pp;

		if (p && memcmp(mac, p->mac_addr, sizeof(MAC_ADDRESS_LEN)))
		{
			return pp;
		}
	}

	return NULL;
}

struct cavan_route_node **cavan_find_route_by_ip(struct cavan_route_table *table, u32 ip)
{
	struct cavan_route_node *p, **pp, **pp_end;

	for (pp = table->route_table, pp_end = pp + table->table_size; pp < pp_end; pp++)
	{
		p = *pp;

		if (p && p->ip_addr == ip)
		{
			return pp;
		}
	}

	return NULL;
}

int cavan_route_table_delete_node(struct cavan_route_table *table, struct cavan_route_node *node)
{
	struct cavan_route_node **pp, **pp_end;

	for (pp = table->route_table, pp_end = pp + table->table_size; pp < pp_end && *pp != node; pp++);

	if (pp < pp_end)
	{
		*pp = NULL;
		return 0;
	}

	return -ENOENT;
}

int cavan_route_table_delete_by_mac(struct cavan_route_table *table, u8 *mac)
{
	struct cavan_route_node **pp;

	pp = cavan_find_route_by_mac(table, mac);
	if (pp)
	{
		*pp = NULL;
		return 0;
	}

	return -ENOENT;
}

int cavan_route_table_delete_by_ip(struct cavan_route_table *table, u32 ip)
{
	struct cavan_route_node **pp;

	pp = cavan_find_route_by_ip(table, ip);
	if (pp)
	{
		*pp = NULL;
		return 0;
	}

	return -ENOENT;
}

u16 udp_checksum(struct ip_header *ip_hdr)
{
	struct udp_pseudo_header udp_pseudo_hdr;
	struct udp_header *udp_hdr;
	u32 checksum;

	udp_hdr = (void *)(ip_hdr + 1);

	udp_pseudo_hdr.src_ip = ip_hdr->src_ip;
	udp_pseudo_hdr.dest_ip = ip_hdr->dest_ip;
	udp_pseudo_hdr.protocol = ip_hdr->protocol_type;
	udp_pseudo_hdr.zero = 0;
	udp_pseudo_hdr.udp_length = udp_hdr->udp_length;
	udp_hdr->udp_checksum = 0;

	checksum = checksum16((u16 *)&udp_pseudo_hdr, sizeof(udp_pseudo_hdr));
	checksum += checksum16((u16 *)udp_hdr, ntohs(udp_hdr->udp_length));

	return ~((checksum + (checksum >> 16)) & 0xFFFF);
}

void inet_sockaddr_init(struct sockaddr_in *addr, const char *ip, u16 port)
{
	LOGD("IP = %s, PORT = %d\n", ip ? ip : "INADDR_ANY", port);
	addr->sin_family = AF_INET;
	addr->sin_port = htons(port);
	addr->sin_addr.s_addr = ip ? inet_addr(ip) : htonl(INADDR_ANY);
}

void unix_sockaddr_init(struct sockaddr_un *addr, const char *pathname)
{
	LOGD("SUN_PATH = %s\n", pathname);

	addr->sun_family = AF_UNIX;
	strncpy(addr->sun_path, pathname, sizeof(addr->sun_path));
}

int inet_create_tcp_link1(const struct sockaddr_in *addr)
{
	int ret;
	int sockfd;

	sockfd = inet_socket(SOCK_STREAM);
	if (sockfd < 0)
	{
		print_error("socket");
		return sockfd;
	}

	ret = inet_connect(sockfd, addr);
	if (ret < 0)
	{
		print_error("inet_connect");
		close(sockfd);
		return ret;
	}

	return sockfd;
}

int unix_create_tcp_link(const char *hostname, u16 port)
{
	int ret;
	int sockfd;
	struct sockaddr_un addr;

	sockfd = unix_socket(SOCK_STREAM);
	if (sockfd < 0)
	{
		pr_error_info("socket");
		return sockfd;
	}

	unix_sockaddr_init(&addr, hostname);

	ret = unix_connect(sockfd, &addr);
	if (ret < 0)
	{
		print_error("inet_connect");
		close(sockfd);
		return ret;
	}

	return sockfd;
}

int inet_create_tcp_link_by_addrinfo(struct addrinfo *info, u16 port, struct sockaddr_in *addr)
{
	int sockfd;

	sockfd = inet_socket(SOCK_STREAM);
	if (sockfd < 0)
	{
		pr_error_info("inet_socket");
		return sockfd;
	}

	while (info)
	{
		if (info->ai_family == AF_INET)
		{
			struct sockaddr_in *p = (struct sockaddr_in *)info->ai_addr;

			p->sin_port = htons(port);
			if (inet_connect(sockfd, p) == 0)
			{
				if (addr)
				{
					addr->sin_addr.s_addr = p->sin_addr.s_addr;
				}

				return sockfd;
			}
		}

		info = info->ai_next;
	}

	close(sockfd);

	return -ENOENT;
}

int inet_create_tcp_link2(const char *hostname, u16 port)
{
	int ret;
	int sockfd;
	struct sockaddr_in addr;

	if (text_cmp(hostname, "localhost") == 0)
	{
		hostname = "127.0.0.1";
	}

	if (inet_aton(hostname, &addr.sin_addr))
	{
		addr.sin_family = AF_INET;
		addr.sin_port = htons(port);
		sockfd = inet_create_tcp_link1(&addr);
	}
	else
	{
		struct addrinfo *info;
		struct addrinfo nints;

		memset(&nints, 0, sizeof(nints));
		nints.ai_family = AF_INET;
		nints.ai_socktype = SOCK_STREAM;
		nints.ai_flags = 0;
		ret = getaddrinfo(hostname, NULL, &nints, &info);
		if (ret < 0 || info == NULL)
		{
			pr_error_info("getaddrinfo");
			return -ENOENT;
		}

		sockfd = inet_create_tcp_link_by_addrinfo(info, port, &addr);
		freeaddrinfo(info);
	}

	if (sockfd < 0)
	{
		return sockfd;
	}

	LOGD("%s => %s:%d\n", hostname, inet_ntoa(addr.sin_addr), port);

	return sockfd;
}

int inet_create_service(int type, u16 port)
{
	int ret;
	int sockfd;
	struct sockaddr_in addr;

	sockfd = inet_socket(type);
	if (sockfd < 0)
	{
		print_error("socket");
		return sockfd;
	}

	inet_sockaddr_init(&addr, NULL, port);

	ret = inet_bind(sockfd, &addr);
	if (ret < 0)
	{
		print_error("bind to port %d failed", port);
		close(sockfd);
		return ret;
	}

	return sockfd;
}

int unix_create_service(int type, const char *pathname)
{
	int ret;
	int sockfd;
	struct sockaddr_un addr;

	sockfd = unix_socket(type);
	if (sockfd < 0)
	{
		print_error("socket");
		return sockfd;
	}

	unix_sockaddr_init(&addr, pathname);
	unlink(pathname);

	ret = unix_bind(sockfd, &addr);
	if (ret < 0)
	{
		print_error("bind to pathname %s failed", pathname);
		close(sockfd);
		return ret;
	}

	return sockfd;
}

int inet_create_tcp_service(u16 port)
{
	int ret;
	int sockfd;

	sockfd = inet_create_service(SOCK_STREAM, port);
	if (sockfd < 0)
	{
		return sockfd;
	}

	ret = inet_listen(sockfd);
	if (ret < 0)
	{
		pr_error_info("listen to port %d failed", port);
		close(sockfd);
		return ret;
	}

	return sockfd;
}

int unix_create_tcp_service(const char *pathname)
{
	int ret;
	int sockfd;

	sockfd = unix_create_service(SOCK_STREAM, pathname);
	if (sockfd < 0)
	{
		return sockfd;
	}

	ret = inet_listen(sockfd);
	if (ret < 0)
	{
		pr_error_info("listen to socket %s failed", pathname);
		close(sockfd);
		return ret;
	}

	return sockfd;
}

ssize_t inet_tcp_sendto(struct sockaddr_in *addr, const void *buff, size_t size)
{
	int sockfd;
	ssize_t sendlen;

	sockfd = inet_create_tcp_link1(addr);
	if (sockfd < 0)
	{
		error_msg("inet_create_tcp_link1");
		return sockfd;
	}

	sendlen = inet_send(sockfd, buff, size);

	close(sockfd);

	return sendlen;
}

u32 get_rand_value(void)
{
	static int seeded;

	if (seeded == 0)
	{
		struct timeval tv;

		gettimeofday(&tv, NULL);
		srand(tv.tv_usec);
		seeded = 1;
	}

	return rand();
}

int inet_bind_rand(int sockfd, int retry)
{
	int ret;
	struct sockaddr_in addr;

	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = htonl(INADDR_ANY);

	do {
		addr.sin_port = get_rand_value() & 0xFFFF;

		ret = inet_bind(sockfd, &addr);
	} while (ret < 0 && retry--);

	return ret < 0 ? ret : (int)ntohs(addr.sin_port);
}

ssize_t inet_send(int sockfd, const char *buff, size_t size)
{
	ssize_t wrlen;
	const char *buff_end;

	for (buff_end = buff + size; buff < buff_end; buff += wrlen)
	{
		wrlen = send(sockfd, buff, buff_end - buff, MSG_NOSIGNAL);
		if (wrlen <= 0)
		{
			return wrlen;
		}
	}

	return size;
}

int inet_tcp_send_file1(int sockfd, int fd)
{
	ssize_t readlen, sendlen;
	char buff[1024];

	while (1)
	{
		readlen = read(fd, buff, sizeof(buff));
		if (readlen < 0)
		{
			print_error("read");
			return readlen;
		}

		if (readlen == 0)
		{
			break;
		}

		sendlen = inet_send(sockfd, buff, readlen);
		if (sendlen < 0)
		{
			print_error("inet_send");
			return sendlen;
		}
	}

	return 0;
}

int inet_tcp_send_file2(int sockfd, const char *filename)
{
	int ret;
	int fd;

	fd = open(filename, O_RDONLY);
	if (fd < 0)
	{
		print_error("open file %s failed", filename);
		return fd;
	}

	ret = inet_tcp_send_file1(sockfd, fd);

	close(fd);

	return ret;
}

int inet_tcp_receive_file1(int sockfd, int fd)
{
	ssize_t recvlen, writelen;
	char buff[1024];

	while (1)
	{
		recvlen = inet_recv(sockfd, buff, sizeof(buff));
		if (recvlen < 0)
		{
			print_error("inet_recv");
			return recvlen;
		}

		if (recvlen == 0)
		{
			break;
		}

		writelen = write(fd, buff, recvlen);
		if (writelen < 0)
		{
			print_error("write");
			return writelen;
		}
	}

	return 0;
}

int inet_tcp_receive_file2(int sockfd, const char *filename)
{
	int ret;
	int fd;

	fd = open(filename, O_WRONLY | O_CREAT | O_TRUNC, 0777);
	if (fd < 0)
	{
		print_error("open file %s failed", filename);
		return fd;
	}

	ret = inet_tcp_receive_file1(sockfd, fd);

	close(fd);

	return ret;
}

int inet_tcp_transfer(int src_sockfd, int dest_sockfd, size_t size)
{
	ssize_t rdlen;
	char buff[size];

	rdlen = inet_recv(src_sockfd, buff, sizeof(buff));
	if (rdlen <= 0)
	{
		return rdlen;
	}

	return inet_send(dest_sockfd, buff, rdlen);
}

int inet_get_sockaddr(int sockfd, const char *devname, struct sockaddr_in *sin_addr)
{
	int ret;
	struct ifreq ifr;

	text_copy(ifr.ifr_ifrn.ifrn_name, devname);

	ret = ioctl(sockfd, SIOCGIFADDR, &ifr);
	if (ret < 0)
	{
		print_error("get deivce %s sockaddr", devname);
		return ret;
	}

	*sin_addr = *(struct sockaddr_in *)&ifr.ifr_addr;

	return 0;
}

int inet_get_devname(int sockfd, int index, char *devname)
{
	int ret;
	struct ifreq req;

	req.ifr_ifru.ifru_ivalue = index;
	ret = ioctl(sockfd, SIOCGIFNAME, &req);
	if (ret < 0)
	{
		print_error("get devices name");
		return ret;
	}

	text_copy(devname, req.ifr_ifrn.ifrn_name);

	return 0;
}

const char *cavan_get_server_hostname(void)
{
	const char *hostname;

	hostname = getenv(CAVAN_IP_ENV_NAME);
	if (hostname)
	{
		return hostname;
	}

	return CAVAN_DEFAULT_IP;
}

u16 cavan_get_server_port(u16 default_port)
{
	const char *port;

	port = getenv(CAVAN_PORT_ENV_NAME);
	if (port == NULL)
	{
		return default_port;
	}

	return text2value_unsigned(port, NULL, 10);
}

int inet_tcp_transmit_loop(int src_sockfd, int dest_sockfd)
{
	char buff[1024];
	ssize_t rwlen;

	while (1)
	{
		rwlen = inet_recv(src_sockfd, buff, sizeof(buff));
		if (rwlen < 0)
		{
			pr_red_info("inet_recv");
			return rwlen;
		}

		if (rwlen == 0)
		{
			break;
		}

		buff[rwlen] = 0;
		println("buff = %s", buff);

		rwlen = inet_send(dest_sockfd, buff, rwlen);
		if (rwlen < 0)
		{
			pr_red_info("inet_send");
			return rwlen;
		}
	}

	return 0;
}

int inet_hostname2sockaddr(const char *hostname, struct sockaddr_in *addr)
{
	int ret;
	struct addrinfo *res, *p;
	struct addrinfo nints;

	addr->sin_family = AF_INET;

	if (inet_aton(hostname, &addr->sin_addr))
	{
		return 0;
	}

	memset(&nints, 0, sizeof(nints));
	nints.ai_family = AF_INET;
	nints.ai_socktype = SOCK_STREAM;
	nints.ai_flags = 0;
	ret = getaddrinfo(hostname, NULL, &nints, &res);
	if (ret < 0)
	{
		pr_error_info("getaddrinfo");
		return ret;
	}

	if (res == NULL)
	{
		pr_red_info("res == NULL");
		return -ENOENT;
	}

	for (p = res; p; p = p->ai_next)
	{
		if (p->ai_family == AF_INET)
		{
			res = p;
			break;
		}
	}

	memcpy(addr, res->ai_addr, res->ai_addrlen);
	freeaddrinfo(res);

	return 0;
}

char *network_url_tostring(const struct network_url *url, char *buff, size_t size, char **tail)
{
	int ret;

	if (buff == NULL || size == 0)
	{
		static char static_buff[1024];

		buff = static_buff;
		size = sizeof(static_buff);
	}

	if (url->protocol[0])
	{
		if (url->port[0])
		{
			ret = snprintf(buff, size, "%s://%s:%s", url->protocol, url->hostname, url->port);
		}
		else
		{
			ret = snprintf(buff, size, "%s://%s", url->protocol, url->hostname);
		}
	}
	else if (url->port[0])
	{
		ret = snprintf(buff, size, "%s:%s", url->hostname, url->port);
	}
	else
	{
		ret = snprintf(buff, size, "%s", url->hostname);
	}

	if (tail)
	{
		if (ret > 0)
		{
			*tail = buff + ret;
		}
		else
		{
			*tail = buff;
		}
	}

	return buff;
}

char *network_parse_url(const char *text, struct network_url *url)
{
	char *p = url->memory;
	char *p_end = p + sizeof(url->memory);

	url->port = NULL;
	url->protocol = NULL;
	url->hostname = url->memory;

	while (p < p_end)
	{
		switch (*text)
		{
		case 0 ... 31:
		case ' ':
		case '/':
			*p = 0;

			if (url->port == NULL)
			{
				url->port = p;
			}

			if (url->protocol == NULL)
			{
				url->protocol = p;
			}

			return (char *) text;

		case ':':
			*p = 0;
			text++;

			if (url->protocol == NULL && text_lhcmp("//", text) == 0)
			{
				url->protocol = url->hostname;
				url->hostname = ++p;
				text += 2;
			}
			else if (IS_NUMBER(*text))
			{
				url->port = ++p;
			}
			else
			{
				return NULL;
			}

			break;

		default:
			*p++ = *text++;
		}
	}

	return NULL;
}

char *network_url_build(char *buff, size_t size, const char *protocol, const char *hostname, u16 port, const char *pathname)
{
	char *buff_end = buff + size;

	if (protocol == NULL || protocol[0] == 0)
	{
		protocol = "tcp";
	}

	if (hostname && hostname[0])
	{
		buff += snprintf(buff, buff_end - buff, "%s://%s", protocol, hostname);
	}
	else
	{
		buff += snprintf(buff, buff_end - buff, "%s://", protocol);
	}

	if (port)
	{
		buff += snprintf(buff, buff_end - buff, ":%d", port);
	}

	if (pathname && pathname[0])
	{
		return text_ncopy(buff, pathname, buff_end - buff);
	}

	return buff;
}

const struct network_protocol *network_get_protocol_by_name(const char *name)
{
	const struct network_protocol *p, *p_end;

	if (name == NULL || name[0] == 0)
	{
		return protocols + NETWORK_PROTOCOL_HTTP;
	}

	for (p = protocols, p_end = p + NELEM(protocols); p < p_end; p++)
	{
		if (text_cmp(name, p->name) == 0)
		{
			return p;
		}
	}

	return NULL;
}

const struct network_protocol *network_get_protocol_by_type(network_protocol_type_t type)
{
	if (type < 0 || type >= NELEM(protocols))
	{
		return NULL;
	}

	return protocols + type;
}

const struct network_protocol *network_get_protocol_by_port(u16 port)
{
	const struct network_protocol *p, *p_end;

	for (p = protocols, p_end = p + NELEM(protocols); p < p_end; p++)
	{
		if (p->port == port)
		{
			return p;
		}
	}

	return NULL;
}

int network_get_port_by_url(const struct network_url *url, const struct network_protocol *protocol)
{
	if (url->port[0])
	{
		return text2value_unsigned(url->port, NULL, 10);
	}
	else if (url->protocol[0])
	{
		if (protocol == NULL)
		{
			protocol = network_get_protocol_by_name(url->protocol);
		}

		if (protocol == NULL)
		{
			pr_red_info("unknown protocol %s", url->protocol);
			return -EINVAL;
		}

		return protocol->port;
	}
	else
	{
		return -EINVAL;
	}
}

bool network_url_equals(const struct network_url *url1, const struct network_url *url2)
{
	if (text_cmp(url1->hostname, url2->hostname))
	{
		return false;
	}

	if (text_cmp(url1->protocol, url2->protocol))
	{
		return false;
	}

	return text_cmp(url1->port, url2->port) == 0;
}

int network_create_socket_mac(const char *if_name, int protocol)
{
	int ret;
	int sockfd;
	struct ifreq req;
	struct sockaddr_ll bind_addr;

	sockfd = socket(PF_PACKET, SOCK_RAW, htons(protocol ? protocol : ETH_P_ALL));
	if (sockfd < 0)
	{
		pr_error_info("socket PF_PACKET SOCK_RAW");
		return sockfd;
	}

	if (if_name == NULL || if_name[0] == 0)
	{
		return sockfd;
	}

	strcpy(req.ifr_name, if_name);

	ret = ioctl(sockfd, SIOCGIFINDEX, &req);
	if (ret < 0)
	{
		pr_error_info("ioctl SIOCGIFINDEX");
		goto out_close_socket;
	}

	bind_addr.sll_family = PF_PACKET;
	bind_addr.sll_ifindex = req.ifr_ifindex;
	bind_addr.sll_protocol = htons(ETH_P_ALL);

	ret = bind(sockfd, (struct sockaddr *)&bind_addr, sizeof(bind_addr));
	if (ret < 0)
	{
		pr_error_info("bind");
		goto out_close_socket;
	}

	return sockfd;

out_close_socket:
	close(sockfd);
	return ret;
}

// ============================================================

static void network_client_udp_close(struct network_client *client)
{
	close(client->sockfd);
}

static ssize_t network_client_udp_send(struct network_client *client, const void *buff, size_t size)
{
	return sendto(client->sockfd, buff, size, 0, &client->addr, client->addrlen);
}

static ssize_t network_client_udp_recv(struct network_client *client, void *buff, size_t size)
{
	return recvfrom(client->sockfd, buff, size, 0, &client->addr, &client->addrlen);
}

static int network_client_udp_talk(struct network_client *client)
{
	u32 magic;
	ssize_t rwlen;

	rwlen = network_client_send_message(client, CAVAN_NETWORK_MAGIC);
	if (rwlen < 4)
	{
		pr_red_info("network_client_send_message");
		return rwlen < 0 ? rwlen : -EFAULT;
	}

	rwlen = network_client_recv_message(client, &magic);
	if (rwlen < 4)
	{
		pr_red_info("network_client_recv_message");
		return rwlen < 0 ? rwlen : -EFAULT;
	}

	if (magic != CAVAN_NETWORK_MAGIC)
	{
		pr_red_info("invalid magic = 0x%08x", magic);
		return -EINVAL;
	}

	return 0;
}

static int network_client_udp_common_open(struct network_client *client)
{
	client->send = network_client_udp_send;
	client->recv = network_client_udp_recv;

	return network_client_udp_talk(client);
}

static int network_client_udp_open(struct network_client *client, const char *hostname, u16 port)
{
	int ret;
	int sockfd;

	ret = inet_hostname2sockaddr(hostname, &client->addr_in);
	if (ret < 0)
	{
		pr_red_info("inet_hostname2sockaddr");
		return ret;
	}

	sockfd = inet_socket(SOCK_DGRAM);
	if (sockfd < 0)
	{
		pr_error_info("inet_socket");
		return sockfd;
	}

	client->sockfd = sockfd;
	client->type = NETWORK_CONNECT_UDP;
	client->addrlen = sizeof(client->addr_in);
	client->addr_in.sin_port = htons(port);
	client->close = network_client_udp_close;

	ret = network_client_udp_common_open(client);
	if (ret < 0)
	{
		pr_red_info("network_client_udp_common_open");
		goto out_client_close;
	}

	return 0;

out_client_close:
	client->close(client);
	return ret;
}

static void network_client_tcp_close(struct network_client *client)
{
	inet_close_tcp_socket(client->sockfd);
}

static ssize_t network_client_tcp_send(struct network_client *client, const void *buff, size_t size)
{
	return send(client->sockfd, buff, size, 0);
}

static ssize_t network_client_tcp_recv(struct network_client *client, void *buff, size_t size)
{
	return recv(client->sockfd, buff, size, 0);
}

static int network_client_tcp_open(struct network_client *client, const char *hostname, u16 port)
{
	int sockfd;

	sockfd = inet_create_tcp_link2(hostname, port);
	if (sockfd < 0)
	{
		pr_red_info("inet_socket");
		return sockfd;
	}

	client->sockfd = sockfd;
	client->type = NETWORK_CONNECT_TCP;
	client->close = network_client_tcp_close;
	client->send = network_client_tcp_send;
	client->recv = network_client_tcp_recv;

	return 0;
}

static void network_client_unix_udp_close(struct network_client *client)
{
	const char *pathname;

	network_client_udp_close(client);

	pathname = network_client_get_data(client);
	unlink(pathname);
	free((void *) pathname);
}

static int network_create_unix_udp_client(struct network_client *client)
{
	int fd;
	int ret;
	char pathname[512];

	text_ncopy(pathname, CAVAN_NETWORK_TEMP_PATH "/socket-client-XXXXXX", sizeof(pathname));

	fd = mkstemp(pathname);
	if (fd < 0)
	{
		pr_error_info("mkstemp `%s'", pathname);
		return fd;
	}

	close(fd);

	ret = unix_create_service(SOCK_DGRAM, pathname);
	if (ret < 0)
	{
		pr_red_info("unix_create_service");
		goto out_unlink_pathname;
	}

	client->sockfd = ret;
	client->type = NETWORK_CONNECT_UNIX_UDP;
	client->addrlen = sizeof(client->addr_un);
	client->close = network_client_unix_udp_close;
	network_client_set_data(client, strdup(pathname));

	return 0;

out_unlink_pathname:
	unlink(pathname);
	return ret;
}

static int network_client_unix_tcp_open(struct network_client *client, const char *hostname)
{
	int sockfd;

	sockfd = unix_create_tcp_link(hostname, 0);
	if (sockfd < 0)
	{
		pr_error_info("unix_socket");
		return sockfd;
	}

	client->sockfd = sockfd;
	client->type = NETWORK_CONNECT_UNIX_TCP;
	client->close = network_client_tcp_close;
	client->send = network_client_tcp_send;
	client->recv = network_client_tcp_recv;

	return 0;
}

static int network_client_unix_udp_open(struct network_client *client, const char *hostname)
{
	int ret;

	ret = network_create_unix_udp_client(client);
	if (ret < 0)
	{
		pr_error_info("inet_socket");
		return ret;
	}

	unix_sockaddr_init(&client->addr_un, hostname);

	ret = network_client_udp_common_open(client);
	if (ret < 0)
	{
		pr_red_info("network_client_udp_common_open");
		goto out_client_close;
	}


	return 0;

out_client_close:
	client->close(client);
	return ret;
}


static int network_client_adb_open(struct network_client *client, const char *hostname, u16 port)
{
	int sockfd;

	sockfd = adb_create_tcp_link(hostname, 0, port);
	if (sockfd < 0)
	{
		pr_red_info("inet_socket");
		return sockfd;
	}

	client->sockfd = sockfd;
	client->type = NETWORK_CONNECT_ADB;
	client->close = network_client_tcp_close;
	client->send = network_client_tcp_send;
	client->recv = network_client_tcp_recv;

	return 0;
}

static int network_client_icmp_open(struct network_client *client, const char *hostname)
{
	int ret;
	int sockfd;

	ret = inet_hostname2sockaddr(hostname, &client->addr_in);
	if (ret < 0)
	{
		pr_red_info("inet_hostname2sockaddr");
		return ret;
	}

	sockfd = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
	if (sockfd < 0)
	{
		pr_error_info("inet_socket");
		return sockfd;
	}

	client->sockfd = sockfd;
	client->type = NETWORK_CONNECT_ICMP;
	client->close = network_client_udp_close;
	client->send = network_client_udp_send;
	client->recv = network_client_udp_recv;

	return 0;
}

static int network_client_ip_open(struct network_client *client, const char *hostname)
{
	int ret;
	int sockfd;

	ret = inet_hostname2sockaddr(hostname, &client->addr_in);
	if (ret < 0)
	{
		pr_red_info("inet_hostname2sockaddr");
		return ret;
	}

	sockfd = socket(AF_INET, SOCK_RAW, IPPROTO_RAW);
	if (sockfd < 0)
	{
		pr_error_info("inet_socket");
		return sockfd;
	}

	client->sockfd = sockfd;
	client->type = NETWORK_CONNECT_IP;
	client->close = network_client_udp_close;
	client->send = network_client_udp_send;
	client->recv = network_client_udp_recv;

	return 0;
}

static int network_client_mac_open(struct network_client *client, const char *if_name)
{
	int sockfd;

	sockfd = network_create_socket_mac(if_name, 0);
	if (sockfd < 0)
	{
		pr_red_info("inet_socket");
		return sockfd;
	}

	client->sockfd = sockfd;
	client->type = NETWORK_CONNECT_MAC;
	client->close = network_client_udp_close;
	client->send = network_client_tcp_send;
	client->recv = network_client_tcp_recv;

	return 0;
}

network_connect_type_t network_connect_type_parse(const char *name, const char *name2)
{
	switch (name[0])
	{
	case 't':
		if (text_cmp(name + 1, "cp") == 0)
		{
			return NETWORK_CONNECT_TCP;
		}
		break;

	case 'u':
		if (text_cmp(name + 1, "dp") == 0)
		{
			return NETWORK_CONNECT_UDP;
		}
		else if (text_lhcmp("nix", name + 1) == 0)
		{
			if (name[4] == '-')
			{
				name2 = name + 5;
			}
			else if (name[4] == 0 && name2[0] == 0)
			{
				return NETWORK_CONNECT_UNIX_TCP;
			}

			if (text_cmp(name2, "tcp") == 0)
			{
				return NETWORK_CONNECT_UNIX_TCP;
			}
			else if (text_cmp(name2, "udp") == 0)
			{
				return NETWORK_CONNECT_UNIX_UDP;
			}
		}
		break;

	case 'a':
		if (text_cmp(name + 1, "db") == 0)
		{
			return NETWORK_CONNECT_ADB;
		}
		break;

	case 'i':
		if (text_cmp(name + 1, "p") == 0)
		{
			return NETWORK_CONNECT_IP;
		}
		else if (text_cmp(name + 1, "cmp") == 0)
		{
			return NETWORK_CONNECT_ICMP;
		}
		break;

	case 'm':
		if (text_cmp(name + 1, "ac") == 0)
		{
			return NETWORK_CONNECT_MAC;
		}
		break;
	}

	return NETWORK_CONNECT_UNKNOWN;
}

const char *network_connect_type_tostring(network_connect_type_t type)
{
	switch (type)
	{
	case NETWORK_CONNECT_TCP:
		return "tcp";
	case NETWORK_CONNECT_UDP:
		return "udp";
	case NETWORK_CONNECT_ADB:
		return "adb";
	case NETWORK_CONNECT_ICMP:
		return "icmp";
	case NETWORK_CONNECT_IP:
		return "ip";
	case NETWORK_CONNECT_MAC:
		return "mac";
	case NETWORK_CONNECT_UNIX_TCP:
		return "unix-tcp";
	case NETWORK_CONNECT_UNIX_UDP:
		return "unix-udp";
	default:
		return "unknown";
	}
}

int network_client_open(struct network_client *client, network_connect_type_t type, const char *hostname, u16 port, const char *pathname)
{
	LOGD("network connect type = %s\n", network_connect_type_tostring(type));

	switch (type)
	{
	case NETWORK_CONNECT_TCP:
		return network_client_tcp_open(client, hostname, port);

	case NETWORK_CONNECT_UDP:
		return network_client_udp_open(client, hostname, port);

	case NETWORK_CONNECT_UNIX_TCP:
		return network_client_unix_tcp_open(client, pathname);

	case NETWORK_CONNECT_UNIX_UDP:
		return network_client_unix_udp_open(client, pathname);

	case NETWORK_CONNECT_ADB:
		return network_client_adb_open(client, hostname, port);

	case NETWORK_CONNECT_ICMP:
		return network_client_icmp_open(client, hostname);

	case NETWORK_CONNECT_IP:
		return network_client_ip_open(client, hostname);

	case NETWORK_CONNECT_MAC:
		return network_client_mac_open(client, hostname);

	default:
		pr_red_info("unknown connect type");
		return -EINVAL;
	}
}

int network_client_open2(struct network_client *client, const char *_url)
{
	u16 port;
	const char *pathname;
	struct network_url url;
	network_connect_type_t type;

	if (_url == NULL)
	{
		pr_red_info("_url == NULL");
		return -EINVAL;
	}

	LOGD("URL = %s\n", _url);

	pathname = network_parse_url(_url, &url);
	if (pathname == NULL)
	{
		pr_red_info("network_parse_url");
		return -EFAULT;
	}

	port = text2value_unsigned(url.port, NULL, 10);
	type = network_connect_type_parse(url.protocol, url.hostname);

	return network_client_open(client, type, url.hostname, port, pathname);
}

void network_client_close(struct network_client *client)
{
	if (client->close)
	{
		client->close(client);
	}
	else
	{
		close(client->sockfd);
	}
}

ssize_t network_client_fill_buff(struct network_client *client, char *buff, size_t size)
{
	ssize_t rdlen;
	const char *buff_end = buff + size;

	for (buff_end = buff + size; buff < buff_end; buff += rdlen)
	{
		rdlen = client->recv(client, buff, buff_end - buff);
		if (rdlen <= 0)
		{
			return -EFAULT;
		}
	}

	return size;
}

ssize_t network_client_send_buff(struct network_client *client, const char *buff, size_t size)
{
	ssize_t wrlen;
	const char *buff_end = buff + size;

	for (buff_end = buff + size; buff < buff_end; buff += wrlen)
	{
		wrlen = client->send(client, buff, buff_end - buff);
		if (wrlen <= 0)
		{
			return -EFAULT;
		}
	}

	return size;
}

ssize_t network_client_recv_file(struct network_client *client, int fd, size_t size)
{
	char buff[1024];
	ssize_t rdlen;
	size_t size_bak = size;
	struct progress_bar bar;

	progress_bar_init(&bar, size);

	while (size)
	{
		rdlen = client->recv(client, buff, sizeof(buff));
		if (rdlen <= 0 || ffile_write(fd, buff, rdlen) < rdlen)
		{
			return -EFAULT;
		}

		size -= rdlen;
		progress_bar_add(&bar, rdlen);
	}

	progress_bar_finish(&bar);

	return size_bak;
}

ssize_t network_client_send_file(struct network_client *client, int fd, size_t size)
{
	char buff[1024];
	ssize_t rdlen;
	size_t size_bak = size;
	struct progress_bar bar;

	progress_bar_init(&bar, size);

	while (size)
	{
		rdlen = ffile_read(fd, buff, sizeof(buff));
		if (rdlen <= 0 || network_client_send_buff(client, buff, rdlen) < rdlen)
		{
			return -EFAULT;
		}

		size -= rdlen;
		progress_bar_add(&bar, rdlen);
	}

	progress_bar_finish(&bar);

	return size_bak;
}

int network_client_exec_redirect(struct network_client *client, int ttyin, int ttyout)
{
	int ret;
	ssize_t rdlen;
	char buff[1024];
	struct pollfd pfds[2];

	pfds[0].events = POLLIN;
	pfds[0].fd = client->sockfd;

	pfds[1].events = POLLIN;
	pfds[1].fd = ttyin;

	while (1)
	{
		ret = poll(pfds, NELEM(pfds), -1);
		if (ret <= 0)
		{
			return -ETIMEDOUT;
		}

		if (pfds[0].revents)
		{
			rdlen = client->recv(client, buff, sizeof(buff));
			if (rdlen <= 0 || write(ttyout, buff, rdlen) < rdlen)
			{
				break;
			}

			fsync(ttyout);
		}

		if (pfds[1].revents)
		{
			rdlen = read(ttyin, buff, sizeof(buff));
			if (rdlen <= 0 || network_client_send_buff(client, buff, rdlen) < rdlen)
			{
				break;
			}

			fsync(client->sockfd);
		}
	}

	return 0;
}

int network_client_exec_main(struct network_client *client, const char *command, int lines, int columns)
{
	int ret;
	int ttyfd;
	pid_t pid;

	ttyfd = cavan_exec_redirect_stdio_popen(command, lines, columns, &pid);
	if (ttyfd < 0)
	{
		pr_red_info("cavan_exec_redirect_stdio_popen");
		return ttyfd;
	}

	ret = network_client_exec_redirect(client, ttyfd, ttyfd);
	if (ret < 0)
	{
		pr_red_info("tcp_dd_exec_redirect_loop");
		goto out_close_ttyfd;
	}

	ret = cavan_exec_waitpid(pid);

out_close_ttyfd:
	close(ttyfd);
	return ret;
}

// ============================================================

static void network_service_udp_close(struct network_service *service)
{
	close(service->sockfd);
}

static int network_service_udp_talk(struct network_service *service, struct network_client *client)
{
	u32 magic;
	int rwlen;

	rwlen = recvfrom(service->sockfd, &magic, sizeof(magic), 0, &client->addr, &client->addrlen);
	if (rwlen < 4)
	{
		pr_error_info("recvfrom");
		return rwlen < 0 ? rwlen : -EFAULT;
	}

	LOGD("magic = 0x%08x\n", magic);

	if (magic != CAVAN_NETWORK_MAGIC)
	{
		pr_red_info("invalid magic = 0x%08x", magic);
		return -EINVAL;
	}

	if (service->type == NETWORK_CONNECT_UNIX_UDP)
	{
		int ret;

		ret = network_create_unix_udp_client(client);
		if (ret < 0)
		{
			pr_red_info("network_create_unix_udp_client");
			return ret;
		}
	}
	else
	{
		client->sockfd = inet_socket(SOCK_DGRAM);
		if (client->sockfd < 0)
		{
			pr_error_info("inet_socket");
			return client->sockfd;
		}
	}

	rwlen = network_client_send_message(client, CAVAN_NETWORK_MAGIC);
	if (rwlen < 4)
	{
		pr_error_info("network_client_send_message");

		if (rwlen >= 0)
		{
			rwlen = -EFAULT;
		}

		goto out_client_close;
	}

	return 0;

out_client_close:
	client->close(client);
	return rwlen;
}

static int network_service_udp_accept(struct network_service *service, struct network_client *client)
{
	ssize_t ret;

	if (service->type == NETWORK_CONNECT_UNIX_UDP)
	{
		client->addrlen = sizeof(client->addr_un);
	}
	else
	{
		client->addrlen = sizeof(client->addr_in);
	}

	client->send = network_client_udp_send;
	client->recv = network_client_udp_recv;
	client->close = network_client_udp_close;

	network_service_lock(service);
	ret = network_service_udp_talk(service, client);
	network_service_unlock(service);

	return ret;
}

static int network_service_udp_open(struct network_service *service, u16 port)
{
	service->sockfd = inet_create_udp_service(port);
	if (service->sockfd < 0)
	{
		pr_red_info("inet_create_udp_service");
		return service->sockfd;
	}

	service->type = NETWORK_CONNECT_UDP;
	service->accept = network_service_udp_accept;
	service->close = network_service_udp_close;

	return 0;
}

static void network_service_tcp_close(struct network_service *service)
{
	inet_close_tcp_socket(service->sockfd);
}

static int network_service_tcp_accept(struct network_service *service, struct network_client *client)
{
	client->addrlen = sizeof(client->addr);

	client->sockfd = accept(service->sockfd, &client->addr, &client->addrlen);
	if (client->sockfd < 0)
	{
		pr_error_info("accept");
		return client->sockfd;
	}

	client->close = network_client_tcp_close;
	client->send = network_client_tcp_send;
	client->recv = network_client_tcp_recv;

	return 0;
}

static int network_service_tcp_open(struct network_service *service, u16 port)
{
	service->sockfd = inet_create_tcp_service(port);
	if (service->sockfd < 0)
	{
		pr_red_info("inet_create_tcp_service");
		return service->sockfd;
	}

	service->type = NETWORK_CONNECT_TCP;
	service->accept = network_service_tcp_accept;
	service->close = network_service_tcp_close;

	return 0;
}

static int network_service_unix_tcp_open(struct network_service *service, const char *pathname)
{
	service->sockfd = unix_create_tcp_service(pathname);
	if (service->sockfd < 0)
	{
		pr_red_info("inet_create_tcp_service");
		return service->sockfd;
	}

	service->type = NETWORK_CONNECT_UNIX_TCP;
	service->accept = network_service_tcp_accept;
	service->close = network_service_tcp_close;

	return 0;
}

static int network_service_unix_udp_open(struct network_service *service, const char *pathname)
{
	service->sockfd = unix_create_udp_service(pathname);
	if (service->sockfd < 0)
	{
		pr_red_info("inet_create_tcp_service");
		return service->sockfd;
	}

	service->type = NETWORK_CONNECT_UNIX_UDP;
	service->accept = network_service_udp_accept;
	service->close = network_service_udp_close;

	return 0;
}

int network_service_open(struct network_service *service, network_connect_type_t type, u16 port, const char *pathname)
{
	int ret;

	ret = mkdir_hierarchy(CAVAN_NETWORK_TEMP_PATH, 0777);
	if (ret < 0)
	{
		pr_red_info("mkdir_hierarchy %s", CAVAN_NETWORK_TEMP_PATH);
		return ret;
	}

	LOGD("network connect type = %s\n", network_connect_type_tostring(type));

	ret = pthread_mutex_init(&service->lock, NULL);
	if (ret < 0)
	{
		pr_error_info("pthread_mutex_init");
		return ret;
	}

	switch (type)
	{
	case NETWORK_CONNECT_TCP:
	case NETWORK_CONNECT_ADB:
		return network_service_tcp_open(service, port);

	case NETWORK_CONNECT_UDP:
		return network_service_udp_open(service, port);

	case NETWORK_CONNECT_UNIX_TCP:
		return network_service_unix_tcp_open(service, pathname);

	case NETWORK_CONNECT_UNIX_UDP:
		return network_service_unix_udp_open(service, pathname);

	default:
		pr_red_info("unsupport connect type %d", type);
		ret = -EINVAL;
	}

	pthread_mutex_destroy(&service->lock);

	return ret;
}

int network_service_open2(struct network_service *service, const char *_url)
{
	u16 port;
	const char *pathname;
	struct network_url url;
	network_connect_type_t type;

	if (_url == NULL)
	{
		pr_red_info("_url == NULL");
		return -EINVAL;
	}

	pathname = network_parse_url(_url, &url);
	if (pathname == NULL)
	{
		pr_red_info("network_parse_url");
		return -EFAULT;
	}

	port = text2value_unsigned(url.port, NULL, 10);
	type = network_connect_type_parse(url.protocol, url.hostname);

	return network_service_open(service, type, port, pathname);
}

void network_service_close(struct network_service *service)
{
	if (service->close)
	{
		service->close(service);
	}
	else
	{
		close(service->sockfd);
	}

	pthread_mutex_destroy(&service->lock);

	remove_directory(CAVAN_NETWORK_TEMP_PATH);
}
