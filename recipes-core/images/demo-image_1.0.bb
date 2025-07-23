# This software is a part of isar demo.
# Copyright (c) Siemens AG, 2025
#
# SPDX-License-Identifier: MIT

require recipes-core/images/isar-image-base.bb

IMAGE_INSTALL += "custom-app"
IMAGE_PREINSTALL += "vim"

USERS += "root"
USER_root[flags] = "clear-text-password"
USER_root[password] = "root"
