"""Deterministic skill extraction against the canonical catalog (Phase 5).

Design decisions
----------------
* One regular expression is compiled once from every canonical name and alias
  in ``data/skills.json``.
* Terms are wrapped in ``(?<!\\w)`` / ``(?!\\w)`` guards, so short skills such
  as ``C`` and ``R`` only match as whole tokens (never inside "city",
  "worker" or "JavaScript"), while symbols such as ``C++`` / ``C#`` still
  match (a plain ``\\b`` fails after a non-word character like ``+``).
* Alternatives are ordered longest-first, so "JavaScript" beats "java"/"js"
  and "C++"/"C#" beat "C" at the same position; ties break alphabetically
  for a deterministic compile order.
* Matching is case-insensitive; results are always canonical catalog names.
* Ordering guarantee: skills are returned in the order they are FIRST
  detected in the input (left to right). Duplicates collapse to that first
  occurrence, so "Java, java, JAVA and Spring" -> ["Java", "Spring Boot"].
* Bare "spring" maps to "Spring Boot" but is skipped when followed by a year
  or the words "semester"/"term" ("Spring 2025" is a calendar reference,
  not the framework).
"""
import json
import re
from pathlib import Path

DATA_FILE = Path(__file__).resolve().parent.parent / "data" / "skills.json"

# Guards apply when the matched span (lower-cased) equals the key.
_BARE_TERM_GUARDS = {
    "spring": re.compile(r"^\s+(?:(?:19|20)\d{2}|semester|term)\b", re.IGNORECASE),
}


def _load_catalog() -> list[tuple[str, list[str]]]:
    with DATA_FILE.open(encoding="utf-8") as handle:
        document = json.load(handle)
    catalog: list[tuple[str, list[str]]] = []
    for item in document["skills"]:
        name = str(item["name"]).strip()
        if not name:
            raise ValueError("every catalog entry needs a non-blank 'name'")
        aliases = [str(alias) for alias in item.get("aliases", [])]
        catalog.append((name, aliases))
    return catalog


def _term_fragment(term: str) -> str:
    """Escape a term and join its words with flexible whitespace."""
    body = r"\s+".join(re.escape(token) for token in term.split())
    return rf"(?<!\w){body}(?!\w)"


def _build_matcher() -> tuple[re.Pattern[str], dict[str, str]]:
    variants: list[tuple[str, str]] = []
    for canonical, aliases in _load_catalog():
        variants.extend((term, canonical) for term in (canonical, *aliases))

    seen: dict[str, str] = {}
    unique: list[tuple[str, str]] = []
    for term, canonical in variants:
        key = term.casefold()
        if key in seen:
            if seen[key] != canonical:
                raise ValueError(
                    f"catalog conflict: '{term}' maps to '{seen[key]}' and '{canonical}'"
                )
            continue
        seen[key] = canonical
        unique.append((term, canonical))

    unique.sort(key=lambda pair: (-len(pair[0]), pair[0].casefold()))

    groups: dict[str, str] = {}
    fragments: list[str] = []
    for index, (term, canonical) in enumerate(unique):
        group = f"skill_{index}"
        fragments.append(f"(?P<{group}>{_term_fragment(term)})")
        groups[group] = canonical
    return re.compile("|".join(fragments), re.IGNORECASE), groups


_MATCHER, _GROUP_TO_CANONICAL = _build_matcher()


def extract_skills(text: str) -> list[str]:
    """Return the canonical skills found in ``text``, in first-detection order."""
    if not text:
        return []
    skills: list[str] = []
    detected: set[str] = set()
    for match in _MATCHER.finditer(text):
        canonical = _GROUP_TO_CANONICAL[match.lastgroup or ""]
        guard = _BARE_TERM_GUARDS.get(match.group(0).strip().casefold())
        if guard is not None and guard.match(text[match.end():]):
            continue
        if canonical not in detected:
            detected.add(canonical)
            skills.append(canonical)
    return skills


__all__ = ["DATA_FILE", "extract_skills"]
