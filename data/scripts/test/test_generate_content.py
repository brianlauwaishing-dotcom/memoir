import unittest
from pathlib import Path

from generate_content import ContentError, read_csv, slugify, split_facts


FIXTURES = Path(__file__).parent / "fixtures"


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


if __name__ == "__main__":
    unittest.main()
