#!/usr/bin/env python
"""解析 uiautomator dump，打印可点击节点的文本与中心坐标（供 adb input tap 使用）。

用法: python tools/ui_nodes.py <dump.xml> [--filter 关键字]
"""
import re
import sys


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: ui_nodes.py <dump.xml> [keyword]")
        return 2
    path = sys.argv[1]
    keyword = sys.argv[2] if len(sys.argv) > 2 else None
    s = open(path, "rb").read().decode("utf-8", "replace")
    nodes = re.findall(r"<node[^>]*/?>", s)
    for n in nodes:
        text = re.search(r'text="([^"]*)"', n)
        desc = re.search(r'content-desc="([^"]*)"', n)
        bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        clickable = 'clickable="true"' in n
        if not bounds:
            continue
        t = (text.group(1) if text else "").strip()
        d = (desc.group(1) if desc else "").strip()
        label = t or d
        if not label:
            continue
        if keyword and keyword not in label:
            continue
        x1, y1, x2, y2 = map(int, bounds.groups())
        cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
        print("%-28s clickable=%-5s center=(%4d,%4d) bounds=[%d,%d][%d,%d]" % (label[:28], clickable, cx, cy, x1, y1, x2, y2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
