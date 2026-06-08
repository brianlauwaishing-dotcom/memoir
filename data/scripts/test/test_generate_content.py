import unittest
from tempfile import TemporaryDirectory
from pathlib import Path

from generate_content import ContentError, apply_assets, read_csv, slugify, split_facts


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

    def write_csv(self, text: str) -> Path:
        directory = TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = Path(directory.name) / "fixture.csv"
        path.write_text(text, encoding="utf-8", newline="\n")
        return path


if __name__ == "__main__":
    unittest.main()
