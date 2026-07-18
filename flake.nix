{
  description = "A RISC-V RV32I processor written with Chisel as a learning exercise";

  inputs = {
    stable-pkgs.url = "github:nixos/nixpkgs?ref=nixos-26.05";
    unstable-pkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
  };

  outputs =
    {
      self,
      stable-pkgs,
      unstable-pkgs,
    }:
    let
      system = "x86_64-linux";
      stable = import stable-pkgs {
        inherit system;
      };
      unstable = import unstable-pkgs {
        inherit system;
      };
      libInputs = with stable; [
        stdenv.cc.cc.lib
      ];
      lib-path = with stable; lib.makeLibraryPath libInputs;
    in
    {
      devShells.${system}.default = stable.mkShellNoCC {
        packages = with stable; [
          act
          bundler
          cargo
          coursier
          gcc
          git
          gnumake
          gtkwave
          jdk21
          just
          llvm
          metals
          pkgsCross.riscv64-embedded.binutils
          pkgsCross.riscv64-embedded.gcc
          ruby
          rust-analyzer
          rustc
          rustfmt
          sbt
          typst
          unstable.sail-riscv
          uv
          verilator
        ];

        shellHook = ''
          export BUNDLE_FROZEN=1
          export "LD_LIBRARY_PATH=$LD_LIBRARY_PATH:${lib-path}"
        '';
      };
    };
}
