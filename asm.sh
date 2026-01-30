#!/usr/bin/env bash

set -euo pipefail

TMPOBJ="/tmp/tmp.o"
TMPBIN="/tmp/tmp.bin"

llvm-mc -triple=riscv32 -filetype=obj -o "$TMPOBJ" "$1"
llvm-objcopy -O binary -j .text "$TMPOBJ" "$TMPBIN"
od -An -tx4 -w -v "$TMPBIN" | sed "s/^ *//"

rm "$TMPOBJ" "$TMPBIN"

