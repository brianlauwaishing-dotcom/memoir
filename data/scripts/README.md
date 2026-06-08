# Content Generator

Run from the repo root:

```bash
python data/scripts/generate_content.py
```

On Windows, use `python` from a Python 3.11+ install or virtual environment. On macOS/Linux, `python3 data/scripts/generate_content.py` is also fine if `python3` points to Python 3.11+.

The generator reads `data/tainan_routes.csv` for route and spot copy, plus `data/tainan-route/_assets.json` for drawable resource names. Edit `_assets.json` only when route or spot image bindings change. Generated JSON under `data/tainan-route/index.json`, `routes/`, and `spots/` must not be edited by hand.
