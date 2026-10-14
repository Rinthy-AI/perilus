{
  description = "A RISC-V RV32I processor written with Chisel as a learning exercise";

  inputs = {
    stable-pkgs.url = "github:nixos/nixpkgs?ref=nixos-26.05";
    unstable-pkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    fenix = {
      url = "github:nix-community/fenix";
      inputs.nixpkgs.follows = "stable-pkgs";
    };
  };

  outputs =
    {
      self,
      stable-pkgs,
      unstable-pkgs,
      fenix,
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
        libGL
        libxkbcommon
        wayland
        libx11
        libxcursor
        libxrandr
        libxi
        libxcb
      ];
      lib-path = with stable; lib.makeLibraryPath libInputs;

      rustToolchain =
        (fenix.packages.${system}.toolchainOf {
          channel = "1.95.0";
          sha256 = "821ff14e4c4a1cbe1e8915f35aff0a3fbbdf8d293ad48ab8f31e3b0440c581f9";
        }).withComponents
          [
            "cargo"
            "clippy"
            "rustc"
            "rustfmt"
            "rust-std"
          ];
    in
    {
      devShells.${system}.default = stable.mkShellNoCC {
        packages = with stable; [
          act
          bundler
          coursier
          gcc
          git
          gnumake
          jdk21
          just
          llvm
          metals
          pkgsCross.riscv64-embedded.binutils
          pkgsCross.riscv64-embedded.gcc
          ruby
          rust-analyzer
          rustToolchain
          sbt
          surfer
          typst
          unstable.sail-riscv
          uv
          verilator
          zenity
        ];

        shellHook = ''
          export BUNDLE_FROZEN=1
          export "LD_LIBRARY_PATH=$LD_LIBRARY_PATH:${lib-path}"
        '';
      };
    };
}
