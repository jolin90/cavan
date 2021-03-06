ARCH ?= host
ROOT_PATH = $(shell pwd)
CAVAN_NAME = cavan
BUILD_PATH = $(ROOT_PATH)/build
BUILD_CORE_PATH = $(BUILD_PATH)/core
APP_CORE_PATH = $(ROOT_PATH)/app/core
INCLUDE_PATH = $(ROOT_PATH)/include
SUB_DIRS = lib cpp app

APP_PREFIX = $(CAVAN_NAME)-
MAKEFILE_CAVAN = $(CAVAN_NAME).mk
BUILD_LIBA = $(BUILD_CORE_PATH)/build_liba.mk
BUILD_LIBSO = $(BUILD_CORE_PATH)/build_libso.mk
BUILD_EXECUTE = $(BUILD_CORE_PATH)/build_execute.mk
BUILD_EXEC_PKG = $(BUILD_CORE_PATH)/build_exec_pkg.mk

OUT_PATH = out/$(ARCH)
OUT_LIB = $(OUT_PATH)/lib
OUT_OBJ = $(OUT_PATH)/obj
OUT_BIN = $(OUT_PATH)/$(BUILD_TYPE)

CC = $(CROSS_COMPILE)gcc
CPP = $(CROSS_COMPILE)g++
LD = $(CROSS_COMPILE)ld
AR = $(CROSS_COMPILE)ar
STRIP = $(CROSS_COMPILE)strip
RM = rm -rf
MAKE = +make
MKDIR = mkdir -p
INSTALL = install -c
CP = cp

CAVAN_OS_NAME = $(shell uname -s)

CFLAGS += -DCAVAN_OS=\"$(CAVAN_OS_NAME)\"

ifeq "${CAVAN_OS_NAME}" "Darwin"
CAVAN_OS_MAC = true
CFLAGS += -DCAVAN_OS_MAC -I${INCLUDE_PATH}/mac
endif

ifeq "${CAVAN_OS_NAME}" "Linux"
CAVAN_OS_LINUX = true
CFLAGS += -DCAVAN_OS_LINUX
endif

CAVAN_PLAT = $(shell $(CC) -dumpmachine)

ifneq ($(CAVAN_OS_MAC),true)
CFLAGS += -frename-registers -fgcse-after-reload -frerun-cse-after-loop -fno-inline-functions-called-once -finline-limit=64 -finline-functions
endif

CFLAGS +=	-Wall -Wundef -Wextra -Werror -Wsign-compare -Winit-self -Wpointer-arith -Wa,--noexecstack -Wstrict-aliasing=2 -Wno-unused-parameter \
			-fno-strict-aliasing -fno-exceptions -ffunction-sections -funwind-tables -fstack-protector -fomit-frame-pointer \
			-g -I$(INCLUDE_PATH) -DCAVAN -DCAVAN_ARCH=\"$(ARCH)\" -DCAVAN_PLAT=\"$(CAVAN_PLAT)\" -include cavan/config.h

CFLAGS += -DCAVAN_ARCH_$(shell echo $(ARCH) | tr '[a-z]' '[A-Z]')

ifeq ($(BUILD_TYPE),debug)
CFLAGS += -DCAVAN_DEBUG

ifneq ($(CAVAN_OS_MAC),true)
CFLAGS += -rdynamic
endif

SUB_DIRS += test
endif

ifeq ($(BUILD_OTHERS),true)
LDFLAGS += -lasound -ldl
endif

CONFIG_CAVAN_SSL_CERT ?= $(ROOT_PATH)/config/ssl/cert.pem
CONFIG_CAVAN_SSL_KEY ?= $(ROOT_PATH)/config/ssl/key.pem

ifeq ($(CONFIG_CAVAN_SSL),true)
CFLAGS += -DCONFIG_CAVAN_SSL
CFLAGS += -DCONFIG_CAVAN_SSL_CERT=\"$(CONFIG_CAVAN_SSL_CERT)\"
CFLAGS += -DCONFIG_CAVAN_SSL_KEY=\"$(CONFIG_CAVAN_SSL_KEY)\"
ifneq ($(CONFIG_CAVAN_SSL_PASSWORD),)
CFLAGS += -DCONFIG_CAVAN_SSL_PASSWORD=\"$(CONFIG_CAVAN_SSL_PASSWORD)\"
endif
LDFLAGS += -lssl
endif

ifneq ($(BUILD_ENTRY),cavan)
CFLAGS += -Wmissing-declarations
endif

ifneq ($(FRAME_SIZE),)
CFLAGS += -Wframe-larger-than=$(FRAME_SIZE)
endif

CPPFLAGS := $(CPPFLAGS) $(CFLAGS)
CFLAGS += -Werror-implicit-function-declaration -Wstrict-prototypes
LDFLAGS += -lm -lrt -lpthread

ifeq ($(BUILD_TYPE),static)
LDFLAGS += -static
endif

ASFLAGS += $(CFLAGS) -D__ASM__
LDFLAGS += -L$(OUT_LIB) -Wl,-rpath,$(ROOT_PATH)/$(OUT_LIB)

DESTDIR = /usr

ifeq ($(Q),@)
MAKEFLAGS += --no-print-directory
endif

GCC_COLORS = error=01;31:warning=01;35:note=01;36:caret=01;32:locus=01:quote=01

export GCC_COLORS

include $(BUILD_CORE_PATH)/defines3.mk

$(foreach path,$(OUT_LIB) $(OUT_BIN),$(shell [ -d $(path) ] || $(MKDIR) $(path)))

all: all-modules
jni:
	@+make -C "${ROOT_PATH}/android/app/CavanJni/jni"

clean distclean:
	@rm -rf "$(OUT_PATH)"

$(foreach sub,$(SUB_DIRS),$(eval $(call build_sub_module,$(sub))))

all-modules: $(MODULES)

$(ROOT_PATH)/include/cavan/config.h : Makefile
	@touch $@

.PRECIOUS: $(APP_OBJ_FILES) $(LIB_OBJ_FILES) $(CAVAN_OBJ_FILES)
.PHONY: uninstall uninstall-header uninstall-lib uninstall-bin
