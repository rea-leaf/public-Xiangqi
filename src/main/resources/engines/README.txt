Put bundled engine binaries in this directory.
Put the network file in the same directory too (e.g. pikafish.nnue).

Supported default names:
- Windows: pikafish.exe / pikafish-avx2.exe / pikafish-avx512.exe
- Linux:   pikafish / pikafish-avx2 / pikafish-avx512
- macOS:   pikafish-macos (or pikafish)

These files will be packaged with the application and auto-copied
to the runtime "engines" folder on first start.

If multiple files start with "pikafish", the app will auto-pick the best one:
- prefer avx512/avx2 variants by name
- probe UCI/UCCI handshake compatibility
- fallback to the highest-priority file when probing fails
