<!--
SPDX-FileCopyrightText: Copyright 2025 Siemens AG
SPDX-License-Identifier: MIT
-->

# Isar Demo

This demo project is designed to showcase some features of [Isar](https://github.com/ilbers/isar/). Isar is a build system designed for creating custom Debian-based images.

## What is this project about?

This is a **demonstration project** that illustrates how to use Isar to build custom Debian-based Linux images. It serves as a practical example for developers who want to learn how to create their own embedded Linux systems based on Debian.

### Key Technologies

- **[Isar](https://github.com/ilbers/isar/)**: A build framework for creating software packages and repeatable generation of Debian-based root filesystems with customizations. Think of it as "Yocto for Debian" - it uses BitBake (the build tool from Yocto) but builds Debian packages instead of custom package formats.
- **[kas](https://kas.readthedocs.io/)**: A setup tool for BitBake-based projects that simplifies repository and build configuration management.
- **Debian Trixie**: The target Debian distribution version for the generated images.

### What This Demo Illustrates

- How to create a project layer (`meta-demo`) on top of the existing Isar layers
- How to build custom Debian packages from source code (e.g., the included C application)
- How to add custom packages built by Isar, as well as packages from Debian upstream, to a target image
- How to configure and customize a Debian-based system image
- How to generate bootable Debian Trixie images tailored for:
  - **Raspberry Pi 4B** (ARM64 architecture with EFI boot)
  - **QEMU AMD64** emulation (for testing without hardware)

### Project Structure

```
isar-demo/
├── kas.yaml                       # Main build configuration (kas setup)
├── kas/                           # Machine-specific configurations
│   └── machine/
│       ├── qemuamd64.yaml         # QEMU AMD64 target
│       └── rpi-arm64-v8-efi.yaml  # Raspberry Pi 4B target
├── meta-demo/                     # Custom Isar layer
│   ├── recipes-app/               # Custom application recipes
│   │   └── custom-app/            # Example C application
│   └── recipes-core/              # Core system recipes
│       ├── images/                # Image definitions
│       │   └── demo-image_1.0.bb  # Main image recipe
│       └── customization/         # System customization hooks
├── kas-container                  # Containerized build wrapper script
└── start-qemu.sh                  # Script to boot QEMU images
```

**Important Note:** This project is intended solely as a demo and learning resource. It should not be used as a basis for product development without proper adaptation and hardening.

## Prerequisites

Before building, ensure you have one of the following container runtimes installed:
- [Docker](https://docs.docker.com/engine/install/)
- [Podman](https://podman.io/docs/installation)

The build uses containerized environments to ensure reproducibility and avoid polluting your host system.

## Build

The build process is managed using the [`kas-container`](kas-container) wrapper script, which handles all the complexity of setting up the build environment.
Please refer to the [kas user guide](https://kas.readthedocs.io/) for more details on kas.

The resulting images will be placed in the `build/tmp/deploy/images/<machine>/` directory within your project.

### For the Raspberry Pi 4B

To build a bootable image for the Raspberry Pi 4B, run:

```bash
./kas-container build kas.yaml:kas/machine/rpi-arm64-v8-efi.yaml
```

This creates a disk image with EFI boot support for ARM64 architecture. The resulting raw disk image (`.wic` file) can be flashed onto an SD card using:
- `dd` command (Linux/macOS)
- [Balena Etcher](https://etcher.balena.io/) (Cross-platform GUI tool)

**Example with dd:**
```bash
sudo dd if=build/tmp/deploy/images/rpi-arm64-v8-efi/demo-image-debian-trixie-rpi-arm64-v8-efi.wic of=/dev/sdX bs=4M status=progress
```
**Note:** Replace `/dev/sdX` with your SD card device. The exact image filename may vary - check your `build/tmp/deploy/images/rpi-arm64-v8-efi/` directory for the actual `.wic` file generated.

### For QEMU AMD64

To build an image for QEMU AMD64 emulation, run:

```bash
./kas-container build kas.yaml:kas/machine/qemuamd64.yaml
```

**Running the Image:**

For booting this image in QEMU, you'll need the following packages installed on your Debian/Ubuntu system:

- `qemu-system-x86`: For emulating Intel/AMD x86-64 CPUs
- `ovmf`: UEFI firmware for QEMU virtual machines

Install them with:
```bash
sudo apt install qemu-system-x86 ovmf
```

The image can be booted using the provided [`start-qemu.sh`](start-qemu.sh) script:

```bash
./start-qemu.sh amd64
```

**Login credentials:**
- Username: `root`
- Password: `root`

The QEMU instance will forward SSH from the guest to `localhost:22222` on your host, allowing you to connect via:
```bash
ssh root@localhost -p 22222
```

## What's Included in the Demo Image?

The generated `demo-image` includes:

- **Base Debian Trixie system** with essential packages
- **Custom application**: A simple C application (`custom-app`) that prints a greeting message
- **vim**: Text editor (pre-installed via `IMAGE_PREINSTALL`)
- **System customization**: Post-installation scripts for additional configuration
- **Root user**: Pre-configured with password `root` for easy access

You can explore the image recipe at [`meta-demo/recipes-core/images/demo-image_1.0.bb`](meta-demo/recipes-core/images/demo-image_1.0.bb) to see how packages are selected and configured.

## Learning Resources

To learn more about Isar and how to create your own custom images:

- [Isar Project on GitHub](https://github.com/ilbers/isar/)
- [Isar Documentation](https://github.com/ilbers/isar/blob/master/doc/user_manual.md)
- [kas Documentation](https://kas.readthedocs.io/)
- [BitBake User Manual](https://docs.yoctoproject.org/bitbake/)

## License

This project is licensed according to the terms of the MIT License.
A copy of the license is provided in [LICENSE](LICENSE).
