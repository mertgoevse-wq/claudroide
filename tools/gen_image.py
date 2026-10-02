#!/usr/bin/env python3
"""Generate an image through the local OmniRoute proxy.

Exists because `claude-media-bridge` routes its `google` provider through
direct Google OAuth only, which is not configured on this device, while the
OmniRoute proxy on localhost *is* reachable and exposes the Gemini image
models. Tries the OpenAI-compatible image endpoint first, then the chat
endpoint with an image modality, because proxies differ in which they expose.

The API key is read from ~/.config/mll/providers/omniroute.env and is never
printed, logged, or written into any output file.
"""
import base64
import json
import os
import pathlib
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("OMNIROUTE_URL", "http://localhost:20128")
ENV_FILE = pathlib.Path.home() / ".config/mll/providers/omniroute.env"


def api_key() -> str:
    for line in ENV_FILE.read_text().splitlines():
        if line.startswith("OMNIROUTE_API_KEY="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("no OMNIROUTE_API_KEY in env file")


def post(path: str, payload: dict, key: str, timeout: int = 300) -> dict:
    req = urllib.request.Request(
        f"{BASE}{path}",
        data=json.dumps(payload).encode(),
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read())


def harvest(obj) -> list[bytes]:
    """Pull base64 image payloads out of whatever shape came back."""
    found = []

    def walk(node):
        if isinstance(node, dict):
            for k, v in node.items():
                if k in ("b64_json", "data", "b64", "image_base64", "bytesBase64Encoded") \
                        and isinstance(v, str) and len(v) > 2048:
                    found.append(v)
                elif k == "url" and isinstance(v, str) and v.startswith("data:image"):
                    found.append(v.split(",", 1)[1])
                else:
                    walk(v)
        elif isinstance(node, list):
            for v in node:
                walk(v)
        elif isinstance(node, str) and node.startswith("data:image"):
            found.append(node.split(",", 1)[1])

    walk(obj)
    out = []
    for b in found:
        try:
            out.append(base64.b64decode(b, validate=False))
        except Exception:
            pass
    return out


def generate(prompt: str, model: str, out: pathlib.Path, size: str = "1024x1024") -> pathlib.Path:
    key = api_key()
    attempts = [
        ("/v1/images/generations",
         {"model": model, "prompt": prompt, "n": 1, "size": size, "response_format": "b64_json"}),
        ("/v1/chat/completions",
         {"model": model, "modalities": ["image", "text"],
          "messages": [{"role": "user", "content": prompt}]}),
    ]
    errors = []
    for path, payload in attempts:
        try:
            body = post(path, payload, key)
        except urllib.error.HTTPError as e:
            detail = e.read()[:300].decode(errors="replace")
            errors.append(f"{path} -> HTTP {e.code}: {detail}")
            continue
        except Exception as e:
            errors.append(f"{path} -> {type(e).__name__}: {e}")
            continue
        images = harvest(body)
        if images:
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(images[0])
            print(f"OK  {path}  model={model}  -> {out}  ({len(images[0])} bytes)")
            return out
        errors.append(f"{path} -> 200 but no image payload; keys={list(body)[:6]}")
    raise SystemExit("all attempts failed:\n  " + "\n  ".join(errors))


if __name__ == "__main__":
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--prompt-file", required=True)
    ap.add_argument("--model", default="kc/google/gemini-3-pro-image")
    ap.add_argument("--out", required=True)
    ap.add_argument("--size", default="1024x1024")
    a = ap.parse_args()
    generate(pathlib.Path(a.prompt_file).read_text(), a.model,
             pathlib.Path(a.out), a.size)
