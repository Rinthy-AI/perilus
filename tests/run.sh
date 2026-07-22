#!/bin/sh

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
test_dir="$script_dir/perilus-rv32i/elfs/rv32i/I"
test_stems=$(find "$test_dir" -name "*.elf" | xargs -L 1 basename -s .elf | sort)
tmp_dir="/tmp/perilus"

if [ ! -d $tmp_dir ]
then
    mkdir "$tmp_dir"
fi

exit_code=0
for test_stem in $test_stems
do
    echo -en "$test_stem:\t"
    riscv64-none-elf-objcopy -O binary "$test_dir/$test_stem.elf" "$tmp_dir/$test_stem.bin"
    if cd "$script_dir/../sim" && cargo run --release -q -- test "$tmp_dir/$test_stem.bin"
    then
        echo -e "PASS"
    else
        exit_code=1
    fi
done

exit $exit_code
