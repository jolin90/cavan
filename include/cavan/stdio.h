#pragma once

#include <cavan.h>
#include <stdarg.h>

#ifdef __cplusplus
#ifndef CONFIG_ANDROID
#include <iostream>
#endif

using namespace std;
#endif

#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG	"Cavan"

#define DEFAULT_CONSOLE_DEVICE	"/dev/tty0"

#define FONT_DEFAULT			0	//set all attributes to their defaults
#define FONT_BOLD				1	//set bold

#define FONT_BLACK_FOREGROUND	30	//set black foreground
#define FONT_RED_FOREGROUND		31	//set red foreground
#define FONT_GREEN_FOREGROUND	32	//set green foreground
#define FONT_BROWN_FOREGROUND	33	//set brown foreground
#define FONT_BLUE_FOREGROUND	34	//set blue foreground
#define FONT_MAGENTA_FOREGROUND	35	//set magenta foreground
#define FONT_CYAN_FOREGROUND	36	//set cyan foreground
#define FONT_WHITE_FOREGROUND	37	//set white foreground
#define FONT_DEFAULT_FOREGROUND	39	//set white foreground

#define FONT_BLACK_BACKGROUND	40	//set black background
#define FONT_RED_BACKGROUND		41	//set red background
#define FONT_GREEN_BACKGROUND	42	//set green background
#define FONT_BROWN_BACKGROUND	43	//set brown background
#define FONT_BLUE_BACKGROUND	44	//set blue background
#define FONT_MAGENTA_BACKGROUND	45	//set magenta background
#define FONT_CYAN_BACKGROUND	46	//set cyan background
#define FONT_WHITE_BACKGROUND	47	//set white background
#define FONT_DEFAULT_BACKGROUND	49	//set default background color

#ifndef __THROW
#define __THROW
#endif

#ifndef __THROWNL
#define __THROWNL __THROW
#endif

#ifdef __cplusplus
#undef __THROWNL
#define __THROWNL
#endif

#ifndef _POSIX_VDISABLE
#define	_POSIX_VDISABLE	'\0'
#endif

#define printf_format(a, b)		__THROWNL __attribute__ ((__format__ (__printf__, a, b)))

#define __printf_format_10__	printf_format(1, 0)
#define __printf_format_12__	printf_format(1, 2)
#define __printf_format_20__	printf_format(2, 0)
#define __printf_format_23__	printf_format(2, 3)
#define __printf_format_30__	printf_format(3, 0)
#define __printf_format_34__	printf_format(3, 4)
#define __printf_format_40__	printf_format(4, 0)
#define __printf_format_45__	printf_format(4, 5)
#define __printf_format_50__	printf_format(5, 0)
#define __printf_format_56__	printf_format(5, 6)

#define println_black(fmt, arg ...)		print_color_text(FONT_BLACK_FOREGROUND, fmt, ##arg)
#define println_red(fmt, arg ...)		print_color_text(FONT_RED_FOREGROUND, fmt, ##arg)
#define println_green(fmt, arg ...)		print_color_text(FONT_GREEN_FOREGROUND, fmt, ##arg)
#define println_brown(fmt, arg ...)		print_color_text(FONT_BROWN_FOREGROUND, fmt, ##arg)
#define println_blue(fmt, arg ...)		print_color_text(FONT_BLUE_FOREGROUND, fmt, ##arg)
#define println_magenta(fmt, arg ...)	print_color_text(FONT_MAGENTA_FOREGROUND, fmt, ##arg)
#define println_cyan(fmt, arg ...)		print_color_text(FONT_CYAN_FOREGROUND, fmt, ##arg)
#define println_white(fmt, arg ...)		print_color_text(FONT_WHITE_FOREGROUND, fmt, ##arg)

#define CAVAN_COLOR_STAND				"\033[0m"
#define CAVAN_COLOR_BOLD				"\033[1m"
#define CAVAN_COLOR_RED					"\033[31m"
#define CAVAN_COLOR_GREEN				"\033[32m"
#define CAVAN_COLOR_BLUE				"\033[34m"
#define CAVAN_COLOR_BROWN				"\033[33m"
#define CAVAN_COLOR_MAGENTA				"\033[35m"

#ifdef CONFIG_ANDROID_NDK
#define PRINT_FORMAT_UID				"ld"
#define PRINT_FORMAT_SIZE				"d"
#define PRINT_FORMAT_SSIZE				"ld"
#define PRINT_FORMAT_INT64				"Ld"
#define PRINT_FORMAT_OFF				"Ld"
#else
#if __WORDSIZE == 64
#define PRINT_FORMAT_SIZE				"ld"
#define PRINT_FORMAT_INT64				"ld"
#define PRINT_FORMAT_OFF				"ld"
#else
#define PRINT_FORMAT_SIZE				"d"
#define PRINT_FORMAT_INT64				"Ld"
#define PRINT_FORMAT_OFF				"Ld"
#endif

#define PRINT_FORMAT_SSIZE				PRINT_FORMAT_SIZE

#if defined(CONFIG_ANDROID) && CONFIG_ANDROID_VERSION < 5
#define PRINT_FORMAT_UID				"ld"
#else
#define PRINT_FORMAT_UID				"d"
#endif
#endif

#define stdin_fd						0
#define stdout_fd						1
#define stderr_fd						2

#define CAVAN_TTY_MODE_DATA				3
#define CAVAN_TTY_MODE_AT				4
#define CAVAN_TTY_MODE_SSH				5
#define CAVAN_TTY_MODE_CMDLINE			6

#define CAVAN_TTY_SET_TITLE(fmt, args ...) \
	do { \
		printf("\033]0;" fmt "\007", ##args); \
		fflush(stdout); \
	} while (0)

// ============================================================

#define cavan_std_info(func, fmt, args ...) \
	func(fmt, ##args)

#define cavan_func_info(func, fmt, args ...) \
	func("%s[%d]: " fmt, __FUNCTION__, __LINE__, ##args)

#define cavan_color_info(func, color, fmt, args ...) \
	func(color fmt CAVAN_COLOR_STAND, ##args)

#define cavan_red_info(func, fmt, args ...) \
	cavan_color_info(func, CAVAN_COLOR_RED, "%s[%d]: " fmt, __FUNCTION__, __LINE__, ##args)

#define cavan_error_info(func, fmt, args ...) \
	if (errno) { \
		cavan_color_info(func, CAVAN_COLOR_RED, "error: %s[%d] (" fmt "): %s (%d)", __FUNCTION__, __LINE__, ##args, strerror(errno), errno); \
	} else { \
		cavan_red_info(func, "error: " fmt, ##args); \
	}

#define cavan_err_info(func, fmt, args ...) \
	cavan_error_info(func, fmt, ##args)

#define cavan_warning_info(func, fmt, args ...) \
	cavan_color_info(func, CAVAN_COLOR_MAGENTA, "warning: %s[%d]: " fmt, __FUNCTION__, __LINE__, ##args)

#define cavan_warn_info(func, fmt, args ...) \
	cavan_warning_info(func, fmt, ##args)

#define cavan_green_info(func, fmt, args ...) \
	cavan_color_info(func, CAVAN_COLOR_GREEN, fmt, ##args)

#define cavan_blue_info(func, fmt, args ...) \
	cavan_color_info(func, CAVAN_COLOR_BLUE, fmt, ##args)

#define cavan_bold_info(func, fmt, args ...) \
	cavan_color_info(func, CAVAN_COLOR_BOLD, fmt, ##args)

#define cavan_brown_info(func, fmt, args ...) \
	cavan_color_info(func, CAVAN_COLOR_BROWN, fmt, ##args)

#define cavan_magenta_info(func, fmt, args ...) \
	cavan_color_info(func, CAVAN_COLOR_MAGENTA, fmt, ##args)

#define cavan_std_pos(func, fmt) \
	func(fmt, __FILE__, __FUNCTION__, __LINE__)

#define cavan_pos_info(func) \
	cavan_std_pos(func, "%s => %s[%d]")

#define cavan_color_pos(func, color) \
	cavan_std_pos(func, color "%s => %s[%d]" CAVAN_COLOR_STAND)

#define cavan_red_pos(func) \
	cavan_color_pos(func, CAVAN_COLOR_RED);

#define cavan_green_pos(func) \
	cavan_color_pos(func, CAVAN_COLOR_GREEN);

#define cavan_blue_pos(func) \
	cavan_color_pos(func, CAVAN_COLOR_BLUE);

#define cavan_bold_pos(func) \
	cavan_color_pos(func, CAVAN_COLOR_BOLD);

#define cavan_date_info(func, name) \
	func(CAVAN_COLOR_GREEN "Cavan %s Build Date: %s %s" CAVAN_COLOR_STAND, name, __DATE__, __TIME__);

#define cavan_result_info(func, ret) \
	((ret) < 0 ? cavan_red_info(func, "Failed") : cavan_green_info(func, "OK"))

// ============================================================

#define pr_info_base(fmt, args ...) \
	printf(fmt "\n", ##args)

#ifdef CONFIG_ANDROID
#ifdef CONFIG_ANDROID_NDK
#include <android-ndk/log.h>
#else
#include <utils/Log.h>
#endif

#ifndef LOGD
#define LOGD					ALOGD
#endif

#ifndef LOGE
#define LOGE					ALOGE
#endif

#define pd_info_base(fmt, args ...) \
	LOGD(fmt "\n", ##args);
#else
#define pd_info_base(fmt, args ...) \
	fprintf(stderr, fmt "\n", ##args)
#endif

#define pr_pd_info(fmt, args ...) \
	do { \
		pr_info_base(fmt, ##args); \
		pd_info_base(fmt, ##args); \
	} while (0)

#ifdef CONFIG_ANDROID_NDK
#define pr_info(fmt, args ...) \
	pr_pd_info(fmt, ##args)
#else
#define pr_info(fmt, args ...) \
	pr_info_base(fmt, ##args)
#endif

#ifdef CONFIG_ANDROID
#define pd_info(fmt, args ...) \
	pr_pd_info(fmt, ##args)
#else
#define pd_info(fmt, args ...) \
	pd_info_base(fmt, ##args)
#endif

// ============================================================

#define pr_std_info(fmt, args ...) \
	cavan_std_info(pr_info, fmt, ##args)

#define pr_func_info(fmt, args ...) \
	cavan_func_info(pr_info, fmt, ##args)

#define pr_color_info(color, fmt, args ...) \
	cavan_color_info(pr_info, fmt, ##args)

#define pr_red_info(fmt, args ...) \
	cavan_red_info(pr_info, fmt, ##args)

#define pr_error_info(fmt, args ...) \
	cavan_error_info(pr_info, fmt, ##args)

#define pr_err_info(fmt, args ...) \
	cavan_err_info(pr_info, fmt, ##args)

#define pr_warning_info(fmt, args ...) \
	cavan_warning_info(pr_info, fmt, ##args)

#define pr_warn_info(fmt, args ...) \
	cavan_warn_info(pr_info, fmt, ##args)

#define pr_green_info(fmt, args ...) \
	cavan_green_info(pr_info, fmt, ##args)

#define pr_blue_info(fmt, args ...) \
	cavan_blue_info(pr_info, fmt, ##args)

#define pr_bold_info(fmt, args ...) \
	cavan_bold_info(pr_info, fmt, ##args)

#define pr_brown_info(fmt, args ...) \
	cavan_brown_info(pr_info, fmt, ##args)

#define pr_magenta_info(fmt, args ...) \
	cavan_magenta_info(pr_info, fmt, ##args)

#define pr_std_pos(fmt) \
	cavan_std_pos(pr_info, fmt)

#define pr_pos_info() \
	cavan_pos_info(pr_info)

#define pr_color_pos(color) \
	cavan_color_pos(pr_info, color)

#define pr_red_pos() \
	cavan_red_pos(pr_info)

#define pr_green_pos() \
	cavan_green_pos(pr_info)

#define pr_blue_pos() \
	cavan_blue_pos(pr_info)

#define pr_bold_pos() \
	cavan_bold_pos(pr_info)

#define pr_date_info(name) \
	cavan_date_info(pr_info, name)

#define pr_result_info(ret) \
	cavan_result_info(pr_info, ret)

// ============================================================

#define pd_std_info(fmt, args ...) \
	cavan_std_info(pd_info, fmt, ##args)

#define pd_func_info(fmt, args ...) \
	cavan_func_info(pd_info, fmt, ##args)

#define pd_color_info(color, fmt, args ...) \
	cavan_color_info(pd_info, fmt, ##args)

#define pd_red_info(fmt, args ...) \
	cavan_red_info(pd_info, fmt, ##args)

#define pd_error_info(fmt, args ...) \
	cavan_error_info(pd_info, fmt, ##args)

#define pd_err_info(fmt, args ...) \
	cavan_err_info(pd_info, fmt, ##args)

#define pd_warning_info(fmt, args ...) \
	cavan_warning_info(pd_info, fmt, ##args)

#define pd_warn_info(fmt, args ...) \
	cavan_warn_info(pd_info, fmt, ##args)

#define pd_green_info(fmt, args ...) \
	cavan_green_info(pd_info, fmt, ##args)

#define pd_blue_info(fmt, args ...) \
	cavan_blue_info(pd_info, fmt, ##args)

#define pd_bold_info(fmt, args ...) \
	cavan_bold_info(pd_info, fmt, ##args)

#define pd_brown_info(fmt, args ...) \
	cavan_brown_info(pd_info, fmt, ##args)

#define pd_magenta_info(fmt, args ...) \
	cavan_magenta_info(pd_info, fmt, ##args)

#define pd_std_pos(fmt) \
	cavan_std_pos(pd_info, fmt)

#define pd_pos_info() \
	cavan_pos_info(pd_info)

#define pd_color_pos(color) \
	cavan_color_pos(pd_info, color)

#define pd_red_pos() \
	cavan_red_pos(pd_info)

#define pd_green_pos() \
	cavan_green_pos(pd_info)

#define pd_blue_pos() \
	cavan_blue_pos(pd_info)

#define pd_bold_pos() \
	cavan_bold_pos(pd_info)

#define pd_date_info(name) \
	cavan_date_info(pd_info, name)

#define pd_result_info(ret) \
	cavan_result_info(pd_info, ret)

// ============================================================

#define show_value(val) \
	println(#val " = %d", val)

#define show_valueh_base(val, len) \
	println(#val " = 0x%0" len "x", val)

#define show_valueh(val) \
	do { \
		switch (sizeof(val)) { \
		case 8: \
			show_value_base(val, "16"); \
			break; \
		case 4: \
			show_value_base(val, "8"); \
			break; \
		case 2: \
			show_value_base(val, "4"); \
			break; \
		default: \
			show_value_base(val, "2"); \
		} \
	} while (0)

#define print_array(a) \
	do { \
		switch (sizeof(a[0])) { \
		case 8: \
			text_show64((const u64 *) a, ARRAY_SIZE(a)); \
			break; \
		case 4: \
			text_show32((const u32 *) a, ARRAY_SIZE(a)); \
			break; \
		case 2: \
			text_show16((const u16 *) a, ARRAY_SIZE(a)); \
			break; \
		default: \
			text_show((const char *) a, sizeof(a)); \
		} \
	} while (0)

#define print_text(text)					print_ntext(text, strlen(text))

#define clear_screen_to_current()			print_text("\033[1J")
#define clear_whole_screen()				print_text("\033[2J")
#define clear_line_to_current()				print_text("\033[1K")
#define clear_whole_line()					print_text("\033[2K")
#define clear_console()						print_text("\033[1J\033[1;1H")
#define set_default_font()					print_text("\033[0m")
#define set_console_cursor(row, col)		print("\033[%d;%dH", row, col)
#define set_console_row(row)				print("\033[%dd", row)
#define set_console_col(col)				print("\033[%dG", col)
#define save_console_cursor()				print_text("\033[%ds")
#define restore_console_cursor()			print_text("\033[%du")

#define cavan_stdio_function_declarer(name, pathname) \
	static FILE *cavan_stdio_fp_##name; \
	FILE *cavan_stdio_##name##_open(void) { \
		FILE *fp; \
		if (likely(cavan_stdio_fp_##name)) { \
			return cavan_stdio_fp_##name; \
		} \
		fp = fopen(pathname, "r+"); \
		if (fp == NULL) { \
			return fp; \
		} \
		setlinebuf(fp); \
		cavan_stdio_fp_##name = fp; \
		return fp; \
	} \
	void cavan_stdio_##name##_close(void) { \
		if (cavan_stdio_fp_##name) { \
			FILE *fp = cavan_stdio_fp_##name; \
			cavan_stdio_fp_##name = NULL; \
			fclose(fp); \
		} \
	} \
	int cavan_stdio_##name##_fflush(void) { \
		FILE *fp = cavan_stdio_##name##_open(); \
		if (likely(fp)) { \
			return fflush(fp); \
		} \
		return -EFAULT; \
	} \
	int cavan_stdio_##name##_putchar(int c) { \
		FILE *fp = cavan_stdio_##name##_open(); \
		if (likely(fp)) { \
			return fputc(c, fp); \
		} \
		return -EFAULT; \
	} \
	int cavan_stdio_##name##_getchar(void) { \
		FILE *fp = cavan_stdio_##name##_open(); \
		if (likely(fp)) { \
			return fgetc(fp); \
		} \
		return -EFAULT; \
	} \
	int cavan_stdio_##name##_puts(const char *text) { \
		FILE *fp = cavan_stdio_##name##_open(); \
		if (likely(fp)) { \
			return fputs(text, fp); \
		} \
		return -EFAULT; \
	} \
	char *cavan_stdio_##name##_gets(char *buff, int size) { \
		FILE *fp = cavan_stdio_##name##_open(); \
		if (likely(fp)) { \
			return fgets(buff, size, fp); \
		} \
		return NULL; \
	} \
	int cavan_stdio_##name##_vprintf(const char *format, va_list ap) { \
		FILE *fp = cavan_stdio_##name##_open(); \
		if (likely(fp)) { \
			return vfprintf(fp, format, ap); \
		} \
		return -EFAULT; \
	} \
	int cavan_stdio_##name##_vprintln(const char *format, va_list ap) { \
		FILE *fp = cavan_stdio_##name##_open(); \
		if (likely(fp)) { \
			return vfprintf(fp, format, ap) | fputc('\n', fp); \
		} \
		return -EFAULT; \
	} \
	int cavan_stdio_##name##_printf(const char *format, ...) { \
		int ret; \
		va_list ap; \
		va_start(ap, format); \
		ret = cavan_stdio_##name##_vprintf(format, ap); \
		va_end(ap); \
		return ret; \
	} \
	int cavan_stdio_##name##_println(const char *format, ...) { \
		int ret; \
		va_list ap; \
		va_start(ap, format); \
		ret = cavan_stdio_##name##_vprintln(format, ap); \
		va_end(ap); \
		return ret; \
	} \
	int cavan_stdio_##name##_scanf(const char *format, ...) { \
		FILE *fp = cavan_stdio_##name##_open(); \
		if (likely(fp)) { \
			int ret; \
			va_list ap; \
			va_start(ap, format); \
			ret = vfscanf(fp, format, ap); \
			va_end(ap); \
			return ret; \
		} \
		return -EFAULT; \
	}

__BEGIN_DECLS

FILE *cavan_stdio_tty_open(void);
void cavan_stdio_tty_close(void);
int cavan_stdio_tty_getchar(void);
int cavan_stdio_tty_putchar(int c);
char *cavan_stdio_tty_gets(char *buff, int size);
int cavan_stdio_tty_puts(const char *text);
int cavan_stdio_tty_fflush(void);
__printf_format_10__ int cavan_stdio_tty_vprintf(const char *format, va_list ap);
__printf_format_10__ int cavan_stdio_tty_vprintln(const char *format, va_list ap);
__printf_format_12__ int cavan_stdio_tty_printf(const char *format, ...);
__printf_format_12__ int cavan_stdio_tty_println(const char *format, ...);
__printf_format_12__ int cavan_stdio_tty_scanf(const char *format, ...);

FILE *cavan_stdio_kmsg_open(void);
void cavan_stdio_kmsg_close(void);
int cavan_stdio_kmsg_getchar(void);
int cavan_stdio_kmsg_putchar(int c);
char *cavan_stdio_kmsg_gets(char *buff, int size);
int cavan_stdio_kmsg_puts(const char *text);
int cavan_stdio_kmsg_fflush(void);
__printf_format_10__ int cavan_stdio_kmsg_vprintf(const char *format, va_list ap);
__printf_format_10__ int cavan_stdio_kmsg_vprintln(const char *format, va_list ap);
__printf_format_12__ int cavan_stdio_kmsg_printf(const char *format, ...);
__printf_format_12__ int cavan_stdio_kmsg_println(const char *format, ...);
__printf_format_12__ int cavan_stdio_kmsg_scanf(const char *format, ...);

int cavan_tty_set_attr(int fd, int action, struct termios *attr);
int cavan_tty_set_mode(int fd, int mode, struct termios *attr_bak);
int cavan_tty_attr_restore(int fd, struct termios *attr);

int cavan_has_char(long sec, long usec);
int cavan_getchar_timed(long sec, long usec);

int print_ntext(const char *text, size_t size);
void print_buffer(const char *buff, size_t size);
void print_title(const char *title, char sep, size_t size);
void print_sep(size_t size);

void print_mem(const char *promp, const u8 *mem, size_t size, ...);

int show_file(const char *dev_name, u64 start, u64 size);
int cat_file(const char *filename);

int open_console(const char *dev_path);
void close_console(void);
void fflush_console(void);

void show_menu(int x, int y, const char *menu[], int count, int select);
int get_menu_select(const char *input_dev_path, const char *menu[], int count);

int fswitch2text_mode(int tty_fd);
int switch2text_mode(const char *tty_path);
int fswitch2graph_mode(int tty_fd);
int switch2graph_mode(const char *tty_path);
void show_author_info(void);

__printf_format_34__ char *sprint(char *buff, size_t size, const char *fmt, ...);
__printf_format_34__ char *sprintln(char *buff, size_t size, const char *fmt, ...);
__printf_format_12__ int print(const char *fmt, ...);
__printf_format_10__ int vprint(const char *fmt, va_list ap);
__printf_format_10__ int vprintln(const char *fmt, va_list ap);
__printf_format_12__ int println(const char *fmt, ...);
__printf_format_23__ void print_bit_mask(u64 value, const char *prompt, ...);
__printf_format_34__ void print_to(int x, int y, const char *fmt, ...);
__printf_format_23__ void print_to_row(int row, const char *fmt, ...);
__printf_format_23__ void print_to_col(int col, const char *fmt, ...);
__printf_format_34__ void println_to(int x, int y, const char *fmt, ...);
__printf_format_23__ void println_to_row(int row, const char *fmt, ...);
__printf_format_23__ void println_to_col(int col, const char *fmt, ...);
__printf_format_20__ void vprint_color_text(int color, const char *fmt, va_list ap);
__printf_format_23__ void print_color_text(int color, const char *fmt, ...);
__printf_format_12__ void print_error_base(const char *fmt, ...);

extern char *size2text(u64 size);
bool cavan_get_choose_yesno(const char *prompt, bool def_value, int timeout_ms);
__printf_format_34__ bool cavan_get_choose_yesno_format(bool def_choose, int timeout_ms, const char *format, ...);

int cavan_async_vprintf(const char *fmt, va_list ap);
int cavan_async_printf(const char *fmt, ...);
int cavan_async_fflush(void);

int cavan_stdio_redirect1(int ttyfds[3]);
int cavan_stdio_redirect2(int fd, int flags);
int cavan_stdio_redirect3(const char *pathname, int flags);

// ============================================================

static inline int cavan_stdio_fflush(void)
{
	return fflush(stdout) | fflush(stderr);
}

static inline void cavan_stdio_setlinebuf(void)
{
	setlinebuf(stdout);
	setlinebuf(stderr);
}

static inline void print_char(char c)
{
	print_ntext(&c, 1);
}

static inline void set_console_font(int font, int foregound, int backgound)
{
	print("\033[%d;%d;%dm", font, foregound, backgound);
}

static inline void set_console_font_simple(int font)
{
	print("\033[%dm", font);
}

static inline void print_string(const char *str)
{
	print_text(str);
	print_char('\n');
}

static inline void print_string_to(int x, int y, const char *str)
{
	set_console_cursor(y, x);
	print_string(str);
}

static inline void print_string_to_row(int row, const char *str)
{
	set_console_row(row);
	print_string(str);
}

static inline void print_string_to_col(int col, const char *str)
{
	set_console_col(col);
	print_string(str);
}

static inline void print_size(u64 size)
{
	print_text(size2text(size));
	print_char('\n');
}

static inline int cavan_tty_get_attr(int fd, struct termios *attr)
{
	return tcgetattr(fd, attr);
}

__END_DECLS;
