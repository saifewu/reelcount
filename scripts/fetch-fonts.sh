#!/usr/bin/env bash
# Poppins and Lora (SIL Open Font License) stand in for Anthropic's
# proprietary Styrene and Tiempos typefaces.
set -euo pipefail

dir="app/src/main/res/font"
base="https://raw.githubusercontent.com/google/fonts/main/ofl"
mkdir -p "$dir"

fetch() {
  [ -s "$dir/$2" ] || curl -fsSL "$base/$1" -o "$dir/$2"
}

fetch poppins/Poppins-Medium.ttf poppins_medium.ttf
fetch "lora/Lora%5Bwght%5D.ttf" lora.ttf
