#!/usr/bin/env python3
"""Secret gate: findet echte Schluessel und ignoriert synthetische Testwerte.

Warum es ueberhaupt existiert: `app/src/test/` enthaelt bewusst Schluesselformen
wie "sk-ant-api03-AAAA..." — sie beweisen, dass der Maskierer greift, und sind
**keine** Geheimnisse. Ein Filter, der nur auf das Praefix schaut, meldet genau
diese Fixtures und blockiert das eigene Repository. Ein Filter, der zu streng
greift, laesst echte Schluessel durch. Beides ist hier gemessen worden, nicht
vermutet (siehe `progress/BUILD-STATE.md`, Sitzung 17).

Die Regel ist deshalb zweiteilig:

1. **Der Wert muss zufaellig sein.** Ein echter Schluessel ist eine
   Zufallsfolge. Ein Beispielwert in einem Test ist es nicht — er wiederholt ein
   Zeichen, enthaelt ein Platzhalterwort ("synthetic", "beispiel", "dummy") oder
   eine erkennbare Testfolge ("abcdef1234567890", "qwerty").
2. **PEM-Material zaehlt erst mit Inhalt.** Die Zeile
   `-----BEGIN RSA PRIVATE KEY-----` steht in vier Testdateien allein, um zu
   beweisen, dass sie geschwaerzt wird. Verdacht ist erst der base64-Rumpf
   ("MII...") in den Folgezeilen — und auch der nur, wenn er nicht selbst
   "synthetic" heisst.

Bewusst **kein** Geheimnis-Management und kein Netzwerkzugriff: Das Skript liest
lokale Dateien und meldet Fundstellen. Es speichert, sendet und schwaerzt nichts.

Aufruf:
    python3 tools/secret_gate.py            # whole repo, exit 1 on hit
    python3 tools/secret_gate.py <pfad>     # only a subtree
"""
import os
import re
import sys

# Mindestlaenge 20 NACH dem Praefix, nicht 40: Ein echter Anthropic-Schluessel
# hat rund 39 Zeichen nach "sk-ant-api03-". Ein Wert von 40 verlangte mehr, als
# ein echter Schluessel ueberhaupt hat — die Regel haette echte Schluessel
# durchgelassen und trotzdem die Fixtures gemeldet.
PREFIX = re.compile(
    r'sk-ant-api\d{2}-[A-Za-z0-9_-]{20,}'
    r'|ghp_[A-Za-z0-9]{20,}'
    r'|github_pat_[A-Za-z0-9_]{20,}'
    r'|AKIA[0-9A-Z]{16}'
)
PEM = re.compile(r'-----BEGIN (?:RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----')
MATERIAL = re.compile(r'MII[A-Za-z0-9+/]{20,}')

# Selbstbenennende Marken. Ein Testwert darf sich als Testwert zu erkennen geben;
# das ist der Unterschied zwischen einem Beispiel und einem echten Schluessel.
PLACEHOLDER = re.compile(
    r'Example|dummy|Platzhalter|NichtEcht|synthetic|fake|beispiel'
    r'|abcdef|1234567890|0123456789|qwerty|asdfgh|ABCDEFGHIJ'
    r'|AAAA|BBBB|CCCC|DDDD|EEEE|FFFF|GGGG|HHHH|IIII|JJJJ|KKKK|LLLL'
    r'|MMMM|NNNN|OOOO|PPPP|QQQQ|RRRR|SSSS|TTTT|UUUU|VVVV|WWWW|XXXX|YYYY|ZZZZ',
    re.I,
)

# Ein Zeichen, achtmal oder laenger wiederholt: immer synthetisch. Ein
# Zufallswert hat keine so langen Laeufer.
REPEATED = re.compile(r'^(.)\1{7,}$')

# `secret_gate_fixtures` steht hier bei demselben Namen wie sein eigener Inhalt:
# Die Dateien darin muessen vom normalen Repo-Lauf AUSGENOMMEN werden, weil sie
# absichtlich echte Formen tragen. Ohne diese Ausnahme meldet der Hauptlauf die
# eigene Gegenprobe und schlaegt bei jedem Push fehl — der Filter wuerde sich
# selbst entdecken. Der Aufruf auf den Fixtures-Pfad selbst ist davon NICHT
# betroffen: dort ist der gesuchte Pfad die Wurzel, nicht ein Unterordner.
SKIP_DIRS = {
    '.git', 'build', '.gradle', '.kotlin', 'node_modules', '.idea',
    'secret_gate_fixtures',
}


def value_of(token: str) -> str:
    """Der Teil nach dem Praefix, ohne Versionsziffern und ohne Anfuehrungszeichen."""
    for pre, suffix in (('sk-ant-api', r'\d{2}-'), ('github_pat_', ''), ('ghp_', ''), ('AKIA', '')):
        i = token.find(pre)
        if i >= 0:
            rest = re.sub('^' + suffix, '', token[i + len(pre):])
            return re.split(r'[^A-Za-z0-9_-]', rest)[0]
    return token


def scan_text(path: str, text: str, hits: list) -> None:
    lines = text.splitlines()
    for i, line in enumerate(lines):
        for match in PREFIX.finditer(line):
            wert = value_of(match.group(0))
            if REPEATED.match(wert) or PLACEHOLDER.search(wert):
                continue
            hits.append(f"{path}:{i + 1}: {line.strip()[:110]}")

        if not PEM.search(line):
            continue
        # Das Material steht in der Regel in den Folgezeilen, nicht in der BEGIN-Zeile.
        for j in range(i, min(i + 4, len(lines))):
            if MATERIAL.search(lines[j]) and not PLACEHOLDER.search(lines[j]):
                hits.append(f"{path}:{j + 1}: PEM-Schluesselmaterial")
                break


def main(root: str = '.') -> int:
    hits = []
    for directory, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for name in files:
            path = os.path.join(directory, name)
            try:
                with open(path, encoding='utf-8', errors='ignore') as handle:
                    scan_text(path, handle.read(), hits)
            except OSError:
                # Unlesbare Datei ist kein Geheimnisfund, aber auch kein Grund
                # fuer den Abbruch: der Rest des Baums wird weiter geprueft.
                pass

    for hit in hits:
        print("::error::Verdaechtiges Schluesselmuster:", hit)
    print(f"Geprueft: {root}. Treffer: {len(hits)}")
    return 1 if hits else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else '.'))
