#include <cavan.h>
#include <sys/stat.h>
#include <cavan/crc.h>
#include <cavan/file.h>
#include <cavan/progress.h>
#include <cavan/text.h>
#include <cavan/parser.h>
#include <linux/loop.h>
#include <cavan/device.h>
#include <cavan/memory.h>

#define CAVAN_FILE_DEBUG				0
#define CAVAN_FILE_PROXY_DEBUG			0

#define MAX_BUFF_LEN					KB(4)
#define MIN_FILE_SIZE					MB(1)
#define CONFIG_ERROR_IF_COPY_REMAIN		0

#ifdef CONFIG_ANDROID_NDK
extern char *mkdtemp (char *__template);
#endif

int file_join(const char *dest_file, char *src_files[], int count)
{
	int ret;
	int i;
	int src_fd, dest_fd;
	struct stat st;
	struct progress_bar bar;

	dest_fd = open(dest_file, O_WRONLY | O_CREAT | O_SYNC | O_TRUNC | O_BINARY, 0777);
	if (dest_fd < 0) {
		pr_error_info("open \"%s\"", dest_file);
		return -1;
	}

	for (i = 0; i < count; i++) {
		println("\"%s\" [join]-> \"%s\"", src_files[i], dest_file);

		src_fd = open(src_files[i], O_RDONLY | O_BINARY);
		if (src_fd < 0) {
			ret = -1;
			pr_error_info("open \"%s\"", src_files[i]);
			goto out_close_dest;
		}

		ret = fstat(src_fd, &st);
		if (ret < 0) {
			pr_error_info("fstat \"%s\"", src_files[i]);
			goto out_close_src;
		}

		progress_bar_init(&bar, st.st_size, 0, PROGRESS_BAR_TYPE_DATA);

		while (1) {
			ssize_t rdlen, wrlen;
			char buff[MAX_BUFF_LEN];

			rdlen = ffile_read(src_fd, buff, sizeof(buff));
			if (rdlen <= 0) {
				if (rdlen == 0) {
					break;
				}

				ret = rdlen;
				pr_error_info("ffile_read \"%s\"", src_files[i]);
				goto out_close_src;
			}

			wrlen = ffile_write(dest_fd, buff, rdlen);
			if (wrlen != rdlen) {
				ret = wrlen < 0 ? wrlen : -EFAULT;
				pr_error_info("ffile_write \"%s\"", dest_file);
				goto out_close_src;
			}

			progress_bar_add(&bar, ret);
		}

		close(src_fd);

		progress_bar_finish(&bar);
	}

	ret = 0;
	goto out_close_dest;

out_close_src:
	close(src_fd);
out_close_dest:
	close(dest_fd);

	return ret;
}

int file_split(const char *file_name, const char *dest_dir, size_t size, int count)
{
	int ret;
	int i;
	int src_fd, dest_fd;
	size_t remain_size;
	struct stat st;
	char dest_pathname[1024], *dest_filename;

	if (size == 0 && count == 0) {
		pr_red_info("Please give the size or count");
		ERROR_RETURN(EINVAL);
	}

	ret = mkdir_hierarchy(dest_dir, 0777);
	if (ret < 0) {
		pr_error_info("Create directory `%s' failed", dest_dir);
		return ret;
	}

	src_fd = open(file_name, O_RDONLY | O_BINARY);
	if (src_fd < 0) {
		pr_error_info("open \"%s\"", file_name);
		return -1;
	}

	ret = fstat(src_fd, &st);
	if (ret < 0) {
		pr_error_info("fstat \"%s\"", file_name);
		goto out_close_src;
	}

	remain_size = st.st_size;

	if (size == 0) {
		size = (remain_size + count - 1) / count;
		if (size == 0) {
			ret = -EINVAL;
			pr_red_info("count to large");
			goto out_close_src;
		}
	}

	dest_filename = cavan_path_cat(dest_pathname, sizeof(dest_pathname), dest_dir, cavan_path_basename_simple(file_name), false);

	for (i = 1; remain_size; i++) {
		ssize_t cpylen;

		sprintf(dest_filename, "-part%02d", i);
		pr_info("%s => %s", file_name, dest_pathname);

		dest_fd = open(dest_pathname, O_WRONLY | O_CREAT | O_TRUNC | O_BINARY, 0777);
		if (dest_fd < 0) {
			pr_error_info("open file `%s' failed", dest_pathname);
			goto out_close_src;
		}

		cpylen = ffile_ncopy(src_fd, dest_fd, remain_size > size ? size : remain_size);
		close(dest_fd);
		if (cpylen < 0) {
			ret = cpylen;
			pr_red_info("ffile_ncopy");
			goto out_close_src;
		}

		remain_size -= cpylen;
	}

	ret = 0;

out_close_src:
	close(src_fd);

	return ret;
}

ssize_t ffile_copy_simple(int src_fd, int dest_fd)
{
	ssize_t cpylen = 0;

	while (1) {
		ssize_t rdlen, wrlen;
		char buff[MAX_BUFF_LEN];

		rdlen = ffile_read(src_fd, buff, sizeof(buff));
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		wrlen = ffile_write(dest_fd, buff, rdlen);
		if (wrlen != rdlen) {
			pr_error_info("ffile_write");
			return wrlen < 0 ? wrlen : -EFAULT;
		}

		cpylen += wrlen;
	}

	return cpylen;
}

ssize_t ffile_copy(int src_fd, int dest_fd)
{
	ssize_t cpylen = 0;
	struct progress_bar bar;

	progress_bar_init(&bar, ffile_get_size(src_fd), 0, PROGRESS_BAR_TYPE_DATA);

	while (1) {
		ssize_t rdlen, wrlen;
		char buff[MAX_BUFF_LEN];

		rdlen = ffile_read(src_fd, buff, sizeof(buff));
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		wrlen = ffile_write(dest_fd, buff, rdlen);
		if (wrlen != rdlen) {
			pr_error_info("ffile_write");
			return wrlen < 0 ? wrlen : -EFAULT;
		}

		cpylen += wrlen;
		progress_bar_add(&bar, wrlen);
	}

	progress_bar_finish(&bar);

	return cpylen;
}

ssize_t file_append(const char *file_src, const char *file_dest)
{
	ssize_t res;
	int fd_src, fd_dest;

	fd_src = open(file_src, READ_FLAGS);
	if (fd_src < 0) {
		pr_error_info("open \"%s\"", file_src);
		return fd_src;
	}

	fd_dest = open(file_dest, WRITE_FLAGS | O_APPEND, 0777);
	if (fd_dest < 0) {
		res = fd_dest;
		pr_error_info("open \"%s\"", file_dest);
		goto out_close_fd_src;
	}

	res = ffile_copy(fd_src, fd_dest);

	close(fd_dest);
out_close_fd_src:
	close(fd_src);

	return res;
}

int file_open_rw_ro(const char *pathname, int flags)
{
	int fd;

	fd = open(pathname, O_RDWR | O_BINARY | flags);
	if (fd >= 0) {
		return fd;
	}

#if CAVAN_FILE_DEBUG
	pr_warn_info("rw open file \"%s\" faild, retry use ro", pathname);
#endif

	fd = open(pathname, O_RDONLY | flags);

#if CAVAN_FILE_DEBUG
	if (fd < 0) {
		pr_error_info("open file \"%s\"", pathname);
	}
#endif

	return fd;
}

static int open_files(const char *src_file, const char *dest_file, int *src_fd, int *dest_fd, int flags)
{
	int ret;
	struct stat st;

	*src_fd = open(src_file, O_RDONLY);
	if (*src_fd < 0) {
		pr_error_info("open \"%s\"", src_file);
		return *src_fd;
	}

	ret = fstat(*src_fd, &st);
	if (ret < 0) {
		pr_error_info("fstat \"%s\"", src_file);
		goto out_close_src_fd;
	}

	*dest_fd = open(dest_file, O_WRONLY | O_CREAT | O_TRUNC | O_BINARY | flags, st.st_mode);
	if (*dest_fd < 0) {
		ret = *dest_fd;
		pr_error_info("open \"%s\"", dest_file);
		goto out_close_src_fd;
	}

	return 0;

out_close_src_fd:
	close(*src_fd);

	return ret;
}

ssize_t file_copy(const char *src_file, const char *dest_file, int flags)
{
	int ret;
	ssize_t cpylen;
	int src_fd, dest_fd;

	ret = open_files(src_file, dest_file, &src_fd, &dest_fd, flags);
	if (ret < 0) {
#if CAVAN_FILE_DEBUG
		pr_error_info("open_files");
#endif
		return ret;
	}

	cpylen = ffile_copy(src_fd, dest_fd);

	close(src_fd);
	close(dest_fd);

	return cpylen;
}

ssize_t file_copy2(int src_fd, const char *dest_file, int flags, mode_t mode)
{
	int dest_fd;
	ssize_t cpylen;

	dest_fd = open(dest_file, flags, mode);
	if (dest_fd < 0) {
#if CAVAN_FILE_DEBUG
		pr_error_info("open file `%s'", dest_file);
#endif
		return dest_fd;
	}

	cpylen = ffile_copy(src_fd, dest_fd);

	close(dest_fd);

	return cpylen;
}

ssize_t file_copy3(const char *src_file, int dest_fd)
{
	int src_fd;
	ssize_t cpylen;

	src_fd = open(src_file, O_RDONLY);
	if (src_fd < 0) {
#if CAVAN_FILE_DEBUG
		pr_error_info("open file `%s'", src_file);
#endif
		return src_fd;
	}

	cpylen = ffile_copy(src_fd, dest_fd);

	close(src_fd);

	return cpylen;
}

size64_t ffile_get_size(int fd)
{
	int ret;
	struct stat st;

	ret = fstat(fd, &st);
	if (ret < 0) {
		pr_error_info("fstat");
		return 0;
	}

	return st.st_size;
}

size64_t file_get_size(const char *filepath)
{
	int ret;
	struct stat st;

	ret = file_stat2(filepath, &st);
	if (ret < 0) {
		pr_error_info("file_stat \"%s\"", filepath);
		return 0;
	}

	return st.st_size;
}

int mkdir_all(const char *pathname)
{
	int ret;
	char dir_name[512];

	if (file_test(pathname, "d") == 0) {
		return 0;
	}

	cavan_path_dirname_base(dir_name, pathname);
	ret = mkdir_all(dir_name);
	if (ret < 0) {
		pr_err_info("create directory \"%s\" failed", dir_name);
		return ret;
	}

	return mkdir(pathname, 0777);
}

int mkdir_hierarchy_length(const char *pathname, size_t length, mode_t mode)
{
	const char *end = pathname + length;
	char pathname_rw[length], *p = pathname_rw;

	if (pathname < end && *pathname == '/') {
		*p++ = '/';
	}

	while (1) {
		int ret;

		while (1) {
			if (pathname >= end) {
				return 0;
			}

			if (*pathname != '/') {
				break;
			}

			pathname++;
		}

		while (pathname < end && *pathname != '/') {
			*p++ = *pathname++;
		}

		*p = 0;

		ret = mkdir(pathname_rw, mode);
		if (ret < 0 && errno != EEXIST) {
			return ret;
		}

		*p++ = '/';
	}

	return 0;
}

int mkdir_parent_hierarchy(const char *pathname, mode_t mode)
{
	const char *p = pathname + strlen(pathname) - 1;

	while (p > pathname && *p == '/') {
		p--;
	}

	while (p > pathname && *p != '/') {
		p--;
	}

	if (p == pathname) {
		return 0;
	}

	return mkdir_hierarchy_length(pathname, p - pathname, mode);
}

int mkdir_hierarchy2(char *pathname, mode_t mode)
{
	char *p = pathname;

	while (1) {
		char c;
		int ret;

		while (*p == '/') {
			p++;
		}

		if (*p == 0) {
			break;
		}

		while (*p && *p != '/') {
			p++;
		}

		c = *p;
		*p = 0;

		ret = mkdir(pathname, mode);
		if (ret < 0 && errno != EEXIST) {
			return ret;
		}

		*p = c;
	}

	return 0;
}

int mkdir_hierarchy3(const char *pathname, mode_t mode)
{
	int ret;
	char *text = strdup(pathname);

	if (text == NULL) {
		pr_err_info("strdup");
		return -ENOMEM;
	}

	ret = mkdir_hierarchy2(text, mode);
	free(text);

	return ret;
}

int mkdir_parent_hierarchy2(char *pathname, mode_t mode)
{
	int ret;
	char *p = pathname + strlen(pathname) - 1;

	while (p > pathname && *p == '/') {
		p--;
	}

	while (p > pathname && *p != '/') {
		p--;
	}

	if (p == pathname) {
		return 0;
	}

	*p = 0;
	ret = mkdir_hierarchy2(pathname, mode);
	*p = '/';

	return ret;
}

int file_create_open(const char *pathname, int flags, mode_t mode)
{
	int ret;

	ret = open(pathname, O_CREAT | O_BINARY | flags, mode);
	if (ret >= 0) {
		return ret;
	}

	ret = mkdir_hierarchy(cavan_path_dirname(pathname), mode);
	if (ret < 0) {
		return ret;
	}

	return open(pathname, O_CREAT | O_BINARY | flags, mode);
}

int ffile_ncopy_simple(int src_fd, int dest_fd, size64_t size)
{
	while (size) {
		ssize_t rdlen, wrlen;
		char buff[MAX_BUFF_LEN];

		rdlen = ffile_read(src_fd, buff, size > sizeof(buff) ? sizeof(buff) : size);
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		wrlen = ffile_write(dest_fd, buff, rdlen);
		if (wrlen != rdlen) {
			pr_error_info("ffile_write");
			return wrlen < 0 ? wrlen : -EFAULT;
		}

		size -= wrlen;
	}

#if CONFIG_ERROR_IF_COPY_REMAIN
	if (size) {
#if __WORDSIZE == 64
		pr_red_info("size = %ld != 0", size);
#else
		pr_red_info("size = %d != 0", size);
#endif

		return -EINVAL;
	}

	return 0;
#else
	return 0;
#endif
}

int ffile_ncopy(int src_fd, int dest_fd, size64_t size)
{
	struct progress_bar bar;

	if (size < MIN_FILE_SIZE) {
		return ffile_ncopy_simple(src_fd, dest_fd, size);
	}

	progress_bar_init(&bar, size, 0, PROGRESS_BAR_TYPE_DATA);

	while (size) {
		ssize_t rdlen, wrlen;
		char buff[MAX_BUFF_LEN];

		rdlen = ffile_read(src_fd, buff, size < sizeof(buff) ? size : sizeof(buff));
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		wrlen = ffile_write(dest_fd, buff, rdlen);
		if (wrlen != rdlen) {
			pr_error_info("ffile_write");
			return wrlen < 0 ? wrlen : -EFAULT;
		}

		progress_bar_add(&bar, wrlen);
		size -= wrlen;
	}

	progress_bar_finish(&bar);

#if CONFIG_ERROR_IF_COPY_REMAIN
	if (size) {
#if __WORDSIZE == 64
		pr_red_info("size = %ld != 0", size);
#else
		pr_red_info("size = %d != 0", size);
#endif

		return -EINVAL;
	}

	return 0;
#else
	return 0;
#endif
}

int file_ncopy(const char *src_file, const char *dest_file, size64_t size, int flags)
{
	int ret;
	int src_fd, dest_fd;

	ret = open_files(src_file, dest_file, &src_fd, &dest_fd, flags);
	if (ret < 0) {
		pr_red_info("open_files");
		return ret;
	}

	ret = ffile_ncopy(src_fd, dest_fd, size);

	close(src_fd);
	close(dest_fd);

	return ret;
}

int file_ncopy2(int src_fd, const char *dest_file, size64_t size, int flags, mode_t mode)
{
	int ret;
	int dest_fd;

	dest_fd = open(dest_file, flags, mode);
	if (dest_fd < 0) {
		pr_error_info("open file `%s'", dest_file);
		return dest_fd;
	}

	ret = ffile_ncopy(src_fd, dest_fd, size);

	close(dest_fd);

	return ret;
}

int vtry_to_open(int flags, va_list ap)
{
	while (1) {
		int fd;
		const char *filename;

		filename = va_arg(ap, const char *);
		if (filename == NULL) {
			return -ENOENT;
		}

		println("open file \"%s\"", filename);

		fd = open(filename, flags);
		if (fd >= 0) {
			return fd;
		}
	}

	return -1;
}

int try_to_open(int flags, ...)
{
	int ret;
	va_list ap;

	va_start(ap, flags);
	ret = vtry_to_open(flags, ap);
	va_end(ap);

	return ret;
}

ssize_t ffile_read(int fd, void *buff, size_t size)
{
#if 0
	void *buff_bak = buff, *buff_end = (char *) buff + size;

	while (buff < buff_end) {
		ssize_t rdlen = read(fd, buff, (char *) buff_end - (char *) buff);

		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			return rdlen;
		}

		buff = (char *) buff + rdlen;
	}

	return (char *) buff - (char *) buff_bak;
#else
	int retry = 0;

	while (1) {
		ssize_t rdlen;

		rdlen = read(fd, buff, size);
		if (likely(rdlen >= 0 || ERRNO_NOT_RETRY())) {
			return rdlen;
		}

		if (++retry > 10) {
			break;
		}

		msleep(100);
	}

	return -EFAULT;
#endif
}

ssize_t ffile_write(int fd, const void *buff, size_t size)
{
#if 0
	const void *buff_bak = buff, *buff_end = (char *) buff + size;

	while (buff < buff_end) {
		ssize_t wrlen = write(fd, buff, (char *) buff_end - (char *) buff);

		if (wrlen < 0) {
			return wrlen;
		}

#if 0
		if (wrlen == 0) {
			break;
		}
#endif

		buff = (char *) buff + wrlen;
	}

	return (char *) buff - (char *) buff_bak;
#else
	int retry = 0;
	size_t size_bak = size;

	while (1) {
		ssize_t wrlen;

		wrlen = write(fd, buff, size);
		if (likely(wrlen >= (ssize_t) size)) {
			break;
		}

		if (wrlen > 0) {
			retry = 0;
			size -= wrlen;
			buff = ADDR_ADD(buff, wrlen);
		} else {
			if (wrlen < 0) {
				if (ERRNO_NOT_RETRY()) {
					return wrlen;
				}

				msleep(100);
			}

			if (++retry > 10) {
				return -EFAULT;
			}
		}
	}

	return size_bak;
#endif
}

ssize_t ffile_writeto(int fd, const void *buff, size_t size, off_t offset)
{
#if 0
	if (lseek(fd, offset, SEEK_SET) != offset) {
#if CAVAN_FILE_DEBUG
		pr_error_info("lseek");
#endif
		return -EFAULT;
	}

	return ffile_write(fd, buff, size);
#else
	int retry = 0;
	size_t size_bak = size;

	while (1) {
		ssize_t wrlen;

		wrlen = pwrite(fd, buff, size, offset);
		if (likely(wrlen >= (ssize_t) size)) {
			break;
		}

		if (wrlen > 0) {
			retry = 0;
			size -= wrlen;
			offset += wrlen;
			buff = ADDR_ADD(buff, wrlen);
		} else {
			if (wrlen < 0) {
				if (ERRNO_NOT_RETRY()) {
					return wrlen;
				}

				msleep(100);
			}

			if (++retry > 10) {
				return -EFAULT;
			}
		}
	}

	return size_bak;
#endif
}

ssize_t file_writeto(const char *file_name, const void *buff, size_t size, off_t offset, int flags)
{
	ssize_t wrlen;
	int fd;

#if CAVAN_FILE_DEBUG
	println("write file = %s", file_name);
	println("size = %s", size2text(size));
	println("offset = %s", size2text(offset));
#endif

	fd = file_create_open(file_name, O_WRONLY | O_CLOEXEC | O_SYNC | flags, 0777);
	if (fd < 0) {
#if CAVAN_FILE_DEBUG
		pr_error_info("open \"%s\"", file_name);
#endif
		return -1;
	}

	wrlen = ffile_writeto(fd, buff, size, offset);

	close(fd);

	return wrlen;
}

ssize_t ffile_readfrom(int fd, void *buff, size_t size, off_t offset)
{
#if 0
	if (lseek(fd, offset, SEEK_SET) != offset) {
		pr_error_info("lseek");
		return -EFAULT;
	}

	return ffile_read(fd, buff, size);
#else
	int retry = 0;

	while (1) {
		ssize_t rdlen;

		rdlen = pread(fd, buff, size, offset);
		if (likely(rdlen >= (ssize_t) size) || ERRNO_NOT_RETRY()) {
			return rdlen;
		}

		if (++retry > 10) {
			break;
		}

		msleep(100);
	}

	return -EFAULT;
#endif
}

ssize_t file_readfrom(const char *file_name, void *buff, size_t size, off_t offset, int flags)
{
	ssize_t rdlen;
	int fd;

#if CAVAN_FILE_DEBUG
	println("read file = %s", file_name);
	println("size = %s", size2text(size));
	println("offset = %s", size2text(offset));
#endif

	fd = open(file_name, O_RDONLY | O_BINARY | flags);
	if (fd < 0) {
#if CAVAN_FILE_DEBUG
		pr_error_info("open \"%s\"", file_name);
#endif
		return -1;
	}

	rdlen = ffile_readfrom(fd, buff, size, offset);

	close(fd);

	return rdlen;
}

ssize_t file_read_text(const char *filename, char *buff, size_t size)
{
	ssize_t rdlen = file_read(filename, buff, size - 1);

	buff[rdlen] = 0;

	while (rdlen > 0 && cavan_isspace(buff[rdlen - 1])) {
		buff[--rdlen] = 0;
	}

	return rdlen;
}

int file_test_read(const char *filename)
{
	char buff[1024];

	return file_readfrom(filename, buff, sizeof(buff), 0, 0);
}

int ffile_show(int fd)
{
	while (1) {
		ssize_t rdlen;
		char buff[16];

		rdlen = ffile_read(fd, buff, sizeof(buff));
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		text_show(buff, rdlen);
	}

	print_char('\n');

	return 0;
}

int ffile_nshow(int fd, size_t size)
{
	if (size == 0) {
		return ffile_show(fd);
	}

	while (size) {
		ssize_t rdlen;
		char buff[MAX_BUFF_LEN];

		rdlen = ffile_read(fd, buff, size > sizeof(buff) ? sizeof(buff) : size);
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		text_show(buff, rdlen);

		size -= rdlen;
	}

	print_char('\n');

	return 0;
}

int file_operation_ro(const char *filename, int (*handle)(int fd))
{
	int fd;
	int ret;

	fd = file_open_ro(filename);
	if (fd < 0) {
		return fd;
	}

	ret = handle(fd);

	close(fd);

	return ret;
}

int file_noperation_ro(const char *filename, size_t size, int (*handle)(int fd, size_t size))
{
	int fd;
	int ret;

	fd = file_open_ro(filename);
	if (fd < 0) {
		pr_error_info("open file %s", filename);
		return fd;
	}

	ret = handle(fd, size);

	close(fd);

	return ret;
}

int file_operation_wo(const char *filename, int (*handle)(int fd))
{
	int fd;
	int ret;

	fd = file_open_wo(filename);
	if (fd < 0) {
		return fd;
	}

	ret = handle(fd);

	close(fd);

	return ret;
}

int file_noperation_wo(const char *filename, size_t size, int (*handle)(int fd, size_t size))
{
	int fd;
	int ret;

	fd = file_open_wo(filename);
	if (fd < 0) {
		return fd;
	}

	ret = handle(fd, size);

	close(fd);

	return ret;
}

int file_operation_rw(const char *filename, int (*handle)(int fd))
{
	int fd;
	int ret;

	fd = file_open_ro(filename);
	if (fd < 0) {
		return fd;
	}

	ret = handle(fd);

	close(fd);

	return ret;
}

int file_noperation_rw(const char *filename, size_t size, int (*handle)(int fd, size_t size))
{
	int fd;
	int ret;

	fd = file_open_ro(filename);
	if (fd < 0) {
		return fd;
	}

	ret = handle(fd, size);

	close(fd);

	return ret;
}

int ffile_cat(int fd)
{
	while (1) {
		ssize_t rdlen;
		char buff[MAX_BUFFER_LEN];

		rdlen = ffile_read(fd, buff, sizeof(buff));
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		print_ntext(buff, rdlen);
	}

	print_char('\n');

	return 0;
}

int ffile_ncat(int fd, size_t size)
{
	if (size == 0) {
		return ffile_cat(fd);
	}

	while (size) {
		ssize_t rdlen;
		char buff[MAX_BUFFER_LEN];

		rdlen = ffile_read(fd, buff, size > sizeof(buff) ? sizeof(buff) : size);
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		print_ntext(buff, rdlen);
		size -= rdlen;
	}

	print_char('\n');

	return 0;
}

int ffile_cmp(int fd1, int fd2, size_t size)
{
	if (ffile_get_size(fd1) != ffile_get_size(fd2)) {
		return 1;
	}

	while (size) {
		ssize_t rdlen;
		char buff1[MAX_BUFF_LEN];
		char buff2[MAX_BUFF_LEN];

		rdlen = ffile_read(fd1, buff1, size > sizeof(buff1) ? sizeof(buff1) : size);
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		rdlen = ffile_read(fd2, buff2, rdlen);
		if (rdlen < 0) {
			pr_error_info("ffile_read");
			return rdlen;
		}

		if (memcmp(buff1, buff2, rdlen) != 0) {
			return 1;
		}

		size -= rdlen;
	}

	return 0;
}

int file_cmp(const char *file1, const char *file2, size_t size)
{
	int ret;
	int fd1, fd2;

	fd1 = open(file1, READ_FLAGS);
	if (fd1 < 0) {
		pr_error_info("open \"%s\"", file1);
		return -1;
	}

	fd2 = open(file2, READ_FLAGS);
	if (fd2 < 0) {
		ret = -1;
		pr_error_info("open \"%s\"", file2);
		goto out_close_file1;
	}

	ret = ffile_cmp(fd1, fd2, size);
	if (ret < 0) {
		pr_err_info("ffile_cmp");
	}

	close(fd2);
out_close_file1:
	close(fd1);

	return ret;
}

int ffile_crc32(int fd, u32 *crc)
{
	print("check file crc32 checksum ...");

	while (1) {
		ssize_t rdlen;
		char buff[MAX_BUFF_LEN];

		rdlen = ffile_read(fd, buff, sizeof(buff));
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		crc[0] = cavan_crc32(crc[0], buff, rdlen);

		print_char('.');
	}

	println(" OK");

	return 0;
}

int file_crc32(const char *file_name, u32 *crc)
{
	int fd;
	int ret;

	fd = open(file_name, READ_FLAGS);
	if (fd < 0) {
		pr_error_info("open \"%s\"", file_name);
		return -1;
	}

	ret = ffile_crc32(fd, crc);
	if (ret < 0) {
		pr_err_info("ffile_crc32");
	}

	close(fd);

	return ret;
}

int ffile_ncrc32(int fd, size_t size, u32 *crc)
{
	struct progress_bar bar;

	progress_bar_init(&bar, size, 0, PROGRESS_BAR_TYPE_DATA);

	while (size) {
		ssize_t rdlen;
		char buff[MAX_BUFF_LEN];

		rdlen = ffile_read(fd, buff, size > sizeof(buff) ? sizeof(buff) : size);
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		crc[0] = cavan_crc32(crc[0], buff, rdlen);
		size -= rdlen;
		progress_bar_add(&bar, rdlen);
	}

	progress_bar_finish(&bar);

	return 0;
}

int file_ncrc32(const char *file_name, size_t size, u32 *crc)
{
	int fd;
	int ret;

	fd = open(file_name, READ_FLAGS);
	if (fd < 0) {
		pr_error_info("open \"%s\"", file_name);
		return -1;
	}

	ret = ffile_ncrc32(fd, size, crc);
	if (ret < 0) {
		pr_err_info("ffile_ncrc32");
	}

	close(fd);

	return ret;
}

int ffile_crc32_seek(int fd, off_t offset, int whence, u32 *crc)
{
	int ret;
	s64 offset_bak;

	offset_bak = lseek(fd, offset, whence);
	if (offset_bak != offset) {
		pr_error_info("lseek");
		return -EFAULT;
	}

	ret = ffile_crc32(fd, crc);
	if (ret < 0) {
		pr_err_info("ffile_crc32");
		return ret;
	}

	if (lseek(fd, offset_bak, SEEK_SET) != offset_bak) {
		pr_error_info("lseek");
		return -EFAULT;
	}

	return 0;
}

int file_crc32_seek(const char *file_name, off_t offset, int whence, u32 *crc)
{
	int fd;
	int ret;

	fd = open(file_name, READ_FLAGS);
	if (fd < 0) {
		pr_error_info("open \"%s\"", file_name);
		return -1;
	}

	ret = ffile_crc32_seek(fd, offset, whence, crc);
	if (ret < 0) {
		pr_err_info("ffile_crc32_seek");
	}

	close(fd);

	return ret;
}

int ffile_ncrc32_seek(int fd, off_t offset, int whence, size_t size, u32 *crc)
{
	int ret;
	s64 offset_bak;

	offset_bak = lseek(fd, offset, whence);
	if (offset_bak != offset) {
		pr_error_info("lseek");
		return -EFAULT;
	}

	ret = ffile_ncrc32(fd, size, crc);
	if (ret < 0) {
		pr_err_info("ffile_crc32");
		return ret;
	}

	if (lseek(fd, offset_bak, SEEK_SET) != offset_bak) {
		pr_error_info("lseek");
		return -EFAULT;
	}

	return 0;
}

int file_ncrc32_seek(const char *file_name, off_t offset, int whence, size_t size, u32 *crc)
{
	int fd;
	int ret;

	fd = open(file_name, READ_FLAGS);
	if (fd < 0) {
		pr_error_info("open \"%s\"", file_name);
		return -1;
	}

	ret = ffile_ncrc32_seek(fd, offset, whence, size, crc);
	if (ret < 0) {
		pr_err_info("ffile_ncrc32_seek");
	}

	close(fd);

	return ret;
}

int file_stat(const char *file_name, struct stat *st)
{
	int ret;
	int fd;

	fd = open(file_name, 0);
	if (fd < 0) {
		return fd;
	}

	ret = fstat(fd, st);

	close(fd);

	return ret;
}

int mode_test(mode_t mode, char type)
{
	switch (mode & S_IFMT) {
	case S_IFBLK:
		if (type == 'b' || type == 'B') {
			return 0;
		}
		break;
	case S_IFCHR:
		if (type == 'c' || type == 'C') {
			return 0;
		}
		break;
	case S_IFDIR:
		if (type == 'd' || type == 'D') {
			return 0;
		}
		break;
	case S_IFIFO:
		if (type == 'p' || type == 'P') {
			return 0;
		}
		break;
	case S_IFLNK:
		if (type == 'l' || type == 'L') {
			return 0;
		}
		break;
	case S_IFREG:
		if (type == 'f' || type == 'F') {
			return 0;
		}
		break;
	case S_IFSOCK:
		if (type == 's' || type == 'S') {
			return 0;
		}
		break;
	default:
		if (type == 'e' || type == 'E') {
			return 0;
		}
		pr_warn_info("unknown file type");
	}

	return -EINVAL;
}

mode_t ffile_get_mode(int fd)
{
	int ret;
	struct stat st;

	ret = fstat(fd, &st);
	if (ret < 0) {
		return 0;
	}

	return st.st_mode;
}

mode_t file_get_mode(const char *pathname)
{
	int ret;
	int fd;
	struct stat st;
	mode_t mode;

	ret = stat(pathname, &st);
	if (ret == 0) {
		return st.st_mode;
	}

	fd = open(pathname, 0);
	if (fd < 0) {
		return 0;
	}

	mode = ffile_get_mode(fd);

	close(fd);

	return mode;
}

mode_t file_get_lmode(const char *pathname)
{
	int ret;
	struct stat st;

	ret = lstat(pathname, &st);
	if (ret == 0) {
		return st.st_mode;
	}

	return file_get_mode(pathname);
}

int file_test(const char *file_name, const char *types)
{
	int ret;
	char buff[8], *p = buff;
	int access_mode = 0;
	mode_t st_mode;

	while (1) {
		switch (*types) {
		case 'w':
		case 'W':
			access_mode |= W_OK;
			break;

		case 'r':
		case 'R':
			access_mode |= R_OK;
			break;

		case 'e':
		case 'E':
			access_mode |= F_OK;
			break;

		case 'x':
		case 'X':
			access_mode |= X_OK;
			break;

		case 0:
			goto label_access;

		default:
			*p++ = *types;
		}

		types++;
	}

label_access:
	ret = access(file_name, access_mode);
	if (ret < 0) {
		return ret;
	}

	if (p == buff) {
		return 0;
	}

	st_mode = file_get_lmode(file_name);

	for (*p = 0, p = buff; *p; p++) {
		if (mode_test(st_mode, *p) == 0) {
			return 0;
		}
	}

	return -EINVAL;
}

int ffile_test(int fd, char type)
{
	int ret;
	struct stat st;

	ret = fstat(fd, &st);
	if (ret < 0) {
		return ret;
	}

	return mode_test(st.st_mode, type);
}

int file_wait(const char *filepath, const char *types, u32 sec)
{
	while (sec && file_test(filepath, types) < 0) {
		print("Wait remain time %d(s)  \r", sec);
		sleep(1);
		sec--;
	}

	return sec ? 0 : -ETIMEDOUT;
}

int file_resize(const char *file_path, off_t length)
{
	int ret;
	int fd;

	fd = open(file_path, O_CREAT | O_WRONLY | O_BINARY, 0777);
	if (fd < 0) {
		pr_error_info("open file \"%s\"", file_path);
		return fd;
	}

	ret = ftruncate(fd, length);

	close(fd);

	return ret;
}

int get_file_pointer(int fd, off_t *fpointer)
{
	off_t ret;

	ret = lseek(fd, 0, SEEK_CUR);
	if (ret < 0) {
		pr_error_info("lseek");
		return ret;
	}

	fpointer[0] = ret;

	return 0;
}

int calculate_file_md5sum(const char *file_path, char *md5sum)
{
#if 0
	int ret;

	ret = system_command("md5sum -b %s > " MD5SUM_FILE_PATH, file_path);
	if (ret < 0) {
		pr_error_info("system_command");
		return ret;
	}

	return read_from(MD5SUM_FILE_PATH, md5sum, MD5SUM_LEN, 0, 0);
#else
	return buff_command2(md5sum, MD5SUM_LEN, "md5sum %s", file_path);
#endif
}

int check_file_md5sum(const char *file_path, char *md5sum)
{
	int ret;
	char buff[1024];

	memcpy(buff, md5sum, MD5SUM_LEN);
	buff[MD5SUM_LEN] = buff[MD5SUM_LEN + 1] = ' ';
	strcpy(buff + MD5SUM_LEN + 2, file_path);

	ret = file_writeto(MD5SUM_FILE_PATH, buff, strlen(buff), 0, O_TRUNC);
	if (ret < 0) {
		pr_err_info("write_to");
		return ret;
	}

	return system_command("md5sum -c " MD5SUM_FILE_PATH);
}

int file_replace_line_simple(const char *file_path, const char *prefix, off_t prefix_size, const char *new_line, off_t new_line_size)
{
	int ret;
	struct buffer *old_buff, *new_buff;

	old_buff = read_lines(file_path);
	if (old_buff == NULL) {
		return -1;
	}

	new_buff = replace_prefix_line(old_buff, prefix, prefix_size, new_line, new_line_size);
	if (new_buff == NULL) {
		ret = -1;
		goto out_free_old_buff;
	}

	ret = write_lines(file_path, new_buff);

	free_buffer(new_buff);
out_free_old_buff:
	free_buffer(old_buff);

	return ret;
}

int file_select_read(int fd, int timeout_ms)
{
	fd_set set_read;
	struct timeval time;

	FD_ZERO(&set_read);
	FD_SET(fd, &set_read);

	time.tv_sec = timeout_ms / 1000;
	time.tv_usec = (timeout_ms % 1000) * 1000;

	return select(fd + 1, &set_read, NULL, NULL, &time);
}

u32 ffile_checksum32_simple(int fd, off_t offset, size_t size)
{
	struct progress_bar bar;
	u64 checksum = 0;

	if (size == 0) {
		ssize_t tmp;

		tmp = ffile_get_size(fd);
		if (tmp < 0) {
			pr_err_info("get file size failed");
			return tmp;
		}

		size = tmp;
	}

	if ((size_t) offset >= size) {
		return -EINVAL;
	}

	if (offset > 0) {
		if (lseek(fd, offset, SEEK_SET) != offset) {
			return -EFAULT;
		}

		size -= offset;
	}

	progress_bar_init(&bar, size, 0, PROGRESS_BAR_TYPE_DATA);

	while (1) {
		ssize_t rdlen;
		u8 buff[MAX_BUFF_LEN];

		rdlen = ffile_read(fd, buff, sizeof(buff));
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		checksum += mem_checksum32_simple(buff, rdlen);
		progress_bar_add(&bar, rdlen);
	}

	progress_bar_finish(&bar);

	checksum = (checksum >> 32) + (checksum & 0xFFFFFFFF);

	return (u32) ((checksum >> 32) + checksum);
}

u16 ffile_checksum16_simple(int fd, off_t offset, size_t size)
{
	u32 checksum = ffile_checksum32_simple(fd, offset, size);

	checksum = (checksum >> 16) + (checksum & 0xFFFF);

	return (u16) ((checksum >> 16) + checksum);
}

u8 ffile_checksum8_simple(int fd, off_t offset, size_t size)
{
	u16 checksum = ffile_checksum16_simple(fd, offset, size);

	checksum = (checksum >> 8) + (checksum & 0xFF);

	return (u8) ((checksum >> 8) + checksum);
}

u32 file_checksum32(const char *filename, off_t offset, size_t size)
{
	int fd;
	u32 checksum;

	fd = file_open_ro(filename);
	if (fd < 0) {
		pr_error_info("open file \"%s\"", filename);
		return 0;
	}

	checksum = ffile_checksum32(fd, offset, size);

	close(fd);

	return checksum;
}

u16 file_checksum16(const char *filename, off_t offset, size_t size)
{
	int fd;
	u16 checksum;

	fd = file_open_ro(filename);
	if (fd < 0) {
		pr_error_info("open file \"%s\"", filename);
		return 0;
	}

	checksum = ffile_checksum16(fd, offset, size);

	close(fd);

	return checksum;
}

u8 file_checksum8(const char *filename, off_t offset, size_t size)
{
	int fd;
	u8 checksum;

	fd = file_open_ro(filename);
	if (fd < 0) {
		pr_error_info("open file \"%s\"", filename);
		return 0;
	}

	checksum = ffile_checksum8(fd, offset, size);

	close(fd);

	return checksum;
}

ssize_t ffile_vprintf(int fd, const char *fmt, va_list ap)
{
	char buff[1024];
	ssize_t wrlen;

	wrlen = vsnprintf(buff, sizeof(buff), fmt, ap);
	wrlen = ffile_write(fd, buff, wrlen);

	return wrlen;
}

ssize_t ffile_printf(int fd, const char *fmt, ...)
{
	ssize_t wrlen;
	va_list ap;

	va_start(ap, fmt);
	wrlen = ffile_vprintf(fd, fmt, ap);
	va_end(ap);

	return wrlen;
}

ssize_t file_vprintf(const char *filename, const char *fmt, va_list ap)
{
	char buff[1024];
	ssize_t wrlen;

	wrlen = vsnprintf(buff, sizeof(buff), fmt, ap);
	wrlen = file_writeto(filename, buff, wrlen, 0, 0);

	return wrlen;
}

ssize_t file_printf(const char *filename, const char *fmt, ...)
{
	ssize_t wrlen;
	va_list ap;

	va_start(ap, fmt);
	wrlen = file_vprintf(filename, fmt, ap);
	va_end(ap);

	return wrlen;
}

int file_set_loop(const char *filename, char *loop_path, u64 offset)
{
	int ret;
	int fd;
	int loop_fd;
	struct loop_info64 loopinfo;

	fd = file_open_rw_ro(filename, 0);
	if (fd < 0) {
		return fd;
	}

	loop_fd = loop_get_fd(filename, loop_path, offset);
	if (loop_fd < 0) {
		ret = loop_fd;
		pr_err_info("loop_get_fd");
		goto out_close_file;
	}

	ret = ioctl(loop_fd, LOOP_SET_FD, fd);
	if (ret < 0) {
		pr_err_info("LOOP_SET_FD");
		goto out_close_loop;
	}

	mem_set8((u8 *) &loopinfo, 0, sizeof(loopinfo));
	if (cavan_path_to_abs_base2(filename, (char *) loopinfo.lo_file_name, sizeof(loopinfo.lo_file_name)) == NULL) {
		ret = -ENOENT;
		goto out_close_loop;
	}
	loopinfo.lo_offset = offset;

	ret = ioctl(loop_fd, LOOP_SET_STATUS64, &loopinfo);
	if (ret < 0) {
		pr_err_info("LOOP_SET_STATUS64");
		goto out_clr_fd;
	}

	goto out_close_loop;

out_clr_fd:
	ioctl(loop_fd, LOOP_CLR_FD, 0);
out_close_loop:
	close(loop_fd);
out_close_file:
	close(fd);

	return ret;
}

int file_mount_to(const char *source, const char *target, const char *fs_type, unsigned long flags, const void *data)
{
	int ret;
	char loop_path[64];

	ret = file_set_loop(source, loop_path, 0);
	if (ret < 0) {
#if CAVAN_FILE_DEBUG
		pr_err_info("file set loop");
#endif
		return ret;
	}

	ret = libc_mount_to(loop_path, target, fs_type, flags, data);
	if (ret < 0) {
#if CAVAN_FILE_DEBUG
		pr_error_info("libc_mount_to");
#endif
		loop_clr_fd(loop_path);
		return ret;
	}

	return 0;
}

int ffile_newly_cmp(int fd1, int fd2)
{
	struct stat st1, st2;

	if (fstat(fd1, &st1) < 0) {
		return -1;
	}

	if (fstat(fd2, &st2) < 0) {
		return 1;
	}

	return st2.st_mtime - st1.st_mtime;
}

int file_newly_cmp(const char *file1, const char *file2)
{
	struct stat st1, st2;

	if (file_stat2(file1, &st1) < 0) {
		return -1;
	}

	if (file_stat2(file2, &st2) < 0) {
		return 1;
	}

	return st2.st_mtime - st1.st_mtime;
}

int chdir_backup(const char *dirname)
{
	static char dir_bak[1024];

	if (dirname == NULL) {
		if (dir_bak[0]) {
			return chdir(dir_bak);
		} else {
			return -ENOENT;
		}
	}

	if (getcwd(dir_bak, sizeof(dir_bak)) == NULL) {
		dir_bak[0] = 0;
	}

	return chdir(dirname);
}

int ffile_delete_char(int fd_in, int fd_out, char c)
{
	while (1) {
		char buff[MAX_BUFFER_LEN];
		ssize_t rdlen, wrlen;

		rdlen = ffile_read(fd_in, buff, sizeof(buff));
		if (rdlen <= 0) {
			if (rdlen == 0) {
				break;
			}

			pr_error_info("ffile_read");
			return rdlen;
		}

		rdlen = mem_delete_char(buff, rdlen, c);
		wrlen = ffile_write(fd_out, buff, rdlen);
		if (wrlen != rdlen) {
			pr_error_info("ffile_write");
			if (wrlen < 0) {
				return wrlen;
			}

			ERROR_RETURN(ENOMEDIUM);
		}
	}

	return 0;
}

int file_delete_char(const char *file_in, const char *file_out, char c)
{
	int ret;
	int fd_in, fd_out;

	fd_in = open(file_in, O_RDONLY | O_BINARY);
	if (fd_in < 0) {
		pr_error_info("open input file \"%s\"", file_in);
		return fd_in;
	}

	fd_out = open(file_out, O_WRONLY | O_CREAT | O_SYNC | O_TRUNC | O_BINARY, 0777);
	if (fd_out < 0) {
		ret = fd_out;
		pr_error_info("open output file \"%s\"", file_out);
		goto out_close_fd_in;
	}

	ret = ffile_delete_char(fd_in, fd_out, c);

	close(fd_out);
out_close_fd_in:
	close(fd_in);

	return ret;
}

int file_hardlink(const char *from, const char *to)
{
	int ret;

	remove(from);

	ret = mkdir_hierarchy(cavan_path_dirname(from), 0777);
	if (ret < 0) {
		return ret;
	}

	return link(to, from);
}

int vfile_try_open(int flags, mode_t mode, va_list ap)
{
	int fd;
	const char *filename;

	while ((filename = va_arg(ap, const char *))) {
		fd = open(filename, flags, mode);
		if (fd >= 0) {
			println("filename = %s", filename);
			return fd;
		}
	}

	return -ENOENT;
}

int file_try_open(int flags, mode_t mode, ...)
{
	int fd;
	va_list ap;

	va_start(ap, mode);
	fd = vfile_try_open(flags, mode, ap);
	va_end(ap);

	return fd;
}

int file_find_and_open(const char *prefix, char *last_path, int start, int end, int flags)
{
	int fd;
	char tmp_path[1024], *p, *p_bak;

	if (start > end) {
		NUMBER_SWAP(start, end);
	}

	if (last_path) {
		p = last_path;
	} else {
		p = tmp_path;
	}

	p_bak = p;

	p = text_copy(p, prefix);

	while (start <= end) {
		value2text_unsigned_simple(start, p, sizeof(tmp_path), 10);

		fd = open(p_bak, flags | O_NONBLOCK);
		if (fd >= 0) {
			return fd;
		}

		start++;
	}

	return -ENOENT;
}

bool file_poll(int fd, short events, int timeout_ms)
{
	struct pollfd pfd = {
		.fd = fd,
		.events = events,
	};

	return poll(&pfd, 1, timeout_ms) > 0;
}

bool file_discard_all(int fd)
{
	int ret;
	ssize_t rdlen;
	char buff[1024];
	struct pollfd pfd = {
		.fd = fd,
		.events = POLLIN,
	};

	while (1) {
		ret = poll(&pfd, 1, 0);
		if (ret < 0) {
			pr_error_info("poll");
			return false;
		}

		if (ret < 1) {
			break;
		}

		rdlen = ffile_read(fd, buff, sizeof(buff));
		if (rdlen < 0) {
			pr_error_info("ffile_read");
			return false;
		}

		if (rdlen < (ssize_t) sizeof(buff)) {
			break;
		}
	}

	return true;
}

char file_type_to_char(mode_t mode)
{
	switch (mode & S_IFMT) {
	case S_IFBLK:
		return 'b';

	case S_IFCHR:
		return 'c';

	case S_IFDIR:
		return 'd';

	case S_IFIFO:
		return 'f';

	case S_IFLNK:
		return 'l';

	default:
		return '-';
	}
}

char *file_permition_tostring(mode_t mode, char *buff, char *buff_end)
{
	int i;
	u32 shift;
	const char *permition_table = "rwx";

	shift = 1 << 8;

	while (shift) {
		for (i = 0; i < 3 && buff < buff_end; i++, shift >>= 1, buff++) {
			if (mode & shift) {
				*buff = permition_table[i];
			} else {
				*buff = '-';
			}
		}
	}

	return buff;
}

const char *month_tostring(int month)
{
	const char *month_table[] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

	if (month >= 0 && month < (int) ARRAY_SIZE(month_table)) {
		return month_table[month];
	}

	return "Unknown";
}

const char *week_tostring(int week)
{
	const char *week_table[] = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

	if (week >= 0 && week < (int) ARRAY_SIZE(week_table)) {
		return week_table[week];
	}

	return "Unknown";
}

int remove_directory(const char *pathname)
{
	int ret;
	char tmppath[1024], *name_p;
	DIR *dp;
	struct dirent *en;

	dp = opendir(pathname);
	if (dp == NULL) {
		pr_error_info("opendir %s failed", pathname);
		return -ENOENT;
	}

	name_p = cavan_path_copy(tmppath, sizeof(tmppath), pathname, true);

	while ((en = readdir(dp))) {
		if (cavan_path_is_dot_name(en->d_name)) {
			continue;
		}

		text_copy(name_p, en->d_name);

		if (en->d_type == DT_DIR) {
			ret = remove_directory(tmppath);
		} else {
			println("remove file \"%s\"", tmppath);
			ret = remove(tmppath);
		}

		if (ret < 0) {
			pr_error_info("delete %s failed", tmppath);
			goto out_close_dp;
		}
	}

	println("remove directory \"%s\"", pathname);
	ret = rmdir(pathname);

out_close_dp:
	closedir(dp);

	return ret;
}

int remove_auto(const char *pathname)
{
	int ret;
	struct stat st;

	ret = lstat(pathname, &st);
	if (ret < 0) {
		pr_error_info("get file %s stat failed", pathname);
		return ret;
	}

	return S_ISDIR(st.st_mode) ? remove_directory(pathname) : remove(pathname);
}

int file_type_test(const char *pathname, mode_t type)
{
	int ret;
	struct stat st;

	ret = file_lstat(pathname, &st);
	if (ret < 0) {
		return 0;
	}

	return (st.st_mode & S_IFMT) == type;
}

int file_type_test2(const char *pathname, mode_t type)
{
	int ret;
	struct stat st;

	ret = file_stat2(pathname, &st);
	if (ret < 0) {
		return 0;
	}

	return (st.st_mode & S_IFMT) == type;
}

int fd_type_test(int fd, mode_t type)
{
	int ret;
	struct stat st;

	ret = fstat(fd, &st);
	if (ret < 0) {
		return 0;
	}

	return (st.st_mode & S_IFMT) == type;
}

size_t fscan_directory1(DIR *dp, void *buff, size_t size)
{
	void *buff_end;
	struct dirent *en;

	buff_end = (char *) buff + size;
	size = 0;

	while (buff < buff_end && (en = readdir(dp))) {
		if (cavan_path_is_dot_name(en->d_name)) {
			continue;
		}

		buff = text_copy(buff, en->d_name) + 1;
		size++;
	}

	return size;
}

size_t fscan_directory2(DIR *dp, void *buff, size_t size1, size_t size2)
{
	void *buff_end;
	struct dirent *en;

	buff_end = (char *) buff + (size1 * size2);
	size1 = 0;

	while (buff < buff_end && (en = readdir(dp))) {
		if (cavan_path_is_dot_name(en->d_name)) {
			continue;
		}

		text_ncopy(buff, en->d_name, size2);

		buff = (char *) buff + size2;
		size1++;
	}

	return size1;
}

int scan_directory(const char *dirpath, void *buff, size_t size1, size_t size2)
{
	DIR *dp;

	dp = opendir(dirpath);
	if (dp == NULL) {
		pr_error_info("opendir %s failed", dirpath);
		return -1;
	}

	size1 = size2 ? fscan_directory2(dp, buff, size1, size2) : fscan_directory1(dp, buff, size1);

	closedir(dp);

	return size1;
}

int file_open_format(int flags, mode_t mode, const char *fmt, ...)
{
	int fd;
	char buff[1024];
	va_list ap;

	va_start(ap, fmt);
	vsnprintf(buff, sizeof(buff), fmt, ap);
	va_end(ap);

	fd = open(buff, flags, mode);
	if (fd < 0) {
		pr_error_info("Open file `%s' failed", buff);
	}

	return fd;
}

size_t ffile_line_count(int fd)
{
	char buff[1024];
	ssize_t rdlen;
	size_t count;

	if (lseek(fd, 0, SEEK_SET) < 0) {
		pr_error_info("lseek");
		return 0;
	}

	count = 0;

	while (1) {
		rdlen = ffile_read(fd, buff, sizeof(buff));
		if (rdlen < 0) {
			pr_error_info("ffile_read");
			return 0;
		}

		if (rdlen) {
			count += mem_byte_count(buff, '\n', rdlen);
#if __WORDSIZE == 64
			println("count = %ld", count);
#else
			println("count = %d", count);
#endif
		} else {
			break;
		}
	}

	return count;
}

size_t file_line_count(const char *filename)
{
	int fd;
	size_t count;

	fd = open(filename, O_RDONLY);
	if (fd < 0) {
		pr_error_info("Open file `%s' failed", filename);
		return 0;
	}

	count = ffile_line_count(fd);
	close (fd);

	return count;
}

int file_mmap(const char *pathname, void **addr, size_t *size, int flags)
{
	int ret;
	int fd;
	struct stat st;
	void *mem;

	if (flags & O_WRONLY) {
		flags = (flags & (~O_WRONLY)) | O_RDWR;
	}

	fd = open(pathname, flags, 0777);
	if (fd < 0) {
		return fd;
	}

	ret = flock(fd, LOCK_SH);
	if (ret < 0) {
		pr_error_info("flock");
		goto out_close_fd;
	}

	if (flags & O_RDWR) {
		flags = PROT_READ | PROT_WRITE;
	} else if (flags & O_WRONLY) {
		flags = PROT_WRITE;
	} else {
		flags = PROT_READ;
	}

	if (flags & PROT_WRITE) {
		st.st_size = *size;
		ret = ftruncate(fd, st.st_size);
		if (ret < 0) {
			pr_error_info("ftruncate");
			goto out_unlock_fd;
		}
	} else {
		ret = fstat(fd, &st);
		if (ret < 0) {
			pr_error_info("fstat");
			goto out_unlock_fd;
		}
	}

	mem = mmap(NULL, st.st_size, flags, MAP_SHARED, fd, 0);
	if (mem == NULL || mem == MAP_FAILED) {
		ret = -EFAULT;
		// pr_error_info("mmap");
		goto out_unlock_fd;
	};

	*addr = mem;
	*size = st.st_size;
	return fd;

out_unlock_fd:
	flock(fd, LOCK_UN);
out_close_fd:
	close(fd);
	return ret;
}

void file_unmap(int fd, void *addr, size_t size)
{
	munmap(addr, size);
	flock(fd, LOCK_UN);
	close(fd);
}

void *file_read_all(const char *pathname, size_t extra, size_t *size)
{
	int fd;
	void *mem;
	ssize_t rdlen;
	off_t last;

	fd = open(pathname, O_RDONLY);
	if (fd < 0) {
		// pr_error_info("open file `%s' failed", pathname);
		return NULL;
	}

	last = lseek(fd, 0, SEEK_END);
	if (last == (off_t) -1) {
		last = KB(100);
	} else {
		lseek(fd, 0, SEEK_SET);
	}

	mem = malloc(last + extra);
	if (mem == NULL) {
		pr_error_info("malloc");
		goto out_close_fd;
	}

	rdlen = ffile_read(fd, mem, last);
	if (rdlen < 0) {
		pr_error_info("ffile_read");
		free(mem);
		mem = NULL;
	} else if (size) {
		*size = rdlen;
	}

out_close_fd:
	close(fd);
	return mem;
}

char *file_read_all_text(const char *pathname, size_t *size)
{
	size_t file_size;

	char *file_mem = (char *) file_read_all(pathname, 1, &file_size);
	if (file_mem == NULL) {
		return NULL;
	}

	file_mem[file_size] = 0;

	if (size) {
		*size = file_size;
	}

	return file_mem;
}

mode_t file_mode2value(const char *text)
{
	int count, index;
	mode_t mode, temp_mode;

	if (text_is_number(text)) {
		return text2value_unsigned(text, NULL, 8);
	}

	mode = 0;
	temp_mode = 0;
	count = 0;
	index = 6;

	while (1) {
		switch (*text) {
		case 0:
			return mode | temp_mode << index;

		case 'r':
		case 'R':
			temp_mode |= 1 << 2;
			break;

		case 'w':
		case 'W':
			temp_mode |= 1 << 1;
			break;

		case 'x':
		case 'X':
			temp_mode |= 1 << 0;
			break;

		case '-':
			break;

		default:
			pr_red_info("Invalid mode char %c", *text);
		}

		if (count < 2) {
			count++;
		} else {
			mode |= temp_mode << index;

			if (index > 0) {
				index -= 3;
			} else {
				break;
			}

			count = 0;
			temp_mode = 0;
		}

		text++;
	}

	return mode;
}

int cavan_mkdir_simple(const char *pathname, struct cavan_mkdir_command_option *option)
{
	int ret;
	char buff[1024], *filename;

	if (option->verbose) {
		pr_info("create directory `%s'", pathname);
	}

	ret = mkdir(pathname, option->mode);
	if (ret == 0) {
		return 0;
	}

	if (errno != EEXIST) {
		pr_error_info("create directory `%s'", pathname);
		return ret;
	}

	if (file_access_e(pathname)) {
		return 0;
	}

	filename = cavan_path_dirname_base(buff, pathname);
	*filename = '/';
	text_copy(filename + 1, CAVAN_TEMP_FILENAME);

	if (mkdtemp(buff) == NULL) {
		pr_error_info("mkdtemp `%s'", buff);
		return -EFAULT;
	}

	pr_warning_info("rename %s => %s", buff, pathname);

	ret = rename(buff, pathname);
	if (ret == 0) {
		return 0;
	}

	rmdir(buff);

	if (file_access_e(pathname)) {
		return 0;
	}

	pr_error_info("rename directory `%s'", pathname);

	return ret;
}

int cavan_mkdir_parents(const char *pathname, struct cavan_mkdir_command_option *option)
{
	int ret;
	char buff[1024], *p, *name;

	if (pathname == NULL || *pathname == 0) {
		pr_red_info("pathname is empty");
		ERROR_RETURN(EINVAL);
	}

	p = text_copy(buff, pathname) - 1;

	while (1) {
		if (p == buff) {
			return 0;
		}

		if (*p != '/') {
			break;
		}

		*p-- = 0;
	}

	for (name = p; name > buff; name--) {
		if (*name == '/') {
			name++;
			break;
		}
	}

	for (p = buff; p < name && *p == '/'; p++);

	while (p < name) {
		if (*p == '/') {
			*p = 0;

			ret = cavan_mkdir_simple(buff, option);
			if (ret < 0) {
				return ret;
			}

			for (*p++ = '/'; p < name && *p == '/'; p++);
		} else {
			p++;
		}
	}

	return cavan_mkdir_simple(buff, option);
}

int cavan_mkdir_main(const char *pathname, struct cavan_mkdir_command_option *option)
{
	umask(0);

	if (option->parents) {
		return cavan_mkdir_parents(pathname, option);
	}

	return cavan_mkdir_simple(pathname, option);
}

int cavan_mkdir_main2(const char *pathname, mode_t mode)
{
	struct cavan_mkdir_command_option option = {
		.mode = mode,
		.verbose = false,
		.parents = true
	};

	return cavan_mkdir_main(pathname, &option);
}

int cavan_file_dump(const char *pathname, size_t width, const char *sep, const char *new_line)
{
	int fd;
	void *addr;
	size_t size;

	fd = file_mmap(pathname, &addr, &size, 0);
	if (fd < 0) {
		pr_red_info("file_mmap");
		return fd;
	}

	cavan_mem_dump(addr, size, width, sep, new_line);

	file_unmap(fd,addr, size);

	return 0;
}

int cavan_temp_file_open(char *pathname, size_t size, const char *filename, bool remove)
{
	int fd;

	cavan_path_build_tmp_path(filename, pathname, size);

	fd = mkstemp(pathname);
	if (fd < 0) {
		pr_error_info("mkstemp `%s'", pathname);
		return fd;
	}

	if (remove) {
		unlink(pathname);
	}

	println("pathname = %s", pathname);

	return fd;
}

off_t cavan_file_seek_next_page(int fd, size_t page_size)
{
	off_t page_mask = page_size - 1;
	off_t offset = lseek(fd, 0, SEEK_CUR);

	if ((offset & page_mask) == 0) {
		return offset;
	}

	return lseek(fd, page_size - (offset & page_mask), SEEK_CUR);
}

off_t cavan_file_seek_page_align(int fd, off_t offset, size_t page_size)
{
	off_t page_mask = page_size - 1;

	if (offset & page_mask) {
		offset = (offset & page_mask) + page_size;
	}

	return lseek(fd, offset, SEEK_SET);
}

struct dirent *cavan_readdir_skip_dot(DIR *dp)
{
	struct dirent *dt;

	while ((dt = readdir(dp))) {
		const char *name = dt->d_name;

		if (name[0] == '.') {
			if (name[1] == 0) {
				continue;
			}

			if (name[1] == '.' && name[2] == 0) {
				continue;
			}
		}

		return dt;
	}

	return NULL;
}

struct dirent *cavan_readdir_skip_hidden(DIR *dp)
{
	struct dirent *dt;

	while ((dt = readdir(dp)) && dt->d_name[0] == '.');

	return dt;
}

int cavan_readdir_to_file(const char *dirpath, int fd)
{
	DIR *dp;
	int ret;
	struct dirent *en;

	dp = opendir(dirpath);
	if (dp == NULL) {
		pr_err_info("opendir %s", dirpath);
		return -ENOENT;
	}

	while ((en = cavan_readdir_skip_dot(dp))) {
		char *name = en->d_name;
		int length = strlen(name);

		name[length] = '\n';

		ret = ffile_write(fd, name, length + 1);
		if (ret < 0) {
			pr_err_info("ffile_write");
			goto out_closedir;
		}
	}

	ret = 0;

out_closedir:
	closedir(dp);
	return ret;
}

int cavan_readdir_to_file2(const char *dirpath, const char *filepath)
{
	int fd;
	int ret;

	fd = open(filepath, O_CREAT | O_WRONLY | O_TRUNC, 0777);
	if (fd < 0) {
		pr_err_info("open file %s", filepath);
		return fd;
	}

	ret = cavan_readdir_to_file(dirpath, fd);

	close(fd);

	return ret;
}

int cavan_readdir_to_file_temp(const char *dirpath, off_t *size)
{
	int fd;
	int ret;
	char pathname[1024];

	fd = cavan_temp_file_open(pathname, sizeof(pathname), "readdir-XXXXXX", true);
	if (fd < 0) {
		pr_red_info("cavan_temp_file_open");
		return fd;
	}

	ret = cavan_readdir_to_file(dirpath, fd);
	if (ret < 0) {
		pr_red_info("cavan_readdir_to_file");
		goto out_close_fd;
	}

	if (size) {
		*size = lseek(fd, 0, SEEK_CUR);
	}

	ret = lseek(fd, 0, SEEK_SET);
	if (ret < 0) {
		pr_err_info("lseek");
		goto out_close_fd;
	}

	return fd;

out_close_fd:
	close(fd);
	return ret;
}

// ================================================================================

int cavan_file_proxy_add(struct cavan_file_proxy_desc *desc, const int fds[2])
{
	int ret;
	struct epoll_event event = {
		.events = EPOLLIN,
		.data.ptr = __UNCONST(fds),
	};

	ret = epoll_ctl(desc->epoll_fd, EPOLL_CTL_ADD, fds[0], &event);
	if (ret < 0) {
		pr_err_info("epoll_ctl EPOLL_CTL_ADD %d: %d", fds[0], ret);
		return ret;
	}

	cavan_atomic_inc(&desc->count);

#if CAVAN_FILE_PROXY_DEBUG
	pr_func_info("epoll = %d, count = %d, fds = [%d -> %d]", desc->epoll_fd, cavan_atomic_get(&desc->count), fds[0], fds[1]);
#endif

	return 0;
}

int cavan_file_proxy_del(struct cavan_file_proxy_desc *desc, const int fds[2])
{
	int ret;

	ret = epoll_ctl(desc->epoll_fd, EPOLL_CTL_DEL, fds[0], NULL);
	if (ret < 0) {
		pr_err_info("epoll_ctl EPOLL_CTL_ADD: %d", ret);
		return ret;
	}

	cavan_atomic_dec(&desc->count);

#if CAVAN_FILE_PROXY_DEBUG
	pr_func_info("epoll = %d, count = %d, fds = [%d -> %d]", desc->epoll_fd, cavan_atomic_get(&desc->count), fds[0], fds[1]);
#endif

	return 0;
}

int cavan_file_proxy_del_array(struct cavan_file_proxy_desc *desc, int fds[][2], int count)
{
	int i;

	for (i = 0; i < count; i++) {
		int ret = cavan_file_proxy_del(desc, fds[i]);
		if (ret < 0) {
			pr_red_info("cavan_file_proxy_del: %d", ret);
			return ret;
		}
	}

	return 0;
}

int cavan_file_proxy_add_array(struct cavan_file_proxy_desc *desc, int fds[][2], int count)
{
	int i;

	for (i = 0; i < count; i++) {
		int ret;
		const int *proxy = fds[i];

		if (proxy[0] < 0 || proxy[1] < 0) {
			continue;
		}

		ret = cavan_file_proxy_add(desc, proxy);
		if (ret < 0) {
			pr_red_info("cavan_file_proxy_add: %d", ret);

			cavan_file_proxy_del_array(desc, fds, i);
			return ret;
		}
	}

	return 0;
}

int cavan_file_proxy_init(struct cavan_file_proxy_desc *desc)
{
	int fd;
#if CAVAN_FILE_PROXY_WAKEUP_ENABLE
	int ret;
#endif

	fd = epoll_create(10);
	if (fd < 0) {
		pr_err_info("epoll_create: %d", fd);
		return fd;
	}

	desc->epoll_fd = fd;
	desc->count = 0;

#if CAVAN_FILE_PROXY_WAKEUP_ENABLE
	ret = pipe2(desc->pipefd, O_CLOEXEC | O_NONBLOCK);
	if (ret < 0) {
		pr_err_info("pipe: %d", ret);
		goto out_close_epoll;
	}
#endif

	return 0;

#if CAVAN_FILE_PROXY_WAKEUP_ENABLE
out_close_epoll:
	close(desc->epoll_fd);
	return ret;
#endif
}

void cavan_file_proxy_deinit(struct cavan_file_proxy_desc *desc)
{
#if CAVAN_FILE_PROXY_WAKEUP_ENABLE
	close(desc->pipefd[0]);
	close(desc->pipefd[1]);
#endif

	close(desc->epoll_fd);
}

int cavan_file_proxy_main_loop(struct cavan_file_proxy_desc *desc)
{
	int ret;
	int msec = -1;
	int epoll_fd = desc->epoll_fd;
#if CAVAN_FILE_PROXY_WAKEUP_ENABLE
	int pipefd[2] = { desc->pipefd[0], 2 };

	ret = cavan_file_proxy_add(desc, pipefd);
	if (ret < 0) {
		pr_red_info("cavan_file_proxy_add: %d", ret);
		return ret;
	}
#endif

	while (cavan_atomic_get(&desc->count)) {
		struct epoll_event events[10];
		const struct epoll_event *p, *p_end;

		ret = epoll_wait(epoll_fd, events, NELEM(events), msec);
		if (ret <= 0) {
			if (errno == EINTR) {
				continue;
			}

			if (ret < 0) {
				pr_err_info("epoll_wait: %d", ret);
			}

			return ret;
		}

		for (p = events, p_end = p + ret; p < p_end; p++) {
			ssize_t rdlen;
			char buff[1024];
			int *fds = p->data.ptr;

			rdlen = ffile_read(fds[0], buff, sizeof(buff));
			if (rdlen <= 0) {
				if (fds[0] == 0) {
					cavan_file_proxy_del(desc, fds);
					msec = 500;
					continue;
				}

				return 0;
			}

			if (ffile_write(fds[1], buff, rdlen) != rdlen) {
				return 0;
			}

#if CAVAN_FILE_PROXY_DEBUG
			println("%d => %d, length = %" PRINT_FORMAT_SIZE, fds[0], fds[1], rdlen);
#endif
		}
	}

	return 0;
}

char *file_abs_path_simple(char *rel_path, char *buff, size_t size, bool logical)
{
	char *buff_end = buff + size;

	if (rel_path[0] != '/') {
		const char *pwd;

		if (logical && (pwd = getenv("PWD"))) {
			buff = text_ncopy(buff, pwd, size);
		} else if (getcwd(buff, size)) {
			buff += strlen(buff);
		}

		if (*buff != '/' && buff < buff_end) {
			*buff++ = '/';
		}
	}

	return text_ncopy(buff, rel_path, buff_end - buff);
}

int file_write_u64(const char *pathname, u64 value)
{
	int length;
	char buff[64];

	length = value2text_unsigned_simple(value, buff, sizeof(buff), 10) - buff;

	return file_write(pathname, buff, length);
}

int file_write_s64(const char *pathname, s64 value)
{
	int length;
	char buff[64];

	length = value2text_simple(value, buff, sizeof(buff), 10) - buff;

	return file_write(pathname, buff, length);
}

u64 file_read_u64(const char *pathname, u64 def_value)
{
	int ret;
	char buff[64];

	ret = file_read(pathname, buff, sizeof(buff));
	if (ret < 0) {
		return def_value;
	}

	return text2value_unsigned(buff, NULL, 10);
}

s64 file_read_s64(const char *pathname, s64 def_value)
{
	int ret;
	char buff[64];

	ret = file_read(pathname, buff, sizeof(buff));
	if (ret < 0) {
		return def_value;
	}

	return text2value(buff, NULL, 10);
}

int cavan_symlink(const char *target, const char *linkpath)
{
	if (symlink(target, linkpath) == 0) {
		return 0;
	}

	if (errno != EEXIST) {
		return -EFAULT;
	}

	if (file_is_directory2(linkpath)) {
		char pathname[1024];
		const char *filename;

		filename = cavan_path_basename_simple(target);
		if (filename == NULL) {
			return -EFAULT;
		}

		cavan_path_cat(pathname, sizeof(pathname), linkpath, filename, false);

		return symlink(target, pathname);
	}

	unlink(linkpath);

	return symlink(target, linkpath);
}

int cavan_rename_part(const char *pathname, const char *start, const char *end)
{
	char buff[1024];
	char *p = buff;
	char *p_end = p + sizeof(buff);
	const char *q = pathname;

	while (q < start && p < p_end) {
		char c = *q;

		*p++ = c;

		if (c == '/') {
			q = start;
			break;
		}

		q++;
	}

	while (q < end && p < p_end) {
		*p++ = *q++;
	}

	*p = 0;

	pd_info("rename: %s => %s", pathname, buff);

	return rename(pathname, buff);
}
