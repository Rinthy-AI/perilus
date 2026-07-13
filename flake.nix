{
  description = "A RISC-V RV32I processor written with Chisel as a learning exercise";

  inputs = {
    stable-pkgs.url = "github:nixos/nixpkgs?ref=nixos-26.05";
  };

  outputs =
    { self, stable-pkgs }:
    let
      system = "x86_64-linux";
      stable = import stable-pkgs {
        inherit system;
      };
    in
    {
      devShells.${system}.default = stable.mkShellNoCC {
        packages = with stable; [
          act
          coursier
          gtkwave
          jdk21
          just
          llvm
          metals
          sbt
          typst
          verilator
        ];
      };
    };
}
