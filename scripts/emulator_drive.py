#!/usr/bin/env python3
"""Drives the debug APK on a running emulator and records what happens.

Before the accessibility service is switched on, the harness reads the screen with uiautomator and taps labelled
controls. uiautomator suspends every other accessibility service while it runs, so once Nega's service is on the
harness stops using it. From then on it sends debug broadcasts (see DebugControlReceiver), takes screenshots and
reads the results from logcat. Everything lands in the output folder. A failed step is logged and the run carries
on, so one run shows as much as possible.
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
RECEIVER = f"{PKG}/com.zdmgold.autoclickernega.lab.DebugControlReceiver"
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


def bcast(action, *extras):
    adb("shell", "am", "broadcast", "-n", RECEIVER, "-a", action, *extras)


def lab_lines():
    return adb("logcat", "-d", "-s", "NegaLab:I")


def wait_log(text, timeout, already=0):
    end = time.time() + timeout
    while time.time() < end:
        if lab_lines().count(text) > already:
            return True
        time.sleep(3)
    return False


def lab_command(label, action, done, timeout, *extras):
    """Sends a lab command and waits until logcat says it finished. No screen reader is used here."""
    before = lab_lines()
    already = before.count(done)
    bcast(action, *extras)
    finished = wait_log(done, timeout, already)
    log(f"{'PASS' if finished else 'FAIL'}  {label}")
    after = lab_lines()
    new = after[len(before):] if after.startswith(before) else after
    log("\n".join(line.split("NegaLab:")[-1].strip() for line in new.splitlines() if "NegaLab" in line))
    adb("logcat", "-d", "-b", "crash")
    return finished


TAP_SCRIPT = (
    '{"schema":1,"name":"emu tap","mode":"simple","screen":{"w":1080,"h":2400,"density":420},'
    '"run":{"loops":1,"globalTimerMs":0,"startDelayMs":0,"countdownSec":0},'
    '"steps":[{"type":"tap","x":540,"y":884,"holdMs":40,"intervalMs":100,"jitterPx":0}]}'
)


def heap_dump_during_repeat():
    """Starts the repeat test, takes a heap dump 28 seconds in, and prints what fills the heap."""
    before = lab_lines()
    already = before.count("done: Repeat test")
    bcast("nega.LAB_REPEAT")
    time.sleep(20)
    pid = adb("shell", "pidof", PKG).strip()
    log(f"app pid {pid}")
    adb("shell", "touch", "/data/local/tmp/h.hprof")
    adb("shell", "chmod", "666", "/data/local/tmp/h.hprof")
    adb("shell", "am", "dumpheap", pid, "/data/local/tmp/h.hprof")
    last = -1
    for _ in range(40):
        time.sleep(3)
        size = adb("shell", "stat", "-c", "%s", "/data/local/tmp/h.hprof").strip()
        if size == last and size not in ("", "0"):
            break
        last = size
    adb("pull", "/data/local/tmp/h.hprof", "/tmp/h.hprof")
    result = subprocess.run(
        ["python3", "scripts/hprof_referrers.py", "/tmp/h.hprof", "2592000", "8"], capture_output=True, text=True, timeout=1500
    )
    log("heap dump size " + str(last) + "\n" + result.stdout + result.stderr[-600:])
    finished = wait_log("done: Repeat test", 900, already)
    log(f"{'PASS' if finished else 'FAIL'}  repeat test")
    after = lab_lines()
    log("\n".join(line.split("NegaLab:")[-1].strip() for line in after[len(before):].splitlines() if "NegaLab" in line))


def background_repeat():
    """The same repeat test with the app in the background, in a fresh process, so no screen is redrawing."""
    log("---- experiment: repeat test with the app in the background ----")
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "settings", "put", "secure", "enabled_accessibility_services", SERVICE)
    adb("shell", "settings", "put", "secure", "accessibility_enabled", "1")
    time.sleep(10)
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    lab_command(
        "background reference", "nega.LAB_REFERENCE_BOX", "done: Capture reference region", 60,
        "--ei", "l", "100", "--ei", "t", "120", "--ei", "r", "700", "--ei", "b", "300",
    )
    lab_command("background frame", "nega.LAB_FRAME", "done: Capture test frame", 60)
    heap_dump_during_repeat()


def main():
    log(adb("shell", "getprop", "ro.build.version.release").strip() + " / " + adb("shell", "wm", "size").strip())
    log(adb("install", "-r", "-g", APK).strip())
    adb("shell", "am", "force-stop", PKG)
    adb("logcat", "-c")
    adb("shell", "am", "start", "-n", MAIN)

    # Screens that need no service: read with uiautomator.
    step("welcome shows", lambda: wait_for("Continue", 40))
    shot("welcome")
    step("continue", lambda: tap("Continue"))
    step("disclosure shows", lambda: wait_for("Agree and continue"))
    shot("disclosure")
    step("agree", lambda: tap("Agree and continue"))
    step("enable step shows", lambda: wait_for("Open accessibility settings"))
    shot("enable_step")
    step("later", lambda: tap("I'll do this later"))
    step("home shows", lambda: wait_for("Open floating panel"))
    shot("home_service_off")
    # The Simple editor: a new script with a tap and a wait, saved, then found on Home.
    step("new script row", lambda: tap("New script"))
    step("editor shows", lambda: wait_for("Add a step"))
    shot("editor_empty")
    step("add step 1", lambda: tap("Add a step"))
    step("kind sheet", lambda: wait_for("What should it do?"))
    shot("kind_sheet")
    step("pick tap", lambda: tap("Tap"))
    step("tap step listed", lambda: wait_for("Tap at", contains=True))
    step("add step 2", lambda: tap("Add a step"))
    step("kind sheet again", lambda: wait_for("What should it do?"))
    step("pick wait", lambda: tap("Wait"))
    step("wait step listed", lambda: wait_for("Wait 1", contains=True))
    shot("editor_two_steps")
    step("open the start-in sheet", lambda: tap("Start in"))
    step("start-in sheet shows", lambda: wait_for("Start now"))
    shot("start_in_sheet")
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(1)
    step("save", lambda: tap("Save"))
    step("saved banner", lambda: wait_for("Saved"))
    shot("editor_saved")
    step("back to home", lambda: tap("Back"))
    step("script listed on home", lambda: wait_for("2 steps", contains=True))
    shot("home_with_script")
    step("open lab", lambda: tap("Detection lab"))
    step("lab shows", lambda: wait_for("Capture reference image"))
    shot("lab_service_off")

    # Switch the service on. From here on uiautomator is not used, because it would suspend the service.
    log(adb("shell", "settings", "put", "secure", "enabled_accessibility_services", SERVICE).strip())
    adb("shell", "settings", "put", "secure", "accessibility_enabled", "1")
    time.sleep(8)
    bound = [line.strip() for line in adb("shell", "dumpsys", "accessibility").splitlines() if "zdmgold" in line]
    log("accessibility dump lines mentioning the app: " + repr(bound[:6]))
    shot("lab_service_on")

    # Detection lab, headless. The reference is the title bar, which does not move.
    lab_command(
        "reference region", "nega.LAB_REFERENCE_BOX", "done: Capture reference region", 60,
        "--ei", "l", "100", "--ei", "t", "120", "--ei", "r", "700", "--ei", "b", "300",
    )
    lab_command("test frame", "nega.LAB_FRAME", "done: Capture test frame", 60)
    lab_command("match", "nega.LAB_MATCH", "done: Match", 90)
    lab_command("leak probe 1", "nega.LAB_LEAK", "done: Leak probe", 120)
    shot("lab_after_match")
    lab_command("rate probe", "nega.LAB_RATE", "done: Capture rate probe", 300)
    lab_command("repeat test", "nega.LAB_REPEAT", "done: Repeat test", 900)
    lab_command("leak probe 2", "nega.LAB_LEAK", "done: Leak probe", 120)
    shot("lab_after_repeat")

    # The quick test on a screen that stays still (Android Settings): no crop, the lab picks its own patch.
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    adb("shell", "am", "start", "-a", "android.settings.SETTINGS")
    time.sleep(4)
    lab_command("quick test on Settings", "nega.LAB_QUICK", "done: Quick test", 300)

    # The floating panel and a real run over another app (Android Settings).
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    adb("shell", "am", "start", "-a", "android.settings.SETTINGS")
    time.sleep(4)
    shot("settings_before")
    bcast("nega.PANEL")
    time.sleep(3)
    log("panel: " + "; ".join(line for line in lab_lines().splitlines() if "panel:" in line)[-300:])
    shot("panel_over_settings")
    bcast("nega.RUN", "--es", "json", "'" + TAP_SCRIPT + "'")
    time.sleep(4)
    log("run: " + "; ".join(line for line in lab_lines().splitlines() if "run:" in line)[-300:])
    shot("after_run_tap")

    # The crop overlay: opened by the lab, dragged over part of Home, confirmed with the check button at the bottom
    # right (56 dp wide, 24 dp from the right edge, 48 dp from the bottom, at 2.625 px per dp), then the same
    # screen is captured and matched. The reference must be found where it was cut.
    adb("shell", "am", "start", "-n", MAIN)
    time.sleep(4)
    shot("home_before_crop")
    before = lab_lines()
    bcast("nega.LAB_CROP")
    time.sleep(4)
    shot("crop_overlay")
    adb("shell", "input", "swipe", "150", "700", "650", "1100", "600")
    time.sleep(1)
    shot("crop_after_drag")
    adb("shell", "input", "tap", "943", "2200")
    time.sleep(3)
    shot("crop_after_confirm")
    finished = wait_log("done: Capture reference |", 30, before.count("done: Capture reference |"))
    log(f"{'PASS' if finished else 'FAIL'}  crop confirmed")
    after = lab_lines()
    log("\n".join(line.split("NegaLab:")[-1].strip() for line in after[len(before):].splitlines() if "NegaLab" in line))
    lab_command("test frame after crop", "nega.LAB_FRAME", "done: Capture test frame", 60)
    lab_command("match after crop", "nega.LAB_MATCH", "done: Match", 90)

    with open(os.path.join(OUT, "logcat.txt"), "w", encoding="utf-8") as handle:
        handle.write(adb("logcat", "-d", "-b", "all", "-v", "time", "*:W"))
    crashes = [line for line in adb("logcat", "-d", "-b", "all", "*:E").splitlines() if "FATAL" in line or PKG in line or "Lab" in line]
    log(f"fatal or app error lines: {len(crashes)}")
    for line in crashes[:30]:
        log(line)


if __name__ == "__main__":
    try:
        main()
    except Exception:  # noqa: BLE001 - keep the error in the published results
        import traceback

        log("HARNESS ERROR\n" + traceback.format_exc())
        sys.exit(1)
