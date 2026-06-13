LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE:= libnasux
LOCAL_SRC_FILES:= nasux.c
include $(BUILD_SHARED_LIBRARY)
