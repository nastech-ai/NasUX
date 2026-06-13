LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libnasux-bootstrap
LOCAL_SRC_FILES := nasux-bootstrap-zip.S nasux-bootstrap.c
include $(BUILD_SHARED_LIBRARY)
