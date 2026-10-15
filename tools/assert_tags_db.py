#!/usr/bin/env python
"""歌曲标签系统 T0 数据断言。

用法: python tools/assert_tags_db.py <aplayer.db 路径> [--expect-version 13]

把设备上的数据库拉回本地后运行；退出码非 0 表示断言失败。
"""
import sqlite3
import sys

EXPECT_TABLES = ["tags", "song_tags"]
EXPECT_INDICES = [
    "index_tags_tagId",
    "index_tags_category_sortOrder",
    "index_song_tags_tagId",
    "index_song_tags_canonicalId",
]
REGRESSION_TABLES = ["play_events", "album_colors", "report_overrides"]


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if not args:
        print("usage: assert_tags_db.py <db> [--expect-version N]")
        return 2
    path = args[0]
    expect_version = 13
    for a in sys.argv[1:]:
        if a.startswith("--expect-version"):
            expect_version = int(a.split("=", 1)[1]) if "=" in a else expect_version

    con = sqlite3.connect(path)
    cur = con.cursor()
    failures = []

    tables = {r[0] for r in cur.execute("SELECT name FROM sqlite_master WHERE type='table'")}
    indices = {r[0] for r in cur.execute("SELECT name FROM sqlite_master WHERE type='index'")}
    version = cur.execute("PRAGMA user_version").fetchone()[0]

    print("user_version =", version, "(expect", str(expect_version) + ")")
    if version != expect_version:
        failures.append("user_version %s != %s" % (version, expect_version))

    for t in EXPECT_TABLES:
        ok = t in tables
        print(("  [ok]   " if ok else "  [FAIL] ") + "table " + t)
        if not ok:
            failures.append("missing table " + t)

    for i in EXPECT_INDICES:
        ok = i in indices
        print(("  [ok]   " if ok else "  [FAIL] ") + "index " + i)
        if not ok:
            failures.append("missing index " + i)

    print("-- 回归：既有表行数（迁移前后应一致）--")
    for t in REGRESSION_TABLES:
        if t in tables:
            n = cur.execute("SELECT COUNT(*) FROM " + t).fetchone()[0]
            print("  %-18s %d rows" % (t, n))
        else:
            print("  %-18s MISSING" % t)
            failures.append("regression table missing: " + t)

    if "tags" in tables:
        n = cur.execute("SELECT COUNT(*) FROM tags").fetchone()[0]
        d = cur.execute("SELECT COUNT(DISTINCT tagId) FROM tags").fetchone()[0]
        print("-- tags: %d rows, %d distinct tagId --" % (n, d))
        if n != d:
            failures.append("tags has duplicate tagId (%d rows vs %d distinct)" % (n, d))
    if "song_tags" in tables:
        n = cur.execute("SELECT COUNT(*) FROM song_tags").fetchone()[0]
        print("-- song_tags: %d rows --" % n)

    con.close()
    if failures:
        print("\nRESULT: FAIL")
        for f in failures:
            print("  - " + f)
        return 1
    print("\nRESULT: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
