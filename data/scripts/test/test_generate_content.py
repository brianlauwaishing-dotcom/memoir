import csv
import unittest
from tempfile import TemporaryDirectory
from pathlib import Path

from generate_content import (
    ContentError,
    apply_assets,
    apply_route_tags,
    read_csv,
    read_route_tags,
    slugify,
    split_facts,
)


FIXTURES = Path(__file__).parent / "fixtures"
HEADER = (
    "Category ZH / Category EN,,,,,,,,,,,,,,,,,,,,,\n"
    "Stop,Name (ZH),Name (EN),Area (ZH),Area (EN),Recommended Time,"
    "Why It Matters (ZH),Why It Matters (EN),Historical Context (ZH),"
    "Historical Context (EN),Key Info (ZH),Key Info (EN),Architectural Features (ZH),"
    "Architectural Features (EN),Modern Use & Preservation (ZH),Modern Use & Preservation (EN),"
    "Photography Tips (ZH),Photography Tips (EN),為甚麼要看?（中）,為甚麼要看?（英）,look closer（中）,look closer（英）\n"
)


class GenerateContentTest(unittest.TestCase):
    def test_slugifier_is_stable(self):
        self.assertEqual("grand_mazu_temple_datianhougong", slugify("Grand Mazu Temple (Datianhougong)"))

    def test_reads_multiline_cells_and_key_info_delimiters(self):
        routes, spots = read_csv(FIXTURES / "key_info_and_multiline.csv")

        self.assertEqual(["route_alpha"], sorted(routes))
        spot = spots["alpha_spot"]
        self.assertEqual(["One", "Two"], spot["facts"]["en"])
        self.assertEqual(["甲", "乙"], spot["facts"]["zh"])
        self.assertEqual(2, len(spot["photographyTips"]))
        self.assertIn("continued", spot["photographyTips"][0]["description"]["en"])

    def test_missing_section_separator_fails(self):
        with self.assertRaisesRegex(ContentError, "missing '/' separator"):
            read_csv(FIXTURES / "missing_separator.csv")

    def test_photo_tips_blank_line_fallback(self):
        _routes, spots = read_csv(FIXTURES / "photo_fallback.csv")

        tips = spots["alpha_spot"]["photographyTips"]
        self.assertEqual(["First tip", "Second tip"], [tip["description"]["en"] for tip in tips])

    def test_split_facts_supports_ascii_and_full_width_pipes(self):
        self.assertEqual(["a", "b", "c"], split_facts("a | b｜ c"))

    def test_duplicate_route_slug_fails(self):
        with self.assertRaisesRegex(ContentError, "duplicate route id: route_alpha"):
            read_csv(self.write_csv(
                HEADER
                + "Route A 路線甲 / Route Alpha,,,,,,,,,,,,,,,,,,,,,\n"
                + "Route B 路線乙 / Route Alpha,,,,,,,,,,,,,,,,,,,,,\n"
            ))

    def test_missing_route_hero_image_binding_fails(self):
        routes = {"route_alpha": {"id": "route_alpha", "heroImage": ""}}
        spots = {}

        with self.assertRaisesRegex(ContentError, "missing _assets.json binding: routes.route_alpha.heroImage"):
            apply_assets(routes, spots, {"routes": {"route_alpha": {}}, "spots": {}})

    def test_missing_spot_hero_image_binding_fails(self):
        routes = {}
        spots = {"alpha_spot": {"id": "alpha_spot", "heroImage": "", "photographyTips": [], "artifacts": []}}

        with self.assertRaisesRegex(ContentError, "missing _assets.json binding: spots.alpha_spot.heroImage"):
            apply_assets(routes, spots, {"routes": {}, "spots": {"alpha_spot": {}}})

    def test_missing_artifact_image_binding_fails(self):
        routes = {}
        spots = {
            "alpha_spot": {
                "id": "alpha_spot",
                "heroImage": "",
                "photographyTips": [],
                "artifacts": [{"id": 7, "image": ""}],
            }
        }

        with self.assertRaisesRegex(
            ContentError,
            r"missing _assets\.json binding: spots\.alpha_spot\.artifacts\.7\.image",
        ):
            apply_assets(
                routes,
                spots,
                {
                    "routes": {},
                    "spots": {
                        "alpha_spot": {
                            "heroImage": "hero",
                            "artifacts": {"7": {}},
                        }
                    },
                },
            )

    def test_artifact_without_gallery_image_omits_field(self):
        routes = {}
        artifact = {"id": 1, "image": ""}
        spots = {
            "alpha_spot": {
                "id": "alpha_spot",
                "heroImage": "",
                "photographyTips": [],
                "artifacts": [artifact],
            }
        }

        apply_assets(
            routes,
            spots,
            {
                "routes": {},
                "spots": {
                    "alpha_spot": {
                        "heroImage": "hero",
                        "artifacts": {"1": {"image": "artifact_image"}},
                    }
                },
            },
        )

        self.assertEqual("artifact_image", artifact["image"])
        self.assertNotIn("galleryImage", artifact)

    def test_artifact_with_gallery_image_emits_field(self):
        routes = {}
        artifact = {"id": 1, "image": ""}
        spots = {
            "alpha_spot": {
                "id": "alpha_spot",
                "heroImage": "",
                "photographyTips": [],
                "artifacts": [artifact],
            }
        }

        apply_assets(
            routes,
            spots,
            {
                "routes": {},
                "spots": {
                    "alpha_spot": {
                        "heroImage": "hero",
                        "artifacts": {"1": {"image": "artifact_image", "galleryImage": "gallery"}},
                    }
                },
            },
        )

        self.assertEqual("artifact_image", artifact["image"])
        self.assertEqual("gallery", artifact["galleryImage"])

    def test_empty_question_en_fails_before_json_write(self):
        with self.assertRaisesRegex(
            ContentError,
            r"empty question for spots\.alpha_spot\.artifacts\[1\]",
        ):
            read_csv(self.write_csv_rows([
                ["Category ZH / Category EN"],
                [
                    "Stop",
                    "Name (ZH)",
                    "Name (EN)",
                    "Area (ZH)",
                    "Area (EN)",
                    "Recommended Time",
                    "Why It Matters (ZH)",
                    "Why It Matters (EN)",
                    "Historical Context (ZH)",
                    "Historical Context (EN)",
                    "Key Info (ZH)",
                    "Key Info (EN)",
                    "Architectural Features (ZH)",
                    "Architectural Features (EN)",
                    "Modern Use & Preservation (ZH)",
                    "Modern Use & Preservation (EN)",
                    "Photography Tips (ZH)",
                    "Photography Tips (EN)",
                    "為甚麼要看?（中）",
                    "為甚麼要看?（英）",
                    "look closer（中）",
                    "look closer（英）",
                ],
                ["Route A 類別 / Route Alpha"],
                [
                    "A",
                    "景點",
                    "Alpha Spot",
                    "區",
                    "Area",
                    "10 min",
                    "重要",
                    "Important",
                    "歷史",
                    "History",
                    "一|二",
                    "One|Two",
                    "建築",
                    "Architecture",
                    "現代",
                    "Modern",
                    "",
                    "",
                    "物件:故事",
                    "Artifact:Story",
                    "物件:問題",
                    "",
                ],
            ]))

    def test_missing_route_tag_entry_fails(self):
        routes = {"route_alpha": {"id": "route_alpha", "tags": []}}
        tags = read_route_tags(FIXTURES / "tags_missing_entry.json")

        with self.assertRaisesRegex(ContentError, "missing _tags.json entry: route_alpha"):
            apply_route_tags(routes, tags)

    def test_empty_route_tag_entry_fails(self):
        routes = {"route_alpha": {"id": "route_alpha", "tags": []}}
        tags = read_route_tags(FIXTURES / "tags_empty_value.json")

        with self.assertRaisesRegex(ContentError, "route route_alpha must declare at least one tag"):
            apply_route_tags(routes, tags)

    def test_unknown_route_tag_fails(self):
        routes = {"route_alpha": {"id": "route_alpha", "tags": []}}
        tags = read_route_tags(FIXTURES / "tags_unknown_value.json")

        with self.assertRaisesRegex(ContentError, "moon-cult"):
            apply_route_tags(routes, tags)

    def test_valid_route_tags_are_applied(self):
        routes = {"route_alpha": {"id": "route_alpha", "tags": []}}
        tags = read_route_tags(FIXTURES / "tags_valid.json")

        apply_route_tags(routes, tags)

        self.assertEqual(["temples", "architecture"], routes["route_alpha"]["tags"])

    def write_csv(self, text: str) -> Path:
        directory = TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = Path(directory.name) / "fixture.csv"
        path.write_text(text, encoding="utf-8", newline="\n")
        return path

    def write_csv_rows(self, rows: list[list[str]]) -> Path:
        directory = TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = Path(directory.name) / "fixture.csv"
        with path.open("w", encoding="utf-8", newline="") as output:
            csv.writer(output).writerows(rows)
        return path


if __name__ == "__main__":
    unittest.main()
