#!/usr/bin/env python
"""查询 song_tags（验证用）。用法: python tools/query_song_tags.py <db> [tagId]"""
import sqlite3
import sys

db = sys.argv[1]
tag = sys.argv[2] if len(sys.argv) > 2 else None
con = sqlite3.connect(db)
q = "select canonicalId, tagId, score, source, deleted from song_tags"
args = ()
if tag:
    q += " where tagId = ?"
    args = (tag,)
q += " order by tagId"
rows = con.execute(q, args).fetchall()
if not rows:
    print("   (none)")
for r in rows:
    cid = (r[0] or "")[:20]
    print("   %-20s %-14s score=%-6s src=%-6s deleted=%s" % (cid, r[1], r[2], r[3], r[4]))
