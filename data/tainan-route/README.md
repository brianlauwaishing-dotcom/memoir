# Tainan Route Content

Files in this directory are generated from `data/tainan_routes.csv` and `data/tainan-route/_assets.json`.

Do not hand-edit `index.json`, `routes/*.json`, or `spots/*.json`; those files are overwritten by the generator. Edit the CSV for copy and structure changes, and edit `_assets.json` only for drawable bindings.

Regenerate from the repo root:

```bash
python data/scripts/generate_content.py
```
