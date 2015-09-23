LOCAL_PATH := $(call my-dir)

GLOBAL_SHARED_LIBRARIES := \
	libcavan \
	libutils \
	libcutils \
	liblog \
	libbinder

GLOBAL_C_INCLUDES := cavan/include

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	SuService.cpp \
	ISuService.cpp

LOCAL_MODULE := libcavan_su
LOCAL_MODULE_TAGS := optional
LOCAL_SHARED_LIBRARIES = $(GLOBAL_SHARED_LIBRARIES)
LOCAL_C_INCLUDES = $(GLOBAL_C_INCLUDES)

include $(BUILD_SHARED_LIBRARY)

GLOBAL_SHARED_LIBRARIES := $(GLOBAL_SHARED_LIBRARIES) libcavan_su

include $(CLEAR_VARS)

LOCAL_SRC_FILES := main_su_server.cpp

LOCAL_MODULE := cavan_su_server
LOCAL_MODULE_TAGS := optional
LOCAL_SHARED_LIBRARIES = $(GLOBAL_SHARED_LIBRARIES)
LOCAL_C_INCLUDES = $(GLOBAL_C_INCLUDES)

include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := su.cpp

LOCAL_MODULE := cavan_su
LOCAL_MODULE_TAGS := optional
LOCAL_SHARED_LIBRARIES = $(GLOBAL_SHARED_LIBRARIES)
LOCAL_C_INCLUDES = $(GLOBAL_C_INCLUDES)

include $(BUILD_EXECUTABLE)