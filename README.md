<!--
SPDX-FileCopyrightText: Copyright 2025 Siemens AG
SPDX-License-Identifier: MIT
-->

# Isar Demo

This demo project is designed to showcase some features of [Isar](https://github.com/ilbers/isar/). Isar is a build system designed for creating custom Debian-based images.

This demo illustrates:
- how to create a project layer on top of the existing Isar layers
- how to add custom packages built by Isar, as well as packages from Debian upstream, to a target image
- how to generate a Debian Trixie image tailored for either the Raspberry Pi 4B or for QEMU AMD64 emulation.
- how to integrate features from other repositories into the image, on the example of [SWUpdate](https://sbabic.github.io/swupdate/swupdate.html) for performing image updates

**Important Note:** This project is intended solely as a demo and should not be used as a basis for product development.

## Build

The build process is managed using the [`kas-container`](kas-container) wrapper script.
Please refer to the [kas user guide](https://kas.readthedocs.io/).
As `kas-container` runs builds in a containerized environment, you'll need [Docker](https://docs.docker.com/engine/install/) or [Podman](https://podman.io/docs/installation) installed.

The resulting images will be placed in the `build/tmp/deploy/images` directory within your project.

### For the Raspberry Pi 4B

To build an image for the Raspberry Pi 4B, run:

```shell
./kas-container build kas.yaml:kas/machine/rpi-arm64-v8-efi.yaml
```

The resulting raw disk image can be flashed onto an SD card using `dd`or [Balena Etcher](https://etcher.balena.io/).

### For QEMU AMD64

To build an image for QEMU AMD64, run:

```shell
./kas-container build kas.yaml:kas/machine/qemuamd64.yaml
```

For booting this image in QEMU, you'll need the following packages installed on your Debian system:

- `qemu-system-x86`: For emulating Intel/AMD CPUs.
- `ovmf`: UEFI firmware for QEMU virtual machines.

The image can be booted using the provided [`start-qemu.sh`](start-qemu.sh) script.
The login credentials are: user: root, password: root.

```shell
./start-qemu.sh amd64
```

## Integrate a new feature into the demo

We'll show how to integrate features from other repositories into this demo, using the example of the [SWUpdate](https://sbabic.github.io/swupdate/swupdate.html) functionality. The [isar-cip-core](https://gitlab.com/cip-project/cip-core/isar-cip-core) repository provides the necessary recipes to get SWUpdate up and running in our image.

Follow these steps to use SWUpdate in the demo image:

### 1. Add the `isar-cip-core` repository

First, we need to tell `kas` where to find the `isar-cip-core` repository.

*   **Update `kas.yaml`:** Edit your [`kas.yaml`](kas.yaml) file to include the new repository.

    ```yaml
      repos:
        # ... other repos ...

        isar-cip-core:
          url: https://gitlab.com/cip-project/cip-core/isar-cip-core.git
          branch: master
    ```

    *Compatibility check:* At this point, you might want to check that the versions of `isar-cip-core` and other repositories (especially `isar`) are compatible.

*   **Update the `kas` lock file:** After adding the repository, make sure the `kas` lock file [`kas.lock.yaml`](kas.lock.yaml) is up to date. This ensures that the build is reproducible by locking the exact commit hash of each repository.

    ```shell
    ./kas-container lock kas.yaml
    ```

*   **Update `.gitignore`:** To keep the repository tidy, add `isar-cip-core` to the [`.gitignore`](.gitignore) file.

### 2. Enable `efibootguard` and configure SWUpdate via `kas` config files

To enable robust, atomic updates with SWUpdate, we utilize `efibootguard` as the bootloader. `efibootguard` is crucial for managing EFI boot entries and ensuring safe A/B updates of the bootloader and system images.

Instead of directly modifying the demo image recipe, we use `kas` configuration files. This modular approach allows you to easily switch between building images with or without SWUpdate functionality.

We use two `kas` config files, adapted from `isar-cip-core`, to achieve this:

*   **[`kas/opt/swupdate.yaml`](kas/opt/swupdate.yaml)**: This file sets up the core SWUpdate configuration.
*   **[`kas/opt/ebg-swu.yaml`](kas/opt/ebg-swu.yaml)**: This file builds upon `swupdate.yaml` by including it. It configures `efibootguard` as the bootloader.

*   **Build the image with SWUpdate enabled:** To build your QEMU image with SWUpdate functionality, include `ebg-swu.yaml` in your `kas-container build` command:

    ```shell
    ./kas-container build ./kas.yaml:kas/machine/qemuamd64.yaml:kas/opt/ebg-swu.yaml
    ```

### 3. Add the SWU file to the demo image

To fully test the SWUpdate functionality, you'll need a `.swu` update file available in your demo image.

*   **Create a recipe for adding the `.swu` file into your demo image:** Create a recipe at `meta-demo/recipes-core/swu-add/swu-add_1.0.bb`:

    ```bitbake
    DESCRIPTION = "Add swu file to image"

    inherit dpkg-raw

    SRC_URI = " \
       file://demo-image-debian-trixie-qemuamd64.swu \
       "

    do_install[cleandirs] = "${D}/home"
    do_install() {
       install -m 700 ${WORKDIR}/demo-image-debian-trixie-qemuamd64.swu ${D}/home
    }
    ```

    This recipe takes your `.swu` file and places it in the `/home` directory of your demo image.

*   **Copy your `.swu` file:** Copy [`demo-image-debian-trixie-qemuamd64.swu`](build/tmp/deploy/images/qemuamd64/demo-image-debian-trixie-qemuamd64.swu) to the `meta-demo/recipes-core/swu-add/files/` directory, right next to its recipe.

*   **Include the recipe into your image:** Add the install instruction to the image recipe [`demo-image_1.0.bb`](meta-demo/recipes-core/images/demo-image_1.0.bb):

    ```bitbake
    IMAGE_INSTALL:append:swupdate = " swu-add"
    ```

*   **Create a visible difference for testing:** To easily confirm that your SWUpdate worked, make a small, visible change between the initial image and the updated image. For example, we'll remove the package `vim` from our initial image.

    Remove the line from [`demo-image_1.0.bb`](meta-demo/recipes-core/images/demo-image_1.0.bb): `IMAGE_PREINSTALL += "vim"`

    *Note:* This change will be present in the image you *build initially*. Your `.swu` file should then contain an image with `vim` again, to demonstrate the update.

*   **Build your image:** Build your image using the SWUpdate-enabled command:

    ```shell
    ./kas-container build ./kas.yaml:kas/machine/qemuamd64.yaml:kas/opt/ebg-swu.yaml
    ```

### 4. Test SWUpdate in the QEMU image

Now that you have a QEMU image with SWUpdate enabled and a sample `.swu` file included, you can test the update process.

*   **Start QEMU:** Boot your newly built QEMU image.

    ```shell
    ./start-qemu.sh amd64
    ```
    Log in with `user: root`, `password: root`.

*   **Navigate and inspect:**

    ```shell
    dpkg -l vim # This initial image still contains vim
    cd /home
    lsblk # Observe the current partition layout. You should see multiple partitions, e.g., /dev/sda4 mounted on /
    bg_printenv # Check the efibootguard environment. You should see 'config partition 0 ustate: 0 (OK)', indicating the currently active partition.
    ```

*   **Perform the update:**

    ```shell
    swupdate -vvv -i demo-image-debian-trixie-qemuamd64.swu
    ```

    This command will apply the update from your `.swu` file to the *inactive* A/B partition. You'll see verbose output from `swupdate` as it processes the update.

*   **Verify update status (before reboot):**

    ```shell
    bg_printenv # You should now see 'config partition 0 ustate: 0 (OK)' and 'config partition 1 ustate: 1 (INSTALLED)'.
    ```

    This indicates that the update has been successfully installed to the inactive partition (e.g., partition 1), and `efibootguard` is ready to switch to it.

*   **Reboot and verify the new partition:**

    ```shell
    reboot
    ```

    After QEMU restarts and you log in again:

    ```shell
    dpkg -l vim # In the updated image, vim has been removed
    lsblk # Observe the new active partition. It should now be the other partition, e.g., /dev/sda5 mounted on /
    bg_printenv # You should now see 'config partition 0 ustate: 0 (OK)' and 'config partition 1 ustate: 2 (TESTING)'.
    ```

    This confirms that the system has successfully booted into the newly updated partition and `efibootguard` has marked it as "TESTING".

*   **Confirm the update:** If everything looks good on the new partition, confirm the update to make it permanent:

    ```shell
    bg_setenv --confirm
    ```

    You should see "Environment update was successful." This marks the new partition as "OK". If you were to reboot again, it would still boot from this confirmed partition. If you *didn't* confirm, the system would automatically revert to the previous "OK" partition on the next reboot.

## License

This project is licensed according to the terms of the MIT License.
A copy of the license is provided in [LICENSE](LICENSE).
