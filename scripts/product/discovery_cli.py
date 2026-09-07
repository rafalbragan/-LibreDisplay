#!/usr/bin/env python3
"""LibreCare Discovery Agent v1 (deterministic collector + bounded AI analysis)."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta, timezone
from html import unescape
from pathlib import Path
from typing import Callable
from urllib import error as urllib_error
from urllib import parse as urllib_parse
from urllib import request as urllib_request

try:
    import yaml  # type: ignore

    HAVE_YAML = True
except Exception:
    HAVE_YAML = False

if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

ROOT = Path(__file__).resolve().parents[2]
PRODUCT = ROOT / "product"
SCHEMA_DIR = PRODUCT / "schema"
OBSERVATIONS_DIR = PRODUCT / "research" / "observations"
REQUIREMENTS_DIR = PRODUCT / "requirements"
DECISIONS_DIR = PRODUCT / "decisions"
GENERATED_DISCOVERY_DIR = PRODUCT / "generated" / "discovery"
VALIDATED_CAPABILITIES_MD = PRODUCT / "generated" / "VALIDATED_CAPABILITIES.md"
SOURCES_CONFIG_DEFAULT = PRODUCT / "discovery" / "sources.json"
QUERY_PACKS_DEFAULT = PRODUCT / "discovery" / "query-packs.json"
DISCOVERY_CACHE_PATH = GENERATED_DISCOVERY_DIR / "cache" / "discovery-cache-v1.json"
DISCOVERY_CLUSTER_REGISTRY_PATH = PRODUCT / "discovery" / "cluster-registry.json"
DISCOVERY_PROPOSED_CLUSTER_REGISTRY_PATH = GENERATED_DISCOVERY_DIR / "cluster-registry.proposed.json"

CLUSTER_REGISTRY_VERSION = 1
CLUSTER_REGISTRY_MATCH_THRESHOLD = 0.72
CLUSTER_REGISTRY_AMBIGUITY_DELTA = 0.02

FORBIDDEN_CACHE_KEYS = {
    "access_token",
    "refresh_token",
    "authorization",
    "client_secret",
    "reddit_client_secret",
    "github_token",
    "password",
    "cookie",
    "set-cookie",
}

MAX_CACHED_EXCERPT_LENGTH = 220
MAX_CACHED_PROBLEM_LENGTH = 180
MAX_CACHED_TEXT_LENGTH = 500
SUPPORTED_LANGUAGES = {"pl", "en", "de", "fr", "es"}
PRIMARY_LANGUAGES = {"pl", "en"}
CROSS_LANGUAGE_SIMILARITY_THRESHOLD = 0.60
LOCAL_QUERY_MATCH_THRESHOLD = 0.60
LOCAL_QUERY_AMBIGUITY_DELTA = 0.05
MAX_GITHUB_REPO_PAGES = 2
GITHUB_REPO_PAGE_SIZE = 100
EVIDENCE_ROLES = {"user_community", "developer_community", "official_reference"}
EVIDENCE_TIER_ORDER = {"WEAK": 0, "SUPPORTED": 1, "CORROBORATED": 2, "STRONG": 3}
EVIDENCE_SCORE_CAPS = {"WEAK": 49, "SUPPORTED": 74, "CORROBORATED": 89, "STRONG": 100}
MAX_TOTAL_COLLECTED = 300

CLASSIFICATIONS = {
    "VALIDATED_CAPABILITY",
    "PRODUCT_PROBLEM",
    "PRODUCT_OPPORTUNITY",
    "TEST_COVERAGE_GAP",
    "SAFETY_GAP",
    "INCONCLUSIVE",
}

SOLVABILITY_VALUES = {"APP", "ABBOTT_LIMITATION", "EXTERNAL_ONLY", "MIXED"}
CONFIDENCE_VALUES = {"low", "medium", "high"}
ELIGIBLE_CLASSIFICATIONS = {"PRODUCT_PROBLEM", "PRODUCT_OPPORTUNITY", "SAFETY_GAP"}

SOURCE_FAMILIES = {"official_vendor", "github_community", "reddit", "other_community", "competitor"}
SOURCE_TYPE_BY_FAMILY = {
    "official_vendor": "community",
    "github_community": "community",
    "reddit": "community",
    "other_community": "community",
    "competitor": "competitor",
}

STOPWORDS_BY_LANGUAGE = {
    "en": {"a", "an", "the", "and", "or", "to", "for", "of", "in", "on", "is", "are", "be", "it", "that", "this", "with", "as", "at", "by", "from", "i", "we", "you", "they", "can", "if", "then"},
    "pl": {"a", "i", "oraz", "lub", "dla", "jest", "są", "sie", "się", "jak", "przy", "na", "do", "od", "po", "z", "ze", "w", "we", "ten", "ta", "to"},
    "de": {"der", "die", "das", "und", "oder", "für", "von", "mit", "ist", "sind", "zu", "im", "in", "auf", "ein", "eine"},
    "fr": {"le", "la", "les", "un", "une", "et", "ou", "pour", "de", "des", "du", "avec", "est", "sont", "dans", "sur"},
    "es": {"el", "la", "los", "las", "un", "una", "y", "o", "para", "de", "del", "con", "es", "son", "en", "por"},
}

CANONICAL_NOISE_TOKENS = {
    "has",
    "have",
    "had",
    "doing",
    "done",
    "today",
    "now",
    "currently",
    "recently",
    "just",
}

CANONICAL_TOKEN_ALIASES = {
    "cant": "cannot",
    "struggle": "cannot",
    "struggles": "cannot",
    "struggled": "cannot",
    "trouble": "cannot",
    "troubles": "cannot",
    "troubled": "cannot",
    "unable": "cannot",
    "difficult": "cannot",
    "difficulty": "cannot",
    "detects": "detect",
    "detected": "detect",
    "detecting": "detect",
    "detection": "detect",
    "readings": "reading",
    "caregivers": "caregiver",
    "missing": "missing",
    "delayed": "delay",
    "stale": "stale",
    "disconnects": "disconnect",
    "alerts": "alert",
    "crashes": "crash",
}

CANONICAL_TOKEN_ALIASES_PL = {
    "odczytu": "missing_reading", "odczytów": "missing_reading", "danych": "data",
    "sygnału": "signal_loss", "rozłącza": "disconnect", "łączy": "connect",
    "alarmy": "alert", "alarm": "alert", "powiadomienie": "notification",
    "działa": "working", "opóźnienie": "delay", "opóźnione": "delay",
    "stare": "stale", "opiekunowi": "caregiver", "opiekun": "caregiver",
}

PROBLEM_INTENT_ALIASES = {
    "en": {
        "missing_data": (
            r"\b(?:missing|no)\s+(?:new\s+)?(?:data|readings?)\b",
            r"\b(?:data|readings?)\s+(?:are\s+)?missing\b",
            r"\bnot\s+receiv(?:e|ing)\s+(?:new\s+)?(?:blood\s+)?glucose\s+(?:data|values?|readings?)\b",
            r"\bno\s+new\s+(?:(?:blood\s+)?glucose\s+)?(?:value|reading)\b",
        ),
        "stale_data": (
            r"\bstale\s+(?:data|readings?)\b",
            r"\b(?:(?:blood\s+)?glucose\s+(?:data|values?|readings?)|(?:cgm|sensor)\s+(?:data|values?|readings?)|readings?)\b.{0,24}\b(?:isn\s+t|aren\s+t|not|stops?|stopped)\s+updating\b",
            r"\bfrozen\s+readings?\b",
            r"\blast\s+received\s+(?:(?:blood\s+)?glucose\s+)?value\s+remains\b",
        ),
        "delay": (r"\bdelay(?:ed|s)?\b",),
        "signal_loss": (r"\bsignal\s+loss\b", r"\blost\s+signal\b"),
        "disconnect": (
            r"\bdisconnect(?:ed|s|ing)?\b",
            r"\b(?:losing|loosing|lost)\s+(?:the\s+)?connection\b",
            r"\bconnection\s+(?:to\s+(?:the\s+)?sensor\s+)?(?:is\s+)?lost\b",
            r"\bconnection\s+drops?\b",
        ),
        "alert_not_firing": (r"\b(?:alerts?|alarms?|notifications?)\b.{0,32}\b(?:not|stop(?:ped)?)\s+(?:firing|working)\b",),
        "false_alert": (r"\bfalse\s+(?:alert|alarm)\b",),
        "repeated_alert": (r"\b(?:repeated|duplicate)\s+(?:alert|alarm|notification)\b",),
        "activation_failure": (
            r"\b(?:activation|activate)\b.{0,24}\b(?:fail|failed|cannot|unable)\b",
            r"\b(?:unsupported|unrecognized)\s+sensor\b",
            r"\bsensor\s+(?:is\s+)?not\s+recognized\b",
        ),
        "connection_failure": (
            r"\b(?:cannot|can\s+t|unable|fails?\s+to)\s+connect\b(?!\s+(?:the\s+)?sensor\b)",
            r"\b(?<!sensor\s)connection\s+(?:failure|failed)\b",
        ),
        "sensor_connection_failure": (
            r"\bsensor\s+connection\s+failed\b",
            r"\b(?:sensor\s+(?:cannot|can\s+t|is\s+unable\s+to)\s+connect|(?:cannot|can\s+t|unable\s+to)\s+connect\s+(?:the\s+)?sensor)\b",
        ),
        "sharing_failure": (r"\bshar(?:e|ing)\b.{0,28}\b(?:fail|failed|not working|missing|delay)\w*\b",),
        "caregiver_visibility": (r"\b(?:data|readings?)\b.{0,40}\b(?:missing|delay\w*|not visible)\b.{0,40}\bcaregiver\b", r"\bcaregiver\b.{0,40}\b(?:cannot see|can't see|missing|delay\w*|not visible)\b"),
        "expiry_notification": (r"\bexpir\w*\b.{0,24}\bnotification\b",),
        "os_update_breakage": (r"\b(?:android|ios|os)\s+update\b.{0,40}\b(?:broke|broken|stop\w*|not working|fail\w*)\b",),
        "history_missing": (r"\b(?:missing|lost|no)\s+history\b",),
        "notification_customization": (r"\bcustomi[sz]\w*\b.{0,24}\bnotifications?\b",),
        "watch_visibility": (r"\b(?:watch|smartwatch)\b.{0,32}\b(?:cannot see|can't see|missing|not visible|blank)\b",),
    },
    "pl": {
        "missing_data": (r"\bbrak\s+(?:nowych\s+)?(?:danych|odczytów?)\b", r"\bnie pokazuje\s+(?:nowych\s+)?(?:danych|odczytów?)\b"),
        "stale_data": (r"\bstare\s+(?:dane|odczyty)\b", r"\bnieaktualne\s+(?:dane|odczyty)\b"),
        "delay": (r"\bopóźn(?:ienie|ione|iony|ionych)\b",),
        "signal_loss": (r"\b(?:utrata|brak)\s+sygnału\b",),
        "disconnect": (r"\brozłącz\w*\b",),
        "alert_not_firing": (r"\b(?:alarmy?|powiadomienia?)\b.{0,32}\b(?:nie dział\w*|przestał\w*)\b",),
        "false_alert": (r"\bfałszyw\w*\s+(?:alarm|alert|powiadomienie)\b",),
        "repeated_alert": (r"\b(?:powtarzające|zduplikowane)\s+(?:alarmy|alerty|powiadomienia)\b",),
        "activation_failure": (r"\b(?:aktywacja|aktywować)\b.{0,24}\b(?:nie działa|nie można|błąd)\b",),
        "connection_failure": (r"\b(?:nie można|nie da się|nie)\s+połącz\w*\b",),
        "sharing_failure": (r"\budostępnian\w*\b.{0,28}\b(?:nie działa|błąd|brak|opóź)\w*\b",),
        "caregiver_visibility": (r"\bnie pokazuje\s+(?:nowych\s+)?(?:danych|odczytów?)\b.{0,40}\bopiekun\w*\b", r"\bopiekun\w*\b.{0,40}\b(?:nie widzi|brak|opóźn\w*)\b"),
        "expiry_notification": (r"\b(?:wygaśnięci|końcu ważności)\w*\b.{0,24}\bpowiadomieni\w*\b",),
        "os_update_breakage": (r"\bpo aktualizacji\b.{0,32}\b(?:android|ios|systemu)\b.{0,40}\b(?:nie dział\w*|przestał\w*)\b",),
        "history_missing": (r"\b(?:brak|utrata|zniknęła)\s+historii\b",),
        "notification_customization": (r"\b(?:dostosowa|personaliz)\w*\b.{0,24}\bpowiadomieni\w*\b",),
        "watch_visibility": (r"\b(?:zegarek|smartwatch)\b.{0,32}\b(?:nie pokazuje|nie widać|brak|pusty)\b",),
    },
    "de": {
        "missing_data": (r"\bkeine\s+(?:daten|messwerte)\b", r"\bfehlende\s+(?:daten|messwerte)\b"),
        "delay": (r"\bverzöger\w*\b",), "signal_loss": (r"\bsignalverlust\b",),
        "disconnect": (r"\bverbindungsabbruch\b",), "alert_not_firing": (r"\balarm\w*\b.{0,24}\bfunktioniert nicht\b",),
        "connection_failure": (r"\bkeine verbindung\b", r"\bverbindung fehlgeschlagen\b"),
        "caregiver_visibility": (r"\b(?:daten|messwerte)\b.{0,32}\b(?:fehlen|verzöger\w*)\b.{0,32}\bbetreuer\w*\b",),
        "history_missing": (r"\b(?:fehlende|keine)\s+historie\b",), "watch_visibility": (r"\buhr\b.{0,24}\bnicht sichtbar\b",),
    },
    "fr": {
        "missing_data": (r"\b(?:données|lectures)\s+manquantes\b",), "delay": (r"\bretard\w*\b",),
        "signal_loss": (r"\bperte de signal\b",), "disconnect": (r"\bdéconnexion\b",),
        "alert_not_firing": (r"\balarme\w*\b.{0,24}\bne fonctionne pas\b",), "connection_failure": (r"\béchec de connexion\b",),
        "caregiver_visibility": (r"\b(?:données|lectures)\b.{0,32}\b(?:manquantes|retard\w*)\b.{0,32}\baidant\w*\b",),
        "history_missing": (r"\bhistorique\s+manquant\b",), "watch_visibility": (r"\bmontre\b.{0,24}\b(?:invisible|vide)\b",),
    },
    "es": {
        "missing_data": (r"\b(?:datos|lecturas)\s+(?:faltantes|ausentes)\b",), "delay": (r"\bretras\w*\b",),
        "signal_loss": (r"\bpérdida de señal\b",), "disconnect": (r"\bdesconexión\b",),
        "alert_not_firing": (r"\balarma\w*\b.{0,24}\bno funciona\b",), "connection_failure": (r"\bfallo de conexión\b",),
        "caregiver_visibility": (r"\b(?:datos|lecturas)\b.{0,32}\b(?:faltan|retras\w*)\b.{0,32}\bcuidador\w*\b",),
        "history_missing": (r"\bhistorial\s+(?:faltante|perdido)\b",), "watch_visibility": (r"\breloj\b.{0,24}\b(?:no visible|vacío)\b",),
    },
}

CONCEPT_INTENT_FACETS = {
    "stale_or_missing_readings": {"missing_data", "stale_data", "delay"},
    "alerts_not_firing": {"alert_not_firing"},
    "false_or_repeated_alerts": {"false_alert", "repeated_alert"},
    "signal_loss_disconnect": {"signal_loss", "disconnect", "connection_failure"},
    "caregiver_remote_monitoring": {"sharing_failure", "caregiver_visibility"},
    "glucose_sharing_delay_or_failure": {"missing_data", "stale_data", "delay", "sharing_failure", "caregiver_visibility"},
    "sensor_activation_connection_failure": {"activation_failure", "connection_failure", "sensor_connection_failure"},
    "sensor_expiry_notification": {"expiry_notification"},
    "phone_os_compatibility": {"os_update_breakage"},
    "watch_widget_glanceability": {"watch_visibility"},
    "history_reports_statistics": {"history_missing"},
    "notification_customization": {"notification_customization"},
}

CANONICAL_CONCEPT_STATEMENTS = {
    "stale_or_missing_readings": ("Glucose readings can be missing, delayed, or stale.", "Odczyty glukozy mogą być niedostępne, opóźnione lub nieaktualne."),
    "alerts_not_firing": ("Glucose alerts may fail to activate.", "Alerty glukozy mogą się nie uruchamiać."),
    "false_or_repeated_alerts": ("Glucose alerts can be false or repeat unnecessarily.", "Alerty glukozy mogą być fałszywe lub niepotrzebnie się powtarzać."),
    "signal_loss_disconnect": ("The sensor/app connection can be lost or become unstable.", "Połączenie sensora z aplikacją może zostać utracone lub stać się niestabilne."),
    "caregiver_remote_monitoring": ("Remote glucose monitoring for caregivers can be unavailable or unreliable.", "Zdalne monitorowanie glukozy przez opiekunów może być niedostępne lub zawodne."),
    "glucose_sharing_delay_or_failure": ("Shared glucose readings can be delayed, missing, or unavailable.", "Udostępniane odczyty glukozy mogą być opóźnione, niedostępne lub nie docierać."),
    "sensor_activation_connection_failure": ("A sensor may fail to activate or connect.", "Aktywacja lub połączenie sensora może się nie powieść."),
    "sensor_expiry_notification": ("Sensor expiry notifications may be missing or inadequate.", "Powiadomienia o wygaśnięciu sensora mogą być niedostępne lub niewystarczające."),
    "phone_os_compatibility": ("Phone or operating-system compatibility can disrupt glucose monitoring.", "Problemy ze zgodnością telefonu lub systemu operacyjnego mogą zakłócać monitorowanie glukozy."),
    "watch_widget_glanceability": ("Current glucose information may not be visible at a glance on a watch.", "Aktualna informacja o glukozie może nie być widoczna na pierwszy rzut oka na zegarku."),
    "history_reports_statistics": ("Glucose history, reports, or statistics can be missing or incomplete.", "Historia, raporty lub statystyki glukozy mogą być niedostępne lub niepełne."),
    "notification_customization": ("Glucose notifications may not offer sufficient customization.", "Powiadomienia dotyczące glukozy mogą nie zapewniać wystarczających możliwości dostosowania."),
}

CANONICAL_FACET_STATEMENTS = {
    ("stale_or_missing_readings", "missing_data"): ("Glucose readings may be missing or fail to arrive.", "Odczyty glukozy mogą być niedostępne lub nie docierać."),
    ("stale_or_missing_readings", "stale_data"): ("Glucose readings can stop updating or remain stale.", "Odczyty glukozy mogą przestać się aktualizować lub pozostać nieaktualne."),
    ("stale_or_missing_readings", "delay"): ("Glucose readings can arrive late.", "Odczyty glukozy mogą docierać z opóźnieniem."),
    ("false_or_repeated_alerts", "false_alert"): ("Glucose alerts can be false.", "Alerty glukozy mogą być fałszywe."),
    ("false_or_repeated_alerts", "repeated_alert"): ("Glucose alerts can repeat unnecessarily.", "Alerty glukozy mogą niepotrzebnie się powtarzać."),
    ("signal_loss_disconnect", "signal_loss"): ("The sensor signal can be lost.", "Sygnał sensora może zostać utracony."),
    ("signal_loss_disconnect", "disconnect"): ("The sensor/app connection can be lost or become unstable.", "Połączenie sensora z aplikacją może zostać utracone lub stać się niestabilne."),
    ("signal_loss_disconnect", "connection_failure"): ("The sensor/app connection can fail.", "Połączenie sensora z aplikacją może się nie powieść."),
    ("glucose_sharing_delay_or_failure", "delay"): ("Shared glucose readings can arrive late.", "Udostępniane odczyty glukozy mogą docierać z opóźnieniem."),
    ("glucose_sharing_delay_or_failure", "sharing_failure"): ("Glucose sharing can fail.", "Udostępnianie danych o glukozie może nie działać."),
    ("sensor_activation_connection_failure", "activation_failure"): ("A sensor may fail to activate.", "Aktywacja sensora może się nie powieść."),
    ("sensor_activation_connection_failure", "sensor_connection_failure"): ("A sensor may fail to connect.", "Połączenie sensora może się nie powieść."),
}

CONCEPT_CONTEXT_PATTERNS = {
    "stale_or_missing_readings": r"\b(?:glucose|cgm|libre|dexcom|readings?|odczyt\w*|glukoz\w*|cukr\w*|messwert\w*|lectures?|lecturas?)\b",
    "caregiver_remote_monitoring": r"\b(?:caregiver|family|share|opiekun\w*|rodzin\w*|udostępn\w*)\b",
    "glucose_sharing_delay_or_failure": r"\b(?:librelinkup|caregiver|opiekun\w*)\b",
    "sensor_activation_connection_failure": r"\b(?:sensor|capteur)\b",
    "sensor_expiry_notification": r"\b(?:sensor|capteur)\b",
    "phone_os_compatibility": r"\b(?:android|ios|phone|telefon|os)\b",
    "watch_widget_glanceability": r"\b(?:watch|smartwatch|zegarek|widget|widżet)\b",
    "history_reports_statistics": r"\b(?:history|historii|historique|historial|verlauf|report\w*|raport\w*)\b",
    "notification_customization": r"\b(?:notification\w*|powiadomieni\w*|benachrichtigung\w*|alarms?|alert\w*)\b",
}

PII_PATTERNS = [
    (re.compile(r"\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b", re.I), "[REDACTED_EMAIL]"),
    (re.compile(r"(?<![\w:])(?:\d{1,3}\.){3}\d{1,3}(?![\w:])"), "[REDACTED_IP]"),
    (re.compile(r"(?<![\w:])(?:[A-F0-9]{0,4}:){2,7}[A-F0-9]{0,4}(?![\w:])", re.I), "[REDACTED_IP]"),
    (re.compile(r"(?<!\w)@[A-Za-z0-9_]{2,30}\b"), "[REDACTED_HANDLE]"),
    (re.compile(r"(?<!\w)(?:\+?\d[\s().-]?){8,15}(?!\w)"), "[REDACTED_PHONE]"),
    (re.compile(r"\b(?:sensor|serial|order|account|device)(?:\s*(?:id|sn)\s*[:#_-]?\s*[A-Z0-9-]{6,}|[\s:#_-]+(?=[A-Z0-9-]*\d)[A-Z0-9-]{8,})\b", re.I), "[REDACTED_IDENTIFIER]"),
    (re.compile(r"\b(?:sensor|serial|order|account|device)\s+[A-Z][A-Z0-9-]{7,}\b"), "[REDACTED_IDENTIFIER]"),
    (re.compile(r"\b(?:bearer\s+|token[\s:=]+)[A-Za-z0-9._~-]{12,}\b", re.I), "[REDACTED_TOKEN]"),
    (re.compile(r"\b(?:authorization|cookie|set-cookie)\s*:\s*[^\s,;]+", re.I), "[REDACTED_SECRET]"),
    (re.compile(r"\b[A-F0-9]{24,}\b", re.I), "[REDACTED_IDENTIFIER]"),
]

MARKETING_MARKERS = {"help center", "continuous glucose monitoring", "official site", "learn more", "sugarmate", "freestyle libre", "dexcom"}
TECHNICAL_MARKERS = {"manifest", "receiver", "sdk", "dependency", "dependencies", "ci", "lint", "refactor", "schema migration", "broadcast", "gradle", "build system", "oop2"}
USER_IMPACT_MARKERS = {
    "no reading", "missing reading", "missing data", "stale", "signal loss", "disconnect", "not working", "won't connect", "cannot connect",
    "connection to sensor is lost", "glucose values aren t updating", "glucose values are not updating", "glucose values not updating",
    "not receiving glucose values", "unsupported sensor",
    "alert", "alarm", "blank", "delayed", "delay", "activate sensor", "loses history", "lost history", "brak odczytu", "brak danych",
    "utrata sygnału", "nie działa", "nie łączy", "rozłącza", "opóź", "stare dane", "przestały działać", "nie pokazuje",
    "crash", "crashes", "awaria",
}


class SourceResult:
    def __init__(self, name: str, family: str, status: str, items: list[dict], detail: str = "", evidence_role: str = "user_community"):
        self.name = name
        self.family = family
        self.status = status
        self.items = items
        self.detail = detail
        self.evidence_role = evidence_role


class DiscoveryCache:
    def __init__(self, path: Path):
        self.path = path
        self._data = {"version": 1, "entries": {}}
        self._load()

    def _load(self) -> None:
        if not self.path.exists():
            return
        try:
            payload = json.loads(self.path.read_text(encoding="utf-8"))
            if isinstance(payload, dict) and isinstance(payload.get("entries"), dict):
                self._data = payload
        except Exception:
            # Invalid cache must never break run determinism.
            self._data = {"version": 1, "entries": {}}

    def get(self, key: str, max_age_seconds: int):
        entries = self._data.get("entries", {})
        row = entries.get(key)
        if not isinstance(row, dict):
            return None
        ts = row.get("cached_at")
        if not isinstance(ts, (int, float)):
            return None
        if time.time() - ts > max(1, max_age_seconds):
            return None
        value = row.get("value")
        return value

    def put(self, key: str, value) -> None:
        _validate_cache_payload(value)
        self._data.setdefault("entries", {})
        self._data["entries"][key] = {"cached_at": int(time.time()), "value": value}

    def save(self) -> None:
        _validate_cache_payload(self._data)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        serialized = json.dumps(self._data, ensure_ascii=False, indent=2)
        # Defensive redaction guard: never persist likely credentials.
        lowered = serialized.lower()
        for marker in ["reddit_client_secret", "authorization", "bearer ", "github_token", "client_secret", "access_token", "refresh_token"]:
            if marker in lowered:
                raise RuntimeError("Cache serialization blocked: credential-like marker detected")
        self.path.write_text(serialized + "\n", encoding="utf-8")


def _truncate_text(text: str, limit: int) -> str:
    cleaned = sanitize_text(text)[0]
    if len(cleaned) <= limit:
        return cleaned
    return cleaned[: limit - 3].rstrip() + "..."


def sanitize_text(text: str) -> tuple[str, bool]:
    cleaned = str(text or "")
    redacted = False
    for pattern, replacement in PII_PATTERNS:
        cleaned, count = pattern.subn(replacement, cleaned)
        redacted = redacted or count > 0
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    return cleaned, redacted


def detect_language(text: str, configured: str = "") -> str:
    configured = configured.lower().strip()
    if configured in SUPPORTED_LANGUAGES:
        return configured
    normalized = normalize_text(text)
    words = set(normalized.split())
    markers = {
        "pl": {"brak", "odczytu", "sygnału", "działa", "alarmy", "opiekun", "danych", "aktualizacji", "łączy"},
        "en": {"readings", "signal", "working", "caregiver", "missing", "delayed", "alert", "alerts", "connection", "disconnects", "losing", "loosing", "drops", "stopped", "false"},
        "de": {"keine", "messwerte", "signalverlust", "alarm", "verbindung", "betreuer"},
        "fr": {"lectures", "manquantes", "alarme", "connexion", "retard", "aidant"},
        "es": {"lecturas", "faltan", "alarma", "conexión", "retraso", "cuidador"},
    }
    scores = {lang: len(words & terms) for lang, terms in markers.items()}
    best = max(scores, key=scores.get)
    return best if scores[best] > 0 and list(scores.values()).count(scores[best]) == 1 else "unknown"


def _default_evidence_role(family: str) -> str:
    if family in {"official_vendor", "competitor"}:
        return "official_reference"
    if family == "github_community":
        return "developer_community"
    return "user_community"


def assess_problem_signal(text: str, title: str, evidence_role: str) -> dict:
    lowered = normalize_text(f"{title} {text}")
    title_lower = normalize_text(title)
    user_facing = any(marker in lowered for marker in USER_IMPACT_MARKERS)
    technical = any(marker in lowered for marker in TECHNICAL_MARKERS) and not user_facing
    title_only = not text.strip() or normalize_text(text) == title_lower
    marketing = evidence_role == "official_reference" and (title_only or any(marker in title_lower for marker in MARKETING_MARKERS))
    quality = 0
    if user_facing:
        quality = 3
    elif evidence_role != "official_reference" and len(lowered.split()) >= 5 and not technical:
        quality = 2
    elif len(lowered.split()) >= 3:
        quality = 1
    eligible = quality >= 2 and not marketing and not technical and evidence_role != "official_reference"
    reason = "accepted" if eligible else "marketing_only" if marketing else "technical_only" if technical else "insufficient_problem_signal"
    return {
        "problem_signal_quality": quality,
        "user_facing": user_facing,
        "technical_only": technical,
        "marketing_only": marketing,
        "eligible_for_clustering": eligible,
        "filter_reason": reason,
    }


def infer_concept(text: str) -> tuple[str, str]:
    lowered = normalize_text(text)
    rules = [
        ("app_startup_failure", "reliability", ("startup", "uruchom", "launch"), ("crash", "crashes", "awaria")),
        ("glucose_sharing_delay_or_failure", "caregiver", ("librelinkup", "caregiver", "opiekun", "family", "rodzin"), ("delay", "delayed", "missing", "brak", "nie pokazuje", "stale")),
        ("alerts_not_firing", "alert_reliability", ("alarm", "alert", "powiadom"), ("not working", "nie działa", "przestały", "missing", "brak")),
        ("signal_loss_disconnect", "connectivity", ("signal", "sygnał", "connect", "połączen", "disconnect", "rozłącza"), ("loss", "utrata", "brak", "failed", "nie łączy")),
        ("sensor_activation_connection_failure", "sensor_lifecycle", ("sensor", "capteur"), ("activate", "activation", "connect", "łączy", "failed", "działa")),
        ("stale_or_missing_readings", "data_freshness", ("reading", "odczyt", "data", "dane"), ("stale", "missing", "brak", "fresh", "current")),
        ("phone_os_compatibility", "compatibility", ("android", "phone", "telefon"), ("update", "aktualizacji", "problem", "stop")),
        ("watch_widget_glanceability", "glanceability", ("watch", "smartwatch", "zegarek", "widget", "widżet"), ("glucose", "glukoza", "cgm", "libre")),
    ]
    for concept, topic, anchors, impacts in rules:
        if any(x in lowered for x in anchors) and any(x in lowered for x in impacts):
            return concept, topic
    return "unclassified", "unclassified"


def load_query_packs(path: Path = QUERY_PACKS_DEFAULT) -> dict:
    payload = read_json(path)
    if payload.get("version") != 1 or not isinstance(payload.get("concepts"), list):
        raise RuntimeError("Invalid Discovery query-packs version or concepts")
    return payload


def iter_queries(query_packs: dict, languages: list[str], primary_languages: list[str] | None = None, max_queries: int = 48):
    concepts = query_packs.get("concepts", [])
    language_cfg = query_packs.get("languages") or {}
    requested_primary = PRIMARY_LANGUAGES if primary_languages is None else set(primary_languages)
    language_order = {lang: idx for idx, lang in enumerate(languages)}
    primary = [lang for lang in languages if lang in requested_primary]
    primary.sort(key=lambda lang: (-float(language_cfg.get(lang, {}).get("priority", 0.0)), language_order[lang]))
    secondary = [lang for lang in languages if lang not in primary]
    secondary.sort(key=lambda lang: (-float(language_cfg.get(lang, {}).get("priority", 0.0)), language_order[lang]))
    ordered = []
    # Cover every concept in primary languages before spending the secondary budget.
    for concept in concepts:
        for language in primary:
            queries = (concept.get("queries") or {}).get(language, [])
            if queries:
                ordered.append((concept, language, 0, queries[0]))
    max_depth = max((len((concept.get("queries") or {}).get(lang, [])) for concept in concepts for lang in primary), default=1)
    for idx in range(1, max_depth):
        for concept in concepts:
            for language in primary:
                queries = (concept.get("queries") or {}).get(language, [])
                if idx < len(queries):
                    ordered.append((concept, language, idx, queries[idx]))
    # Half-budget languages get one rotating starter query per concept first.
    secondary_rows = []
    if secondary:
        for concept_idx, concept in enumerate(concepts):
            language = secondary[concept_idx % len(secondary)]
            queries = (concept.get("queries") or {}).get(language, [])
            if queries:
                secondary_rows.append((concept, language, 0, queries[0]))
    secondary_budget = min(len(secondary_rows), max_queries // 4) if secondary else 0
    selected = ordered[: max_queries - secondary_budget] + secondary_rows[:secondary_budget]
    for concept, language, idx, query in selected:
        yield {
            "query": str(query), "language": language,
            "query_id": f"{concept['concept_id']}:{language}:{idx + 1}",
            "concept_id": concept["concept_id"], "topic_id": concept["topic_id"],
        }


def bounded_query_rows(query_packs: dict, languages: list[str], max_queries: int, primary_languages: list[str] | None = None) -> list[dict]:
    rows = list(iter_queries(query_packs, languages, primary_languages=primary_languages, max_queries=max_queries))
    if rows:
        return rows
    return [{"query": "Libre missing readings", "language": "en", "query_id": "fallback:en:1", "concept_id": "stale_or_missing_readings", "topic_id": "data_freshness"}]


def _validate_cache_payload(value, path: str = "cache") -> None:
    if isinstance(value, dict):
        for key, item in value.items():
            key_str = str(key).lower()
            if key_str in FORBIDDEN_CACHE_KEYS:
                raise RuntimeError(f"Cache serialization blocked: forbidden key detected at {path}.{key_str}")
            _validate_cache_payload(item, f"{path}.{key_str}")
        return
    if isinstance(value, list):
        for idx, item in enumerate(value):
            _validate_cache_payload(item, f"{path}[{idx}]")
        return
    if isinstance(value, str):
        lowered = value.lower()
        for marker in ["<html", "</html", "<body", "</body", "<script", "</script", "<style", "</style"]:
            if marker in lowered:
                raise RuntimeError(f"Cache serialization blocked: raw HTML detected at {path}")


def _normalize_cached_item(item: dict, source_name: str, source_family: str, source_type: str) -> dict | None:
    normalized = _normalize_item_fields(item, source_name, source_family, source_type)
    if not normalized:
        return None
    cached = {
        "canonical_url": normalized["canonical_url"],
        "source_name": normalized["source_name"],
        "source_family": normalized["source_family"],
        "source_identity": normalized["source_identity"],
        "source_type": normalized["source_type"],
        "retrieved_at": normalized["retrieved_at"],
        "content_hash": normalized["content_hash"],
        "excerpt": _truncate_text(normalized["excerpt"], MAX_CACHED_EXCERPT_LENGTH),
        "problem_statement": _truncate_text(normalized["problem_statement"], MAX_CACHED_PROBLEM_LENGTH),
        "canonical_problem_key": normalized["canonical_problem_key"],
        "canonical_problem_fingerprint": normalized["canonical_problem_fingerprint"],
        "persona": normalized["persona"],
        "mode": normalized["mode"],
        "module": normalized["module"],
        "type": normalized["type"],
        "severity": normalized["severity"],
        "frequency": normalized["frequency"],
        "confidence": normalized["confidence"],
        "language": normalized["language"],
        "concept_id": normalized["concept_id"],
        "topic_id": normalized["topic_id"],
        "query_id": normalized["query_id"],
        "evidence_role": normalized["evidence_role"],
        "problem_signal_quality": normalized["problem_signal_quality"],
        "user_facing": normalized["user_facing"],
        "technical_only": normalized["technical_only"],
        "marketing_only": normalized["marketing_only"],
        "eligible_for_clustering": normalized["eligible_for_clustering"],
        "filter_reason": normalized["filter_reason"],
        "privacy_redacted": normalized["privacy_redacted"],
        "updated_at": normalized.get("updated_at", ""),
    }
    return cached


def _cache_get_items(cache: DiscoveryCache, key: str, max_age_seconds: int) -> list[dict] | None:
    value = cache.get(key, max_age_seconds)
    if not isinstance(value, list):
        return None
    items = [item for item in value if isinstance(item, dict)]
    return items or None


def _cache_put_items(cache: DiscoveryCache, key: str, items: list[dict]) -> None:
    cache.put(key, items)


class GitHubIssueClient:
    def __init__(self, owner: str, repo: str, token: str):
        self.owner = owner
        self.repo = repo
        self.token = token
        self.base = f"https://api.github.com/repos/{owner}/{repo}"

    def _request(self, method: str, path: str, payload: dict | None = None):
        url = f"{self.base}{path}"
        data = None
        headers = {
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {self.token}",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "LibreCare-Discovery-Agent/1.0",
        }
        if payload is not None:
            data = json.dumps(payload).encode("utf-8")
            headers["Content-Type"] = "application/json"
        req = urllib_request.Request(url, data=data, headers=headers, method=method)
        with urllib_request.urlopen(req, timeout=20) as resp:
            body = resp.read().decode("utf-8")
            return json.loads(body) if body else {}

    def find_issue_by_marker(self, marker: str) -> dict | None:
        page = 1
        while page <= 10:
            issues = self._request("GET", f"/issues?state=all&labels=product-inbox&per_page=100&page={page}")
            if not isinstance(issues, list) or not issues:
                return None
            for issue in issues:
                if "pull_request" in issue:
                    continue
                body = issue.get("body") or ""
                if marker in body:
                    return issue
            page += 1
        return None

    def create_issue(self, title: str, body: str, labels: list[str]) -> dict:
        return self._request("POST", "/issues", {"title": title, "body": body, "labels": labels})


GITHUB_CLIENT_FACTORY = GitHubIssueClient


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def read_json(path: Path):
    with path.open("r", encoding="utf-8-sig") as fh:
        return json.load(fh)


def write_json(path: Path, payload: dict | list) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as fh:
        json.dump(payload, fh, ensure_ascii=False, indent=2)
        fh.write("\n")


def _validate_registry_entry(entry: dict) -> bool:
    """Validate a single registry entry has required fields."""
    if not isinstance(entry, dict):
        return False
    cid = str(entry.get("cluster_id") or "").strip()
    key = str(entry.get("canonical_problem_key") or "").strip()
    return bool(cid and key)


def _strict_validate_registry_payload(payload) -> None:
    """Strictly validate registry payload for existing files. Raises RuntimeError if invalid."""
    if not isinstance(payload, dict):
        raise RuntimeError(f"Registry file is not a JSON object: got {type(payload).__name__}")

    if "entries" not in payload:
        raise RuntimeError("Registry file missing required 'entries' field")

    entries = payload.get("entries")
    if not isinstance(entries, list):
        raise RuntimeError(f"Registry 'entries' field is not a list: got {type(entries).__name__}")

    for idx, entry in enumerate(entries):
        if not isinstance(entry, dict):
            raise RuntimeError(f"Registry entry[{idx}] is not an object: got {type(entry).__name__}")
        if not _validate_registry_entry(entry):
            raise RuntimeError(f"Registry entry[{idx}] missing required fields (cluster_id, canonical_problem_key)")

    version = payload.get("version")
    if version != CLUSTER_REGISTRY_VERSION:
        raise RuntimeError(f"Unsupported registry version: {version}. Expected {CLUSTER_REGISTRY_VERSION}.")


def _cluster_registry_payload(payload) -> dict:
    """Normalize valid registry payload. For existing files, strict validation is done separately."""
    if not isinstance(payload, dict):
        return {"version": CLUSTER_REGISTRY_VERSION, "entries": []}
    entries = payload.get("entries")
    if not isinstance(entries, list):
        entries = []
    out = []
    for raw in entries:
        if not isinstance(raw, dict):
            continue
        cid = str(raw.get("cluster_id") or "").strip()
        key = str(raw.get("canonical_problem_key") or "").strip()
        if not cid or not key:
            continue
        out.append(
            {
                "cluster_id": cid,
                "canonical_problem_key": _truncate_text(key, 220),
                "problem_fingerprint": str(raw.get("problem_fingerprint") or "").strip(),
                "persona": _truncate_text(str(raw.get("persona") or "unknown"), 80),
                "module": _truncate_text(str(raw.get("module") or "unknown"), 120),
                "created_at": str(raw.get("created_at") or "").strip(),
                "last_seen_at": str(raw.get("last_seen_at") or "").strip(),
            }
        )
    out.sort(key=lambda x: x["cluster_id"])
    return {"version": int(payload.get("version") or CLUSTER_REGISTRY_VERSION), "entries": out}


def load_cluster_registry(path: Path = DISCOVERY_CLUSTER_REGISTRY_PATH) -> dict:
    if not path.exists():
        return {"version": CLUSTER_REGISTRY_VERSION, "entries": []}
    try:
        payload = read_json(path)
        # Strict validation for existing files
        _strict_validate_registry_payload(payload)
        cleaned = _cluster_registry_payload(payload)
        return cleaned
    except RuntimeError:
        # Re-raise our own validation errors
        raise
    except Exception as e:
        # Wrap any other errors (JSONDecodeError, etc.)
        raise RuntimeError(f"Failed to load cluster registry from {path}: {e}") from e


def _deterministic_cluster_id_for_key(canonical_problem_key: str, persona: str, module: str) -> str:
    material = "|".join([canonical_problem_key.strip(), persona.strip(), module.strip()])
    digest = hashlib.sha1(material.encode("utf-8")).hexdigest()[:12].upper()
    return f"DISC-{digest}"


def _registry_match_score(cluster: dict, entry: dict) -> float:
    cluster_tokens = set(_canonical_problem_tokens(str(cluster.get("canonical_problem_key") or "")))
    entry_tokens = set(_canonical_problem_tokens(str(entry.get("canonical_problem_key") or "")))
    score = jaccard_similarity(cluster_tokens, entry_tokens)
    persona = str(cluster.get("persona_candidate") or "")
    module = str(cluster.get("module_candidate") or "")
    if persona and entry.get("persona") and persona != entry.get("persona"):
        score *= 0.9
    if module and entry.get("module") and module != entry.get("module"):
        score *= 0.95
    return score


def resolve_cluster_ids_with_registry(
    clusters: list[dict],
    registry: dict,
    now_iso: str,
    observation_clusters: list[dict] | None = None,
) -> tuple[list[dict], dict, dict]:
    entries = [dict(x) for x in (registry.get("entries") or []) if isinstance(x, dict)]
    existing_ids = {str(x.get("cluster_id") or "") for x in entries}
    matched_existing = 0
    new_entries = 0
    ambiguous_matches = []
    excluded_weak_or_noise = 0

    for cluster in clusters:
        cluster_key = str(cluster.get("canonical_problem_key") or "")
        cluster["canonical_problem_key"] = cluster_key
        persona = str(cluster.get("persona_candidate") or "unknown")
        module = str(cluster.get("module_candidate") or "unknown")
        canonical_fp = str(cluster.get("canonical_problem_fingerprint") or "")
        if not canonical_fp and cluster_key:
            canonical_fp = hashlib.sha256(cluster_key.encode("utf-8")).hexdigest()[:16]
            cluster["canonical_problem_fingerprint"] = canonical_fp

        if not cluster.get("canonical_grounding_safe", True) or not cluster_key or not canonical_fp:
            cluster["identity_ambiguous"] = True
            cluster["identity_ambiguous_candidates"] = []
            excluded_weak_or_noise += 1
            continue

        scored = []
        for entry in entries:
            score = _registry_match_score(cluster, entry)
            if score >= CLUSTER_REGISTRY_MATCH_THRESHOLD:
                scored.append((score, str(entry.get("cluster_id") or ""), entry))
        scored.sort(key=lambda x: (-x[0], x[1]))

        if len(scored) >= 2 and abs(scored[0][0] - scored[1][0]) <= CLUSTER_REGISTRY_AMBIGUITY_DELTA:
            cluster["identity_ambiguous"] = True
            cluster["identity_ambiguous_candidates"] = [
                {"cluster_id": scored[0][1], "score": round(scored[0][0], 3)},
                {"cluster_id": scored[1][1], "score": round(scored[1][0], 3)},
            ]
            ambiguous_matches.append(
                {
                    "provisional_cluster_id": cluster.get("cluster_id"),
                    "canonical_problem_key": cluster_key,
                    "candidates": cluster["identity_ambiguous_candidates"],
                }
            )
            continue

        if scored:
            chosen = scored[0][2]
            cluster["cluster_id"] = str(chosen.get("cluster_id") or cluster["cluster_id"])
            cluster["identity_ambiguous"] = False
            chosen["last_seen_at"] = now_iso
            matched_existing += 1
            continue

        reused_cluster_id = _reuse_existing_cluster_id(cluster, observation_clusters or [])
        if reused_cluster_id:
            cluster_id = reused_cluster_id
        else:
            cluster_id = _deterministic_cluster_id_for_key(cluster_key, persona, module)
        if cluster_id in existing_ids:
            salt = hashlib.sha1(f"{cluster_key}|{persona}|{module}|{len(entries)}".encode("utf-8")).hexdigest()[:6].upper()
            cluster_id = f"DISC-{salt}{cluster_id[-6:]}"
        cluster["cluster_id"] = cluster_id
        cluster["identity_ambiguous"] = False
        registry_eligible = cluster.get("quality_gate_passes", True) and EVIDENCE_TIER_ORDER.get(str(cluster.get("evidence_tier") or "SUPPORTED"), 0) >= EVIDENCE_TIER_ORDER["SUPPORTED"]
        if not registry_eligible:
            excluded_weak_or_noise += 1
            continue
        entry = {
            "cluster_id": cluster_id,
            "canonical_problem_key": _truncate_text(cluster_key, 220),
            "problem_fingerprint": canonical_fp,
            "persona": _truncate_text(persona, 80),
            "module": _truncate_text(module, 120),
            "created_at": now_iso,
            "last_seen_at": now_iso,
        }
        entries.append(entry)
        existing_ids.add(cluster_id)
        new_entries += 1

    entries.sort(key=lambda x: x["cluster_id"])
    proposed = {"version": CLUSTER_REGISTRY_VERSION, "entries": entries}
    summary = {
        "existing_entries": len(registry.get("entries") or []),
        "matched_existing": matched_existing,
        "new_entries": new_entries,
        "ambiguous_matches": ambiguous_matches,
        "excluded_weak_or_noise": excluded_weak_or_noise,
        "changed": bool(new_entries or matched_existing),
    }
    return clusters, summary, proposed


def load_record(path: Path):
    suffix = path.suffix.lower()
    if suffix == ".json":
        return read_json(path)
    if suffix in {".yaml", ".yml"}:
        if not HAVE_YAML:
            raise RuntimeError("PyYAML is required for YAML parsing")
        with path.open("r", encoding="utf-8") as fh:
            return yaml.safe_load(fh)
    raise RuntimeError(f"Unsupported record type: {path}")


def iter_record_files(directory: Path):
    if not directory.exists():
        return
    for path in sorted(directory.iterdir()):
        if not path.is_file():
            continue
        if path.name.startswith("."):
            continue
        if "TEMPLATE" in path.name.upper():
            continue
        if path.suffix.lower() in {".json", ".yaml", ".yml"}:
            yield path


def canonicalize_url(url: str) -> str:
    parsed = urllib_parse.urlsplit(url.strip())
    scheme = (parsed.scheme or "https").lower()
    netloc = parsed.netloc.lower()
    path = re.sub(r"/+", "/", parsed.path or "/")
    if path != "/" and path.endswith("/"):
        path = path[:-1]
    q = urllib_parse.parse_qsl(parsed.query, keep_blank_values=False)
    kept = []
    for k, v in q:
        lk = k.lower()
        if lk.startswith("utm_"):
            continue
        if lk in {"fbclid", "gclid", "mc_cid", "mc_eid"}:
            continue
        kept.append((lk, v.strip()))
    kept.sort()
    query = urllib_parse.urlencode(kept)
    return urllib_parse.urlunsplit((scheme, netloc, path, query, ""))


def normalize_text(text: str) -> str:
    lowered = unescape(text).lower()
    lowered = re.sub(r"https?://\S+", " ", lowered)
    lowered = re.sub(r"[^\w\s]", " ", lowered, flags=re.UNICODE)
    lowered = re.sub(r"\s+", " ", lowered).strip()
    return lowered


def tokenize(text: str, language: str = "unknown") -> list[str]:
    stopwords = STOPWORDS_BY_LANGUAGE.get(language, set())
    return [t for t in normalize_text(text).split(" ") if t and t not in stopwords and len(t) > 2]


def text_fingerprint(text: str, language: str = "unknown") -> str:
    tokens = sorted(set(tokenize(text, language)))
    return hashlib.sha256(" ".join(tokens).encode("utf-8")).hexdigest()[:16]


def _stem_canonical_token(token: str) -> str:
    if len(token) > 6 and token.endswith("ies"):
        return token[:-3] + "y"
    if len(token) > 5 and token.endswith("ing"):
        return token[:-3]
    if len(token) > 4 and token.endswith("ed"):
        return token[:-2]
    if len(token) > 4 and token.endswith("s"):
        return token[:-1]
    return token


def _canonical_problem_tokens(text: str, language: str = "unknown") -> list[str]:
    canonical = []
    aliases = CANONICAL_TOKEN_ALIASES_PL if language == "pl" else CANONICAL_TOKEN_ALIASES
    for token in tokenize(text, language):
        mapped = aliases.get(token, token)
        mapped = _stem_canonical_token(mapped)
        if not mapped or mapped in STOPWORDS_BY_LANGUAGE.get(language, set()) or mapped in CANONICAL_NOISE_TOKENS:
            continue
        if len(mapped) > 2:
            canonical.append(mapped)
    return sorted(set(canonical))


def canonical_problem_key(text: str, language: str = "unknown") -> str:
    tokens = _canonical_problem_tokens(text, language)
    if not tokens:
        return text_fingerprint(text)
    return " ".join(tokens)


def canonical_problem_fingerprint(text: str, language: str = "unknown") -> str:
    return hashlib.sha256(canonical_problem_key(text, language).encode("utf-8")).hexdigest()[:16]


def content_hash(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def jaccard_similarity(a_tokens: set[str], b_tokens: set[str]) -> float:
    if not a_tokens and not b_tokens:
        return 1.0
    if not a_tokens or not b_tokens:
        return 0.0
    inter = len(a_tokens & b_tokens)
    union = len(a_tokens | b_tokens)
    return inter / union if union else 0.0


def _safe_excerpt(text: str, limit: int = 220) -> str:
    cleaned = re.sub(r"\s+", " ", text).strip()
    if len(cleaned) <= limit:
        return cleaned
    return cleaned[: limit - 3].rstrip() + "..."


def _first_sentence(text: str) -> str:
    compact = re.sub(r"\s+", " ", text).strip()
    if not compact:
        return ""
    return re.split(r"(?<=[.!?])\s+", compact, maxsplit=1)[0]


def _problem_from_text(text: str) -> str:
    sentence = _first_sentence(text)
    if len(sentence) < 24:
        sentence = text
    return _safe_excerpt(sentence, 180)


def _extract_html_text(html: str) -> str:
    text = re.sub(r"<script\b[^>]*>.*?</script>", " ", html, flags=re.IGNORECASE | re.DOTALL)
    text = re.sub(r"<style\b[^>]*>.*?</style>", " ", text, flags=re.IGNORECASE | re.DOTALL)
    text = re.sub(r"<[^>]+>", " ", text)
    return re.sub(r"\s+", " ", unescape(text)).strip()


def _extract_html_title(html: str) -> str:
    m = re.search(r"<title[^>]*>(.*?)</title>", html, flags=re.IGNORECASE | re.DOTALL)
    if not m:
        return ""
    return _safe_excerpt(unescape(re.sub(r"\s+", " ", m.group(1))).strip(), 140)


def _source_status_for_failure(name: str) -> str:
    upper = name.upper()
    if "ABBOTT" in upper:
        return "ABBOTT_SOURCE_DEGRADED"
    if "REDDIT" in upper:
        return "REDDIT_DISABLED"
    return f"{upper}_DEGRADED"


def _sanitized_failure_detail(exc: Exception) -> str:
    if isinstance(exc, urllib_error.HTTPError):
        if exc.code in {401, 403}:
            return f"HTTP {exc.code}: authorization/access problem"
        if exc.code == 429:
            return "HTTP 429: rate limit"
        return f"HTTP {exc.code}: upstream request failed"
    return _truncate_text(str(exc), 300)


def _bounded_retry_after(resp_headers) -> float:
    value = ""
    if resp_headers:
        value = resp_headers.get("Retry-After", "")
    try:
        seconds = float(value)
    except Exception:
        seconds = 0.0
    return max(0.0, min(seconds, 3.0))


def _request_bytes(
    url: str,
    method: str,
    timeout: float,
    retries: int,
    headers: dict | None = None,
    data: bytes | None = None,
) -> bytes:
    last_exc = None
    for attempt in range(max(1, retries)):
        req = urllib_request.Request(url, headers=headers or {}, method=method, data=data)
        try:
            with urllib_request.urlopen(req, timeout=timeout) as resp:
                return resp.read()
        except urllib_error.HTTPError as exc:
            last_exc = exc
            if exc.code == 429 and attempt + 1 < max(1, retries):
                time.sleep(_bounded_retry_after(exc.headers))
                continue
            raise
        except Exception as exc:  # noqa: BLE001
            last_exc = exc
    raise RuntimeError(f"Fetch failed: {url}: {last_exc}")


def _fetch_url(url: str, timeout: float, retries: int) -> str:
    headers = {"User-Agent": "LibreCare-Discovery-Agent/1.0 (+public-source-analysis)"}
    raw = _request_bytes(url, "GET", timeout=timeout, retries=retries, headers=headers)
    return raw.decode("utf-8", errors="replace")


def _fetch_json(url: str, timeout: float, retries: int, headers: dict | None = None) -> dict | list:
    merged_headers = {"User-Agent": "LibreCare-Discovery-Agent/1.0 (+public-source-analysis)"}
    if headers:
        merged_headers.update(headers)
    raw = _request_bytes(url, "GET", timeout=timeout, retries=retries, headers=merged_headers)
    return json.loads(raw.decode("utf-8", errors="replace"))


def _post_form_json(url: str, form: dict[str, str], timeout: float, retries: int, headers: dict | None = None) -> dict:
    merged_headers = {"User-Agent": "LibreCare-Discovery-Agent/1.0 (+public-source-analysis)"}
    if headers:
        merged_headers.update(headers)
    body = urllib_parse.urlencode(form).encode("utf-8")
    raw = _request_bytes(url, "POST", timeout=timeout, retries=retries, headers=merged_headers, data=body)
    payload = json.loads(raw.decode("utf-8", errors="replace"))
    return payload if isinstance(payload, dict) else {}


def _cache_key(source_name: str, suffix: str) -> str:
    return f"{source_name}::{suffix}"


def _lookback_window_key(lookback_days: int) -> str:
    return f"lookback-days:{max(1, min(int(lookback_days), 1825))}"


def _timestamp_at_or_after(value, cutoff: datetime) -> bool:
    try:
        if isinstance(value, bool) or value in {None, ""}:
            return False
        if isinstance(value, (int, float)):
            timestamp = datetime.fromtimestamp(float(value), tz=timezone.utc)
        else:
            raw = str(value).strip()
            if re.fullmatch(r"\d+(?:\.\d+)?", raw):
                timestamp = datetime.fromtimestamp(float(raw), tz=timezone.utc)
            else:
                timestamp = datetime.fromisoformat(raw.replace("Z", "+00:00"))
                if timestamp.tzinfo is None:
                    timestamp = timestamp.replace(tzinfo=timezone.utc)
                timestamp = timestamp.astimezone(timezone.utc)
        return timestamp >= cutoff
    except (OverflowError, TypeError, ValueError):
        return False


def _items_within_lookback(items: list[dict], cutoff: datetime) -> list[dict]:
    return [item for item in items if _timestamp_at_or_after(item.get("updated_at"), cutoff)]


def problem_intent_facets(text: str, language: str, concept_id: str) -> set[str]:
    if not concept_id or concept_id == "unclassified":
        return set()
    normalized = normalize_text(text)
    return {
        facet
        for facet, patterns in PROBLEM_INTENT_ALIASES.get(language, {}).items()
        if any(re.search(pattern, normalized, flags=re.IGNORECASE) for pattern in patterns)
    }


def _github_issue_candidate(issue: dict, repo_full: str) -> dict | None:
    if not isinstance(issue, dict) or "pull_request" in issue:
        return None
    title, title_redacted = sanitize_text(str(issue.get("title") or ""))
    body, body_redacted = sanitize_text(str(issue.get("body") or ""))
    url = canonicalize_url(str(issue.get("html_url") or ""))
    updated_at = str(issue.get("updated_at") or "").strip()
    if not title or not url or not updated_at:
        return None
    body_excerpt = _safe_excerpt(body, MAX_CACHED_TEXT_LENGTH)
    context = _truncate_text(f"{title}. {body_excerpt}" if body_excerpt else title, MAX_CACHED_EXCERPT_LENGTH)
    return {
        "canonical_url": url,
        "problem_statement": _truncate_text(title, MAX_CACHED_PROBLEM_LENGTH),
        "excerpt": context,
        "source_identity": repo_full,
        "updated_at": updated_at,
        "privacy_redacted": bool(title_redacted or body_redacted),
    }


def _local_github_query_match(candidate: dict, query_rows: list[dict]) -> dict | None:
    text = str(candidate.get("excerpt") or candidate.get("problem_statement") or "")
    detected_language = detect_language(text)
    applicable_languages = {detected_language} if detected_language in SUPPORTED_LANGUAGES else {
        str(row.get("language")) for row in query_rows if row.get("language") in SUPPORTED_LANGUAGES
    }
    inferred_concept, _ = infer_concept(text)
    best_by_concept: dict[str, tuple[float, dict]] = {}
    for row in query_rows:
        language = str(row.get("language") or "")
        if language not in applicable_languages:
            continue
        concept_id = str(row.get("concept_id") or "")
        query = str(row.get("query") or "")
        context_pattern = CONCEPT_CONTEXT_PATTERNS.get(concept_id)
        if context_pattern and not re.search(context_pattern, normalize_text(text), flags=re.IGNORECASE):
            continue
        issue_facets = problem_intent_facets(text, language, concept_id)
        compatible_facets = issue_facets & CONCEPT_INTENT_FACETS.get(concept_id, set())
        if not compatible_facets:
            continue
        issue_tokens = set(_canonical_problem_tokens(text, language))
        query_tokens = set(_canonical_problem_tokens(query, language))
        similarity = jaccard_similarity(issue_tokens, query_tokens)
        score = 0.60 + (0.30 * similarity)
        if inferred_concept == concept_id:
            score += 0.10
        topic_id = str(row.get("topic_id") or "")
        if topic_id and topic_id in normalize_text(text):
            score += 0.05
        current = best_by_concept.get(concept_id)
        ranked = (round(score, 6), {**row, "language": language})
        if current is None or ranked[0] > current[0] or (ranked[0] == current[0] and str(row.get("query_id")) < str(current[1].get("query_id"))):
            best_by_concept[concept_id] = ranked
    ranked_concepts = sorted(best_by_concept.values(), key=lambda value: (-value[0], str(value[1].get("query_id"))))
    if not ranked_concepts or ranked_concepts[0][0] < LOCAL_QUERY_MATCH_THRESHOLD:
        return None
    if len(ranked_concepts) > 1 and ranked_concepts[0][0] - ranked_concepts[1][0] <= LOCAL_QUERY_AMBIGUITY_DELTA:
        return None
    return {**ranked_concepts[0][1], "match_score": ranked_concepts[0][0]}


def _normalize_item_fields(item: dict, source_name: str, source_family: str, source_type: str) -> dict | None:
    url = canonicalize_url(str(item.get("url") or item.get("canonical_url") or "").strip())
    raw_text = str(item.get("text") or "").strip()
    text, redacted = sanitize_text(raw_text)
    if not url or not text:
        return None
    problem_raw, problem_redacted = sanitize_text(str(item.get("problem_statement") or text))
    problem = _problem_from_text(problem_raw)
    language = detect_language(f"{problem} {text}", str(item.get("language") or ""))
    evidence_role = str(item.get("evidence_role") or _default_evidence_role(source_family))
    if evidence_role not in EVIDENCE_ROLES:
        evidence_role = _default_evidence_role(source_family)
    quality = assess_problem_signal(text, problem, evidence_role)
    inferred_concept, inferred_topic = infer_concept(f"{problem} {text}")
    canonical_key = canonical_problem_key(problem, language)
    return {
        "canonical_url": url,
        "source_name": source_name,
        "source_family": source_family,
        "source_identity": str(item.get("source_identity") or source_name),
        "source_type": source_type,
        "retrieved_at": utc_now(),
        "content_hash": content_hash(text),
        "excerpt": _truncate_text(_safe_excerpt(text), MAX_CACHED_TEXT_LENGTH),
        "problem_statement": _truncate_text(problem, MAX_CACHED_PROBLEM_LENGTH),
        "canonical_problem_key": canonical_key,
        "canonical_problem_fingerprint": canonical_problem_fingerprint(problem, language),
        "persona": item.get("persona", "caregiver"),
        "mode": item.get("mode", item.get("persona", "caregiver")),
        "module": item.get("module", "Home / Monitoring"),
        "type": item.get("type", "usability"),
        "severity": item.get("severity", "medium"),
        "frequency": item.get("frequency", "occasional"),
        "confidence": item.get("confidence", "medium"),
        "language": language,
        "concept_id": str(item.get("concept_id") or inferred_concept),
        "topic_id": str(item.get("topic_id") or inferred_topic),
        "query_id": str(item.get("query_id") or "fixture"),
        "evidence_role": evidence_role,
        "privacy_redacted": bool(item.get("privacy_redacted") or redacted or problem_redacted),
        "updated_at": str(item.get("updated_at") or item.get("published_at") or ""),
        **quality,
    }


def _collect_fixture_items(source: dict, max_items: int) -> list[dict]:
    source_name = str(source.get("name", "unknown"))
    source_family = str(source.get("family", "other_community"))
    source_type = SOURCE_TYPE_BY_FAMILY.get(source_family, "community")
    out: list[dict] = []
    evidence_role = str(source.get("evidence_role") or _default_evidence_role(source_family))
    for raw in (source.get("fixture_items") or [])[:max_items]:
        item = _normalize_item_fields({**raw, "evidence_role": raw.get("evidence_role", evidence_role)}, source_name, source_family, source_type)
        if item:
            out.append(item)
    return out


def _collect_official_pages(source: dict, max_items: int, timeout: float, retries: int, cache: DiscoveryCache, cache_max_age_seconds: int) -> list[dict]:
    out: list[dict] = []
    source_name = str(source.get("name", "unknown"))
    source_family = str(source.get("family", "official_vendor"))
    source_type = SOURCE_TYPE_BY_FAMILY.get(source_family, "community")
    for idx, raw_url in enumerate(source.get("urls") or []):
        if idx >= max_items:
            break
        canon = canonicalize_url(str(raw_url))
        key = _cache_key(source_name, f"html:{canon}")
        cached_items = _cache_get_items(cache, key, cache_max_age_seconds)
        if cached_items is not None:
            out.extend(cached_items[: max_items - len(out)])
            continue

        html = _fetch_url(canon, timeout=timeout, retries=retries)
        title = _extract_html_title(html)
        plain = _extract_html_text(html)
        if not plain:
            continue
        headline = title or _problem_from_text(plain)
        text = f"{headline}. {_safe_excerpt(plain, 500)}"
        item = _normalize_cached_item(
            {
                "url": canon,
                "text": text,
                "problem_statement": headline,
                "evidence_role": "official_reference",
                "language": source.get("language", ""),
                "concept_id": "official_context",
                "topic_id": "official_reference",
                "query_id": f"official:{idx + 1}",
                "persona": "caregiver",
                "mode": "caregiver",
                "module": "Home / Monitoring",
                "type": "usability",
                "severity": "low",
                "frequency": "unknown",
                "confidence": "low",
            },
            source_name,
            source_family,
            source_type,
        )
        if item:
            _cache_put_items(cache, key, [item])
            out.append(item)
    return out


def _collect_github_issue_search(
    source: dict, max_items: int, timeout: float, retries: int, cache: DiscoveryCache,
    cache_max_age_seconds: int, query_packs: dict, languages: list[str], lookback_days: int,
    primary_languages: list[str] | None = None,
) -> list[dict]:
    out: list[dict] = []
    source_name = str(source.get("name", "unknown"))
    source_family = str(source.get("family", "github_community"))
    source_type = SOURCE_TYPE_BY_FAMILY.get(source_family, "community")
    repos = [str(repo).strip().strip("/") for repo in source.get("repos", []) if "/" in str(repo)]
    cutoff = datetime.now(timezone.utc) - timedelta(days=max(1, min(lookback_days, 1825)))
    since = cutoff.replace(microsecond=0).isoformat().replace("+00:00", "Z")
    max_pages = max(1, min(int(source.get("max_pages", MAX_GITHUB_REPO_PAGES)), MAX_GITHUB_REPO_PAGES))
    query_rows = bounded_query_rows(query_packs, languages, max_queries=48, primary_languages=primary_languages)
    headers = {"Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28"}
    concept_counts: dict[str, int] = {}
    seen_urls: set[str] = set()
    for repo_full in repos:
        for page in range(1, max_pages + 1):
            if len(out) >= max_items:
                break
            encoded = urllib_parse.urlencode({
                "state": "all", "since": since, "sort": "updated", "direction": "desc",
                "per_page": GITHUB_REPO_PAGE_SIZE, "page": page,
            })
            api_url = f"https://api.github.com/repos/{repo_full}/issues?{encoded}"
            key = _cache_key(source_name, f"gh-repo-issues:{_lookback_window_key(lookback_days)}:{repo_full}:{page}")
            cached_page = cache.get(key, cache_max_age_seconds)
            if isinstance(cached_page, dict) and isinstance(cached_page.get("items"), list):
                candidates = [row for row in cached_page["items"] if isinstance(row, dict)]
                page_size = int(cached_page.get("page_size") or 0)
            else:
                data = _fetch_json(api_url, timeout=timeout, retries=retries, headers=headers)
                if not isinstance(data, list):
                    raise RuntimeError("GitHub repository issues response is not a JSON array")
                page_size = len(data)
                candidates = []
                for issue in data[:GITHUB_REPO_PAGE_SIZE]:
                    candidate = _github_issue_candidate(issue, repo_full)
                    if candidate:
                        candidates.append(candidate)
                cache.put(key, {"page_size": page_size, "items": candidates})
            for candidate in _items_within_lookback(candidates, cutoff):
                if len(out) >= max_items:
                    break
                url = str(candidate.get("canonical_url") or "")
                if not url or url in seen_urls:
                    continue
                query_meta = _local_github_query_match(candidate, query_rows)
                if not query_meta or concept_counts.get(query_meta["concept_id"], 0) >= 10:
                    continue
                item = _normalize_cached_item(
                    {
                        "url": url, "text": candidate.get("excerpt", ""), "problem_statement": candidate.get("problem_statement", ""),
                        "source_identity": candidate.get("source_identity", repo_full), "persona": "unknown", "mode": "unknown", "module": "unknown",
                        "type": "usability", "severity": "medium", "frequency": "unknown", "confidence": "medium",
                        "evidence_role": source.get("evidence_role", "developer_community"), "language": query_meta["language"],
                        "concept_id": query_meta["concept_id"], "topic_id": query_meta["topic_id"], "query_id": query_meta["query_id"],
                        "updated_at": candidate.get("updated_at", ""), "privacy_redacted": candidate.get("privacy_redacted", False),
                    }, source_name, source_family, source_type,
                )
                if item:
                    out.append(item)
                    seen_urls.add(url)
                    concept_counts[query_meta["concept_id"]] = concept_counts.get(query_meta["concept_id"], 0) + 1
            if page_size < GITHUB_REPO_PAGE_SIZE:
                break
    return out


def _collect_reddit_oauth(
    source: dict, max_items: int, timeout: float, retries: int, cache: DiscoveryCache,
    cache_max_age_seconds: int, query_packs: dict | None = None, languages: list[str] | None = None,
    lookback_days: int = 365, primary_languages: list[str] | None = None,
) -> SourceResult:
    name = str(source.get("name", "reddit"))
    family = str(source.get("family", "reddit"))
    if not os.environ.get("REDDIT_CLIENT_ID") or not os.environ.get("REDDIT_CLIENT_SECRET"):
        return SourceResult(name=name, family=family, status="REDDIT_DISABLED", items=[], evidence_role=str(source.get("evidence_role", "user_community")))

    cid = os.environ.get("REDDIT_CLIENT_ID", "")
    secret = os.environ.get("REDDIT_CLIENT_SECRET", "")
    basic = base64.b64encode(f"{cid}:{secret}".encode("utf-8")).decode("ascii")
    token_headers = {
        "Authorization": f"Basic {basic}",
        "Content-Type": "application/x-www-form-urlencoded",
    }
    token_payload = _post_form_json(
        "https://www.reddit.com/api/v1/access_token",
        {"grant_type": "client_credentials"},
        timeout=timeout,
        retries=retries,
        headers=token_headers,
    )

    access_token = ""
    if isinstance(token_payload, dict):
        access_token = str(token_payload.get("access_token") or "")
    if not access_token:
        return SourceResult(name=name, family=family, status="REDDIT_DISABLED", items=[], detail="oauth_token_missing")

    subreddits = source.get("subreddits") or ["diabetes", "Type1Diabetes"]
    query_packs = query_packs or {"concepts": []}
    languages = languages or ["pl", "en"]
    max_queries = max(1, min(int(source.get("max_queries", 24)), 48))
    out: list[dict] = []
    concept_counts: dict[str, int] = {}
    cutoff = datetime.now(timezone.utc) - timedelta(days=max(1, min(lookback_days, 1825)))
    for sub in subreddits:
        for query_meta in bounded_query_rows(query_packs, languages, max_queries=max_queries, primary_languages=primary_languages):
            if len(out) >= max_items:
                break
            query = query_meta["query"]
            if sub.lower() in {"polska", "poland"} and not re.search(r"\b(libre|dexcom|cgm|glukoz|glucose)\b", query, re.I):
                continue
            limit = min(10, max_items - len(out))
            params = urllib_parse.urlencode({"q": query, "restrict_sr": "on", "sort": "new", "t": "all", "limit": limit, "raw_json": 1})
            endpoint = f"https://oauth.reddit.com/r/{sub}/search?{params}"
            key = _cache_key(name, f"reddit-search:{_lookback_window_key(lookback_days)}:{sub}:{query_meta['query_id']}")
            cached_items = _cache_get_items(cache, key, cache_max_age_seconds)
            if cached_items is not None:
                cached_items = _items_within_lookback(cached_items, cutoff)
                remaining_concept = max(0, 10 - concept_counts.get(query_meta["concept_id"], 0))
                selected = cached_items[: min(max_items - len(out), remaining_concept)]
                out.extend(selected)
                concept_counts[query_meta["concept_id"]] = concept_counts.get(query_meta["concept_id"], 0) + len(selected)
                continue
            data = _fetch_json(endpoint, timeout=timeout, retries=retries, headers={"Authorization": f"Bearer {access_token}"})
            posts = ((data.get("data") or {}).get("children") or []) if isinstance(data, dict) else []
            normalized_items = []
            for post in posts[:limit]:
                if concept_counts.get(query_meta["concept_id"], 0) >= 10:
                    continue
                pdata = post.get("data") or {} if isinstance(post, dict) else {}
                created_utc = pdata.get("created_utc")
                if not _timestamp_at_or_after(created_utc, cutoff):
                    continue
                title, selftext, permalink = str(pdata.get("title") or "").strip(), str(pdata.get("selftext") or "").strip(), str(pdata.get("permalink") or "").strip()
                if not title or not permalink:
                    continue
                item = _normalize_cached_item(
                    {
                        "url": f"https://www.reddit.com{permalink}", "text": f"{title}. {_safe_excerpt(selftext, 180)}", "problem_statement": title,
                        "source_identity": f"reddit_{sub}",
                        "persona": "caregiver", "module": "Home / Monitoring", "type": "usability", "severity": "medium", "frequency": "occasional",
                        "confidence": "low", "evidence_role": "user_community", "language": query_meta["language"], "concept_id": query_meta["concept_id"],
                        "topic_id": query_meta["topic_id"], "query_id": query_meta["query_id"], "updated_at": created_utc or "",
                    }, name, family, SOURCE_TYPE_BY_FAMILY.get(family, "community"),
                )
                if item:
                    normalized_items.append(item)
                    out.append(item)
                    concept_counts[query_meta["concept_id"]] = concept_counts.get(query_meta["concept_id"], 0) + 1
            if normalized_items:
                _cache_put_items(cache, key, normalized_items)

    return SourceResult(name=name, family=family, status="OK" if out else "EMPTY", items=out, evidence_role="user_community")


def _collect_rss_atom(source: dict, max_items: int, timeout: float, retries: int) -> list[dict]:
    if not source.get("allowlisted"):
        raise RuntimeError("RSS/Atom source is not explicitly allowlisted")
    out = []
    for feed_url in source.get("urls", []):
        raw = _fetch_url(str(feed_url), timeout, retries)
        root = ET.fromstring(raw)
        rows = root.findall(".//item") or root.findall(".//{http://www.w3.org/2005/Atom}entry")
        for row in rows[: max_items - len(out)]:
            def value(*names):
                for tag in names:
                    node = row.find(tag)
                    if node is not None:
                        return node.get("href", "") or (node.text or "")
                return ""
            title = value("title", "{http://www.w3.org/2005/Atom}title")
            summary = value("description", "{http://www.w3.org/2005/Atom}summary", "{http://www.w3.org/2005/Atom}content")
            link = value("link", "{http://www.w3.org/2005/Atom}link")
            published = value("pubDate", "{http://www.w3.org/2005/Atom}published", "{http://www.w3.org/2005/Atom}updated")
            item = _normalize_cached_item({"url": link, "text": f"{title}. {_extract_html_text(summary)}", "problem_statement": title,
                "published_at": published, "evidence_role": source.get("evidence_role", "user_community"), "language": source.get("language", "")},
                str(source.get("name")), str(source.get("family")), SOURCE_TYPE_BY_FAMILY.get(str(source.get("family")), "community"))
            if item:
                out.append(item)
    return out[:max_items]


def collect_from_source(
    source: dict,
    max_items: int,
    timeout: float,
    retries: int,
    cache: DiscoveryCache | None = None,
    cache_max_age_seconds: int = 21600,
    query_packs: dict | None = None,
    languages: list[str] | None = None,
    lookback_days: int = 365,
    github_token: str = "",
    primary_languages: list[str] | None = None,
) -> SourceResult:
    name = str(source.get("name", "unknown"))
    family = str(source.get("family", "other_community"))
    evidence_role = str(source.get("evidence_role") or _default_evidence_role(family))
    if evidence_role not in EVIDENCE_ROLES:
        return SourceResult(name=name, family=family, status="INVALID_EVIDENCE_ROLE", items=[], evidence_role=evidence_role)
    if family not in SOURCE_FAMILIES:
        return SourceResult(name=name, family=family, status="INVALID_SOURCE_FAMILY", items=[], evidence_role=evidence_role)
    if source.get("enabled") is False:
        return SourceResult(name=name, family=family, status="DISABLED", items=[], evidence_role=evidence_role)

    fixture_items = _collect_fixture_items(source, max_items=max_items)
    if fixture_items:
        return SourceResult(name=name, family=family, status="OK", items=fixture_items, evidence_role=evidence_role)

    local_cache = cache or DiscoveryCache(DISCOVERY_CACHE_PATH)

    kind = str(source.get("kind") or "").strip().lower()
    if not kind:
        if family == "reddit":
            kind = "reddit_oauth"
        elif "github" in name:
            kind = "github_issues"
        else:
            kind = "official_pages"

    try:
        if kind == "reddit_oauth":
            return _collect_reddit_oauth(source, max_items, timeout, retries, local_cache, cache_max_age_seconds, query_packs, languages, lookback_days, primary_languages)
        if kind in {"github_issues", "github_issue_search"}:
            items = _collect_github_issue_search(source, max_items, timeout, retries, local_cache, cache_max_age_seconds, query_packs or {"concepts": []}, languages or ["pl", "en"], lookback_days, primary_languages)
            return SourceResult(name=name, family=family, status="OK" if items else "EMPTY", items=items, evidence_role=evidence_role)
        if kind == "official_pages":
            items = _collect_official_pages(source, max_items, timeout, retries, local_cache, cache_max_age_seconds)
            return SourceResult(name=name, family=family, status="OK" if items else "EMPTY", items=items, evidence_role=evidence_role)
        if kind == "rss_atom":
            items = _collect_rss_atom(source, max_items, timeout, retries)
            return SourceResult(name=name, family=family, status="OK" if items else "EMPTY", items=items, evidence_role=evidence_role)
        return SourceResult(name=name, family=family, status="INVALID_SOURCE_KIND", items=[], evidence_role=evidence_role)
    except Exception as exc:  # noqa: BLE001
        return SourceResult(name=name, family=family, status=_source_status_for_failure(name), items=[], detail=_sanitized_failure_detail(exc), evidence_role=evidence_role)


def dedupe_items(items: list[dict]) -> tuple[list[dict], dict]:
    exact_seen = set()
    normalized_seen_in_source = set()
    out = []
    stats = {"exact_duplicates": 0, "normalized_duplicates": 0}
    for item in items:
        source_identity = str(item.get("source_identity") or item.get("source_name") or "unknown")
        exact_key = (source_identity, item.get("canonical_url"), item.get("content_hash"))
        if exact_key in exact_seen:
            stats["exact_duplicates"] += 1
            continue
        exact_seen.add(exact_key)

        problem_text = str(item.get("problem_statement") or "")
        language = str(item.get("language") or "unknown")
        fp = text_fingerprint(problem_text, language)
        item["problem_fingerprint"] = fp
        item["canonical_problem_key"] = canonical_problem_key(problem_text, language)
        item["canonical_problem_fingerprint"] = canonical_problem_fingerprint(problem_text, language)
        dup_key = (source_identity, fp)
        if dup_key in normalized_seen_in_source:
            stats["normalized_duplicates"] += 1
            continue
        normalized_seen_in_source.add(dup_key)
        out.append(item)
    return out, stats


def _stable_cluster_id_from_items(items: list[dict]) -> str:
    material = str(_canonical_cluster_grounding(items).get("canonical_problem_key") or "")
    if not material:
        signatures = []
        for item in items:
            concept_id = str(item.get("concept_id") or "unclassified")
            language = str(item.get("language") or "unknown")
            text = f"{item.get('problem_statement') or ''}. {item.get('excerpt') or ''}"
            facets = problem_intent_facets(text, language, concept_id) & CONCEPT_INTENT_FACETS.get(concept_id, set())
            signatures.append(f"{concept_id}:{','.join(sorted(facets)) or 'none'}")
        material = "unsafe|" + "|".join(sorted(set(signatures)))
    digest = hashlib.sha1(material.encode("utf-8")).hexdigest()[:12].upper()
    return f"DISC-{digest}"


def _canonical_cluster_grounding(items: list[dict]) -> dict:
    """Derive bounded cluster meaning only from the existing concept/facet taxonomy."""
    concepts = sorted(set(str(item.get("concept_id") or "unclassified") for item in items))
    topics = sorted(set(str(item.get("topic_id") or "unclassified") for item in items))
    if len(concepts) != 1 or concepts[0] not in CANONICAL_CONCEPT_STATEMENTS:
        return {
            "canonical_grounding_safe": False,
            "canonical_grounding_reason": "No single supported Discovery concept",
            "canonical_problem_statement": "",
            "canonical_problem_statement_pl": "",
            "canonical_problem_key": "",
            "canonical_problem_fingerprint": "",
            "shared_intent_facets": [],
        }

    concept_id = concepts[0]
    facet_sets = []
    for item in items:
        language = str(item.get("language") or "unknown")
        evidence_text = f"{item.get('problem_statement') or ''}. {item.get('excerpt') or ''}"
        facet_sets.append(problem_intent_facets(evidence_text, language, concept_id) & CONCEPT_INTENT_FACETS[concept_id])
    shared_facets = sorted(set.intersection(*facet_sets)) if facet_sets else []
    if len(items) > 1 and facet_sets and all(facet_sets) and not shared_facets:
        return {
            "canonical_grounding_safe": False,
            "canonical_grounding_reason": "Evidence has distinct intent facets without a shared facet",
            "canonical_problem_statement": "",
            "canonical_problem_statement_pl": "",
            "canonical_problem_key": "",
            "canonical_problem_fingerprint": "",
            "shared_intent_facets": [],
        }

    statements = CANONICAL_CONCEPT_STATEMENTS[concept_id]
    if len(shared_facets) == 1:
        statements = CANONICAL_FACET_STATEMENTS.get((concept_id, shared_facets[0]), statements)
    topic_id = topics[0] if len(topics) == 1 else "mixed"
    facet_material = ",".join(shared_facets) if shared_facets else "concept"
    canonical_key = f"concept:{concept_id}|topic:{topic_id}|facets:{facet_material}"
    return {
        "canonical_grounding_safe": True,
        "canonical_grounding_reason": "Shared Discovery concept and intent facets",
        "canonical_problem_statement": statements[0],
        "canonical_problem_statement_pl": statements[1],
        "canonical_problem_key": canonical_key,
        "canonical_problem_fingerprint": hashlib.sha256(canonical_key.encode("utf-8")).hexdigest()[:16],
        "shared_intent_facets": shared_facets,
    }


def _structured_evidence_items(cluster: dict) -> list[dict]:
    """Build bounded, sanitized source facts without account/user metadata."""
    return [
        {
            "source_identity": str(item.get("source_identity") or item.get("source_name") or "unknown"),
            "source_family": str(item.get("source_family") or "unknown"),
            "problem_statement": _truncate_text(str(item.get("problem_statement") or ""), MAX_CACHED_PROBLEM_LENGTH),
            "excerpt": _truncate_text(str(item.get("excerpt") or ""), MAX_CACHED_EXCERPT_LENGTH),
        }
        for item in sorted(cluster.get("raw_items") or [], key=lambda row: (str(row.get("source_identity") or row.get("source_name") or "unknown"), str(row.get("canonical_url") or "")))[:6]
    ]


def _existing_cluster_matches() -> list[dict]:
    matches: list[dict] = []
    for path in iter_record_files(OBSERVATIONS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        text = str(rec.get("problem_statement") or rec.get("text") or "").strip()
        cid = str(rec.get("cluster_id") or "").strip()
        if not text or not cid:
            continue
        matches.append(
            {
                "cluster_id": cid,
                "problem_statement": text,
                "problem_fingerprint": str(rec.get("problem_fingerprint") or "").strip(),
                "persona": str(rec.get("persona") or "").strip(),
                "module": str(rec.get("module") or "").strip(),
                "source_type": str(rec.get("source_type") or "").strip(),
            }
        )
    return matches


def _reuse_existing_cluster_id(cluster: dict, existing_clusters: list[dict]) -> str | None:
    if not existing_clusters:
        return None
    problem = str(cluster.get("normalized_problem") or "").strip()
    if not problem:
        return None
    persona = str(cluster.get("persona_candidate") or "").strip()
    module = str(cluster.get("module_candidate") or "").strip()
    best_score = 0.0
    best_cluster_id = None
    for existing in existing_clusters:
        score = _text_similarity(problem, existing["problem_statement"])
        if persona and existing.get("persona") and persona != existing.get("persona"):
            score *= 0.9
        if module and existing.get("module") and module != existing.get("module"):
            score *= 0.95
        if score > best_score:
            best_score = score
            best_cluster_id = existing["cluster_id"]
    return best_cluster_id if best_score >= 0.70 else None


def cluster_items(items: list[dict], threshold: float = 0.40, existing_clusters: list[dict] | None = None) -> list[dict]:
    clusters: list[dict] = []
    eligible_items = [item for item in items if item.get("eligible_for_clustering", True)]
    for item in sorted(eligible_items, key=lambda i: (i.get("concept_id", ""), i.get("problem_fingerprint", ""), i.get("canonical_url", ""))):
        language = str(item.get("language") or "unknown")
        tokens = set(_canonical_problem_tokens(str(item.get("problem_statement") or ""), language))
        placed = False
        for cluster in clusters:
            matches_existing = False
            for existing in cluster["items"]:
                existing_language = str(existing.get("language") or "unknown")
                existing_tokens = set(_canonical_problem_tokens(str(existing.get("problem_statement") or ""), existing_language))
                similarity = jaccard_similarity(tokens, existing_tokens)
                same_concept = item.get("concept_id") not in {None, "", "unclassified"} and item.get("concept_id") == existing.get("concept_id")
                if language == existing_language:
                    matches_existing = similarity >= threshold or (same_concept and similarity >= 0.10)
                else:
                    shared_facets = problem_intent_facets(str(item.get("problem_statement") or ""), language, str(item.get("concept_id") or "")) & problem_intent_facets(
                        str(existing.get("problem_statement") or ""), existing_language, str(existing.get("concept_id") or "")
                    )
                    matches_existing = same_concept and (similarity >= max(threshold, CROSS_LANGUAGE_SIMILARITY_THRESHOLD) or bool(shared_facets))
                if matches_existing:
                    break
            if matches_existing:
                cluster["items"].append(item)
                cluster["token_union"] = cluster["token_union"] | tokens
                placed = True
                break
        if not placed:
            clusters.append({"items": [item], "token_union": tokens, "concept_id": item.get("concept_id"), "language": language})

    out = []
    for raw in clusters:
        c_items = raw["items"]
        grounding = _canonical_cluster_grounding(c_items)
        problem_fps = sorted(
            set(
                str(item.get("canonical_problem_fingerprint") or item.get("problem_fingerprint") or "")
                for item in c_items
                if str(item.get("problem_statement") or "").strip()
            )
        )
        cluster_id = _stable_cluster_id_from_items(c_items)
        if existing_clusters:
            reused_cluster_id = _reuse_existing_cluster_id(
                {
                    "normalized_problem": grounding.get("canonical_problem_statement", ""),
                    "persona_candidate": "unknown",
                    "module_candidate": "unknown",
                },
                existing_clusters,
            )
            if reused_cluster_id:
                cluster_id = reused_cluster_id

        source_identities = sorted(set(str(x.get("source_identity") or x.get("source_name") or "unknown") for x in c_items))
        source_families = sorted(set(str(x.get("source_family") or "unknown") for x in c_items))
        languages = sorted(set(str(x.get("language") or "unknown") for x in c_items))
        evidence_roles = sorted(set(str(x.get("evidence_role") or "user_community") for x in c_items))
        evidence = [
            _truncate_text(x.get("excerpt") or _safe_excerpt(str(x.get("problem_statement") or "")), MAX_CACHED_EXCERPT_LENGTH)
            for x in c_items
        ]
        urls = sorted(set(str(x.get("canonical_url") or "") for x in c_items if x.get("canonical_url")))

        persona_counts: dict[str, int] = {}
        mode_counts: dict[str, int] = {}
        module_counts: dict[str, int] = {}
        for it in c_items:
            persona = str(it.get("persona", "unknown"))
            mode = str(it.get("mode", persona))
            module = str(it.get("module", "unknown"))
            persona_counts[persona] = persona_counts.get(persona, 0) + 1
            mode_counts[mode] = mode_counts.get(mode, 0) + 1
            module_counts[module] = module_counts.get(module, 0) + 1

        persona = sorted(persona_counts.items(), key=lambda kv: (-kv[1], kv[0]))[0][0]
        mode = sorted(mode_counts.items(), key=lambda kv: (-kv[1], kv[0]))[0][0]
        module = sorted(module_counts.items(), key=lambda kv: (-kv[1], kv[0]))[0][0]
        normalized_problem = str(grounding.get("canonical_problem_statement") or "")
        evidence_count = len(c_items)
        identity_count = len(source_identities)
        family_count = len(source_families)
        language_count = len(languages)
        if identity_count >= 3 and family_count >= 2 and (language_count >= 2 or evidence_count >= 5):
            evidence_tier = "STRONG"
        elif (identity_count >= 2 and family_count >= 2) or identity_count >= 3:
            evidence_tier = "CORROBORATED"
        elif identity_count >= 2 or evidence_count >= 3:
            evidence_tier = "SUPPORTED"
        else:
            evidence_tier = "WEAK"
        genuine_problem = any(x.get("user_facing", False) or x.get("problem_signal_quality", 2) >= 2 for x in c_items)
        quality_gate = genuine_problem and not all(x.get("technical_only", False) for x in c_items) and not all(x.get("marketing_only", False) for x in c_items) and not all(x.get("evidence_role") == "official_reference" for x in c_items)
        concepts = sorted(set(str(x.get("concept_id") or "unclassified") for x in c_items))
        topics = sorted(set(str(x.get("topic_id") or "unclassified") for x in c_items))

        out.append(
            {
                "cluster_id": cluster_id,
                "normalized_problem": normalized_problem,
                **grounding,
                "persona_candidate": persona,
                "mode_candidate": mode,
                "module_candidate": module,
                "evidence_items": evidence,
                "source_urls": urls,
                "source_families": source_families,
                "source_identities": source_identities,
                "source_count": len(c_items),
                "evidence_item_count": evidence_count,
                "independent_source_identity_count": identity_count,
                "independent_source_family_count": family_count,
                "languages": languages,
                "language_count": language_count,
                "evidence_roles": evidence_roles,
                "concept_id": concepts[0] if len(concepts) == 1 else "mixed",
                "topic_id": topics[0] if len(topics) == 1 else "mixed",
                "evidence_tier": evidence_tier,
                "quality_gate_passes": quality_gate,
                "genuine_problem_signal": genuine_problem,
                "fingerprints": problem_fps,
                "raw_items": c_items,
            }
        )

    out.sort(key=lambda c: c["cluster_id"])
    return out


def _text_similarity(a: str, b: str) -> float:
    return jaccard_similarity(set(tokenize(a)), set(tokenize(b)))


def load_foundation_index() -> dict:
    requirements = []
    decisions = []
    observations = []
    capabilities = []

    for path in iter_record_files(REQUIREMENTS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        requirements.append(
            {
                "id": rec.get("id", path.stem),
                "text": str(rec.get("problem", "")),
                "status": str(rec.get("status", "")),
                "path": str(path.relative_to(ROOT)),
            }
        )

    for path in iter_record_files(DECISIONS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        decisions.append(
            {
                "id": rec.get("id", path.stem),
                "subject": str(rec.get("subject", "")),
                "decision": str(rec.get("decision", "")),
                "status": str(rec.get("status", "")),
                "path": str(path.relative_to(ROOT)),
            }
        )

    for path in iter_record_files(OBSERVATIONS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        observations.append(
            {
                "id": rec.get("id", path.stem),
                "text": str(rec.get("problem_statement", "")),
                "path": str(path.relative_to(ROOT)),
                "cluster_id": rec.get("cluster_id"),
                "problem_fingerprint": rec.get("problem_fingerprint"),
            }
        )

    if VALIDATED_CAPABILITIES_MD.exists():
        text = VALIDATED_CAPABILITIES_MD.read_text(encoding="utf-8")
        for line in text.splitlines():
            if line.strip().startswith("**Capability:**"):
                capabilities.append(line.split("**Capability:**", 1)[1].strip())

    return {
        "requirements": requirements,
        "decisions": decisions,
        "observations": observations,
        "capabilities": capabilities,
    }


def match_cluster_to_foundation(cluster: dict, foundation: dict) -> dict:
    problem_candidates = {
        str(cluster.get("canonical_problem_statement") or cluster.get("normalized_problem") or "").strip(),
        *(str(item.get("problem_statement") or "").strip() for item in (cluster.get("raw_items") or [])),
    }
    problem_candidates.discard("")

    def best_similarity(text: str) -> float:
        return max((_text_similarity(problem, text) for problem in sorted(problem_candidates)), default=0.0)

    best_req = (0.0, None)
    for req in foundation["requirements"]:
        score = best_similarity(req["text"])
        if score > best_req[0]:
            best_req = (score, req)

    best_cap = (0.0, None)
    for cap in foundation["capabilities"]:
        score = best_similarity(cap)
        if score > best_cap[0]:
            best_cap = (score, cap)

    best_dec = (0.0, None)
    for dec in foundation["decisions"]:
        score = best_similarity(f"{dec['subject']} {dec['decision']}")
        if score > best_dec[0]:
            best_dec = (score, dec)

    linked = []
    suppress_reproposal = False
    suppress_reason = ""

    if best_req[1] and best_req[0] >= 0.68:
        linked.append({"type": "requirement", "id": best_req[1]["id"], "score": round(best_req[0], 3), "path": best_req[1]["path"]})

    if best_cap[1] and best_cap[0] >= 0.68:
        linked.append({"type": "validated_capability", "id": best_cap[1], "score": round(best_cap[0], 3)})

    if best_dec[1] and best_dec[0] >= 0.68:
        linked.append(
            {
                "type": "decision",
                "id": best_dec[1]["id"],
                "status": best_dec[1]["status"],
                "score": round(best_dec[0], 3),
                "path": best_dec[1]["path"],
            }
        )
        if str(best_dec[1]["status"]).upper() in {"HOLD", "REJECT", "REJECTED"}:
            suppress_reproposal = True
            suppress_reason = f"Existing decision {best_dec[1]['id']} has status {best_dec[1]['status']}"

    return {
        "linked": linked,
        "suppress_reproposal": suppress_reproposal,
        "suppress_reason": suppress_reason,
        "best_requirement_score": best_req[0],
        "best_capability_score": best_cap[0],
    }


def _foundation_match_summary(match: dict) -> str:
    linked = match.get("linked") or []
    if not linked:
        return "Brak silnego dopasowania do Product Foundation"
    parts = []
    for row in linked:
        rtype = row.get("type")
        rid = row.get("id")
        score = row.get("score")
        if rtype == "decision" and row.get("status"):
            parts.append(f"decision:{rid}({row.get('status')}, score={score})")
        else:
            parts.append(f"{rtype}:{rid}(score={score})")
    return "; ".join(parts)


def build_ai_prompt(run_id: str, clusters: list[dict], model: str) -> str:
    compact = []
    for c in clusters:
        compact.append(
            {
                "cluster_id": c["cluster_id"],
                "normalized_problem": c["normalized_problem"],
                "canonical_problem_statement": c.get("canonical_problem_statement", ""),
                "canonical_problem_statement_pl": c.get("canonical_problem_statement_pl", ""),
                "canonical_problem_key": c.get("canonical_problem_key", ""),
                "persona_candidate": c["persona_candidate"],
                "module_candidate": c["module_candidate"],
                "independent_source_family_count": c["independent_source_family_count"],
                "independent_source_identity_count": c.get("independent_source_identity_count", 0),
                "evidence_item_count": c.get("evidence_item_count", 0),
                "languages": c.get("languages", []),
                "evidence_tier": c.get("evidence_tier", "WEAK"),
                "concept_id": c.get("concept_id", "unclassified"),
                "topic_id": c.get("topic_id", "unclassified"),
                "shared_intent_facets": c.get("shared_intent_facets", []),
                "source_families": c["source_families"],
                "source_identities": c.get("source_identities", []),
                "structured_evidence_items": _structured_evidence_items(c),
                "source_urls": c["source_urls"][:12],
                "foundation_match": c.get("foundation_match", {}),
            }
        )

    payload = {
        "task": "Classify LibreCare discovery clusters. Advisory only.",
        "run_id": run_id,
        "model": model,
        "required_output": {
            "type": "object",
            "required": ["clusters"],
            "clusters_item_required": [
                "cluster_id",
                "classification",
                "persona",
                "problem_statement",
                "problem_statement_pl",
                "evidence_summary",
                "source_diversity_summary",
                "current_librecare_match",
                "solvability",
                "impact_score",
                "frequency_score",
                "evidence_score",
                "solvability_score",
                "novelty_score",
                "effort_score",
                "confidence",
                "counterargument",
                "candidate_recommendation",
            ],
            "classification_enum": sorted(CLASSIFICATIONS),
            "solvability_enum": sorted(SOLVABILITY_VALUES),
            "confidence_enum": sorted(CONFIDENCE_VALUES),
            "score_fields": {
                "impact_score": "JSON integer, one of: 0, 1, 2, 3, 4, 5 (MUST be an integer, not decimal or string)",
                "frequency_score": "JSON integer, one of: 0, 1, 2, 3, 4, 5 (MUST be an integer, not decimal or string)",
                "evidence_score": "JSON integer, one of: 0, 1, 2, 3, 4, 5 (MUST be an integer, not decimal or string)",
                "solvability_score": "JSON integer, one of: 0, 1, 2, 3, 4, 5 (MUST be an integer, not decimal or string)",
                "novelty_score": "JSON integer, one of: 0, 1, 2, 3, 4, 5 (MUST be an integer, not decimal or string)",
                "effort_score": "JSON integer, one of: 0, 1, 2, 3, 4, 5 (MUST be an integer, not decimal or string)",
            },
            "score_example": {
                "example_entry": {
                    "cluster_id": "DISC-ABC123",
                    "impact_score": 4,
                    "frequency_score": 3,
                    "evidence_score": 4,
                    "solvability_score": 4,
                    "novelty_score": 2,
                    "effort_score": 2,
                }
            },
        },
        "constraints": [
            "Return JSON only.",
            "Problem statement must describe problem, not solution.",
            "The output persona MUST exactly equal persona_candidate. If persona_candidate is unknown, persona MUST be unknown.",
            "For a cluster with multiple structured_evidence_items, problem_statement MUST state only the common denominator supported by every evidence item.",
            "Keep details supported by only one source in evidence_summary; do not present them as shared facts in problem_statement.",
            "Use shared_intent_facets and concept_id as deterministic grounding hints; do not add causes, conditions, devices, operating systems, or warning behavior not shared by every evidence item.",
            "problem_statement_pl MUST express exactly the same grounded meaning as problem_statement and MUST NOT add claims.",
            "Do not infer caregiver, senior, or clinician from generic CGM evidence.",
            "Exactly one classification per cluster.",
            "Do not accept requirements or start implementation.",
            "Never recommend insulin dose, bolus, basal, insulin ratio, or therapy adjustment.",
            "Return concise English problem_statement and concise Polish problem_statement_pl without long source reproduction.",
            "CRITICAL: Score fields (impact_score, frequency_score, evidence_score, solvability_score, novelty_score, effort_score) MUST be JSON integers in the range 0-5. Do NOT use decimals (e.g., 4.5), text (e.g., 'high'), fractions (e.g., '4/5'), or ranges (e.g., '3-4'). Each score must be exactly one of: 0, 1, 2, 3, 4, 5.",
        ],
        "clusters": compact,
    }
    return json.dumps(payload, ensure_ascii=False, indent=2)


def build_ai_repair_prompt(run_id: str, clusters: list[dict], validation_errors: list[str], original_payload: dict, model: str) -> str:
    """Build a bounded repair prompt for schema and evidence-grounding failures."""
    compact = []
    for c in clusters:
        compact.append(
            {
                "cluster_id": c["cluster_id"],
                "normalized_problem": c["normalized_problem"],
                "canonical_problem_statement": c.get("canonical_problem_statement", ""),
                "canonical_problem_statement_pl": c.get("canonical_problem_statement_pl", ""),
                "persona_candidate": c["persona_candidate"],
                "module_candidate": c["module_candidate"],
                "concept_id": c.get("concept_id", "unclassified"),
                "shared_intent_facets": c.get("shared_intent_facets", []),
                "structured_evidence_items": _structured_evidence_items(c),
                "problem_statement_pl_required": True,
            }
        )

    payload = {
        "task": "Repair LibreCare AI cluster analysis. SCHEMA AND EVIDENCE-GROUNDING REPAIR ONLY.",
        "run_id": run_id,
        "model": model,
        "instruction": "Your previous analysis had schema or evidence-grounding errors below. Return the COMPLETE corrected clusters array using the exact same cluster_ids. Correct invalid fields from the bounded evidence context without adding claims. Do NOT add/remove clusters or change valid analytical intent.",
        "validation_errors": validation_errors[:10],  # Show first 10 errors
        "original_payload": original_payload,
        "score_requirements": {
            "impact_score": "MUST be JSON integer: one of exactly 0, 1, 2, 3, 4, 5. NOT decimal, NOT string, NOT text label.",
            "frequency_score": "MUST be JSON integer: one of exactly 0, 1, 2, 3, 4, 5. NOT decimal, NOT string, NOT text label.",
            "evidence_score": "MUST be JSON integer: one of exactly 0, 1, 2, 3, 4, 5. NOT decimal, NOT string, NOT text label.",
            "solvability_score": "MUST be JSON integer: one of exactly 0, 1, 2, 3, 4, 5. NOT decimal, NOT string, NOT text label.",
            "novelty_score": "MUST be JSON integer: one of exactly 0, 1, 2, 3, 4, 5. NOT decimal, NOT string, NOT text label.",
            "effort_score": "MUST be JSON integer: one of exactly 0, 1, 2, 3, 4, 5. NOT decimal, NOT string, NOT text label.",
        },
        "constraints": [
            "Return JSON object with 'clusters' array only.",
            "Preserve all cluster_ids from original analysis.",
            "Preserve valid analytical intent (classification, scores, etc.).",
            "Fix ONLY the schema or evidence-grounding violations listed in validation_errors.",
            "persona MUST exactly equal persona_candidate; unknown MUST remain unknown.",
            "For multiple evidence items, problem_statement must contain only their common denominator; source-specific details belong only in evidence_summary.",
            "problem_statement_pl must express exactly the same grounded meaning without additional claims.",
            "Do NOT change cluster membership or add/remove clusters.",
            "Do NOT make new product decisions.",
            "Do NOT start implementation.",
            "Score fields MUST be JSON integers 0-5.",
            "problem_statement_pl is required and must be concise Polish.",
        ],
        "clusters": compact,
    }
    return json.dumps(payload, ensure_ascii=False, indent=2)


def run_copilot_json(prompt: str, model: str) -> str:
    if model != "gpt-5.4-mini":
        raise RuntimeError(f"Discovery requires model gpt-5.4-mini, got: {model}")

    cmd = [
        "copilot",
        "-s",
        "--no-ask-user",
        "--disable-builtin-mcps",
        "--available-tools=view,grep,glob",
        "--allow-tool=read",
        "--deny-tool=write",
        "--deny-tool=shell",
        "--model=gpt-5.4-mini",
        "-p",
        prompt,
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True, check=False)
    if proc.returncode != 0:
        stderr = (proc.stderr or "").strip()
        if "model" in stderr.lower() and "gpt-5.4-mini" in stderr.lower():
            raise RuntimeError("Copilot CLI failed: gpt-5.4-mini is unavailable in this environment")
        raise RuntimeError(f"Copilot CLI failed: {stderr[:400]}")
    return proc.stdout


def extract_json_object(raw: str) -> dict:
    text = raw.strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?", "", text).strip()
        text = re.sub(r"```$", "", text).strip()
    try:
        return json.loads(text)
    except Exception:
        match = re.search(r"\{[\s\S]*\}", text)
        if not match:
            raise
        return json.loads(match.group(0))


def normalize_ai_score(val) -> int | None:
    """
    Normalize score values deterministically.

    Accepts and normalizes to int 0-5:
    - JSON int: 4 -> 4
    - integral JSON float: 4.0 -> 4
    - numeric string: "4" -> 4

    Rejects and returns None:
    - bool (True/False)
    - decimals (4.5, "4.5")
    - fractions ("4/5")
    - text labels ("high")
    - ranges ("3-4")
    - null/None
    - missing
    - out of range

    Returns int 0-5 if valid, None if invalid.
    """
    if val is None:
        return None

    # Reject bool explicitly (before int check since bool is int subclass)
    if isinstance(val, bool):
        return None

    # Accept JSON int
    if isinstance(val, int):
        if 0 <= val <= 5:
            return val
        return None

    # Accept integral float (4.0)
    if isinstance(val, float):
        if val == int(val) and 0 <= int(val) <= 5:
            return int(val)
        return None

    # Accept numeric string containing single digit 0-5
    if isinstance(val, str):
        val_stripped = val.strip()
        # Must be exactly 1 character and a digit 0-5
        if len(val_stripped) == 1 and val_stripped in "012345":
            return int(val_stripped)
        return None

    # Reject everything else
    return None


def normalize_ai_output(payload: dict) -> dict:
    """Normalize AI output score fields deterministically. Returns modified payload."""
    if not isinstance(payload, dict):
        return payload
    entries = payload.get("clusters")
    if not isinstance(entries, list):
        return payload

    score_keys = [
        "impact_score",
        "frequency_score",
        "evidence_score",
        "solvability_score",
        "novelty_score",
        "effort_score",
    ]

    for entry in entries:
        if not isinstance(entry, dict):
            continue
        for key in score_keys:
            if key in entry:
                normalized = normalize_ai_score(entry[key])
                if normalized is not None:
                    entry[key] = normalized
                # If normalization failed, leave the invalid value
                # so validate_ai_output can report it

    return payload


def validate_ai_output(payload: dict, clusters: list[dict]) -> tuple[bool, list[str]]:
    errors = []
    if not isinstance(payload, dict):
        return False, ["AI payload is not a JSON object"]
    entries = payload.get("clusters")
    if not isinstance(entries, list):
        return False, ["AI payload missing 'clusters' array"]

    expected_ids = {c["cluster_id"] for c in clusters}
    clusters_by_id = {c["cluster_id"]: c for c in clusters}
    got_ids = set()
    seen_ids: dict[str, int] = {}
    required = {
        "cluster_id",
        "classification",
        "persona",
        "problem_statement",
        "problem_statement_pl",
        "evidence_summary",
        "source_diversity_summary",
        "current_librecare_match",
        "solvability",
        "impact_score",
        "frequency_score",
        "evidence_score",
        "solvability_score",
        "novelty_score",
        "effort_score",
        "confidence",
        "counterargument",
        "candidate_recommendation",
    }

    if len(entries) != len(clusters):
        errors.append(f"AI payload cluster count mismatch: expected {len(clusters)}, got {len(entries)}")

    for idx, row in enumerate(entries):
        if not isinstance(row, dict):
            errors.append(f"clusters[{idx}] is not an object")
            continue
        missing = [k for k in sorted(required) if k not in row]
        if missing:
            errors.append(f"clusters[{idx}] missing fields: {missing}")
            continue
        cid = str(row["cluster_id"])
        seen_ids[cid] = seen_ids.get(cid, 0) + 1
        if seen_ids[cid] > 1:
            errors.append(f"clusters[{idx}] duplicate cluster_id: {cid}")
        got_ids.add(cid)
        if cid not in expected_ids:
            errors.append(f"clusters[{idx}] unknown cluster_id: {cid}")
        else:
            cluster = clusters_by_id[cid]
            persona_candidate = str(cluster.get("persona_candidate") or "unknown")
            if row["persona"] != persona_candidate:
                errors.append(
                    f"clusters[{idx}] persona must equal persona_candidate: expected {persona_candidate}, got {row['persona']}"
                )
            evidence_text = " ".join(
                f"{item.get('problem_statement') or ''} {item.get('excerpt') or ''}"
                for item in (cluster.get("raw_items") or [])
            ).lower()
            output_statements = f"{row.get('problem_statement') or ''} {row.get('problem_statement_pl') or ''}".lower()
            persona_terms = {
                "caregiver": ("caregiver", "opiekun", "aidant", "betreuer", "cuidador"),
                "senior": ("senior", "elderly", "starsz"),
                "clinician": ("clinician", "doctor", "physician", "lekarz", "klinicyst"),
            }
            for persona_name, terms in persona_terms.items():
                if any(term in output_statements for term in terms) and not any(term in evidence_text for term in terms):
                    errors.append(f"clusters[{idx}] unsupported {persona_name} claim in problem statement")
        if row["classification"] not in CLASSIFICATIONS:
            errors.append(f"clusters[{idx}] invalid classification: {row['classification']}")
        if row["solvability"] not in SOLVABILITY_VALUES:
            errors.append(f"clusters[{idx}] invalid solvability: {row['solvability']}")
        if row["confidence"] not in CONFIDENCE_VALUES:
            errors.append(f"clusters[{idx}] invalid confidence: {row['confidence']}")
        for score_key in [
            "impact_score",
            "frequency_score",
            "evidence_score",
            "solvability_score",
            "novelty_score",
            "effort_score",
        ]:
            val = row.get(score_key)
            if isinstance(val, bool) or not isinstance(val, int) or val < 0 or val > 5:
                errors.append(f"clusters[{idx}] {score_key} must be int 0..5")

    missing_cluster_ids = expected_ids - got_ids
    if missing_cluster_ids:
        errors.append(f"AI output missing clusters: {sorted(missing_cluster_ids)}")
    return not errors, errors


def apply_governance(cluster: dict, ai_row: dict) -> dict:
    out = dict(ai_row)
    match = cluster.get("foundation_match", {})

    out["problem_statement"] = str(cluster.get("canonical_problem_statement") or cluster.get("normalized_problem") or "")
    out["problem_statement_pl"] = str(cluster.get("canonical_problem_statement_pl") or out.get("problem_statement_pl") or "")

    if match.get("best_capability_score", 0.0) >= 0.68:
        out["classification"] = "VALIDATED_CAPABILITY"

    if match.get("best_requirement_score", 0.0) >= 0.75:
        out["suppressed"] = True
        out["suppressed_reason"] = "Strong match to existing requirement"

    if match.get("suppress_reproposal"):
        out["suppressed"] = True
        out["suppressed_reason"] = match.get("suppress_reason")

    if out.get("classification") not in CLASSIFICATIONS:
        out["classification"] = "INCONCLUSIVE"

    if out.get("classification") in {"VALIDATED_CAPABILITY", "TEST_COVERAGE_GAP", "INCONCLUSIVE"}:
        out["eligible_for_inbox"] = False
        out["exclusion_reason"] = f"Classification {out['classification']} is not eligible"
    elif out.get("solvability") in {"ABBOTT_LIMITATION", "EXTERNAL_ONLY"}:
        out["eligible_for_inbox"] = False
        out["exclusion_reason"] = f"Solvability {out['solvability']} not app-solvable"
    elif out.get("suppressed"):
        out["eligible_for_inbox"] = False
        out["exclusion_reason"] = out.get("suppressed_reason", "Suppressed by existing decision")
    else:
        out["eligible_for_inbox"] = out.get("classification") in ELIGIBLE_CLASSIFICATIONS

    if not cluster.get("quality_gate_passes", True):
        out["eligible_for_inbox"] = False
        out["exclusion_reason"] = "Deterministic problem-signal quality gate failed"
    elif EVIDENCE_TIER_ORDER.get(str(cluster.get("evidence_tier") or "WEAK"), 0) < EVIDENCE_TIER_ORDER["CORROBORATED"]:
        out["eligible_for_inbox"] = False
        out["exclusion_reason"] = f"Evidence tier {cluster.get('evidence_tier', 'WEAK')} cannot enter Product Inbox"

    return out


def compute_score(cluster: dict, decision: dict) -> int:
    impact = int(decision.get("impact_score", 0))
    freq = int(decision.get("frequency_score", 0))
    evidence = int(decision.get("evidence_score", 0))
    solvability = int(decision.get("solvability_score", 0))
    novelty = int(decision.get("novelty_score", 0))
    effort = int(decision.get("effort_score", 0))
    source_diversity = min(int(cluster.get("independent_source_family_count", 0)), 3)

    strategic_fit = 5 if cluster.get("persona_candidate") == "caregiver" else 3
    raw = (
        impact * 9
        + freq * 8
        + evidence * 8
        + solvability * 9
        + novelty * 7
        + source_diversity * 4
        + strategic_fit * 3
        - effort * 5
    )
    tier = str(cluster.get("evidence_tier") or "WEAK")
    return max(0, min(EVIDENCE_SCORE_CAPS.get(tier, 49), int(raw)))


def select_top10_and_watchlist(governed_rows: list[dict]) -> tuple[list[dict], list[dict]]:
    ranked = sorted(governed_rows, key=lambda row: (-row["score"], row["cluster"]["cluster_id"]))
    def governance_candidate(row: dict) -> bool:
        governed = row["governed"]
        cluster = row["cluster"]
        return (
            cluster.get("quality_gate_passes", True)
            and not cluster.get("identity_ambiguous", False)
            and governed.get("classification") in ELIGIBLE_CLASSIFICATIONS
            and governed.get("solvability") not in {"ABBOTT_LIMITATION", "EXTERNAL_ONLY"}
            and not governed.get("suppressed", False)
        )
    top10 = [row for row in ranked if governance_candidate(row) and row["cluster"].get("evidence_tier", "SUPPORTED") in {"SUPPORTED", "CORROBORATED", "STRONG"}][:10]
    watchlist = [row for row in ranked if governance_candidate(row) and row["cluster"].get("evidence_tier") == "WEAK"][:10]
    return top10, watchlist


def next_observation_name(existing_names: set[str], date_stamp: str) -> tuple[str, str]:
    idx = 1
    while True:
        obs_id = f"OBS-{date_stamp}-{idx:02d}"
        filename = f"{obs_id}.json"
        if filename not in existing_names:
            return obs_id, filename
        idx += 1


def load_existing_observation_fingerprints() -> tuple[set[str], set[str]]:
    fps = set()
    clusters = set()
    for path in iter_record_files(OBSERVATIONS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        fp = rec.get("problem_fingerprint")
        cid = rec.get("cluster_id")
        if isinstance(fp, str) and fp:
            fps.add(fp)
        if isinstance(cid, str) and cid:
            clusters.add(cid)
    return fps, clusters


def create_observations_from_clusters(clusters: list[dict], run_id: str) -> list[str]:
    created = []
    OBSERVATIONS_DIR.mkdir(parents=True, exist_ok=True)
    existing_files = {p.name for p in OBSERVATIONS_DIR.glob("OBS-*.json")}
    existing_fp, existing_cluster_ids = load_existing_observation_fingerprints()
    date_stamp = datetime.now(timezone.utc).strftime("%Y%m%d")

    for cluster in clusters:
        if cluster.get("identity_ambiguous"):
            continue
        if not cluster.get("quality_gate_passes", True):
            continue
        if EVIDENCE_TIER_ORDER.get(str(cluster.get("evidence_tier") or "WEAK"), 0) < EVIDENCE_TIER_ORDER["SUPPORTED"]:
            continue
        match = cluster.get("foundation_match", {})
        if match.get("best_capability_score", 0.0) >= 0.80:
            continue

        cid = cluster["cluster_id"]
        fp = str(cluster.get("canonical_problem_fingerprint") or "")
        if not fp:
            continue
        if cid in existing_cluster_ids or (fp and fp in existing_fp):
            continue

        first = cluster["raw_items"][0]
        obs_id, filename = next_observation_name(existing_files, date_stamp)
        payload = {
            "id": obs_id,
            "created_at": utc_now(),
            "source_type": first.get("source_type", "community"),
            "source_reference": cluster["source_urls"][0] if cluster.get("source_urls") else "",
            "persona": cluster.get("persona_candidate", "unknown"),
            "mode": cluster.get("mode_candidate", cluster.get("persona_candidate", "unknown")),
            "module": cluster.get("module_candidate", "unknown"),
            "type": first.get("type", "usability"),
            "severity": first.get("severity", "medium"),
            "frequency": first.get("frequency", "unknown"),
            "confidence": first.get("confidence", "medium"),
            "evidence": cluster.get("evidence_items", [])[:3] or [cluster.get("normalized_problem", "")],
            "problem_statement": cluster.get("canonical_problem_statement", cluster.get("normalized_problem", "")),
            "status": "new",
            "cluster_id": cid,
            "problem_fingerprint": fp,
            "discovery_run_id": run_id,
        }
        path = OBSERVATIONS_DIR / filename
        write_json(path, payload)
        existing_files.add(filename)
        existing_cluster_ids.add(cid)
        if fp:
            existing_fp.add(fp)
        created.append(str(path.relative_to(ROOT)))

    return created


def build_inbox_issue_body(entry: dict, marker: str) -> str:
    cluster = entry["cluster"]
    governed = entry["governed"]
    safety_prefix = "\nWYMAGA PRZEGLĄDU BEZPIECZEŃSTWA\n" if governed["classification"] == "SAFETY_GAP" else ""
    lines = [
        "### Typ zgłoszenia",
        "Problem" if governed["classification"] in {"PRODUCT_PROBLEM", "SAFETY_GAP"} else "Pomysł",
        "",
        "### Persona użytkownika",
        cluster.get("persona_candidate", "Opiekun"),
        "",
        "### Moduł",
        cluster.get("module_candidate", "Inne / nie wiem"),
        "",
        "### Co zauważyłeś(-aś) / czego potrzebujesz?",
        cluster.get("canonical_problem_statement_pl") or cluster.get("canonical_problem_statement") or governed.get("problem_statement", ""),
        "",
        "### Dlaczego to ważne?",
        f"Discovery score: {entry['score']}/100. {governed.get('candidate_recommendation', '')}" + safety_prefix,
        "",
        "### Kontekst / przykład",
        f"Na podstawie dostępnych danych: {governed.get('evidence_summary', '')}",
        "",
        "Źródła:",
    ]
    lines.extend([f"- {u}" for u in cluster.get("source_urls", [])[:8]])
    lines.extend(["", marker])
    return "\n".join(lines)


def publish_top3(top_ranked: list[dict], repo_owner: str, repo_name: str, github_token: str) -> list[dict]:
    client = GITHUB_CLIENT_FACTORY(repo_owner, repo_name, github_token)
    created = []
    eligible_rows = [row for row in top_ranked if row["governed"].get("eligible_for_inbox")]
    for entry in eligible_rows[:3]:
        marker = f"<!-- LIBRECARE_DISCOVERY_CLUSTER: {entry['cluster']['cluster_id']} -->"
        existing = client.find_issue_by_marker(marker)
        if existing:
            created.append(
                {
                    "cluster_id": entry["cluster"]["cluster_id"],
                    "action": "SKIPPED_EXISTS",
                    "issue_number": existing.get("number"),
                    "issue_url": existing.get("html_url"),
                }
            )
            continue

        issue = client.create_issue(
            title=f"[Skrzynka Produktowa] Discovery {entry['cluster']['cluster_id']}",
            body=build_inbox_issue_body(entry, marker),
            labels=["product-inbox"],
        )
        created.append(
            {
                "cluster_id": entry["cluster"]["cluster_id"],
                "action": "CREATED",
                "issue_number": issue.get("number"),
                "issue_url": issue.get("html_url"),
            }
        )
    return created


def run_discovery(
    sources_file: Path,
    max_items_per_source: int,
    publish_top3_flag: bool,
    repo_owner: str,
    repo_name: str,
    github_token: str,
    ai_mode: str,
    ai_model: str,
    ai_response_file: Path | None,
    timeout: float,
    retries: int,
    cache_max_age_seconds: int,
    query_packs_file: Path | None = None,
    languages: list[str] | None = None,
    primary_languages: list[str] | None = None,
    lookback_days: int = 365,
) -> dict:
    started_at = utc_now()
    run_stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M")
    run_id = f"DISCOVERY-{run_stamp}"

    status = "SUCCESS"
    errors: list[str] = []
    ai_calls = 0

    cache = DiscoveryCache(DISCOVERY_CACHE_PATH)
    query_path = query_packs_file or QUERY_PACKS_DEFAULT
    query_packs = load_query_packs(query_path) if query_path.exists() else {"version": 1, "languages": {}, "concepts": []}
    configured_languages = [lang for lang in (languages or ["pl", "en", "de", "fr", "es"]) if lang in SUPPORTED_LANGUAGES]
    requested_primary = ["pl", "en"] if primary_languages is None else primary_languages
    configured_primary = [lang for lang in requested_primary if lang in configured_languages]

    sources_cfg = read_json(sources_file)
    sources = sources_cfg.get("sources", [])

    source_results = []
    all_items = []
    for source in sources:
        remaining_budget = MAX_TOTAL_COLLECTED - len(all_items)
        if remaining_budget <= 0:
            break
        result = collect_from_source(
            source,
            max_items=min(max_items_per_source, 30, remaining_budget),
            timeout=timeout,
            retries=retries,
            cache=cache,
            cache_max_age_seconds=cache_max_age_seconds,
            query_packs=query_packs,
            languages=configured_languages,
            primary_languages=configured_primary,
            lookback_days=lookback_days,
            github_token=github_token,
        )
        accepted_count = sum(1 for item in result.items if item.get("eligible_for_clustering", True))
        observed_languages = sorted(set(str(item.get("language") or "unknown") for item in result.items))
        kind = str(source.get("kind") or "").strip().lower()
        if kind in {"github_issues", "github_issue_search", "reddit_oauth"} or (not kind and result.family in {"github_community", "reddit"}):
            requested_languages = list(configured_languages)
        elif source.get("language") in SUPPORTED_LANGUAGES:
            requested_languages = [str(source["language"])]
        else:
            requested_languages = []
        source_results.append(
            {
                "name": result.name,
                "family": result.family,
                "status": result.status,
                "detail": result.detail,
                "count": len(result.items),
                "collected": len(result.items),
                "accepted": accepted_count,
                "filtered": len(result.items) - accepted_count,
                "evidence_role": result.evidence_role,
                "requested_languages": requested_languages,
                "observed_languages": observed_languages,
            }
        )
        all_items.extend(result.items)
        if result.status.endswith("DEGRADED") or result.status == "REDDIT_DISABLED":
            if status == "SUCCESS":
                status = "DEGRADED"

    deduped_items, dedupe_stats = dedupe_items(all_items)
    filter_summary = {"duplicate": dedupe_stats["exact_duplicates"] + dedupe_stats["normalized_duplicates"]}
    for item in all_items:
        reason = str(item.get("filter_reason") or "accepted")
        if reason != "accepted":
            filter_summary[reason] = filter_summary.get(reason, 0) + 1
        if item.get("privacy_redacted"):
            filter_summary["privacy_redacted"] = filter_summary.get("privacy_redacted", 0) + 1
    # Build logical clusters without identity reuse first; stateful registry assigns final stable IDs.
    clusters = cluster_items(deduped_items)
    observation_clusters = _existing_cluster_matches()

    cluster_registry = None
    registry_summary = {
        "existing_entries": 0,
        "matched_existing": 0,
        "new_entries": 0,
        "ambiguous_matches": [],
        "excluded_weak_or_noise": 0,
        "changed": False,
    }
    proposed_registry = {"version": CLUSTER_REGISTRY_VERSION, "entries": []}

    try:
        cluster_registry = load_cluster_registry(DISCOVERY_CLUSTER_REGISTRY_PATH)
    except RuntimeError as exc:
        status = "FAILED"
        errors.append(f"Registry load failed (fail-closed): {exc}")
        cluster_registry = None

    if cluster_registry is not None:
        clusters, registry_summary, proposed_registry = resolve_cluster_ids_with_registry(
            clusters,
            cluster_registry,
            now_iso=utc_now(),
            observation_clusters=observation_clusters,
        )
    else:
        # Registry is unavailable; prevent inbox publication
        for cluster in clusters:
            cluster["identity_ambiguous"] = True
            cluster["identity_ambiguous_candidates"] = [
                {"cluster_id": cluster["cluster_id"], "score": 0.0},
            ]

    foundation = load_foundation_index()
    for c in clusters:
        c["foundation_match"] = match_cluster_to_foundation(c, foundation)

    created_observations = []

    if ai_mode == "copilot" and ai_model != "gpt-5.4-mini":
        status = "FAILED"
        errors.append("Copilot mode requires exact model gpt-5.4-mini")

    if ai_response_file:
        raw = ai_response_file.read_text(encoding="utf-8")
        ai_calls = 1
    elif ai_mode == "copilot" and clusters and status != "FAILED":
        prompt = build_ai_prompt(run_id, clusters, model=ai_model)
        try:
            raw = run_copilot_json(prompt, model=ai_model)
        except Exception as exc:  # noqa: BLE001
            status = "FAILED"
            errors.append(str(exc))
            raw = "{}"
        ai_calls = 1
    else:
        payload = {"clusters": []}
        for c in clusters:
            cls = "PRODUCT_PROBLEM"
            if c["foundation_match"].get("best_capability_score", 0) >= 0.68:
                cls = "VALIDATED_CAPABILITY"
            payload["clusters"].append(
                {
                    "cluster_id": c["cluster_id"],
                    "classification": cls,
                    "persona": c["persona_candidate"],
                    "problem_statement": c.get("canonical_problem_statement", c["normalized_problem"]),
                    "problem_statement_pl": c.get("canonical_problem_statement_pl", "Problem wymaga przeglądu Product Ownera."),
                    "evidence_summary": _safe_excerpt("; ".join(c["evidence_items"]), 160),
                    "source_diversity_summary": f"{c.get('evidence_item_count', c['source_count'])} items / {c.get('independent_source_identity_count', 0)} identities / {c['independent_source_family_count']} families",
                    "current_librecare_match": _foundation_match_summary(c.get("foundation_match", {})),
                    "solvability": "APP",
                    "impact_score": 3,
                    "frequency_score": 3,
                    "evidence_score": 3,
                    "solvability_score": 3,
                    "novelty_score": 3,
                    "effort_score": 2,
                    "confidence": "medium",
                    "counterargument": "Evidence may still be incomplete.",
                    "candidate_recommendation": "Needs human review.",
                }
            )
        raw = json.dumps(payload)
        ai_calls = 0

    ai_payload = {}
    if clusters:
        try:
            ai_payload = extract_json_object(raw)
        except Exception as exc:  # noqa: BLE001
            status = "FAILED"
            errors.append(f"AI output parse failed: {exc}")
            ai_payload = {}

    # Normalize AI scores (deterministic fix for common output issues)
    if ai_payload and clusters:
        ai_payload = normalize_ai_output(ai_payload)

    governed_rows = []
    if clusters and ai_payload:
        valid, ai_errors = validate_ai_output(ai_payload, clusters)
        if not valid and ai_mode == "copilot" and ai_calls < 2:
            # First validation failed; attempt one schema repair
            try:
                repair_prompt = build_ai_repair_prompt(run_id, clusters, ai_errors, ai_payload, model=ai_model)
                repair_raw = run_copilot_json(repair_prompt, model=ai_model)
                ai_calls += 1
                # Parse repaired output
                try:
                    ai_payload = extract_json_object(repair_raw)
                    ai_payload = normalize_ai_output(ai_payload)
                    valid, ai_errors = validate_ai_output(ai_payload, clusters)
                except Exception as exc:  # noqa: BLE001
                    errors.append(f"AI repair output parse failed: {exc}")
                    valid = False
            except Exception as exc:  # noqa: BLE001
                errors.append(f"AI repair call failed: {exc}")
                valid = False

        if not valid:
            status = "FAILED"
            errors.extend(ai_errors)
        else:
            ai_map = {x["cluster_id"]: x for x in ai_payload["clusters"]}
            for cluster in clusters:
                governed = apply_governance(cluster, ai_map[cluster["cluster_id"]])
                if cluster.get("identity_ambiguous"):
                    governed["eligible_for_inbox"] = False
                    governed["exclusion_reason"] = "Cluster identity ambiguous; requires human registry review"
                score = compute_score(cluster, governed)
                governed_rows.append({"cluster": cluster, "governed": governed, "score": score})

    if ai_calls > 2:
        status = "FAILED"
        errors.append(f"AI call budget exceeded: {ai_calls} > 2")

    top10, watchlist_rows = select_top10_and_watchlist(governed_rows)

    if status != "FAILED":
        created_observations = create_observations_from_clusters(clusters, run_id)

    created_issues = []
    if status != "FAILED" and publish_top3_flag and top10:
        if not (repo_owner and repo_name and github_token):
            status = "DEGRADED"
            errors.append("publish_top3 requested but GitHub credentials/repo not provided")
        else:
            created_issues = publish_top3(top10, repo_owner=repo_owner, repo_name=repo_name, github_token=github_token)

    finished_at = utc_now()

    report_top10 = []
    for idx, row in enumerate(top10):
        deterministic_match = row["cluster"].get("foundation_match", {})
        report_top10.append(
            {
                "rank": idx + 1,
                "score": row["score"],
                "cluster_id": row["cluster"]["cluster_id"],
                "problem": row["cluster"].get("canonical_problem_statement"),
                "problem_statement": row["cluster"].get("canonical_problem_statement"),
                "problem_statement_pl": row["cluster"].get("canonical_problem_statement_pl"),
                "persona": row["governed"].get("persona"),
                "classification": row["governed"].get("classification"),
                "evidence_count": len(row["cluster"].get("evidence_items", [])),
                "evidence_item_count": row["cluster"].get("evidence_item_count", 0),
                "independent_source_identity_count": row["cluster"].get("independent_source_identity_count", 0),
                "independent_source_family_count": row["cluster"].get("independent_source_family_count", 0),
                "languages": row["cluster"].get("languages", []),
                "language_count": row["cluster"].get("language_count", 0),
                "evidence_tier": row["cluster"].get("evidence_tier", "WEAK"),
                "concept_id": row["cluster"].get("concept_id", "unclassified"),
                "topic_id": row["cluster"].get("topic_id", "unclassified"),
                "qualification_reason": "Quality gate passed and evidence tier is at least SUPPORTED",
                "source_families": row["cluster"].get("source_families", []),
                "source_identities": row["cluster"].get("source_identities", []),
                "current_librecare_match": _foundation_match_summary(deterministic_match),
                "foundation_match_links": deterministic_match.get("linked", []),
                "ai_current_librecare_match": row["governed"].get("current_librecare_match"),
                "solvability": row["governed"].get("solvability"),
                "effort": row["governed"].get("effort_score"),
                "confidence": row["governed"].get("confidence"),
                "counterargument": row["governed"].get("counterargument"),
                "source_urls": row["cluster"].get("source_urls", []),
                "eligibility": bool(row["governed"].get("eligible_for_inbox")),
                "exclusion_reason": row["governed"].get("exclusion_reason", ""),
                "suppressed_reason": row["governed"].get("suppressed_reason", ""),
            }
        )

    report_watchlist = []
    for row in watchlist_rows:
        cluster = row["cluster"]
        report_watchlist.append({
            "cluster_id": cluster["cluster_id"], "score": row["score"], "evidence_tier": "WEAK",
            "concept_id": cluster.get("concept_id"), "topic_id": cluster.get("topic_id"),
            "problem_statement": cluster.get("canonical_problem_statement"), "problem_statement_pl": cluster.get("canonical_problem_statement_pl"),
            "languages": cluster.get("languages", []), "source_identities": cluster.get("source_identities", []),
            "source_families": cluster.get("source_families", []), "evidence_item_count": cluster.get("evidence_item_count", 0),
            "why_promising": "Deterministic user-facing problem signal passed quality filtering.",
            "why_weak": "Only one independent source identity currently corroborates this signal.",
            "corroboration_needed": "A second independent source identity or three independent evidence items are required.",
            "eligible_for_inbox": False,
        })

    report = {
        "run_id": run_id,
        "started_at": started_at,
        "finished_at": finished_at,
        "status": status,
        "counts": {
            "COLLECTED": len(all_items),
            "AFTER_DEDUPE": len(deduped_items),
            "CLUSTERS": len(clusters),
            "AI_CALLS": ai_calls,
            "FILTERED_NOISE": sum(value for key, value in filter_summary.items() if key != "privacy_redacted"),
            "WATCHLIST": len(report_watchlist),
            "TOP10": len(report_top10),
            "OBSERVATIONS_CREATED": len(created_observations),
            "PRODUCT_INBOX_ACTIONS": len(created_issues),
        },
        "source_status": source_results,
        "dedupe": dedupe_stats,
        "configured_languages": configured_languages,
        "primary_languages": configured_primary,
        "lookback_days": lookback_days,
        "filter_summary": filter_summary,
        "top10": report_top10,
        "watchlist": report_watchlist,
        "created_observations": created_observations,
        "top3_issue_actions": created_issues,
        "errors": errors,
        "cluster_registry": {
            "existing_entries": registry_summary.get("existing_entries", 0),
            "matched_existing": registry_summary.get("matched_existing", 0),
            "new_entries": registry_summary.get("new_entries", 0),
            "ambiguous_matches": registry_summary.get("ambiguous_matches", []),
            "excluded_weak_or_noise": registry_summary.get("excluded_weak_or_noise", 0),
            "proposed_registry_path": "",
        },
        "safety_guards": {
            "max_ai_calls": 2,
            "ai_calls_used": ai_calls,
            "no_auto_acceptance": True,
            "no_auto_implementation": True,
            "no_auto_merge": True,
        },
    }

    GENERATED_DISCOVERY_DIR.mkdir(parents=True, exist_ok=True)
    proposed_registry_path = DISCOVERY_PROPOSED_CLUSTER_REGISTRY_PATH
    if registry_summary.get("changed"):
        write_json(proposed_registry_path, proposed_registry)
        report["cluster_registry"]["proposed_registry_path"] = str(proposed_registry_path.relative_to(ROOT))

    json_path = GENERATED_DISCOVERY_DIR / f"DISCOVERY-{run_stamp}.json"
    md_path = GENERATED_DISCOVERY_DIR / f"DISCOVERY-{run_stamp}.md"
    write_json(json_path, report)
    md_path.write_text(render_markdown_report(report), encoding="utf-8")

    try:
        cache.save()
    except Exception as exc:  # noqa: BLE001
        report["status"] = "DEGRADED" if report["status"] == "SUCCESS" else report["status"]
        report["errors"].append(f"Cache save degraded: {exc}")

    report["json_report_path"] = str(json_path.relative_to(ROOT))
    report["md_report_path"] = str(md_path.relative_to(ROOT))
    return report


def _report_section_for_item(item: dict) -> str:
    if item.get("eligibility"):
        return "TOP CANDIDATES"
    if item.get("classification") == "TEST_COVERAGE_GAP":
        return "TEST / RESEARCH GAPS"
    if item.get("classification") == "INCONCLUSIVE":
        return "INSUFFICIENT EVIDENCE"
    if item.get("solvability") in {"ABBOTT_LIMITATION", "EXTERNAL_ONLY"}:
        return "ABBOTT LIMITATION / NOT LIBRECARE-SOLVABLE"
    if item.get("classification") == "VALIDATED_CAPABILITY" or item.get("suppressed_reason") or (
        "existing requirement" in str(item.get("exclusion_reason", "")).lower()
    ):
        return "ALREADY COVERED"
    return "WORTH REVIEWING"


def render_markdown_report(report: dict) -> str:
    lines = [
        f"# LibreCare Discovery Report — {report['run_id']}",
        "",
        f"- Status: **{report['status']}**",
        f"- Started: {report['started_at']}",
        f"- Finished: {report['finished_at']}",
        f"- COLLECTED: {report['counts']['COLLECTED']}",
        f"- AFTER_DEDUPE: {report['counts']['AFTER_DEDUPE']}",
        f"- CLUSTERS: {report['counts']['CLUSTERS']}",
        f"- AI_CALLS: {report['counts']['AI_CALLS']}",
        f"- Configured languages: {', '.join(report.get('configured_languages', []))}",
        f"- Lookback days: {report.get('lookback_days', 365)}",
        f"- FILTERED_NOISE: {report['counts'].get('FILTERED_NOISE', 0)}",
        f"- TOP10: {report['counts'].get('TOP10', 0)}",
        f"- WATCHLIST: {report['counts'].get('WATCHLIST', 0)}",
        f"- OBSERVATIONS_CREATED: {report['counts'].get('OBSERVATIONS_CREATED', 0)}",
        f"- PRODUCT_INBOX_ACTIONS: {report['counts'].get('PRODUCT_INBOX_ACTIONS', 0)}",
        "",
        "## Source Status",
        "",
    ]
    for src in report.get("source_status", []):
        lines.append(f"- {src['name']}: {src['status']} | role={src.get('evidence_role')} | requested_languages={','.join(src.get('requested_languages', [])) or '-'} | observed_languages={','.join(src.get('observed_languages', [])) or '-'} | collected={src.get('collected', 0)} accepted={src.get('accepted', 0)} filtered={src.get('filtered', 0)}")

    lines.extend(["", "## Filter Summary", ""])
    for reason, count in sorted(report.get("filter_summary", {}).items()):
        lines.append(f"- {reason}: {count}")

    reg = report.get("cluster_registry") or {}
    lines.extend(
        [
            "",
            "## Cluster Registry",
            "",
            f"- Existing entries: {reg.get('existing_entries', 0)}",
            f"- Matched existing: {reg.get('matched_existing', 0)}",
            f"- New entries: {reg.get('new_entries', 0)}",
            f"- Ambiguous matches: {len(reg.get('ambiguous_matches', []))}",
            f"- Excluded weak/noise: {reg.get('excluded_weak_or_noise', 0)}",
        ]
    )
    if reg.get("proposed_registry_path"):
        lines.append(f"- Proposed registry artifact: {reg.get('proposed_registry_path')}")

    sections = [
        "TOP CANDIDATES",
        "WORTH REVIEWING",
        "ALREADY COVERED",
        "ABBOTT LIMITATION / NOT LIBRECARE-SOLVABLE",
        "TEST / RESEARCH GAPS",
        "INSUFFICIENT EVIDENCE",
    ]

    bucket: dict[str, list[dict]] = {name: [] for name in sections}
    for item in report.get("top10", []):
        bucket[_report_section_for_item(item)].append(item)

    for section in sections:
        lines.extend(["", f"## {section}", ""])
        rows = bucket[section]
        if not rows:
            lines.append("Brak pozycji.")
            lines.append("")
            continue
        for item in rows:
            lines.append(f"TOP {item['rank']} — {item['score']}/100")
            lines.append("")
            lines.append("Problem:")
            lines.append(str(item.get("problem", "")))
            lines.append(f"Polish: {item.get('problem_statement_pl', '')}")
            lines.append(f"Evidence tier: {item.get('evidence_tier', '')}")
            lines.append(f"Concept/topic: {item.get('concept_id', '')} / {item.get('topic_id', '')}")
            lines.append("")
            lines.append("Persona:")
            lines.append(str(item.get("persona", "")))
            lines.append("")
            lines.append("Classification:")
            lines.append(str(item.get("classification", "")))
            lines.append("")
            lines.append("Evidence:")
            lines.append(
                f"{item.get('evidence_count', 0)} items / {len(item.get('source_identities', []))} independent sources"
            )
            lines.append("")
            lines.append("Current coverage:")
            lines.append(str(item.get("current_librecare_match", "")))
            lines.append("")
            lines.append("Solvability:")
            lines.append(str(item.get("solvability", "")))
            lines.append("")
            lines.append("Effort:")
            lines.append(str(item.get("effort", "")))
            lines.append("")
            lines.append("Confidence:")
            lines.append(str(item.get("confidence", "")))
            lines.append("")
            lines.append("Counterargument:")
            lines.append(str(item.get("counterargument", "")))
            lines.append("")
            lines.append("Sources:")
            for url in item.get("source_urls", []):
                lines.append(f"- {url}")
            lines.append("")

    lines.extend(["", "## WATCHLIST", ""])
    if not report.get("watchlist"):
        lines.append("Brak pozycji.")
    for item in report.get("watchlist", []):
        lines.extend([
            f"- **{item.get('cluster_id')}** — {item.get('score')}/100 — WEAK",
            f"  - EN: {item.get('problem_statement', '')}",
            f"  - PL: {item.get('problem_statement_pl', '')}",
            f"  - Promising: {item.get('why_promising', '')}",
            f"  - Still weak: {item.get('why_weak', '')}",
            f"  - Needed: {item.get('corroboration_needed', '')}",
        ])

    if report.get("errors"):
        lines.extend(["## Errors", ""])
        for err in report["errors"]:
            lines.append(f"- {err}")

    return "\n".join(lines).rstrip() + "\n"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="LibreCare Discovery Agent v1.5")
    parser.add_argument("--sources-file", default=str(SOURCES_CONFIG_DEFAULT))
    parser.add_argument("--query-packs-file", default=str(QUERY_PACKS_DEFAULT))
    parser.add_argument("--languages", default="pl,en,de,fr,es")
    parser.add_argument("--primary-languages", default="pl,en")
    parser.add_argument("--lookback-days", type=int, default=365)
    parser.add_argument("--max-items-per-source", type=int, default=30)
    parser.add_argument("--publish-top3", action="store_true")
    parser.add_argument("--repo-owner", default="")
    parser.add_argument("--repo-name", default="")
    parser.add_argument("--github-token", default="")
    parser.add_argument("--ai-mode", choices=["copilot", "heuristic"], default="heuristic")
    parser.add_argument("--ai-model", default="gpt-5.4-mini")
    parser.add_argument("--ai-response-file", default="")
    parser.add_argument("--timeout", type=float, default=12.0)
    parser.add_argument("--retries", type=int, default=2)
    parser.add_argument("--cache-max-age-seconds", type=int, default=21600)
    parser.add_argument("--output-file", default="")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    sources_file = Path(args.sources_file)
    ai_response_file = Path(args.ai_response_file) if args.ai_response_file else None

    if args.max_items_per_source < 1 or args.max_items_per_source > 30:
        print("ERROR: --max-items-per-source must be 1..30", file=sys.stderr)
        return 2
    languages = [x.strip().lower() for x in args.languages.split(",") if x.strip()]
    primary_languages = [x.strip().lower() for x in args.primary_languages.split(",") if x.strip()]
    if not languages or any(x not in SUPPORTED_LANGUAGES for x in languages) or any(x not in languages for x in primary_languages):
        print("ERROR: invalid --languages/--primary-languages", file=sys.stderr)
        return 2
    if args.lookback_days < 1 or args.lookback_days > 1825:
        print("ERROR: --lookback-days must be 1..1825", file=sys.stderr)
        return 2

    report = run_discovery(
        sources_file=sources_file,
        max_items_per_source=args.max_items_per_source,
        publish_top3_flag=bool(args.publish_top3),
        repo_owner=args.repo_owner,
        repo_name=args.repo_name,
        github_token=args.github_token,
        ai_mode=args.ai_mode,
        ai_model=args.ai_model,
        ai_response_file=ai_response_file,
        timeout=args.timeout,
        retries=args.retries,
        cache_max_age_seconds=max(1, int(args.cache_max_age_seconds)),
        query_packs_file=Path(args.query_packs_file),
        languages=languages,
        primary_languages=primary_languages,
        lookback_days=args.lookback_days,
    )

    if args.output_file:
        write_json(Path(args.output_file), report)

    print(f"DISCOVERY_RUN_ID: {report['run_id']}")
    print(f"DISCOVERY_STATUS: {report['status']}")
    print(f"DISCOVERY_REPORT_JSON: {report['json_report_path']}")
    print(f"DISCOVERY_REPORT_MD: {report['md_report_path']}")
    print(f"DISCOVERY_AI_CALLS: {report['counts']['AI_CALLS']}")
    if report.get("errors"):
        for err in report["errors"]:
            print(f"DISCOVERY_ERROR: {err}")

    return 0 if report["status"] != "FAILED" else 1


if __name__ == "__main__":
    raise SystemExit(main())
