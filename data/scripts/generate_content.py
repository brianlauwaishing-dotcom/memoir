import csv
import json
import re
import sys
from pathlib import Path

if sys.version_info < (3, 11):
    sys.exit("Python 3.11+ required")

ROOT = Path(__file__).resolve().parents[2]
CSV_PATH = ROOT / "data" / "tainan_routes.csv"
CONTENT_DIR = ROOT / "data" / "tainan-route"
ASSETS_PATH = CONTENT_DIR / "_assets.json"
ROUTES_DIR = CONTENT_DIR / "routes"
SPOTS_DIR = CONTENT_DIR / "spots"

SECTION_RE = re.compile(r"^Route\s+[A-Z]\b(.*)$")
PHOTO_MARKER_RE = re.compile(r"[①②③④⑤⑥⑦⑧⑨⑩ロヮワ]\s*")
ENTRY_MARKER_RE = re.compile(r"[①②③④⑤⑥⑦⑧⑨⑩ロヮワ]\s*")

EXPECTED_COLUMNS = {
    "stop": "Stop",
    "name_zh": "Name (ZH)",
    "name_en": "Name (EN)",
    "area_zh": "Area (ZH)",
    "area_en": "Area (EN)",
    "duration": "Recommended Time",
    "why_zh": "Why It Matters (ZH)",
    "why_en": "Why It Matters (EN)",
    "history_zh": "Historical Context (ZH)",
    "history_en": "Historical Context (EN)",
    "facts_zh": "Key Info (ZH)",
    "facts_en": "Key Info (EN)",
    "architecture_zh": "Architectural Features (ZH)",
    "architecture_en": "Architectural Features (EN)",
    "modern_zh": "Modern Use & Preservation (ZH)",
    "modern_en": "Modern Use & Preservation (EN)",
    "photo_zh": "Photography Tips (ZH)",
    "photo_en": "Photography Tips (EN)",
    "artifact_zh": "為甚麼要看?（中）",
    "artifact_en": "為甚麼要看?（英）",
    "look_zh": "look closer（中）",
    "look_en": "look closer（英）",
}


class ContentError(Exception):
    pass


def slugify(value: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "_", value.lower())
    slug = re.sub(r"_+", "_", slug)
    return slug.strip("_")


def split_bilingual(value: str) -> tuple[str, str]:
    parts = re.split(r"\s*/\s*", value.strip(), maxsplit=1)
    if len(parts) < 2:
        raise ContentError(f"section header missing '/' separator: {value}")
    return parts[0].strip(), parts[1].strip()


def detect_section(row: list[str]) -> tuple[str, str] | None:
    first = row[0].strip() if row else ""
    match = SECTION_RE.match(first)
    if not match:
        return None
    return split_bilingual(match.group(1))


def header_label(cell: str) -> str:
    lines = [line.strip() for line in cell.splitlines() if line.strip()]
    if not lines:
        return ""
    for line in reversed(lines):
        if re.search(r"[A-Za-z]", line):
            return line
    return lines[0]


def parse_header(row: list[str]) -> dict[str, int]:
    labels = {header_label(cell): i for i, cell in enumerate(row)}
    mapping: dict[str, int] = {}
    missing: list[str] = []
    for field, label in EXPECTED_COLUMNS.items():
        if label in labels:
            mapping[field] = labels[label]
        else:
            missing.append(label)
    if missing:
        joined = ", ".join(missing)
        raise ContentError(f"missing or renamed CSV columns: {joined}")
    return mapping


def get(row: list[str], mapping: dict[str, int], key: str) -> str:
    index = mapping[key]
    return row[index].strip() if index < len(row) else ""


def localized(en: str, zh: str) -> dict[str, str]:
    return {"en": en.strip(), "zh": zh.strip()}


def split_facts(value: str) -> list[str]:
    return [part.strip() for part in re.split(r"[|｜]", value) if part.strip()]


def split_marked_entries(value: str, marker_re: re.Pattern[str]) -> list[str]:
    if not value.strip():
        return []
    parts = [part.strip() for part in marker_re.split(value) if part.strip()]
    if len(parts) > 1:
        return parts
    return [part.strip() for part in re.split(r"\n\s*\n", value) if part.strip()]


def split_photo_tips(value: str) -> list[str]:
    return split_marked_entries(value, PHOTO_MARKER_RE)


def split_artifacts(value: str) -> list[tuple[str, str]]:
    entries = split_marked_entries(value, ENTRY_MARKER_RE)
    artifacts: list[tuple[str, str]] = []
    for entry in entries:
        title, separator, description = entry.partition(":")
        if not separator:
            title, separator, description = entry.partition("：")
        if separator:
            artifacts.append((title.strip(), description.strip()))
        else:
            artifacts.append((entry.strip(), ""))
    return artifacts


def read_csv(path: Path) -> tuple[dict[str, dict], dict[str, dict]]:
    with open(path, encoding="utf-8-sig", newline="") as csv_file:
        rows = list(csv.reader(csv_file))
    if len(rows) < 2:
        raise ContentError("CSV must contain a banner row and header row")

    mapping = parse_header(rows[1])
    current_category = split_bilingual(rows[0][0])
    current_route: dict | None = None
    route_order = 0
    routes: dict[str, dict] = {}
    spots: dict[str, dict] = {}

    for row_number, row in enumerate(rows[2:], start=3):
        if not row or not any(cell.strip() for cell in row):
            continue

        section = detect_section(row)
        if section is not None:
            title_zh, title_en = section
            route_id = slugify(title_en)
            if route_id in routes:
                raise ContentError(f"duplicate route id: {route_id} at CSV row {row_number}")
            current_route = {
                "id": route_id,
                "title": localized(title_en, title_zh),
                "category": localized(current_category[1], current_category[0]),
                "heroImage": "",
                "description": localized("", ""),
                "tags": [slugify(current_category[1])],
                "journey": [],
            }
            routes[route_id] = current_route
            route_order = 0
            continue

        first = row[0].strip()
        if "/" in first and not first.startswith("Route "):
            current_category = split_bilingual(first)
            continue
        if not current_route or not first:
            continue

        route_order += 1
        spot_id = slugify(get(row, mapping, "name_en"))
        photo_tips_en = split_photo_tips(get(row, mapping, "photo_en"))
        photo_tips_zh = split_photo_tips(get(row, mapping, "photo_zh"))
        artifacts_en = split_artifacts(get(row, mapping, "artifact_en"))
        artifacts_zh = split_artifacts(get(row, mapping, "artifact_zh"))

        spot = {
            "id": spot_id,
            "title": localized(get(row, mapping, "name_en"), get(row, mapping, "name_zh")),
            "heroImage": "",
            "duration": localized(get(row, mapping, "duration"), get(row, mapping, "duration")),
            "whyItMatters": localized(get(row, mapping, "why_en"), get(row, mapping, "why_zh")),
            "historicalContext": localized(get(row, mapping, "history_en"), get(row, mapping, "history_zh")),
            "architecturalFeatures": localized(get(row, mapping, "architecture_en"), get(row, mapping, "architecture_zh")),
            "modernUse": localized(get(row, mapping, "modern_en"), get(row, mapping, "modern_zh")),
            "facts": {
                "en": split_facts(get(row, mapping, "facts_en")),
                "zh": split_facts(get(row, mapping, "facts_zh")),
            },
            "photographyTips": [
                {
                    "id": i + 1,
                    "description": localized(
                        photo_tips_en[i] if i < len(photo_tips_en) else "",
                        photo_tips_zh[i] if i < len(photo_tips_zh) else "",
                    ),
                    "image": "",
                }
                for i in range(max(len(photo_tips_en), len(photo_tips_zh)))
            ],
            "artifacts": [
                {
                    "id": i + 1,
                    "title": localized(
                        artifacts_en[i][0] if i < len(artifacts_en) else "",
                        artifacts_zh[i][0] if i < len(artifacts_zh) else "",
                    ),
                    "description": localized(
                        artifacts_en[i][1] if i < len(artifacts_en) else "",
                        artifacts_zh[i][1] if i < len(artifacts_zh) else "",
                    ),
                    "image": "",
                }
                for i in range(max(len(artifacts_en), len(artifacts_zh)))
            ],
        }

        spots.setdefault(spot_id, spot)
        current_route["journey"].append(
            {
                "order": route_order,
                "spotId": spot_id,
                "title": spot["title"],
            }
        )
        if not current_route["description"]["en"] and spot["whyItMatters"]["en"]:
            current_route["description"] = spot["whyItMatters"]

    return routes, spots


def read_assets(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise ContentError(f"missing _assets.json: {path}") from exc
    except json.JSONDecodeError as exc:
        raise ContentError(f"invalid _assets.json: {exc}") from exc


def apply_assets(routes: dict[str, dict], spots: dict[str, dict], assets: dict) -> None:
    route_assets = assets.get("routes", {})
    spot_assets = assets.get("spots", {})

    for route_id in sorted(routes):
        binding = route_assets.get(route_id)
        if binding is None:
            raise ContentError(f"missing _assets.json binding: routes.{route_id}")
        hero_image = binding if isinstance(binding, str) else binding.get("heroImage", "")
        if not hero_image:
            raise ContentError(f"missing _assets.json binding: routes.{route_id}.heroImage")
        routes[route_id]["heroImage"] = hero_image

    for spot_id in sorted(spots):
        binding = spot_assets.get(spot_id)
        if binding is None:
            raise ContentError(f"missing _assets.json binding: spots.{spot_id}")
        hero_image = binding.get("heroImage", "")
        if not hero_image:
            raise ContentError(f"missing _assets.json binding: spots.{spot_id}.heroImage")
        spots[spot_id]["heroImage"] = hero_image
        photo_images = binding.get("photographyTipImages", [])
        artifact_images = binding.get("artifactImages", [])
        for i, tip in enumerate(spots[spot_id]["photographyTips"]):
            tip["image"] = photo_images[i] if i < len(photo_images) else spots[spot_id]["heroImage"]
        for i, artifact in enumerate(spots[spot_id]["artifacts"]):
            artifact["image"] = artifact_images[i] if i < len(artifact_images) else spots[spot_id]["heroImage"]


def write_json(path: Path, obj: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(obj, ensure_ascii=False, indent=2, sort_keys=True)
    with open(path, "w", encoding="utf-8", newline="\n") as output:
        output.write(text)
        output.write("\n")


def generate() -> None:
    routes, spots = read_csv(CSV_PATH)
    apply_assets(routes, spots, read_assets(ASSETS_PATH))

    ROUTES_DIR.mkdir(parents=True, exist_ok=True)
    SPOTS_DIR.mkdir(parents=True, exist_ok=True)
    for stale in [*ROUTES_DIR.glob("*.json"), *SPOTS_DIR.glob("*.json")]:
        stale.unlink()

    route_ids = sorted(routes)
    spot_ids = sorted(spots)
    write_json(CONTENT_DIR / "index.json", {"routes": route_ids, "spots": spot_ids})
    for route_id in route_ids:
        write_json(ROUTES_DIR / f"{route_id}.json", routes[route_id])
    for spot_id in spot_ids:
        write_json(SPOTS_DIR / f"{spot_id}.json", spots[spot_id])


def main() -> int:
    try:
        generate()
    except ContentError as exc:
        print(exc, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
