# Dummy digit model (debug builds only)

`digits-0.0.0-dummy-fixed.*` is a contract-conformant test model produced by the backend repo
(`ml/fixtures/dummy/`, regenerate with `uv run tensoscan-ml dummy-model`). It ignores the image and always
reports 128/82/67, so it only exercises the LiteRT path end to end. Release builds ship no model and use ML Kit.
Set `tensoscan.detector=litert` in `local.properties` to try it in a debug build.
