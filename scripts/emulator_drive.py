#!/usr/bin/env python3
"""Drives the debug APK on a running emulator, the way a person would, and records what happens.

It reads the screen through uiautomator (the accessibility tree), taps labelled controls, takes screenshots and
copies the Detection lab report out of the screen. Everything lands in the output folder. A failed step is logged
and the run carries on, so one run shows as much as possible.
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

APK, OUT = sys.argv[1], sys.argv[2]
PKG = "com.zdmgold.autoclickernega.debug"
SERVICE = f"{PKG}/com.zdmgold.autoclickernega.service.NegaAccessibilityService"
MAIN = f"{PKG}/com.zdmgold.autoclickernega.MainActivity"
os.makedirs(OUT, exist_ok=True)
LOG = open(os.path.join(OUT, "results.txt"), "w", encoding="utf-8")
SHOTS = 0


def log(message):
    print(message, flush=True)
    LOG.write(message + "\n")
    LOG.flush()


def adb(*args, timeout=180):
    result = subprocess.run(["adb", *args], capture_output=True, text=True, timeout=timeout)
    return result.stdout + result.stderr


def shot(name):
    global SHOTS
    SHOTS += 1
    path = os.path.join(OUT, f"{SHOTS:02d}_{name}.png")
    data = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout
    with open(path, "wb") as handle:
        handle.write(data)


def dump():
    for flags in (["--windows"], []):
        out = adb("shell", "uiautomator", "dump", *flags, "/sdcard/ui.xml")
        if "dumped" in out.lower():
            break
    raw = adb("shell", "cat", "/sdcard/ui.xml")
    try:
        root = ET.fromstring(raw[raw.index("<"):])
    except (ValueError, ET.ParseError):
        return []
    nodes = []
    for node in root.iter("node"):
        numbers = re.findall(r"\d+", node.get("bounds", ""))
        if len(numbers) != 4:
            continue
        left, top, right, bottom = map(int, numbers)
        nodes.append(
            {
                "text": node.get("text", ""),
                "desc": node.get("content-desc", ""),
                "cx": (left + right) // 2,
                "cy": (top + bottom) // 2,
                "box": (left, top, right, bottom),
            }
        )
    return nodes


def matches(node, label, contains):
    for value in (node["text"], node["desc"]):
        if value and ((label.lower() in value.lower()) if contains else (value == label)):
            return True
    return False


def find(label, contains=False, nodes=None):
    for node in nodes if nodes is not None else dump():
        if matches(node, label, contains):
            return node
    return None


def wait_for(label, timeout=40, contains=False):
    end = time.time() + timeout
    while time.time() < end:
        node = find(label, contains)
        if node:
            return node
        time.sleep(1.5)
    return None


def scroll_down():
    adb("shell", "input", "swipe", "540", "1900", "540", "700", "400")
    time.sleep(1)


def scroll_up():
    adb("shell", "input", "swipe", "540", "700", "540", "1900", "400")
    time.sleep(1)


def tap(label, timeout=30, contains=False, scroll=False):
    end = time.time() + timeout
    scrolled = 0
    while time.time() < end:
        node = find(label, contains)
        if node:
            adb("shell", "input", "tap", str(node["cx"]), str(node["cy"]))
            return True
        if scroll and scrolled < 4:
            scroll_down()
            scrolled += 1
        else:
            time.sleep(1.5)
    return False


def step(name, action):
    try:
        outcome = action()
        log(f"{'PASS' if outcome else 'FAIL'}  {name}")
        return bool(outcome)
    except Exception as error:  # noqa: BLE001 - the harness must keep going
        log(f"ERROR {name}: {error!r}")
        return False


def lab_report():
    for node in dump():
        if node["text"].startswith("Nega Detection lab report"):
            return node["text"]
    return None


def lab_message(text, timeout):
    return wait_for(text, timeout=timeout, contains=True)


def to_top():
    for _ in range(4):
        scroll_up()


def main():
    log(adb("shell", "getprop", "ro.build.version.release").strip() + " / " + adb("shell", "wm", "size").strip())
    log(adb("install", "-r", "-g", APK).strip())
    adb("shell", "am", "force-stop", PKG)
    adb("logcat", "-c")
    adb("shell", "am", "start", "-n", MAIN)

    # Onboarding.
    step("welcome shows", lambda: wait_for("Continue", 40))
    shot("welcome")
    step("continue", lambda: tap("Continue"))
    step("disclosure shows", lambda: wait_for("Agree and continue"))
    shot("disclosure")
    step("agree", lambda: tap("Agree and continue"))
    step("enable step shows", lambda: wait_for("Open accessibility settings"))
    shot("enable_step")
    log(adb("shell", "settings", "put", "secure", "enabled_accessibility_services", SERVICE).strip())
    adb("shell", "settings", "put", "secure", "accessibility_enabled", "1")
    step("service becomes ready", lambda: wait_for("all set", 40, contains=True))
    shot("ready")
    step("go to home", lambda: tap("Go to Home"))
    step("home shows", lambda: wait_for("Open floating panel"))
    shot("home")

    # Detection lab: a static region (the title bar) is cut and found again at the same place.
    step("open lab", lambda: tap("Detection lab"))
    step("lab shows", lambda: wait_for("Capture reference image"))
    shot("lab")
    step("capture reference starts", lambda: tap("Capture reference image"))
    step("crop overlay appears", lambda: wait_for("Use this area", 30, contains=True))
    shot("crop_before_drag")
    adb("shell", "input", "swipe", "100", "70", "700", "250", "600")
    time.sleep(1)
    shot("crop_after_drag")
    step("confirm crop", lambda: tap("Use this area", contains=True))
    step("reference captured", lambda: lab_message("Reference captured", 30))
    shot("reference_done")
    step("capture frame", lambda: tap("Capture test frame", scroll=True))
    step("frame captured", lambda: lab_message("Test frame captured", 40))
    to_top()
    step("match", lambda: tap("Match at each confidence", scroll=True))
    step("match finished", lambda: lab_message("Match finished", 60))
    log("---- report after match ----")
    log(lab_report() or "report not found")
    shot("after_match")
    step("rate probe", lambda: tap("Screenshot rate probe", scroll=True))
    step("rate probe finished", lambda: lab_message("Rate probe finished", 240))
    log("---- report after rate probe ----")
    log(lab_report() or "report not found")
    to_top()
    step("repeat test", lambda: tap("Repeat test", contains=True, scroll=True))
    step("repeat test finished", lambda: lab_message("Repeat test finished", 600))
    log("---- report after repeat test ----")
    log(lab_report() or "report not found")
    shot("after_repeat")

    # The floating panel and a real run over another app (Android Settings).
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    adb("shell", "am", "start", "-a", "android.settings.SETTINGS")
    time.sleep(3)
    shot("settings_before")
    adb("shell", "am", "start", "-n", MAIN)
    step("home again", lambda: wait_for("Open floating panel", 30))
    step("open panel", lambda: tap("Open floating panel"))
    time.sleep(3)
    shot("panel_over_settings")
    log("panel nodes: " + repr([n["desc"] or n["text"] for n in dump() if n["desc"]][:30]))
    step("add tap marker", lambda: tap("Add tap"))
    time.sleep(2)
    shot("marker_added")
    step("start run", lambda: tap("Start"))
    time.sleep(1.5)
    shot("countdown")
    time.sleep(6)
    shot("running_after_taps")
    log("run nodes: " + repr([n["desc"] or n["text"] for n in dump() if n["desc"] or n["text"]][:40]))
    step("stop run", lambda: tap("Stop", timeout=10))
    time.sleep(1)
    shot("stopped")

    with open(os.path.join(OUT, "logcat.txt"), "w", encoding="utf-8") as handle:
        handle.write(adb("logcat", "-d", "-v", "time", "*:W"))
    crashes = [line for line in adb("logcat", "-d", "*:E").splitlines() if "FATAL" in line or PKG in line]
    log(f"fatal or app error lines: {len(crashes)}")
    for line in crashes[:30]:
        log(line)


if __name__ == "__main__":
    main()
