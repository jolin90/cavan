COMMON_OPTION = --prefix= --build=$(CAVAN_BUILD_PLAT) --host=$(CAVAN_HOST_PLAT) --target=$(CAVAN_TARGET_PLAT)

all:
	$(Q)$(SRC)/configure $(OPT) $(COMMON_OPTION)
	$(Q)+make
	$(Q)+make DESTDIR=$(TOOLCHIAN_PATH) install
