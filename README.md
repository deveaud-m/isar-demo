# Isar Demo

This demo project is designed to show some features of the build system [isar](https://github.com/ilbers/isar/):

- how to create a project layer on top of the isar layers
- how to add custom packages built by isar and packages from Debian upstream to an image
- how to generate a Debian Trixie image for the Raspberry Pi 4B or for QEMU AMD64

The project is intended solely as a demo and should not be used as a basis for product development.

## Build

The build can be done using [kas-container](https://github.com/siemens/kas/blob/master/kas-container).

### For the Raspberry Pi 4B

```
./kas-container build kas.yaml:kas/machine/rpi-arm64-v8-efi.yaml
```

The resulting raw disk image can be flashed onto an SD card.

### For QEMU AMD64

```
./kas-container build kas.yaml:kas/machine/qemuamd64.yaml
```

The image can be booted via (user: root, password: root):

```shell
./start-qemu.sh amd64
```

## Integrate a new isar feature

To demonstrate how the integrate of single features in the base demo layer works, we choose to add "SWUpdate".

Following steps are required:

1. Add another layer to this demo, e.g. cip-core: <https://gitlab.com/cip-project/cip-core/isar-cip-core>
   - Update kas configuration: edit `kas.yaml` and add a new layer:

      ```yaml
        isar-cip-core:
          url: https://gitlab.com/cip-project/cip-core/isar-cip-core.git
          branch: master
      ```

   - Build the new layer:

      ```shell
      ./kas-container build ./kas.yaml:kas/machine/qemuamd64.yaml
      ```

2. You need to use `efibootguard` with `SWUpdate` in your isar image because efibootguard manages EFI boot entries and ensures safe, atomic updates of the bootloader and system images.
   - To exchange the bootload from `grub` to `efibootguard` (part of cip-core), add following change into the file `meta-demo/recipes-core/images/demo-image_1.0.bb`:

      ```text
      # Change bootloader from grub to efibootguard
      inherit efitbootguard
      ```

   - Call kas container build command again:

      ```shell
      ./kas-container build ./kas.yaml:kas/machine/qemuamd64.yaml
      ```

   - Change image description by using the proper `wks` file from `isar-cip-core` layer:

      ```shell
      cp isar-cip-core/wic/qemu-amd64-efibootguard.wks.in meta-demo/wic/qemu-amd64-efibootguard.wks.in
      ```

   - Add WKF_FILE variable in  the demo image description file `demo-image_1.0.bb`:

      ```text
      WKS_FILE = "${MACHINE}-efibootguard.wks.in" 
      ```

    > Make sure the machine name matched the kas machine name `kas/machine/qemuamd64.yaml`

   - Call kas container build command again:

      ```shell
        ./kas-container build ./kas.yaml:kas/machine/qemuamd64.yaml
        # FIXME
        ERROR: /work/build/../../repo/meta-demo/recipes-core/images/demo-image_1.0.bb: WKS_FILE 'qemuamd64-efibootguard.wks.in' not found
        ```

## License

This project is licensed according to the terms of the MIT License.
A copy of the license is provided in [LICENSE](LICENSE).
