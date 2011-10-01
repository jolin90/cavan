ROOT_PATH = $(shell pwd)
BUILD_PATH = $(ROOT_PATH)/build
PACKAGE_PATH = $(ROOT_PATH)/package
PATCH_PATH = $(ROOT_PATH)/patch
SCRIPT_PATH = $(BUILD_PATH)/script
DOWNLOAD_PATH = $(ROOT_PATH)/download

WORK_PATH = $(ROOT_PATH)/work
SRC_PATH = $(WORK_PATH)/src
UTILS_PATH = $(WORK_PATH)/utils
DECOMP_PATH = $(WORK_PATH)/decomp
MARK_PATH = $(WORK_PATH)/mark
OUT_PATH = $(WORK_PATH)/out

SYSROOT_PATH = $(WORK_PATH)/sysroot/$(CAVAN_TARGET_ARCH)
OUT_TOOLCHIAN = $(OUT_PATH)/toolchian/$(CAVAN_TARGET_ARCH)
OUT_LIBRARY = $(OUT_PATH)/library/$(CAVAN_TARGET_ARCH)
OUT_UTILS = $(OUT_PATH)/utils
MARK_TOOLCHIAN = $(MARK_PATH)/toolchian/$(CAVAN_TARGET_ARCH)
MARK_LIBRARY = $(MARK_PATH)/library/$(CAVAN_TARGET_ARCH)
MARK_UTILS = $(MARK_PATH)/utils

BUILD_CORE = $(BUILD_PATH)/core
BUILD_TOOLCHIAN = $(BUILD_PATH)/toolchian
BUILD_LIBRARY = $(BUILD_PATH)/library
BUILD_UTILS = $(BUILD_PATH)/utils

MARK_TOOLCHIAN_READY = $(MARK_TOOLCHIAN)/ready
MARK_LIBRARY_READY = $(MARK_LIBRARY)/ready
MARK_UTILS_READY = $(MARK_UTILS)/ready

MAKEFILE_TOOLCHIAN = $(BUILD_TOOLCHIAN)/main.mk
MAKEFILE_LIBRARY = $(BUILD_LIBRARY)/main.mk
MAKEFILE_UTILS = $(BUILD_UTILS)/main.mk
MAKEFILE_DEFINES = $(BUILD_CORE)/defines.mk
MAKEFILE_INSTALL = $(BUILD_CORE)/install.mk

PYTHON_PARSER = $(SCRIPT_PATH)/parser.py

PATH := $(PATH):$(SYSROOT_PATH)/bin:$(SYSROOT_PATH)/bin:$(SYSROOT_PATH)/usr/bin:$(SYSROOT_PATH)/usr/sbin:$(SYSROOT_PATH)/usr/local/bin:$(SYSROOT_PATH)/usr/local/sbin
PATH := $(PATH):$(UTILS_PATH)/bin:$(UTILS_PATH)/bin:$(UTILS_PATH)/usr/bin:$(UTILS_PATH)/usr/sbin:$(UTILS_PATH)/usr/local/bin:$(UTILS_PATH)/usr/local/sbin

export ROOT_PATH PACKAGE_PATH BUILD_PATH PATCH_PATH SCRIPT_PATH
export SRC_PATH SYSROOT_PATH UTILS_PATH DECOMP_PATH PATH DOWNLOAD_PATH
export OUT_PATH OUT_UTILS OUT_TOOLCHIAN OUT_LIBRARY
export BUILD_CORE BUILD_TOOLCHIAN BUILD_LIBRARY BUILD_UTILS
export MARK_PATH MARK_TOOLCHIAN MARK_LIBRARY MARK_UTILS
export MARK_TOOLCHIAN_READY MARK_LIBRARY_READY MARK_UTILS_READY
export MAKEFILE_DEFINES MAKEFILE_INSTALL
export PYTHON_PARSER XML_APPLICATION

$(info ============================================================)
$(info CAVAN_BUILD_ARCH = $(CAVAN_BUILD_ARCH))
$(info CAVAN_BUILD_PLAT = $(CAVAN_BUILD_PLAT))
$(info CAVAN_TARGET_ARCH = $(CAVAN_TARGET_ARCH))
$(info CAVAN_TARGET_PLAT = $(CAVAN_TARGET_PLAT))
$(info CPU_BINUTILS_OPTION = $(CPU_BINUTILS_OPTION))
$(info CPU_GCC_OPTION = $(CPU_GCC_OPTION))
$(info KERNEL_VERSION = $(KERNEL_VERSION))
$(info BINUTILS_VERSION = $(BINUTILS_VERSION))
$(info GCC_VERSION = $(GCC_VERSION))
$(info GLIBC_VERSION = $(GLIBC_VERSION))
$(info PACKAGE_PATH = $(PACKAGE_PATH))
$(info PATCH_PATH = $(PATCH_PATH))
$(info ============================================================)

include $(MAKEFILE_DEFINES)

all: build_env $(MARK_LIBRARY_READY)
	$(Q)echo "All tools compile successfull"

$(MARK_LIBRARY_READY): $(MARK_TOOLCHIAN_READY)
	$(Q)+make -f $(MAKEFILE_LIBRARY)

$(MARK_TOOLCHIAN_READY): $(MARK_UTILS_READY)
	$(Q)+make -f $(MAKEFILE_TOOLCHIAN)

$(MARK_UTILS_READY):
	$(Q)+make -f $(MAKEFILE_UTILS)

clean:
	$(Q)rm $(WORK_PATH) -rfv

build_env:
	$(Q)mkdir $(SRC_PATH) $(SYSROOT_PATH) $(UTILS_PATH) $(OUT_UTILS) $(OUT_TOOLCHIAN) $(OUT_LIBRARY) $(DECOMP_PATH) $(DOWNLOAD_PATH) -pv
	$(Q)mkdir $(MARK_TOOLCHIAN) $(MARK_LIBRARY) $(MARK_UTILS) -pv

.PHONY: build_env
